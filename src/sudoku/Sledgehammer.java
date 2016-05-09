package sudoku;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import common.TestIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
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
import sudoku.Puzzle.DimensionType;

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
	
	/**
	 * <p>The minimum number ({@value}) of Rules in a source combo relevant to Sledgehammer analysis. 
	 * Because Rules {@link Rule#verifyFinalState() automatically detect} when they need to 
	 * collapse and also automatically perform the resulting collapse, single-source 
	 * Sledgehammers won't be available in the target. This leaves two-Rule sources as 
	 * the smallest source combinations.</p>
	 */
	private static final int MIN_SRC_COMBO_SIZE = 2;
	
	private static final int MIN_SLEDGEHAMMER_SIZE = 2;
	
	/**
	 * <p>Constructs a SledgeHammer2 that works to solve the specified Puzzle.</p>
	 * @param target the Puzzle that this SledgeHammer2 tries to solve.
	 */
	public Sledgehammer(Sudoku puzzle) {
		super(puzzle);
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
		
		Collection<Rule> distinctRules = distinctRules();
		Map<Integer,List<Rule>> map = distinctRules.stream().collect(
				Collectors.toMap(
						(Rule rule)->rule.size(), 
						(Rule rule)->{List<Rule> result = new ArrayList<>(1); result.add(rule); return result;}, 
						(list1,list2) -> {list1.addAll(list2); return list1;}));
		
		Collection<Rule> distinctRulesSize = new ArrayList<>();
		for(int size = MIN_SRC_COMBO_SIZE; size<=distinctRules.size()/2; ++size){
			if(map.containsKey(size)){
				distinctRulesSize.addAll(map.get(size));
			}
			
			//For each disjoint source combo
			ComboGen<Rule> reds = new ComboGen<>(distinctRulesSize, size, size);
			for(Pair<List<Rule>,ToolSet<Claim>> comboAndUnion : new UnionIterable(reds)){ //TODO 
				List<Rule> srcCombo = comboAndUnion.getA();
				ToolSet<Claim> srcUnion = comboAndUnion.getB();
				
				//For each recipient combo derivable from that source combo
				Set<Fact> nearbyRules = rulesIntersecting(srcCombo, srcUnion, distinctRulesSize);
				for(List<Fact> recipientCombo : new ComboGen<>(nearbyRules, srcCombo.size(), srcCombo.size())){
					
					//If the source and recipient combos make a valid sledgehammer scenario
					Set<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipientCombo, srcUnion); 
					if(claimsToSetFalse != null && !claimsToSetFalse.isEmpty()){
						return resolve(claimsToSetFalse);
					}
				}
			}
		}
		
		return null;
	}
	
	/*
	 * TODO rebuild sledgehammer-search so it starts from a seed Rule and grows out from there.
	 */
	protected SolutionEvent process2(){
		return null;
	}
	
	/*
	 * TODO rebuild sledgehammer-search so it only (or primarily) seeks out inter-RegionSpecies pairings.
	 * that way, all srcs are of %srcSpecies% and all recips are of %recipSpecies%, which 
	 * is a way of simultaneously generalizing LineHatch (X-Wing, Swordfish, Jellyfish), GroupLocalization 
	 * (Naked Pairs, Hidden Pairs, Naked Triples, Hidden Triples, etc.), and that weird row/column-box 
	 * interaction whose name I don't know because it's degenerate in 9x9 into Sledgehammer while 
	 * hopefully maintaining the speed of solution found in earlier working solvers from before I knew 
	 * about Sledgehammer.
	 */
	protected SolutionEvent regionSpeciesPairProcessing(){
		
		Collection<Rule> distinctRules = distinctRules();
		
		/*Map<Integer,List<Rule>> sizeToRules = distinctRules.stream().collect(
				Collectors.toMap(
						(Rule rule)->rule.size(), 
						(Rule rule)->{List<Rule> result = new ArrayList<>(1); result.add(rule); return result;}, 
						(list1,list2) -> {list1.addAll(list2); return list1;}));*/
		
		/*Puzzle p = target.nodeStream().findFirst().get().getPuzzle(); //XXX a real solution, please
		for(List<RegionSpecies> types : new ComboGen<RegionSpecies>(Arrays.asList(RegionSpecies.values()), 2,2)){
			for(IndexInstance dim : p.getIndices(dimensionType(types,p))){
				for(int size = 2; size<target.sideLength(); size++){
					
					List<Rule> srcRecip = distinctRules.stream().filter((rule)->types.contains(rule.getType())).collect(Collectors.toList());
					
				}
			}
		}*/
		
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
	
	private static Set<Claim> falsifiedClaims(List<Rule> comboA, List<Rule> comboB){
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
	
	private DimensionType dimensionType(List<RegionSpecies> pair, Puzzle p){
		Set<DimensionType> dims0 = pair.get(0).dimsOutsideRule(p);
		Set<DimensionType> dims1 = pair.get(1).dimsOutsideRule(p);
		
		dims1.retainAll(dims0);
		
		if(dims1.size() != 1){
			throw new IllegalStateException("1 != "+dims1.size());
		} else{
			return dims1.iterator().next();
		}
	}
	
	private Collection<Rule> distinctRules(){
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
	
	/**
	 * <p>Returns a set of all the Rules that intersect any of the 
	 * Rules in <tt>sources</tt>.</p>
	 * @param union a pre-computed {@link Sledgehammer#sideEffectUnion(Collection, boolean) mass-union} 
	 * of the Claims in the Rules in <tt>sources</tt>
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in <tt>sources</tt>, excluding the Rules in <tt>sources</tt>.
	 */
	private Set<Fact> rulesIntersecting(List<Rule> sources, Set<Claim> srcUnion, Collection<Rule> distinctRulesSize){
		Set<Fact> result = srcUnion.stream().collect(Collector.of(
				ArrayList::new, 
				(List<Fact> a, Claim t) -> a.addAll(t), 
				(List<Fact> a1, List<Fact> a2) -> {a1.addAll(a2); return a1;}, 
				(List<Fact> a) -> new HashSet<>(a)));
		result.removeAll(sources);
		result.retainAll(distinctRulesSize);
		return result;
	}
	
	private class UnionIterable implements Iterable<Pair<List<Rule>,ToolSet<Claim>>>{
		
		Iterable<List<Rule>> wrappedIterable;
		
		UnionIterable(Iterable<List<Rule>> iterable){
			wrappedIterable = iterable;
		}
		
		@Override
		public Iterator<Pair<List<Rule>,ToolSet<Claim>>> iterator(){
			class UnionIterator implements Iterator<Pair<List<Rule>,ToolSet<Claim>>>{
				
				ToolSet<Claim> union;
				Iterator<List<Rule>> wrappedIterator;
				
				/**
				 * <p>Constructs a UnionIterator wrapping <tt>wrappedIterator</tt>.</p>
				 * @param wrappedIterator an Iterator whose outputs are filtered by 
				 * this UnionIterator
				 */
				UnionIterator(Iterable<List<Rule>> iterable){
					TestIterator<List<Rule>> wrappedIterator = new TestIterator<>(iterable.iterator());
					wrappedIterator.addTest((ruleList)->setUnion(sideEffectUnion(ruleList,true))!=null);
					this.wrappedIterator = wrappedIterator;;
					this.union = new ToolSet<>(0);
				}
				
				@Override
				public boolean hasNext(){
					return wrappedIterator.hasNext();
				}
				
				@Override
				public Pair<List<Rule>,ToolSet<Claim>> next(){
					
					return new Pair<>(wrappedIterator.next(),this.union);
				}
				
				/**
				 * <p>Sets this UnionIterator's <tt>union</tt></p>
				 * @param union the new <tt>union</tt> of this UnionIterator
				 * @return the parameter <tt>union</tt>
				 */
				ToolSet<Claim> setUnion(ToolSet<Claim> union){
					
					return this.union = union;
				}
			}
			
			return new UnionIterator(wrappedIterable);
		}
	}
	
	/* *
	 * <p>The size ({@value}) that at least one of the recipient Rules must have 
	 * in order for this Sledgehammer solution to have at least one Claim available 
	 * for removal.</p>
	 * 
	 * <p>More generally, at least one recip rules must have at least one Claim that is 
	 * not one of the srcClaims.</p>
	 */
	/*public static final int ESSENTIAL_RECIPIENT_SIZE = 3;*/
	
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
	private static Set<Claim> sledgehammerValidityCheck(List<? extends Fact> srcRules, List<? extends Fact> recipRules, final ToolSet<Claim> srcClaims){
		
		/*//make sure at least one recip has a size at least 3
		Predicate<Fact> trimmableRule = (rule) -> {
			Set<Claim> dump = new HashSet<>(rule);
			dump.removeAll(srcClaims);
			return !dump.isEmpty();
		};
		if(!recipRules.stream().anyMatch(trimmableRule)){
			return null;
		}*/
		
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
	 * <p>Returns true if <tt>c</tt> can still be true given that all the 
	 * Claims in <tt>givens</tt> are known true, false otherwise.</p>
	 * 
	 * @param c the Claim whose truth is being assessed
	 * @param givens the Claims whose truth is assumed while assessing 
	 * the truth of <tt>c</tt>.
	 * @return true if <tt>c</tt> is not guaranteed to be false given 
	 * that all the claims in <tt>givens</tt> are true, false otherwise.
	 */
	private static boolean isPossibleClaim(Claim c, List<Claim> givens){
		return !c.visibleClaims().intersects(givens);
	}
	
	/**
	 * <p>Unions all the collections in <tt>srcCombo</tt> into one set and returns 
	 * that set, unless some elements are shared among the collections in 
	 * srcCombo, in which case, if <tt>nullIfNotDisjoint</tt> is true, null is 
	 * returned instead.</p>
	 * @param colcol a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @param nullIfNotDisjoint controls whether an intersection among the elements 
	 * of <tt>srcCombo</tt> results in <tt>null</tt> being returned.
	 * @return <tt>null</tt> if <tt>nullIfNotDisjoint</tt> is <tt>true</tt> and 
	 * some of the elements of <tt>srcCombo</tt> intersect each other, or otherwise 
	 * the mass-union of all the elements of <tt>srcCombo</tt>.
	 */
	static <T> ToolSet<T> sideEffectUnion(Collection<? extends Collection<T>> colcol, boolean nullIfNotDisjoint){
		ToolSet<T> result = new ToolSet<>();
		
		int cumulativeSize = 0;
		for(Collection<T> redBag : colcol){
			result.addAll(redBag);
			cumulativeSize += redBag.size();
		}
		
		return !nullIfNotDisjoint || cumulativeSize==result.size() ? result : null;
	}
	
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