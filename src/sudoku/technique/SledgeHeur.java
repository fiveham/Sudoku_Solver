package sudoku.technique;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Puzzle;
import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.Puzzle.RuleType;
import sudoku.time.TechniqueEvent;

//TODO describe sledgehammer solution scenarios and their stipulations in the class javadoc
/**
 * <p>The sledgehammer technique for solving sudoku puzzles is defined 
 * at http://onigame.livejournal.com/18580.html and at 
 * http://onigame.livejournal.com/20626.html and uses one collection 
 * of specific statements of the rules of a sudoku network against 
 * another collection of such statements to determine that certain 
 * claims about that network are false.</p>
 * 
 * <p>The sledgehammer technique relies on the fact that a sudoku puzzle can 
 * be interpreted as a graph of {@link Claim truth-claims} about the values of the 
 * cells ("Cell x,y, has value z.") connected with {@link Rule groupings of those truth-claims} of 
 * which exactly one claim from a given grouping is true and all the rest are 
 * false.</p>
 * 
 * <p>These groupings of claims are pedantic and precise statements 
 * of the general rules that specify what makes a solution to a sudoku target valid. 
 * For example, the rule that any row contains each value exactly once 
 * pedantically expands into 81 statements in a 9x9 puzzle: 9 statements 
 * about an individual row, each of which is actually 9 statements 
 * specifying a particular value: "Row y has value z in exactly one cell." 
 * The rule that any cell contains exactly one value similarly becomes 81 
 * pedantic statements: "For cell x,y, only one z value is correct."</p>
 * 
 * <p>The sledgehammer technique generalizes a great number of analysis 
 * techniques including
 * <ul><li>naked singles</li>
 * <li>hidden singles</li>
 * <li>naked pairs/triples/etc.</li>
 * <li>x-wing, swordfish, jellyfish, etc.</li>
 * <li>xy-wing, xyz-wing, etc.</li>
 * </ul></p>
 * 
 * <p>This is achieved by modeling those situation as the collapse of several 
 * "recipient" rules onto several "source" rules so that all the claims belonging 
 * to the recipient rules but not belonging to any of the source rules are 
 * known to be false. No matter which (viable) set of claims among the source 
 * rules is the source rules' set of true claims, all the recipient rules' 
 * claims that aren't also source claims must be false.</p>
 * 
 * <p>Sledgehammer solutions have a {@code size} equal to the number of source rules 
 * in the given solution scenario. This is also the minimum number of recipient 
 * rules in a sledgehammer of that {@code size}. In this implementation, it is only allowed for there 
 * to be exactly {@code size} recipients in a given sledgehammer scenario. This is 
 * because any sledgehammer scenario with more recipients than sources can be 
 * reinterpreted with no loss of veracity as multiple sledgehammers with exactly 
 * {@code size} recipients.</p>
 * 
 * <p>In this implementation, each source rule must intersect at least two recipient 
 * rules because otherwise either the one-recipient source rule does not have all its 
 * claims accounted for and the overall sledgehammer arrangement is not actionable, or 
 * the one-recipient source does have all its claims accounted for by one recipient, 
 * in which case the sledgehammer being examined can be broken down into two or more 
 * smaller sledgehammers. This implementation constructs sledgehammers starting at the 
 * smallest possible non-trivial size and increasing the size of the sledgehammers it 
 * looks for; so, the latter case should never be available for action by this implementation 
 * of this technique at all.</p>
 * 
 * <p>In this implementation, each recipient rule must intersect at least two source 
 * rules because, if it only intersects one of the sources, then either 
 * <ul>
 * <li>If that source only intersects one recipient (this recipient), then the overall 
 * sledgehammer in question can be broken into multiple smaller-size sledgehammers, in 
 * which case those sledgehammers will already have been explored by this implementation.</li>
 * <li>If that source intersects multiple recipient rules, then any solution-state among 
 * the sources that doesn't verify one of the Claims shared by this recipient and its source 
 * should merely disjoin the recipient from its source, leaving it with exactly those 
 * of its Claims that were never elements of the recipient's source rule. The contradicts 
 * how a sledgehammer should work, which is to remove all the non-source Claims of all 
 * the recipients.</li>
 * </ul></p>
 * 
 * <p>In general we can say that sources and recipients must connect with at least two 
 * rules of the complementary role because the resolution of a sledgehammer scenario is 
 * an identity statement: "These source Rules <i>are</i> these recipient Rules." As such, 
 * any one of the involved rules connected to only one of the rules of the complementary 
 * role must be that exact other rule once it is subjected to the identity-sharing 
 * stipulation of the sledgehammer. If a simple "this Rule is this other Rule" statement 
 * is true, then it proper to identify and resolve that smaller-scale situation as a 
 * separate concern not involved with this implementation.</p>
 * 
 * <p>A sledgehammer of size 1 is trivial and not accounted for by this implementation. 
 * Since a sledgehammer of size 1 has exactly one source rule, detection and resolution 
 * of size-1 sledgehammers is automated. Rules {@link Rule#validateFinalState(TechniqueEvent) check themselves} 
 * for size-1 sledgehammer actionability every time some Claims are removed from them, 
 * and if that Rule is a proper subset of any of the rules that share any of its Claims, 
 * the superset is collapsed onto the subset before the method that removed some Claims 
 * from the subset returns.</p>
 * 
 * @author fiveham
 *
 */
