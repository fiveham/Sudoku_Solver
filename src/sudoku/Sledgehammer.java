package sudoku;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import common.TestIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import sudoku.Puzzle.RegionSpecies;

/**
 * <p>The sledgehammer technique for solving sudoku puzzles is defined 
 * at http://onigame.livejournal.com/18580.html and at 
 * http://onigame.livejournal.com/20626.html and uses one collection 
 * of specific statements of the rules of a sudoku target against 
 * another collection of such statements to determine that certain 
 * claims about the sudoku target are false.</p>
 * 
 * <p>The sledgehammer technique relies on the fact that the target can 
 * be interpreted as a collection of {@link Claim truth-claims} about the values of the 
 * cells ("Cell x,y, has value z.") and {@link Rule groupings of those truth-claims} of 
 * which exactly one claim from a grouping is true and all the rest are 
 * false.</p>
 * 
 * <p>These groupings of claims are pedantic and precise statements 
 * of the rules that specify what makes a solution to a sudoku target valid. 
 * For example, the rule that any row contains each value exactly once 
 * pedantically expands into 81 statements in a 9x9 target: 9 statements 
 * about an individual row, each of which is actually 9 statements 
 * specifying a particular value: "Row y has value z in exactly one cell." 
 * The rule that any cell contains exactly one value similarly becomes 81 
 * pedantic statements: "For cell x,y, only one z value is correct."</p>
 * 
 * <p>The sledgehammer technique generalizes a great number of analysis 
 * techniques including
 * <ul><li>cell death</li>
 * <li>organ failure</li>
 * <li>naked pairs/triples/etc.</li>
 * <li>x-wing, swordfish, jellyfish, etc.</li>
 * <li>xy-wing, xyz-wing, etc.</li>
 * </ul></p>
 * 
 * <p>This is effected by modeling those situation as the collapse of several 
 * recipient rules onto several source rules so that all the claims belonging 
 * to the recipient rules but not belonging to any of the source rules are 
 * known to be false. No matter which (viable) set of claims among the source 
 * rules is the source rules' set of true claims, all the recipient rules' 
 * claims that aren't also source claims must be false.</p>
 * 
 * @author fiveham
 *
 */
public class Sledgehammer extends Technique {
	
	public static final Collector<Rule,?,Map<Integer,List<Rule>>> MAP_RULES_BY_SIZE = Collectors.toMap(
			Sledgehammer::appropriateSledgehammerSize, 
			(Rule rule)->{List<Rule> result = new ArrayList<>(1); result.add(rule); return result;}, 
			(list1,list2) -> {list1.addAll(list2); return list1;});
	
	/**
	 * <p>The minimum number ({@value}) of Rules in a source combo relevant to Sledgehammer analysis. 
	 * Because Rules {@link Rule#verifyFinalState() automatically detect} when they need to 
	 * collapse and also automatically perform the resulting collapse, single-source 
	 * Sledgehammers won't be available in the target. This leaves two-Rule sources as 
	 * the smallest source combinations.</p>
	 */
	private static final int MIN_SRC_COMBO_SIZE = 2;
	
	private static final int MIN_SLEDGEHAMMER_SIZE = 2;
	
	private final VisibleCache visibleCache;
	private final Collection<Rule> distinctRules;
	private final Map<Integer,List<Rule>> distinctRulesBySledgehammerSize;
	
	/**
	 * <p>Constructs a SledgeHammer2 that works to solve the specified Puzzle.</p>
	 * @param target the Puzzle that this SledgeHammer2 tries to solve.
	 */
	public Sledgehammer(Sudoku puzzle) {
		super(puzzle);
		
		this.visibleCache = new VisibleCache();
		this.distinctRules = distinctRules(target);
		this.distinctRulesBySledgehammerSize = distinctRules.stream().collect(MAP_RULES_BY_SIZE);
	}
	
