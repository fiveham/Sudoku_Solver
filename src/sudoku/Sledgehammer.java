package sudoku;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import sudoku.Puzzle.RuleType;

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
 * of size-1 sledgehammers is automated. Rules {@link Rule#validateFinalState(SolutionEvent) check themselves} 
 * for size-1 sledgehammer actionability every time some Claims are removed from them, 
 * and if that Rule is a proper subset of any of the rules that share any of its Claims, 
 * the superset is collapsed onto the subset before the method that removed some Claims 
 * from the subset returns.</p>
 * 
 * @author fiveham
 *
 */
public class Sledgehammer extends Technique {
	
	/**
	 * <p>The {@code valueMapper} for the {@link Collectors#toMap(Function,Function,BinaryOperator) toMap} 
	 * calls that define {@link #MAP_SOURCES_BY_SIZE} and {@link #MAP_RULES_BY_SIZE}.</p>
	 */
	private static final Function<Rule,List<Rule>> VALUE_MAPPER = (Rule rule) -> {
		List<Rule> result = new ArrayList<>(1); 
		result.add(rule); 
		return result;
	};
	
	/**
	 * <p>The {@code mergeFunction} for the {@link Collectors#toMap(Function,Function,BinaryOperator) toMap} 
	 * calls that define {@link #MAP_SOURCES_BY_SIZE} and {@link #MAP_RULES_BY_SIZE}.</p>
	 */
	private static final BinaryOperator<List<Rule>> MERGE_FUNCTION = (list1,list2) -> {
		list1.addAll(list2); 
		return list1;
	};
	
	/**
	 * <p>Collects a list of distinct Rules from a Sudoku into a Map partitioning them 
	 * according to the minimum size of a sledgehammer in which those Rules can serve 
	 * as a source.</p>
	 */
	public static final Collector<Rule,?,Map<Integer,List<Rule>>> MAP_SOURCES_BY_SIZE = Collectors.toMap(
			Sledgehammer::sledgehammerSizeIfSource, 
			VALUE_MAPPER, 
			MERGE_FUNCTION);
	
	/**
	 * <p>Collects a list of distinct Rules from a Sudoku into a Map partitioning them 
	 * according to the minimum size of a sledgehammer in which those Rules can serve 
	 * as a recipient.</p>
	 */
	public static final Collector<Rule,?,Map<Integer,List<Rule>>> MAP_RULES_BY_SIZE = Collectors.toMap(
			Rule::size, 
			VALUE_MAPPER, 
			MERGE_FUNCTION);
	
	public static final int MIN_RECIPIENT_COUNT_PER_SOURCE = 2;
	public static final int MIN_SOURCE_COUNT_PER_RECIPIENT = 2;
	
	private static final int MIN_SLEDGEHAMMER_SIZE = 2;
	
	private final VisibleCache visibleCache;
	
	private final Collection<Rule> distinctRules;
	private final Map<Integer,List<Rule>> distinctRulesBySledgehammerSize;
	private final Map<Integer,List<Rule>> distinctSourcesBySledgehammerSize;
	
	private boolean builtSrcComboAtLastSize = true;
	
	/**
	 * <p>Constructs a SledgeHammer that works to solve the specified Puzzle.</p>
	 * @param target the Puzzle that this SledgeHammer tries to solve.
	 */
	public Sledgehammer(Sudoku puzzle) {
		super(puzzle);
		
		this.visibleCache = new VisibleCache();
		
		this.distinctRules = new ArrayList<>();
		this.distinctRulesBySledgehammerSize = new HashMap<>();
		this.distinctSourcesBySledgehammerSize = new HashMap<>();
	}
	
	private static void populateDistinctRules(Sledgehammer sh){
		sh.distinctRules.addAll(distinctRules(sh.target));
		sh.distinctRulesBySledgehammerSize.putAll( sh.distinctRules.stream().collect(MAP_RULES_BY_SIZE) );
		sh.distinctSourcesBySledgehammerSize.putAll( sh.distinctRules.stream().collect(MAP_SOURCES_BY_SIZE) );
	}
	
	/**
	 * <p>Returns the minimum size of a sledgehammer solution scenario for which 
	 * {@code r} could be a source. The size is usually {@code r.size()}, but in 
	 * some cases is less, if {@code r} can have two or more of its Claims accounted 
	 * for by a single other Rule. In that case, combinations of {@code r}'s 
	 * {@link Rule#visibleRules() visible Rules} are explored, starting at the 
	 * smallest possible size and working up until a combination of visible Rules 
	 * around {@code r} is found that accounts for all of {@code r}'s Claims.</p>
	 * @param r the Rule whose minimum acceptable Sledgehammer size in which 
	 * {@code r} can act as a source is determined
	 * @throws IllegalStateException if {@code r} has more {@link Rule#visibleRules() visible Rules} 
	 * than it would have if none of its Claims shared any Rules in common, which 
	 * is impossible, or if no combination of Rules visible to {@code r} with 
	 * combination-size not greater than {@code r.size()} accounts for all 
	 * {@code r}'s Claims
	 * @return the minimum size of a sledgehammer solution scenario for which 
	 * {@code r} could be a source
	 */
	private static int sledgehammerSizeIfSource(Rule r){
		
		//Maximum number of recipient rules attachable to this Rule r: 
		//Inclusive upper bound on the minimum size of Sledgehammer where r is a source.
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
			for(List<Rule> visibleCombo : new ComboGen<>(visible, MIN_RECIPIENT_COUNT_PER_SOURCE, r.size())){
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
	 * {@code target}
	 */
	@Override
	protected SolutionEvent process(){
		return processByGrowth();
	}
	
	/**
	 * <p>Finds sledgehammer solution scenarios by growing them from a seed source Rule, which 
	 * limits the combinatorial space to be explored to find all sledgehammer scenarios.</p>
	 * 
	 * <p>Iterates over all possible sledgehammer sizes for this {@code Technique}'s {@code target}. 
	 * For each size, iterates over all the Rules that can serve as sources in sledgehammers of 
	 * that size. All other possible source Rules at that size that have not served as a seed Rule 
	 * at that size are used as a pool ("&amp;" mask) of which any other potential sources near the 
	 * current seed Rule must be a member.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	private SolutionEvent processByGrowth(){
		if(distinctRules.isEmpty()){
			populateDistinctRules(this);
		}
		
		for(int size = MIN_SLEDGEHAMMER_SIZE; size <= target.size()/2 && builtSrcComboAtLastSize; ++size){
			Debug.log("At srcCombo size "+size); //DEBUG
			builtSrcComboAtLastSize = false;
			SolutionEvent event = exploreSourceCombos(new ArrayList<>(0), size, distinctSourcesAtSize(size));
			if(event != null){
				return event;
			}
		}
		
		return null;
	}
	
	/**
	 * <p></p>
	 * @param initialSources the established sources in the source-combinations being built
	 * @param size the number of sources in the source-combinations being built
	 * @param sourceMask a collection of Rules that are capable of serving (and allowed to serve) 
	 * as sources in a sledgehammer at this {@code size} and having the construction history of 
	 * {@code initialSources} so that potential sources that have been fully explored can be skipped
	 * @param whenBuiltSourceCombo an action to be performed when a source combo is successfully 
	 * built at the specified {@code size}
	 * @return a {@link SolutionEvent} describing the resolution of a sledgehammer built using 
	 * {@code initialSources} as-is or as a starting population, or {@code null} if no resolvable 
	 * sledgehammer is found
	 */
	private SolutionEvent exploreSourceCombos(List<Rule> initialSources, int size, Set<Rule> sourceMask){
		if(initialSources.size() < size){
			Set<Rule> localSourceMask = new HashSet<>(sourceMask);
			for(Rule newSource : sourcePool(initialSources, sourceMask, size)){
				localSourceMask.remove(newSource);
				
				List<Rule> newSources = new ArrayList<>(initialSources.size()+1);
				newSources.addAll(initialSources);
				newSources.add(newSource);
				
				SolutionEvent event = exploreSourceCombos(newSources, size, localSourceMask);
				if(event != null){
					return event;
				}
			}
			return null;
		} else if(initialSources.size() == size){
			builtSrcComboAtLastSize = true;
			return exploreRecipientCombos(initialSources);
		} else{
			throw new IllegalStateException("Current source-combination size greater than prescribed sledgehammer size: "+initialSources.size() + " > " + size);
		}
	}
	
	//TODO reuse ingredients (visible cloud, visVisible cloud) externally in the calling context
	private Set<Rule> sourcePool(List<Rule> initialSources, Set<Rule> sourceMask, int size){
		if(initialSources.isEmpty()){
			return sourceMask;
		} else{
			Set<Rule> visibles = new HashSet<>();
			for(Rule src : initialSources){
				visibles.addAll(visibleCache.get(src, size));
			}
			visibles.removeAll(initialSources);
			visibles.retainAll(sourceMask);
			
			Set<Rule> visVisibles = new HashSet<>();
			for(Rule visible : visibles){
				visVisibles.addAll(visibleCache.get(visible, size));
			}
			visVisibles.removeAll(initialSources);
			visVisibles.removeAll(visibles);
			visVisibles.retainAll(sourceMask);
			
			return new HashSet<>(visVisibles);
		}
	}
	
	/**
	 * <p></p>
	 * @param srcCombo a closely-connected, disjoint collection of distinct Rules suitable 
	 * for use as sources in a sledgehammer solution scenario
	 * @param distinctRuleMask
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	private SolutionEvent exploreRecipientCombos(List<Rule> srcCombo){
		//For each recipient combo derivable from srcCombo that disjoint, closely connected source combo
		for(List<Rule> recipientCombo : recipientCombinations(srcCombo)){
			
			//If the source and recipient combos make a valid sledgehammer scenario
			Set<Claim> claimsToSetFalse = areSledgehammerScenario(srcCombo, recipientCombo);
			if(claimsToSetFalse != null && !claimsToSetFalse.isEmpty()){
				return resolve(claimsToSetFalse, srcCombo, recipientCombo, SolveEventSledgehammer::new);
			}
		}
		return null;
	}
	
	private final Map<Integer,List<Rule>> distinctRulesAtSizeCache = new HashMap<>();
	
	private List<Rule> distinctRulesAtSize(int size){
		return distinctRulesOfTypeAtSize(size, distinctRulesAtSizeCache, distinctRulesBySledgehammerSize, ArrayList::new);
	}
	
	private final Map<Integer,Set<Rule>> distinctSourcesAtSizeCache = new HashMap<>();
	
	private Set<Rule> distinctSourcesAtSize(int size){
		return distinctRulesOfTypeAtSize(size, distinctSourcesAtSizeCache, distinctSourcesBySledgehammerSize, HashSet::new);
	}
	
	private <C extends Collection<Rule>> C distinctRulesOfTypeAtSize(int size, Map<Integer,C> cache, Map<Integer,List<Rule>> rulesOfType, Supplier<C> supplier){
		if(cache.containsKey(size)){
			return cache.get(size);
		} else{
			C result = supplier.get();
			if(rulesOfType.containsKey(size)){
				result.addAll(rulesOfType.get(size));
			}
			if(size>0){
				result.addAll(distinctRulesOfTypeAtSize(size-1, cache, rulesOfType, supplier));
			}
			
			cache.put(size, result);
			
			return result;
		}
	}
	
	/**
	 * <p>Returns a collection of the Rules of {@code target} such that 
	 * if there are any Rules in {@code target} that are equal as {@code Set}s, 
	 * then only one of them appears.</p>
	 * @param target the Sudoku whose distinct Rules are identified
	 * @return the distinct Rules found in {@code target}
	 */
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
	
	/**
	 * <p>Returns a set of all the Rules that intersect at least two of 
	 * the Rules in {@code sources}.</p>
	 * 
	 * <p>The requirement that no source Rule should be considered for 
	 * use as a recipient Rule is accounted for implicitly by the disjointness 
	 * testing this method performs on the {@code sources}, accumulating 
	 * a cloud of Rules visible to the {@code sources} and making sure that 
	 * none of the {@code sources} is in that cloud. That cloud is later 
	 * used as the basis of the collection of Rules sent to the constructor 
	 * of the ComboGen that is returned, except in cases where 
	 * {@link #NO_COMBOS no recipient-combinations} should be iterated over 
	 * at all.</p>
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @param distinctRuleMask a collection of Rules that are allowed to be 
	 * used in the returned ComboGen
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in {@code sources}, excluding the Rules in {@code sources}.
	 */
	private ComboGen<Rule> recipientCombinations(List<Rule> sources){
		
		List<Rule> unconnectedSources = new ArrayList<>(sources);
		int prevUnconSrcCount = unconnectedSources.size();
		
		//seed the "connected" area with a source Rule
		Map<Rule,Set<Rule>> rulesVisibleToConnectedSources = new HashMap<>();
		addVisibles(rulesVisibleToConnectedSources, unconnectedSources.remove(unconnectedSources.size()-1));
		
		//connect as many of the sources together as possible
		while(unconnectedSources.size() != prevUnconSrcCount){
			prevUnconSrcCount = unconnectedSources.size();
			
			//try to connect each currently unconnected source to the "connected" area
			for(Iterator<Rule> iter = unconnectedSources.iterator(); iter.hasNext();){
				Rule currentUnconnectedSource = iter.next();
				
				//No source Rule can be directly visible to another source Rule
				if(rulesVisibleToConnectedSources.containsKey(currentUnconnectedSource)){
					return NO_COMBOS;
				}
				
				//If the current Rule is 4 steps away from at least one other source Rule, 
				Set<Rule> visibleToCurrentRule = visibleCache.get(currentUnconnectedSource, sources.size());
				if(!Collections.disjoint(visibleToCurrentRule, rulesVisibleToConnectedSources.keySet())){
					addVisibles(rulesVisibleToConnectedSources, currentUnconnectedSource);
					iter.remove();
				}
			}
		}
		
		if(unconnectedSources.size() == 0){
			Set<Rule> recipientsVisibleToMultipleSources = new HashSet<>();
			List<Set<Rule>> sourcesSeenByRemainingRecipients = rulesVisibleToConnectedSources.keySet().stream()
					.filter((r) -> rulesVisibleToConnectedSources.get(r).size() >= MIN_SOURCE_COUNT_PER_RECIPIENT)
					.peek((r) -> recipientsVisibleToMultipleSources.add(r))
					.map((r) -> rulesVisibleToConnectedSources.get(r))
					.collect(Collectors.toList());
			recipientsVisibleToMultipleSources.retainAll(distinctRulesAtSize(sources.size()));
			Map<Rule,Integer> sourceCounts = countingUnion(sourcesSeenByRemainingRecipients);
			
			if(recipientsVisibleToMultipleSources.size() >= sources.size() 
					&& sourceCounts.size() >= sources.size() 
					&& sourceCounts.keySet().stream().allMatch((r) -> sourceCounts.get(r) >= MIN_RECIPIENT_COUNT_PER_SOURCE)){
				return new ComboGen<>(recipientsVisibleToMultipleSources, sources.size(), sources.size());
			}
		}
		return NO_COMBOS;
	}
	
	private static final ComboGen<Rule> NO_COMBOS = new ComboGen<>(Collections.emptyList(), 0, 0);
	
	private void addVisibles(Map<Rule,Set<Rule>> map, Rule seer){
		for(Rule visible : seer.visibleRules()){
			if(map.containsKey(visible)){
				map.get(visible).add(seer);
			} else{
				Set<Rule> value = new HashSet<>();
				value.add(seer);
				map.put(visible, value);
			}
		}
	}
	
	private static <T> void addAll(Map<T,Integer> map, Collection<? extends T> collection){
		for(T t : collection){
			map.put(t, map.containsKey(t) 
					? 1+map.get(t) 
					: 1 );
		}
	}
	
	private Set<Claim> areSledgehammerScenario(Collection<Rule> sources, Collection<Rule> recipients){
		ToolSet<Claim> sourceClaims = sideEffectUnion(sources,false);
		ToolSet<Claim> recipClaims = sideEffectUnion(recipients,false);
		
		if(recipClaims.hasProperSubset(sourceClaims)){
			recipClaims.removeAll(sourceClaims);
			return recipClaims;
		} else{
			return null;
		}
	}
	
	/**
	 * <p>Sets all the Claims in {@code claimsToSetFalse} false.</p>
	 * @param claimsToSetFalse the Claims to be set false
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	static SolutionEvent resolve(Set<Claim> claimsToSetFalse, Collection<? extends Fact> src, Collection<? extends Fact> recip, Resolve res){
		SolutionEvent time = res.resolve(claimsToSetFalse, src, recip);
		claimsToSetFalse.stream().forEach((c)->c.setFalse(time));
		return time;
	}
	
	static interface Resolve{
		public SolutionEvent resolve(Set<Claim> falsified, Collection<? extends Fact> src, Collection<? extends Fact> recip);
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
	private SolutionEvent regionSpeciesPairProcessing(){
		
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
	
	public static final Predicate<Rule> IS_CELL = (r) -> r.getType() == RuleType.CELL;
	public static final Predicate<Rule> IS_BOX = (r) -> r.getType() == RuleType.BOX;
	public static final Predicate<Rule> IS_COLUMN = (r) -> r.getType() == RuleType.COLUMN;
	public static final Predicate<Rule> IS_ROW = (r) -> r.getType() == RuleType.ROW;
	
	public static final Function<Sudoku,NCuboid<Integer>> STD_NCUBOID_SRC = (s)->new NCuboid<>(dimSource.apply(s));
	public static final Function<Sudoku,NCuboid<Integer>> ALT_NCUBOID_SRC = (s)->new NCuboid<>(dimSource.apply(s), IntStream.range(0,s.magnitude()).mapToObj((i)->(Integer)i).collect(Collectors.toList()));
	
	private static enum TypePair{
		CELL_COL(STD_NCUBOID_SRC, IS_CELL, IS_COLUMN, (r,l) -> l.get(0) == r.stream().findFirst().get().getX()), 
		CELL_ROW(STD_NCUBOID_SRC, IS_CELL, IS_ROW,    (r,l) -> l.get(0) == r.stream().findFirst().get().getY()), 
		CELL_BOX(STD_NCUBOID_SRC, IS_CELL, IS_BOX,    (r,l) -> l.get(0).equals(boxIndex(r))), 
		BOX_ROW (ALT_NCUBOID_SRC, IS_BOX,  IS_ROW,    (r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxY(r)), 
		BOX_COL (ALT_NCUBOID_SRC, IS_BOX,  IS_COLUMN, (r,l) -> l.get(0) == r.stream().findFirst().get().getZ() && l.get(1) == boxX(r)), 
		ROW_COL (STD_NCUBOID_SRC, IS_ROW,  IS_COLUMN, (r,l) -> l.get(0) == r.stream().findFirst().get().getZ());
		
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
							s.factStream().map((f)->(Rule)f).filter(isTypeA.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList()), 
							s.factStream().map((f)->(Rule)f).filter(isTypeB.and((r) -> ruleIsDim.test(r,list))).collect(Collectors.toList())))
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
		
		for(Collection<T> collection : collections){
			addAll(result, collection);
		}
		
		return result;
	}
	
	/**
	 * <p>A {@link java.util.HashMap HashMap} whose {@link HashMap#get(Object) get} method 
	 * automatically checks for the argument's existence in the Map and adds a mapping 
	 * from the specified Rule to a Set of that Rule's {@link Rule#visibleRules() visible Rules} 
	 * to the Map.</p>
	 * @author fiveham
	 *
	 */
	private class VisibleCache extends HashMap<Pair<Rule,Integer>,Set<Rule>>{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -1935783525022691906L;

		@Override
		public Set<Rule> get(Object o){
			if(containsKey(o)){
				return super.get(o);
			} else if(o instanceof Pair){
				Pair<?,?> pair = (Pair<?,?>)o;
				if(pair.getA() instanceof Rule && pair.getB() instanceof Integer){
					@SuppressWarnings("unchecked")
					Pair<Rule,Integer> pri = (Pair<Rule,Integer>) pair;
					Rule r = pri.getA();
					Integer i = pri.getB();
					
					Set<Rule> result = r.visibleRules();
					result.retainAll(distinctRulesAtSize(i));
					
					super.put(pri, result);
					return result;
				}
			}
			
			throw new IllegalArgumentException("specified key must be a Pair<Rule,Integer>");
		}
		
		public Set<Rule> get(Rule rule, int size){
			Pair<Rule,Integer> pair = new Pair<>(rule,size);
			if(containsKey(pair)){
				return super.get(pair);
			} else{
				Set<Rule> result = pair.getA().visibleRules();
				result.retainAll(distinctRulesAtSize(pair.getB()));
				
				super.put(pair, result);
				return result;
			}
		}
	}
	
	/**
	 * <p>Represents an event (group) when a valid sledgehammer 
	 * has been found and is being resolved.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventSledgehammer extends SolutionEvent{
		
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