public class SledgeHeur extends AbstractTechnique {
	
	public static final int MIN_RECIPIENT_COUNT_PER_SOURCE = 2;
	public static final int MIN_SOURCE_COUNT_PER_RECIPIENT = 2;
	
	private static final int MIN_SLEDGEHAMMER_SIZE = 2;
	
	/**
	 * <p>Constructs a SledgeHammer that works to solve the specified Puzzle.</p>
	 * @param target the Puzzle that this SledgeHammer tries to solve.
	 */
	public SledgeHeur(Sudoku puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>The {@code mergeFunction} for the {@link Collectors#toMap(Function,Function,BinaryOperator) toMap} 
	 * calls that define {@link #MAP_SOURCES_BY_SIZE} and {@link #MAP_RULES_BY_SIZE}.</p>
	 */
	public static <E, C extends Collection<E>> C mergeCollections(C c1, C c2){
		c1.addAll(c2);
		return c1;
	}
	
	/**
	 * <p>Collects a list of distinct Rules from a Sudoku into a Map partitioning them 
	 * according to the minimum size of a sledgehammer in which those Rules can serve.</p>
	 * @param ruleStream
	 * @param sledgehammerSize given a Rule from {@code ruleStream}, outputs the minimum 
	 * size of a sledgehammer solution scenario in which that Rule can serve
	 * @return
	 */
	public static Map<Integer,List<Rule>> mapRulesBySize(Stream<Rule> ruleStream, Function<? super Rule, ? extends Integer> sledgehammerSize){
		return ruleStream.collect(Collectors.toMap(
				sledgehammerSize, 
				SledgeHeur::singletonList, 
				SledgeHeur::mergeCollections));
	}
	
	/**
	 * <p>The {@code valueMapper} for the {@link Collectors#toMap(Function,Function,BinaryOperator) toMap} 
	 * calls that define {@link #MAP_SOURCES_BY_SIZE} and {@link #MAP_RULES_BY_SIZE}.</p>
	 */
	public static <T> List<T> singletonList(T t){
		List<T> result = new ArrayList<>(1);
		result.add(t);
		return result;
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
	 * {@code target}
	 */
	@Override
	protected TechniqueEvent process(){
		return regionSpeciesPairProcessing();
	}
	
	/**
	 * <p>Sets all the Claims in {@code claimsToSetFalse} false.</p>
	 * @param claimsToSetFalse the Claims to be set false
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	static TechniqueEvent resolve(Set<Claim> claimsToSetFalse, Collection<? extends Fact> src, Collection<? extends Fact> recip, Resolve res){
		TechniqueEvent time = res.resolve(claimsToSetFalse, src, recip);
		claimsToSetFalse.stream().forEach((c)->c.setFalse(time));
		return time;
	}
	
	static interface Resolve{
		public TechniqueEvent resolve(Set<Claim> falsified, Collection<? extends Fact> src, Collection<? extends Fact> recip);
	}
	
	/**
	 * <p>Looks for sledgehammer solution scenarios exclusively as relations between 
	 * Rules of different {@link Puzzle.RuleType types}. This allows this implementation 
	 * of the sledgehammer technique to simultaneously generalize X-Wing, Swordfish, 
	 * Jellyfish, naked pairs, naked triples, naked quadruples, hidden pairs, hidden 
	 * triples, hidden quadruples, etc. without the overheard incurred by earlier 
	 * implementations of this class which looked at all combinations of all types of 
	 * Rules.</p>
	 * 
	 * <p>This is a heuristic technique, meant to be used before ColorChain but requiring 
	 * a complete form of Sledgehammer that does explore all possible valid source combos 
	 * to be included in the Solver's techniques following ColorChain.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	private TechniqueEvent regionSpeciesPairProcessing(){
		
		for(TypePair types : TypePair.values()){
			for(Pair<Collection<Rule>,Collection<Rule>> pack : types.packs(target)){
				Collection<Rule> a = pack.getA();
				Collection<Rule> b = pack.getB();
				
				for(int size = MIN_SLEDGEHAMMER_SIZE; size<Math.min(a.size(), b.size()); size++){
					for(List<Rule> comboA : new ComboGen<>(a, size,size)){
						for(List<Rule> comboB : new ComboGen<>(b, size,size)){
							Set<Claim> falsify = falsifiedClaims(comboA, comboB);
							
							if(falsify != null){
								return resolve(falsify, comboA, comboB, SolveEventSledgehammer::new);
							}
						}
					}
				}
			}
		}
		
		return null;
	}
	
	private static Set<Claim> falsifiedClaims(Collection<Rule> comboA, Collection<Rule> comboB){
		Set<Claim> unionA = massUnion(comboA);
		Set<Claim> unionB = massUnion(comboB);
		
		Set<Claim> inters = new HashSet<>(unionA);
		inters.retainAll(unionB);
		
		unionA.removeAll(inters);
		unionB.removeAll(inters);
		
		if(unionA.isEmpty() != unionB.isEmpty()){
			return unionA.isEmpty() ? unionB : unionA;
		} else{
			return null;
		}
	}
	
	private static int boxIndex(Rule r){
		int mag = r.iterator().next().getPuzzle().magnitude();
		return boxY(r)*mag + boxX(r);
	}
	
	private static int boxY(Rule r){
		Claim c = r.iterator().next();
		int y = c.getY();
		return y/c.getPuzzle().magnitude();
	}
	
	private static int boxX(Rule r){
		Claim c = r.iterator().next();
		int x = c.getX();
		return x/c.getPuzzle().magnitude();
	}
	
	public static final Function<Sudoku,List<Integer>> DIMSOURCE = (s) -> IntStream.range(0,s.sideLength()).mapToObj(Integer.class::cast).collect(Collectors.toList());
	
	private static final Function<Sudoku,NCuboid<Integer>> STD_NCUBOID_SRC = (s)->new NCuboid<>(DIMSOURCE.apply(s));
	private static final Function<Sudoku,NCuboid<Integer>> ALT_NCUBOID_SRC = (s)->new NCuboid<>(DIMSOURCE.apply(s), IntStream.range(0,s.magnitude()).mapToObj(Integer.class::cast).collect(Collectors.toList()));
	
	private static enum TypePair{
		CELL_COL(STD_NCUBOID_SRC, RuleType.CELL::isTypeOf, RuleType.COLUMN::isTypeOf, (r,l) -> l.get(0) == r.stream().findFirst().get().getX()), 
		CELL_ROW(STD_NCUBOID_SRC, RuleType.CELL::isTypeOf, RuleType.ROW::isTypeOf,    (r,l) -> l.get(0) == r.stream().findFirst().get().getY()), 
		CELL_BOX(STD_NCUBOID_SRC, RuleType.CELL::isTypeOf, RuleType.BOX::isTypeOf,    (r,l) -> l.get(0).equals(boxIndex(r))), 
		BOX_ROW (ALT_NCUBOID_SRC, RuleType.BOX::isTypeOf,  RuleType.ROW::isTypeOf,    (r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxY(r)), 
		BOX_COL (ALT_NCUBOID_SRC, RuleType.BOX::isTypeOf,  RuleType.COLUMN::isTypeOf, (r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxX(r)), 
		ROW_COL (STD_NCUBOID_SRC, RuleType.ROW::isTypeOf,  RuleType.COLUMN::isTypeOf, (r,l) -> l.get(0) == r.stream().findFirst().get().getZ());
		
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
		 * each of which unites Rules that can be recipients or sources in certain sledgehammer 
		 * solution scenarios.</p>
		 * 
		 * <p>There exists a pack for each flat layer of the spatial cube of the claims of a 
		 * Puzzle, one for each box of a printed puzzle, and six more for each unvertical slice 
		 * of the puzzle cube: three on each layer for each of the two unvertical orientations. 
		 * Each pack as such pertains 
		 * to all possible intersections of Rules of two specified RegionSpecies such that all 
		 * those Rules of either RegionSpecies in that pack share a certain 
		 * {@link Puzzle#IndexInstance dimensional value} in common.</p>
		 * @param s
		 * @return
		 */
		public Iterable<Pair<Collection<Rule>,Collection<Rule>>> packs(Sudoku s){
			return StreamSupport.stream(nCuboidSource.apply(s).spliterator(),false)
					.map((list) -> new Pair<Collection<Rule>,Collection<Rule>>(
							s.factStream().map(Rule.class::cast).filter(isTypeA.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList()), 
							s.factStream().map(Rule.class::cast).filter(isTypeB.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList())))
					.collect(Collectors.toList());
		}
	}
	
	/**
	 * <p>Unions all the collections in {@code srcCombo} into one set and returns 
	 * that set, unless some elements are shared among the collections in 
	 * srcCombo, in which case, if {@code nullIfNotDisjoint} is true, null is 
	 * returned instead.</p>
	 * @param collections a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @param nullIfNotDisjoint controls whether an intersection among the elements 
	 * of {@code srcCombo} results in {@code null} being returned.
	 * @return {@code null} if {@code nullIfNotDisjoint} is {@code true} and 
	 * some of the elements of {@code srcCombo} intersect each other, or otherwise 
	 * the mass-union of all the elements of {@code srcCombo}.
	 */
	public static <E, C extends Collection<E>> Set<E> massUnion(Collection<C> collections){
		return collections.stream().collect(massUnionCollector());
	}
	
	public static <S, T extends Collection<S>> Collector<T,?,Set<S>> massUnionCollector(){
		return Collector.of(
				HashSet<S>::new, 
				Set::addAll, 
				SledgeHeur::mergeCollections);
	}
	
	/**
	 * <p>Represents an event (group) when a valid sledgehammer 
	 * has been found and is being resolved.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventSledgehammer extends TechniqueEvent{
		
		private final Collection<Fact> src, recip;
		
		private SolveEventSledgehammer(Set<Claim> claimsToSetFalse, Collection<? extends Fact> src, Collection<? extends Fact> recip){
			super(claimsToSetFalse);
			this.src = new ArrayList<>(src);
			this.recip = new ArrayList<>(recip);
		}
		
		@Override
		public String toString(){
			return "Sledgehammer scenario: "+src+" ARE "+recip + super.toString();
		}
	}
}