	private static int appropriateSledgehammerSize(Rule r){
		
		//Number of Claims on this Rule.
		//Maximum number of recipient rules attachable to this Rule r: 
		//Inclusive upper bound on the minimum size of Sledgehammer for 
		//which r can be a source.
		int upperBound = r.size();
		
		//number of Rules visible to r if none of r's Claims share any 
		//Rule other than r.
		int rulesOtherThanR = Claim.INIT_OWNER_COUNT - 1;
		int noLoopVisibleRuleCount = r.size() * rulesOtherThanR;
		
		Set<Rule> visible = r.visibleRules();
		int actualVisibleCount = visible.size();
		
		if(noLoopVisibleRuleCount == actualVisibleCount){
			return upperBound;
		} else if(noLoopVisibleRuleCount > actualVisibleCount){
			for(List<Rule> visibleCombo : new ComboGen<>(visible, 2, r.size())){ //MAGIC
				if(sideEffectUnion(visibleCombo, false).containsAll(r)){
					return visibleCombo.size();
				}
			}
			throw new IllegalStateException("Could not determine true min Sledgehammer size for "+r);
		} else{
			throw new IllegalStateException("noLoopVisibleRuleCount ("+noLoopVisibleRuleCount+") < actualVisibleCount ("+actualVisibleCount+")");
		}
	}
	
	/**
	 * <p>Iterates over all the possible pairs of source-combination and 
	 * recipient-combination, checks each pair for validity as a sledgehammer 
	 * solution scenario, and, if the scenario is valide, resolves it and then 
	 * returns a SolutionEvent detailing the event and any subsequent automatic 
	 * resolution events.</p>
	 * @return a SolutionEvent detailing the sledgehammer solution event that 
	 * this method found and resolved and detailing any subsequent automatic 
	 * resolution events as {@link common.time.Time#children() children} of 
	 * the sledgehammer event, or null if this method made no changes to its 
	 * <tt>target</tt>
	 */
	@Override
	protected SolutionEvent process(){
		
		Collection<Rule> distinctRulesAtSize = new ArrayList<>();
		for(int i=0; i<MIN_SRC_COMBO_SIZE; ++i){
			if(distinctRulesBySledgehammerSize.containsKey(i)){
				distinctRulesAtSize.addAll(distinctRulesBySledgehammerSize.get(i));
			}
		}
		for(int size = MIN_SRC_COMBO_SIZE; size<=distinctRules.size()/2; ++size){
			if(distinctRulesBySledgehammerSize.containsKey(size)){
				distinctRulesAtSize.addAll(distinctRulesBySledgehammerSize.get(size));
			}
			
			//For each disjoint, closely connected source combo
			for(List<Rule> srcCombo : new ComboGen<>(distinctRulesAtSize, size, size)){
				if(sourceComboMostlyValid(srcCombo)){
					
					//For each recipient combo derivable from that source combo
					for(List<Fact> recipientCombo : recipientCombinations(srcCombo, distinctRulesAtSize)){
						
						//If the source and recipient combos make a valid sledgehammer scenario
						Set<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipientCombo);
						if(claimsToSetFalse != null && !claimsToSetFalse.isEmpty()){
							return resolve(claimsToSetFalse);
						}
					}
				}
			}
		}
		
