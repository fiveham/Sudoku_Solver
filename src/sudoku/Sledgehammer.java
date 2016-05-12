package sudoku;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import common.TestIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
			for(List<Rule> srcCombo : new ComboGen<>(distinctRulesSize, size, size)){
				if(sourceComboMostlyValid(srcCombo)){
					
					//For each recipient combo derivable from that source combo
					Set<Fact> nearbyRules = possibleRecipients(srcCombo, distinctRulesSize);
					for(List<Fact> recipientCombo : new ComboGen<>(nearbyRules, srcCombo.size(), srcCombo.size())){
						
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
	
	/*
	 * TODO rebuild sledgehammer-search so it starts from a seed Rule and grows out from there.
	 */
	protected SolutionEvent componentGrowthProcessing(){
		
		/*Collection<Rule> distinctRules = distinctRules();
		
		for(int size = MIN_SLEDGEHAMMER_SIZE; size <= target.size()/2; size++){
			final int currentSize = size;
			for(Rule seed : distinctRules.stream().filter((r)->r.size() <= currentSize).collect(Collectors.toList())){
				
				///for each sledgehammer arrangement grown from this seed
				for(Pair<Collection<Rule>,Collection<Rule>> sledgehammer : sledgeIterable(size, seed)){
					
					//if this arrangement is actionable, take action
					Set<Claim> falsify = falsifiedClaims(sledgehammer.getA(), sledgehammer.getB());
					if(falsify != null){
						return resolve(falsify);
					}
				}
			}
		}
		
		return null;*/
		
		Collection<Rule> distinctRules = distinctRules();
		
		for(int size = MIN_SLEDGEHAMMER_SIZE; size <= target.size()/2; size++){
			for(Rule seed : distinctRules){
				Pair<Collection<Rule>,Collection<Rule>> sledgehammer = seekSledgehammer(seed, size);
				if(sledgehammer != null){
					Set<Claim> falsified = sideEffectUnion(sledgehammer.getB(),false);
					falsified.removeAll(sideEffectUnion(sledgehammer.getA(),false));
					if(!falsified.isEmpty()){
						return resolve(falsified);
					}
				}
			}
		}
		
		return null;
	}
	
	private Pair<Collection<Rule>,Collection<Rule>> seekSledgehammer(Rule seed, int size){
		return null; //TODO stub
	}
	
	
	
	
	
	/* *
	 * <p>Returns an object that provides an Iterator that provides source-combo <tt>Pair</tt>s 
	 * for sledgehammer solution scenarios.</p>
	 * @param seed
	 * @return
	 */
	/*private Iterable<Pair<Collection<Rule>,Collection<Rule>>> sledgeIterable(int size, Rule seed){
		Sledge result = new Sledge(null, SledgeType.SOURCE, seed);
		
		List<Sledge> layer = new ArrayList<>();
		layer.add(result);
		while(!layer.isEmpty()){
			List<Sledge> nextLayer = new ArrayList<>();
			for(Sledge s : layer){
				nextLayer.addAll(s.grow());
			}
			layer = nextLayer;
		}
		
		class SledgeIterable implements Iterable<Pair<Collection<Rule>,Collection<Rule>>>{
			private Time root;
			SledgeIterable(Time root){
				this.root = root;
			}
			@Override
			public Iterator<Pair<Collection<Rule>,Collection<Rule>>> iterator(){
				class SledgeIterator implements Iterator<Pair<Collection<Rule>,Collection<Rule>>>{
					private final Iterator<Time> iter;
					SledgeIterator(Iterator<Time> iter){
						this.iter = iter;
					}
					@Override
					public boolean hasNext(){
						return iter.hasNext();
					}
					@Override
					public Pair<Collection<Rule>,Collection<Rule>> next(){
						Time leaf = iter.next();
						List<Time> trail = leaf.upTrail();
						Map<SledgeType,List<Rule>> map = trail.stream()
								.map((t)->(Sledge)t)
								.collect(Collectors.toMap(
										(Sledge s)->s.type, 
										(Sledge s)->s.rules, 
										(left,right) -> {left.addAll(right); return left;}));
						return new Pair<>(map.get(SledgeType.SOURCE), map.get(SledgeType.RECIP));
					}
				}
				
				return new SledgeIterator(root.iterator());
			}
		}
		
		return new SledgeIterable(result);
	}*/
	
	/*private static class Sledge extends AbstractTime{
		
		private final SledgeType type;
		private final Map<SledgeType,List<Rule>> map;
		
		Sledge(Sledge parent, SledgeType type, Rule... rules){
			super(parent);
			this.type = type;
			this.map = new HashMap<>();
			for(SledgeType st : SledgeType.values()){
				List<Rule> value = st == type 
						? new ArrayList<>(Arrays.asList(rules)) 
						: (st == type.complement() 
								? Collections.emptyList() 
								: new ArrayList<>());
				map.put(st, value);
			}
		}
		
		@Override
		public Sledge parent(){
			return (Sledge) super.parent();
		}
		
		Collection<Rule> sources(){
			return map.get(SledgeType.SOURCE);
		}
		
		Collection<Rule> ignored(){
			return map.get(SledgeType.IGNORE);
		}
		
		Collection<Rule> recipients(){
			return map.get(SledgeType.RECIP);
		}
		
		Collection<Rule> allSources(){
			return allOfType(SledgeType.SOURCE, upTrail());
		}
		
		Collection<Rule> allIgnored(){
			return allOfType(SledgeType.IGNORE, upTrail());
		}
		
		Collection<Rule> allRecipients(){
			return allOfType(SledgeType.RECIP, upTrail());
		}
		
		private static Collection<Rule> allOfType(SledgeType sledgeType, Collection<Time> upTrail){
			return upTrail.stream()
					.map((t)->(Sledge)t)
					.collect(Collector.of(
							ArrayList::new, 
							(List<Rule> a, Sledge t) -> a.addAll(t.map.get(sledgeType)), 
							(List<Rule> left, List<Rule> right) -> {
								left.addAll(right); 
								return left;
							}));
		}
		
		private boolean hasGrown = false;
		
		*//* *
		 * <p>Adds children to this Sledge for each possible further refinement 
		 * of the sledgehammer solution scenario to which this Sledge pertains.</p>
		 * @return this Sledge's children after this method call
		 *//*
		List<Sledge> grow(){
			if(!hasGrown){
				
				Collection<Rule> sources, ignored, recipients;
				{
					List<Time> upTrail = upTrail();
					sources = allOfType(SledgeType.SOURCE, upTrail);
					ignored = allOfType(SledgeType.IGNORE, upTrail);
					recipients = allOfType(SledgeType.RECIP, upTrail);
				}
				
				
				
				
				
				
				
				hasGrown = true;
			}
			return children().stream().map((t)->(Sledge)t).collect(Collectors.toList());
		}
	}*/
	
	/*private enum SledgeType{
		SOURCE, 
		IGNORE, 
		RECIP;
		
		SledgeType complement(){
			return values()[values().length - 1 - ordinal()];
		}
	}*/
	
	/*private static Collection<Collection<Rule>> visibleRules(Rule r, Collection<Rule> distinctRules){
		return r.stream()
				.map((c) -> {
					Set<Rule> temp = c.stream()
							.map((f)->(Rule)f)
							.collect(Collectors.toSet()); 
					temp.remove(r);
					temp.retainAll(distinctRules);
					return temp;
				})
				.collect(Collectors.toSet());
	}*/
	
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
	 * <p>Returns a set of all the Rules that intersect at least two of 
	 * the Rules in <tt>sources</tt>.</p> 
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @param distinctRulesAtSize a collection of Rules that are 
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in <tt>sources</tt>, excluding the Rules in <tt>sources</tt>.
	 */
	private Set<Fact> possibleRecipients(List<Rule> sources, Collection<Rule> distinctRulesAtSize){
		List<Set<Rule>> visibleRules = sources.stream().collect(Collector.of(
				ArrayList::new, 
				(List<Set<Rule>> a, Rule source) -> a.add(source.visibleRules()), 
				(left,right) -> {left.addAll(right); return left;}));
		
		Map<Rule,Integer> countSet = countingUnion(visibleRules);
		
		Set<Fact> result = countSet.keySet().stream().filter((r) -> countSet.get(r) > 1).collect(Collectors.toSet());
		result.retainAll(distinctRulesAtSize);
		return result;
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
	public static boolean sourceComboMostlyValid(List<Rule> ruleList){
		Set<Rule> allVisibleRules = ruleList.get(0).visibleRules();
		
		for(int i = 1; i<ruleList.size(); ++i){
			Set<Rule> visibleToCurrentRule = ruleList.get(i).visibleRules();
			
			if(Collections.disjoint(visibleToCurrentRule, allVisibleRules)){
				return false;
			} else{
				allVisibleRules.addAll(visibleToCurrentRule);
			}
		}
		
		return Collections.disjoint(allVisibleRules, ruleList);
	};
	
	/*private class UnionIterable implements Iterable<List<Rule>>{
		
		Iterable<List<Rule>> wrappedIterable;
		
		UnionIterable(Iterable<List<Rule>> iterable){
			wrappedIterable = iterable;
		}
		
		@Override
		public Iterator<List<Rule>> iterator(){
			class UnionIterator implements Iterator<List<Rule>>{
				
				Iterator<List<Rule>> wrappedIterator;
				
				*//* *
				 * <p>Constructs a UnionIterator wrapping <tt>wrappedIterator</tt>.</p>
				 * @param wrappedIterator an Iterator whose outputs are filtered by 
				 * this UnionIterator
				 *//*
				UnionIterator(Iterable<List<Rule>> iterable){
					TestIterator<List<Rule>> wrappedIterator = new TestIterator<>(iterable.iterator());
					wrappedIterator.addTest(RULES_CONNECTED_BY_INTERMEDIATE_RULES_ARE_CONNECTED_COMPONENT);
					this.wrappedIterator = wrappedIterator;;
				}
				
				@Override
				public boolean hasNext(){
					return wrappedIterator.hasNext();
				}
				
				@Override
				public List<Rule> next(){
					
					return wrappedIterator.next();
				}
			}
			
			return new UnionIterator(wrappedIterable);
		}
	}*/
	
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
	private static Set<Claim> sledgehammerValidityCheck(List<? extends Fact> srcRules, List<? extends Fact> recipRules){
		
		/*//make sure at least one recip has a size at least 3
		Predicate<Fact> trimmableRule = (rule) -> {
			Set<Claim> dump = new HashSet<>(rule);
			dump.removeAll(srcClaims);
			return !dump.isEmpty();
		};
		if(!recipRules.stream().anyMatch(trimmableRule)){
			return null;
		}*/
		
		final ToolSet<Claim> srcClaims = sideEffectUnion(srcRules,true);
		
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