		return null;
	}
	
	private static Collection<Rule> distinctRules(Sudoku target){
		class RuleWrap{
			private final Rule wrapped;
			RuleWrap(Rule rule){
				this.wrapped = rule;
			}
			
			@Override
			public int hashCode(){
				return wrapped.superHashCode();
			}
			
			@Override
			public boolean equals(Object o){
				if(o instanceof RuleWrap){
					RuleWrap rw = (RuleWrap) o;
					return wrapped.superEquals(rw.wrapped);
				}
				return false;
			}
		}
		return target.factStream()
				.map((f) -> (Rule)f)
				.map(RuleWrap::new)
				.distinct()
				.map((rw)->rw.wrapped)
				.collect(Collectors.toList());
	}
	
	private class VisibleCache extends HashMap<Rule,Set<Rule>>{
		@Override
		public Set<Rule> get(Object o){
			if(containsKey(o)){
				return super.get(o);
			} else{
				if(o instanceof Rule){
					Rule r = (Rule) o;
					
					Set<Rule> result = r.visibleRules();
					result.retainAll(distinctRules);
					super.put(r, result);
					
					return result;
				} else{
					throw new IllegalArgumentException("specified key must be a Rule");
				}
			}
		}
	}
	
	/**
	 * <p>Returns true if none of the Rules in <tt>ruleList</tt> intersect any 
	 * of the other Rules in the list and every Rule in <tt>ruleList</tt> shares at least 
	 * one {@link Rule#visibleRules() visible Rule} in common with at least one 
	 * other Rule in the <tt>ruleList</tt>.</p>
	 * 
	 * <p>This method partially checks the validity of a combination of Rules that 
	 * might be a source combination for a sledgehammer solution scenario. The Rules 
	 * of a sledgehammer source combo must be disjoint from one another; as such, 
	 * no Rule from a valid source combo will be visible from any other Rule in the 
	 * same valid source combo, because a Rule's visible Rules are the Rules that that 
	 * Rule intersects. All the Rules of a valid source combo must also form a single 
	 * connected component, under the stipulation that Rules share an edge if they 
	 * share a visible Rule in common.</p>
	 * 
	 * <p>One aspect of the validity of a sledgehammer source combo that is left out 
	 * of this method's analysis is that there must exist space (positions) for a valid 
	 * recipient combo among the Rules visible to the Rules of a valid source combo. 
	 * That analysis is outsourced to {@link possibleRecipients()} and the stipulation 
	 * that a recipient combo must have as many elements as its source combo.</p>
	 * 
	 * @param ruleList a candidate source combo for a sledgehammer solution scenario 
	 * being tested for validity
	 * @return true if none of the Rules in <tt>ruleList</tt> intersect any 
	 * of the other Rules in the list and every Rule in <tt>ruleList</tt> shares at least 
	 * one {@link Rule#visibleRules() visible Rule} in common with at least one 
	 * other Rule in the <tt>ruleList</tt>, false otherwise
	 */
	private boolean sourceComboMostlyValid(List<Rule> ruleList){ //TODO make this method return the collection of recipient Rules for this src combo
		List<Rule> internalList = new ArrayList<>(ruleList);
		
		int oldSize = internalList.size();
		Set<Rule> allVisibleRules = visibleCache.get(internalList.remove(internalList.size()-1));
		
		while(internalList.size() != oldSize){
			oldSize = internalList.size();
			
			for(Iterator<Rule> iter = internalList.iterator(); iter.hasNext();){
				Rule r = iter.next();
				
				Set<Rule> visibleToCurrentRule = visibleCache.get(r);
				
				if(!Collections.disjoint(visibleToCurrentRule, allVisibleRules)){
					allVisibleRules.addAll(visibleToCurrentRule);
					iter.remove();
				}
			}
		}
		
		return oldSize == 0 && Collections.disjoint(allVisibleRules, ruleList);
	}
	
	/**
	 * <p>Returns a set of all the Rules that intersect at least two of 
	 * the Rules in <tt>sources</tt>.</p> 
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @param distinctRulesAtSize a collection of Rules that are 
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in <tt>sources</tt>, excluding the Rules in <tt>sources</tt>.
	 */
	private ComboGen<Fact> recipientCombinations(List<Rule> sources, Collection<Rule> distinctRulesAtSize){
		List<Set<Rule>> visibleRules = sources.stream().collect(Collector.of(
				ArrayList::new, 
				(List<Set<Rule>> a, Rule source) -> a.add(visibleCache.get(source)), 
				(left,right) -> {left.addAll(right); return left;}));
		
		Map<Rule,Integer> countSet = countingUnion(visibleRules);
		
		Set<Fact> result = countSet.keySet().stream().filter((r) -> countSet.get(r) > 1).collect(Collectors.toSet());
		result.retainAll(distinctRulesAtSize);
		return new ComboGen<>(result, sources.size(), sources.size());
	}
	
	/**
	 * <p>If the specified lists of source and recipient Rules together constitute a valid 
	 * Sledgehammer solution scenario, then a set of the Claims (from the recipients) 
	 * to be set false is returned, otherwise <tt>null</tt> is returned.</p>
	 * 
	 * <p><tt>reds</tt> and <tt>greens</tt> are a valid Sledgehammer solution scenario if <ul>
	 * <li>The union of all the recipient Rules contains as a proper subset the union of all 
	 * the source Rules.</li>
	 * <li>None of the source Rules shares any Claims with any other source Rule (accounted 
	 * for by a call to {@link #sideEffectUnion(Collection,boolean) sideEffectUnion(reds,true)} 
	 * in <tt>process()</tt> before this method is called)</li>
	 * <li>The number of recipient Rules equals the number of source Rules</li>
	 * <li>The source and recipient Rules constitute a connected subgraph of the target, 
	 * such that a Rule is a vertex and such that two such vertices are connected if their 
	 * Rules share at least one Claim.</li>
	 * </ul></p>
	 * 
	 * @param srcRules source Rules, for which every {@link #isPossibleSolution(List) possible} 
	 * solution must falsify all the non-<tt>reds</tt> Claims in <tt>greens</tt> in 
	 * order for the specified Sledgehammer solution scenario constituted by those two parameter 
	 * to be valid
	 * 
	 * @param recipRules recipient Rules. Every Claim among these that is not also accounted for 
	 * among <tt>reds</tt> must be guaranteed false in every {@link #isPossibleSolution(List) possible}
	 * solution-state of the <tt>reds</tt>
	 * 
	 * @param srcClaims the union of all the elements of <tt>reds</tt>, specified as a 
	 * parameter for convenience since it is needed here, was previously generated in 
	 * the {@link #process() calling context}, and remains unchanged since then
	 * 
	 * @return null if the Sledgehammer solution scenario defined by <tt>reds</tt> and 
	 * <tt>greens</tt> is not valid, a set of the Claims to be set false if the specified 
	 * solution scenario is valid
	 */
	private static Set<Claim> sledgehammerValidityCheck(List<? extends Fact> srcRules, List<? extends Fact> recipRules){
		
		ToolSet<Claim> srcClaims = sideEffectUnion(srcRules,true);
		
		// make sure that recip rules collectively subsume src rules
		ToolSet<Claim> recipClaims = sideEffectUnion(recipRules,false);
		if( !recipClaims.hasProperSubset(srcClaims) ){
			return null;
		}
		
		recipClaims.removeAll(srcClaims); //TODO accumulate the union then removeAll of the source union in a separate step, depending on which set is larger
		
		return allRecipFalsifiedByAnySolution(recipClaims, srcRules) ? recipClaims : null;
	}
	
	private static boolean allRecipFalsifiedByAnySolution(Set<Claim> recipClaims, List<? extends Fact> srcRules){
		/*Set<Claim> visibleToRecipClaims = recipClaims.stream().collect(Collector.of(
				HashSet::new, 
				(HashSet<Claim> a, Claim t) -> a.addAll(t.visibleClaims()), 
				(HashSet<Claim> a1, HashSet<Claim> a2) -> {a1.addAll(a2); return a1;}, 
				Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH));*/
		boolean hasSolutionState = false;
		for(List<Claim> solution : new TestIterator<List<Claim>>(new NCuboid<Claim>(srcRules).iterator(),POSSIBLE_SOLUTION).iterable()){
			hasSolutionState = true;
			for(Claim recipClaim : recipClaims){
				if( !solution.stream().anyMatch((solClaim) -> solClaim.intersects(recipClaim)) ){
					return false;
				}
			}
		}
		return hasSolutionState;
	}
	
	/**
	 * <p>Returns false if the specified solution-state is impossible, true 
	 * otherwise. A solution-state is impossible if any two specified Claims 
	 * share at least one Rule in common.</p>
	 */
	public static final Predicate<List<Claim>> POSSIBLE_SOLUTION = (solutionState) -> sideEffectUnion(solutionState,true) != null;
	
	
	/**
	 * <p>Sets all the Claims in <tt>claimsToSetFalse</tt> false.</p>
	 * @param claimsToSetFalse the Claims to be set false
	 * @return true if any of the <tt>claimsToSetFalse</tt> were set 
	 * from possible to false, false otherwise.
	 */
	private SolutionEvent resolve(Collection<Claim> claimsToSetFalse){
		SolutionEvent time = new SolveEventSledgehammer(claimsToSetFalse);
		claimsToSetFalse.stream().filter(Claim.CLAIM_IS_BEING_SET_FALSE.negate()).forEach((c)->c.setFalse(time));
		return time;
	}
	
	/*
	 * TODO rebuild sledgehammer-search so it starts from a seed Rule and grows out from there.
	 */
	protected SolutionEvent componentGrowthProcessing(){
		
		for(int size = 2; size <= 2; ++size){//for(int size = MIN_SLEDGEHAMMER_SIZE; size <= target.size()/2; size++){
			
			//for each possible seed src Rule, 
			//get its visibles' visibles
			//If the size is 2, then just iterate over all of those vis-visibles, 
			//and for each one construct a pair containing it and the seed Rule 
			//and then check normally to see if there's a recipient combination for 
			//that source combo pair. If there's not, move on to the next source combo 
			//pair for the current seed Rule. If you've exhausted all the vis-visibles 
			//for the current seed, move on to the next seed, and its vis-visibles.
			//Make sure to add the old seed to a collection of Rules that are banned 
			//from use as sources for the current size.
			//If the size is 3, then iterate over the pairs described in the previous 
			//paragraph, and for each one of them, get a set of Rules that's the 
			//intersection of the visibleRules of the first Rule of the pair and 
			//the visibleRules of the second Rule of the pair, then iterate over the 
			//Rule elements of that collection so as to constitute every possible 
			//triplet.
			
			
			/*for(Rule seed : distinctRules){
				Pair<Collection<Rule>,Collection<Rule>> sledgehammer = seekSledgehammer(seed, size);
				if(sledgehammer != null){
					Set<Claim> falsified = sideEffectUnion(sledgehammer.getB(),false);
					falsified.removeAll(sideEffectUnion(sledgehammer.getA(),false));
					if(!falsified.isEmpty()){
						return resolve(falsified);
					}
				}
			}*/
		}
		
		return null;
	}
	
	/**
	 * <p>Grows a sledgehammer solution scenario around <tt>seed</tt>, assuming 
	 * <tt>seed</tt> is a source Rule. If no such scenario can be built, returns 
	 * null. If a scenario is found, returns a Pair whose first element names 
	 * all the source Rules of the sledgehammer solution scenario and whose 
	 * second element names all the recipient Rules of the sledgehammer solution 
	 * scenario.</p>
	 * @param seed
	 * @param size
	 * @return
	 */
	private Pair<Collection<Rule>,Collection<Rule>> seekSledgehammer(Rule seed, int size){
		return null; //TODO stub
	}
	
	/**
	 * <p>Looks for sledgehammer solution scenarios exclusively as relations between 
	 * Rules of different {@link Puzzle.RegionSpecies types}. This allows this implementation 
	 * of the sledgehammer technique to simultaneously generalize X-Wing, Swordfish, 
	 * Jellyfish, naked pairs, naked triples, naked quadruples, hidden pairs, hidden 
	 * triples, hidden quadruples, etc. without the overheard incurred by earlier 
	 * implementations of this class which looked at all combinations of all types of 
	 * Rules.</p>
	 * 
	 * <p>This is a heuristic technique, meant to be used before ColorChain but requiring 
	 * a complete form of Sledgehammer that does explore all possible valid source combos 
	 * to be included in the Solver's techniques following ColorChain.</p>
	 * @return
	 */
	protected SolutionEvent regionSpeciesPairProcessing(){
		
		for(TypePair types : TypePair.values()){
			for(Pair<Collection<Rule>,Collection<Rule>> pack : types.packs(target)){
				Collection<Rule> a = pack.getA();
				Collection<Rule> b = pack.getB();
				
				for(int size = MIN_SLEDGEHAMMER_SIZE; size<Math.min(a.size(), b.size()); size++){
					for(List<Rule> comboA : new ComboGen<>(a, size,size)){
						for(List<Rule> comboB : new ComboGen<>(b, size,size)){
							Set<Claim> falsify = falsifiedClaims(comboA, comboB);
							
							if(falsify != null){
								return resolve(falsify);
							}
						}
					}
				}
			}
		}
		
		return null;
	}
	
	private static Set<Claim> falsifiedClaims(Collection<Rule> comboA, Collection<Rule> comboB){
		ToolSet<Claim> unionA = sideEffectUnion(comboA, false);
		Set<Claim> unionB = sideEffectUnion(comboB, false);
		Set<Claim> inters = unionA.intersection(unionB);
		
		unionA.removeAll(inters);
		unionB.removeAll(inters);
		
		if(unionA.isEmpty() != unionB.isEmpty()){
			return unionA.isEmpty() ? unionB : unionA;
		} else{
			return null;
		}
	}
	
	public static int boxIndex(Rule r){
		int mag = r.stream().findFirst().get().getPuzzle().magnitude();
		return boxY(r)*mag + boxX(r);
	}
	
	public static int boxY(Rule r){
		Claim c = r.stream().findFirst().get();
		int y = c.getY();
		return y/c.getPuzzle().magnitude();
	}
	
	public static int boxX(Rule r){
		Claim c = r.stream().findFirst().get();
		int x = c.getX();
		return x/c.getPuzzle().magnitude();
	}
	
	public static final Function<Sudoku,List<Integer>> dimSource = (s) -> IntStream.range(0,s.sideLength()).mapToObj((i)->(Integer)i).collect(Collectors.toList());
	
	public static final Predicate<Rule> IS_CELL = (r) -> r.getType() == RegionSpecies.CELL;
	public static final Predicate<Rule> IS_BOX = (r) -> r.getType() == RegionSpecies.BOX;
	public static final Predicate<Rule> IS_COLUMN = (r) -> r.getType() == RegionSpecies.COLUMN;
	public static final Predicate<Rule> IS_ROW = (r) -> r.getType() == RegionSpecies.ROW;
	
	private static enum TypePair{
		CELL_COL((p)->new NCuboid<>(dimSource.apply(p)), 
				IS_CELL, IS_COLUMN,
				(r,l) -> r.stream().findFirst().get().getX()==l.get(0)), 
		CELL_ROW((p)->new NCuboid<>(dimSource.apply(p)), 
				IS_CELL, IS_ROW, 
				(r,l) -> r.stream().findFirst().get().getY()==l.get(0)), 
		CELL_BOX((p)->new NCuboid<>(dimSource.apply(p)), 
				IS_CELL, IS_BOX, 
				(r,l) -> l.get(0).equals(boxIndex(r))), 
		BOX_ROW ((p)->new NCuboid<>(dimSource.apply(p), IntStream.range(0,p.magnitude()).mapToObj((i)->(Integer)i).collect(Collectors.toList())), 
				IS_BOX, IS_ROW, 
				(r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxY(r)), 
		BOX_COL ((p)->new NCuboid<>(dimSource.apply(p), IntStream.range(0,p.magnitude()).mapToObj((i)->(Integer)i).collect(Collectors.toList())), 
				IS_BOX, IS_COLUMN,
				(r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxX(r)), 
		ROW_COL ((p)->new NCuboid<>(dimSource.apply(p)), 
				IS_ROW, IS_COLUMN, 
				(r,l) -> l.get(0) == r.stream().findFirst().get().getZ());
		
		private Function<Sudoku,NCuboid<Integer>> nCuboidSource;
		private Predicate<Rule> isTypeA;
		private Predicate<Rule> isTypeB;
		private BiPredicate<Rule,List<Integer>> ruleIsDim;
		
		private TypePair(Function<Sudoku,NCuboid<Integer>> nCuboidSource, Predicate<Rule> isTypeA, Predicate<Rule> isTypeB, BiPredicate<Rule,List<Integer>> ruleIsDim){
			this.nCuboidSource = nCuboidSource;
			this.isTypeA = isTypeA;
			this.isTypeB = isTypeB;
			this.ruleIsDim = ruleIsDim;
		}
		
		/**
		 * <p>Returns an Iterable whose Iterator returns Pairs of Collections such that each 
		 * Collection in a Pair contains all the Rules of a certain RegionSpecies pertaining 
		 * to a specific pack. A pack is a geometrically bound subset of the Rules in a Sudoku, 
		 * each of which groups together Rules that can be recipients or sources in a short-
		 * form Sledgehammer.</p>
		 * 
		 * <p>There exists a pack for each flat layer of the spatial cube of the claims of a 
		 * Puzzle, one for each box of a printed puzzle, and six more for each unvertical slice 
		 * of the puzzle cube: three one each layer for each of the two unvertical orientations. 
		 * Each pack as such pertains 
		 * to all possible intersections of Rules of two specified RegionSpecies such that all 
		 * those Rules of either RegionSpecies in that pack share a certain 
		 * {@link Puzzle#IndexInstance dimensional value} in common.</p>
		 * @param s
		 * @return
		 */
		public Iterable<Pair<Collection<Rule>,Collection<Rule>>> packs(Sudoku s){
			/*NCuboid<Integer> ncube = nCuboidSource.apply(s);
			
			List<List<Rule>> a = new ArrayList<>(s.sideLength());
			List<List<Rule>> b = new ArrayList<>(s.sideLength());*/
			
			return StreamSupport.stream(nCuboidSource.apply(s).spliterator(),false)
					.map(
							(list) -> new Pair<Collection<Rule>,Collection<Rule>>(
									s.factStream().map((f)->(Rule)f).filter(isTypeA.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList()), 
									s.factStream().map((f)->(Rule)f).filter(isTypeB.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList())))
					.collect(Collectors.toList());
			
			/*for(List<Integer> l : ncube){
				a.add( s.factStream().map((f)->(Rule)f).filter(isTypeA.and((r) -> ruleIsDim.test(r,l))).collect(Collectors.toList()) );
				b.add( s.factStream().map((f)->(Rule)f).filter(isTypeB.and((r) -> ruleIsDim.test(r,l))).collect(Collectors.toList()) );
			}
			
			List<Pair<Collection<Rule>,Collection<Rule>>> result = new ArrayList<>(s.sideLength());
			
			for(int i=0; i<a.size(); i++){
				result.add(new Pair<Collection<Rule>,Collection<Rule>>(a.get(i), b.get(i)));
			}
			
			return result;*/
		}
	}
	/**
	 * <p>Unions all the collections in <tt>srcCombo</tt> into one set and returns 
	 * that set, unless some elements are shared among the collections in 
	 * srcCombo, in which case, if <tt>nullIfNotDisjoint</tt> is true, null is 
	 * returned instead.</p>
	 * @param collections a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @param nullIfNotDisjoint controls whether an intersection among the elements 
	 * of <tt>srcCombo</tt> results in <tt>null</tt> being returned.
	 * @return <tt>null</tt> if <tt>nullIfNotDisjoint</tt> is <tt>true</tt> and 
	 * some of the elements of <tt>srcCombo</tt> intersect each other, or otherwise 
	 * the mass-union of all the elements of <tt>srcCombo</tt>.
	 */
	static <T> ToolSet<T> sideEffectUnion(Collection<? extends Collection<T>> collections, boolean nullIfNotDisjoint){
		ToolSet<T> result = new ToolSet<>();
		
		int cumulativeSize = 0;
		for(Collection<T> redBag : collections){
			result.addAll(redBag);
			cumulativeSize += redBag.size();
		}
		
		return !nullIfNotDisjoint || cumulativeSize==result.size() ? result : null;
	}
	
	static <T> Map<T,Integer> countingUnion(Collection<? extends Collection<T>> collections){
		Map<T,Integer> result = new HashMap<>();
		
		for(Collection<T> col : collections){
			for(T t : col){
				result.put(t, result.containsKey(t) 
						? result.get(t)+1 
						: 1);
			}
		}
		
		return result;
	}
	
	/**
	 * <p>Represents an event (group) when a valid sledgehamemr 
	 * has been found and is being resolved.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventSledgehammer extends SolutionEvent{
		private SolveEventSledgehammer(Collection<Claim> claimsToSetFalse){
			falsified().addAll(claimsToSetFalse);
		}
	}
}