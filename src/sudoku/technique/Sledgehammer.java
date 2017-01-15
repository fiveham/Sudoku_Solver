package sudoku.technique;

import common.Sets;
import common.BackedSet;
import common.ComboGen;
import common.Pair;
import common.ToolSet;
import common.Universe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sudoku.Claim;
import sudoku.Debug;
import sudoku.Fact;
import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

//TODO describe sledgehammer solution scenarios and their stipulations in the class javadoc
/**
 * <p>The sledgehammer technique for solving sudoku puzzles is defined at
 * http://onigame.livejournal.com/18580.html and at http://onigame.livejournal.com/20626.html and
 * uses one collection of specific statements of the rules of a sudoku network against another
 * collection of such statements to determine that certain claims about that network are false.</p>
 * <p>The sledgehammer technique relies on the fact that a sudoku puzzle can be interpreted as a
 * graph of {@link Claim truth-claims} about the values of the cells ("Cell x,y, has value z.")
 * connected with {@link Rule groupings of those truth-claims} of which exactly one claim from a
 * given grouping is true and all the rest are false.</p> <p>These groupings of claims are pedantic
 * and precise statements of the general rules that specify what makes a solution to a sudoku target
 * valid. For example, the rule that any row contains each value exactly once pedantically expands
 * into 81 statements in a 9x9 puzzle: 9 statements about an individual row, each of which is
 * actually 9 statements specifying a particular value: "Row y has value z in exactly one cell." The
 * rule that any cell contains exactly one value similarly becomes 81 pedantic statements: "For cell
 * x,y, only one z value is correct."</p> <p>The sledgehammer technique generalizes a great number
 * of analysis techniques including <ul><li>naked singles</li> <li>hidden singles</li> <li>naked
 * pairs/triples/etc.</li> <li>x-wing, swordfish, jellyfish, etc.</li> <li>xy-wing, xyz-wing,
 * etc.</li> </ul></p> <p>This is achieved by modeling those situation as the collapse of several
 * "recipient" rules onto several "source" rules so that all the claims belonging to the recipient
 * rules but not belonging to any of the source rules are known to be false. No matter which
 * (viable) set of claims among the source rules is the source rules' set of true claims, all the
 * recipient rules' claims that aren't also source claims must be false.</p> <p>Sledgehammer
 * solutions have a {@code size} equal to the number of source rules in the given solution scenario.
 * This is also the minimum number of recipient rules in a sledgehammer of that {@code size}. In
 * this implementation, it is only allowed for there to be exactly {@code size} recipients in a
 * given sledgehammer scenario. This is because any sledgehammer scenario with more recipients than
 * sources can be reinterpreted with no loss of veracity as multiple sledgehammers with exactly
 * {@code size} recipients.</p> <p>In this implementation, each source rule must intersect at least
 * two recipient rules because otherwise either the one-recipient source rule does not have all its
 * claims accounted for and the overall sledgehammer arrangement is not actionable, or the
 * one-recipient source does have all its claims accounted for by one recipient, in which case the
 * sledgehammer being examined can be broken down into two or more smaller sledgehammers. This
 * implementation constructs sledgehammers starting at the smallest possible non-trivial size and
 * increasing the size of the sledgehammers it looks for; so, the latter case should never be
 * available for action by this implementation of this technique at all.</p> <p>In this
 * implementation, each recipient rule must intersect at least two source rules because, if it only
 * intersects one of the sources, then either <ul> <li>If that source only intersects one recipient
 * (this recipient), then the overall sledgehammer in question can be broken into multiple
 * smaller-size sledgehammers, in which case those sledgehammers will already have been explored by
 * this implementation.</li> <li>If that source intersects multiple recipient rules, then any
 * solution-state among the sources that doesn't verify one of the Claims shared by this recipient
 * and its source should merely disjoin the recipient from its source, leaving it with exactly those
 * of its Claims that were never elements of the recipient's source rule. The contradicts how a
 * sledgehammer should work, which is to remove all the non-source Claims of all the
 * recipients.</li> </ul></p> <p>In general we can say that sources and recipients must connect with
 * at least two rules of the complementary role because the resolution of a sledgehammer scenario is
 * an identity statement: "These source Rules <i>are</i> these recipient Rules." As such, any one of
 * the involved rules connected to only one of the rules of the complementary role must be that
 * exact other rule once it is subjected to the identity-sharing stipulation of the sledgehammer. If
 * a simple "this Rule is this other Rule" statement is true, then it proper to identify and resolve
 * that smaller-scale situation as a separate concern not involved with this implementation.</p>
 * <p>A sledgehammer of size 1 is trivial and not accounted for by this implementation. Since a
 * sledgehammer of size 1 has exactly one source rule, detection and resolution of size-1
 * sledgehammers is automated. Rules {@link Rule#validateState(TechniqueEvent) check themselves} for
 * size-1 sledgehammer actionability every time some Claims are removed from them, and if that Rule
 * is a proper subset of any of the rules that share any of its Claims, the superset is collapsed
 * onto the subset before the method that removed some Claims from the subset returns.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class Sledgehammer extends AbstractTechnique<Sledgehammer> {
	
	public static final int MIN_RECIPIENT_COUNT_PER_SOURCE = 2;
	public static final int MIN_SOURCE_COUNT_PER_RECIPIENT = 2;
	
    /**
     * <p>The subset-based principle of Sledgehammer scenarios can apply to source Rules of sizes 1
     * and 2, but those sizes are accounted for fully by
     * {@link Rule#validateState automatic resolution} and {@link ColorChain xy-chain analysis}
     * respectively.</p>
     */
	private static final int MIN_SLEDGEHAMMER_SIZE = 3;
	
	private final VisibleCache visibleCache;
	
	private final Collection<Fact> distinctRules;
	private final Map<Integer,List<Fact>> distinctRulesBySledgehammerSize;
	private final Map<Integer,List<Fact>> distinctSourcesBySledgehammerSize;
	
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
		sh.distinctRulesBySledgehammerSize.putAll(mapRulesBySize(sh.distinctRules.stream(), Fact::size));
		sh.distinctSourcesBySledgehammerSize.putAll(mapRulesBySize(sh.distinctRules.stream(), Sledgehammer::sledgehammerSizeIfSource));
	}
	
    /**
     * <p>Collects a list of distinct Rules from a Sudoku into a Map partitioning them according to
     * the minimum size of a sledgehammer in which those Rules can serve.</p>
     * @param ruleStream
     * @param sledgehammerSize given a Rule from {@code ruleStream}, outputs the minimum size of a
     * sledgehammer solution scenario in which that Rule can serve
     * @return
     */
	private static Map<Integer,List<Fact>> mapRulesBySize(Stream<Fact> ruleStream, Function<? super Fact, ? extends Integer> sledgehammerSize){
		return ruleStream.collect(Collectors.toMap(
				sledgehammerSize, 
				(r) -> {
					List<Fact> result = new ArrayList<>(1);
					result.add(r);
					return result;
				}, 
				Sets::mergeCollections));
	}
	
    /**
     * <p>Returns the minimum size of a sledgehammer solution scenario for which {@code r} could be
     * a source. The size is usually {@code r.size()}, but in some cases is less, if {@code r} can
     * have two or more of its Claims accounted for by a single other Rule. In that case,
     * combinations of {@code r}'s {@link Rule#visibleRules() visible Rules} are explored, starting
     * at the smallest possible size and working up until a combination of visible Rules around
     * {@code r} is found that accounts for all of {@code r}'s Claims.</p>
     * @param r the Rule whose minimum acceptable Sledgehammer size in which {@code r} can act as a
     * source is determined
     * @throws IllegalStateException if {@code r} has more {@link Rule#visibleRules() visible Rules}
     * than it would have if none of its Claims shared any Rules in common, which is impossible, or
     * if no combination of Rules visible to {@code r} with combination-size not greater than
     * {@code r.size()} accounts for all {@code r}'s Claims
     * @return the minimum size of a sledgehammer solution scenario for which {@code r} could be a
     * source
     */
	private static int sledgehammerSizeIfSource(Fact r){
		
		//Maximum number of recipient rules attachable to this Rule r: 
		//Inclusive upper bound on the minimum size of Sledgehammer where r is a source.
		int upperBound = r.size();
		
		//number of Rules visible to r if none of r's Claims share any 
		//Rule other than r.
		int rulesOtherThanR = Claim.INIT_OWNER_COUNT - 1;
		int noLoopVisibleRuleCount = r.size() * rulesOtherThanR;
		
		Set<Fact> visible = r.visible();
		int actualVisibleCount = visible.size();
		
		if(noLoopVisibleRuleCount == actualVisibleCount){
			return upperBound;
		} else if(noLoopVisibleRuleCount > actualVisibleCount){
			for(List<Fact> visibleCombo : new ComboGen<>(visible, MIN_RECIPIENT_COUNT_PER_SOURCE, r.size())){
				if(unionContainsAll(visibleCombo, r)){
					return visibleCombo.size();
				}
			}
			throw new IllegalStateException("Could not determine true min Sledgehammer size for "+r);
		} else{
			throw new IllegalStateException("noLoopVisibleRuleCount ("+noLoopVisibleRuleCount+") < actualVisibleCount ("+actualVisibleCount+")");
		}
	}
	
  private static boolean unionContainsAll(List<Fact> visibleCombo, Fact r){
    return r.parallelStream()
        .allMatch(
            (c) -> visibleCombo.parallelStream()
                .anyMatch(
                    (f) -> f.contains(c)));
  }
	
    /**
     * <p>Iterates over all the possible pairs of source-combination and recipient-combination,
     * checks each pair for validity as a sledgehammer solution scenario, and, if the scenario is
     * valide, resolves it and then returns a SolutionEvent detailing the event and any subsequent
     * automatic resolution events.</p>
     * @return a SolutionEvent detailing the sledgehammer solution event that this method found and
     * resolved and detailing any subsequent automatic resolution events as
     * {@link common.time.Time#children() children} of the sledgehammer event, or null if this
     * method made no changes to its {@code target}
     */
	@Override
	protected TechniqueEvent process(){
		return processByGrowth();
	}
	
    /**
     * <p>Finds sledgehammer solution scenarios by growing them from a seed source Rule, which
     * limits the combinatorial space to be explored to find all sledgehammer scenarios.</p>
     * <p>Iterates over all possible sledgehammer sizes for this {@code Technique}'s {@code target}.
     * For each size, iterates over all the Rules that can serve as sources in sledgehammers of that
     * size. All other possible source Rules at that size that have not served as a seed Rule at
     * that size are used as a pool ("&amp;" mask) of which any other potential sources near the
     * current seed Rule must be a member.</p>
     * @return a SolutionEvent describing the changes made to the puzzle, or null if no changes were
     * made
     */
	private TechniqueEvent processByGrowth(){
		if(distinctRules.isEmpty()){
			populateDistinctRules(this);
		}
		
		Debug.log(target.nodeStream().findFirst().get().getPuzzle().possibilitiesToString()); //DEBUG
		
		for(int size = MIN_SLEDGEHAMMER_SIZE; size <= target.size()/2 && builtSrcComboAtLastSize; ++size){
			
			Debug.log("At srcCombo size "+size); //DEBUG
			
			builtSrcComboAtLastSize = false;
			TechniqueEvent event = exploreSourceCombos(
					new ArrayList<>(0), 
					new HashSet<>(0), 
					new HashSet<>(0), 
					size, 
					distinctSourcesAtSize(size));
			if(event != null){
				return event;
			}
		}
		
		return null;
	}
	
	private Universe<Fact> factUniverse(){
		return target.nodeStream().findFirst().get().getPuzzle().factUniverse();
	}
	
    /**
     * <p></p>
     * @param oldSrcCombo the established sources in the source-combinations being built
     * @param size the number of sources in the source-combinations being built
     * @param sourceMask a collection of Rules that are capable of serving (and allowed to serve) as
     * sources in a sledgehammer at this {@code size} and having the construction history of
     * {@code initialSources} so that potential sources that have been fully explored can be skipped
     * @param whenBuiltSourceCombo an action to be performed when a source combo is successfully
     * built at the specified {@code size}
     * @return a {@link TechniqueEvent} describing the resolution of a sledgehammer built using
     * {@code initialSources} as-is or as a starting population, or {@code null} if no resolvable
     * sledgehammer is found
     */
	private TechniqueEvent exploreSourceCombos(List<Fact> oldSrcCombo, Set<Fact> oldVisCloud, Set<Fact> oldVisVisCloud, int size, Set<Fact> sourceMask){
		if(oldSrcCombo.size() < size){
			Set<Fact> localSourceMask = new BackedSet<>(factUniverse(), sourceMask);
			for(Fact newSource : sourcePool(oldVisVisCloud, sourceMask, size, oldSrcCombo.isEmpty())){
				localSourceMask.remove(newSource); //mask for next iteration level

				Set<Fact> newVisVisCloud = null;
				Set<Fact> newVisCloud = null;
				List<Fact> newSrcCombo;
				{
					newSrcCombo = new ArrayList<>(oldSrcCombo.size()+1);
					newSrcCombo.addAll(oldSrcCombo);
					newSrcCombo.add(newSource);
				}
				
				//if newSrcCombo.size() == size, newVisCloud and newVisVisCloud won't get used
				if(newSrcCombo.size() < size){
					Set<Fact> visibleToNewSource = visibleCache.get(newSource, size); 
					
					newVisCloud = new BackedSet<>(factUniverse(), oldVisCloud);
					newVisCloud.addAll(visibleToNewSource);
					
					Set<Fact> visVis = new BackedSet<>(factUniverse(), oldVisVisCloud);
					visibleToNewSource.stream()
							.map((v) -> visibleCache.get(v, size))
							.forEach(visVis::addAll);
					visVis.removeAll(newSrcCombo);
					visVis.removeAll(newVisCloud);
					newVisVisCloud = visVis;
				}
				
				TechniqueEvent event = exploreSourceCombos(
						newSrcCombo, 
						newVisCloud, 
						newVisVisCloud, 
						size, 
						localSourceMask);
				if(event != null){
					return event;
				}
			}
			return null;
		} else if(oldSrcCombo.size() == size){
			builtSrcComboAtLastSize = true;
			return exploreRecipientCombos(oldSrcCombo);
		} else{
			throw new IllegalStateException("Current source-combination size greater than prescribed sledgehammer size: "+oldSrcCombo.size() + " > " + size);
		}
	}
	
	private Set<Fact> sourcePool(Set<Fact> initVisVisibles, Set<Fact> sourceMask, int size, boolean isEmpty){
		if(isEmpty){
			return sourceMask;
		} else{
			Set<Fact> result = new BackedSet<>(factUniverse(), initVisVisibles);
			result.retainAll(sourceMask);
			return result;
		}
	}
	
    /**
     * <p></p>
     * @param srcCombo a closely-connected, disjoint collection of distinct Rules suitable for use
     * as sources in a sledgehammer solution scenario
     * @param distinctRuleMask
     * @return a SolutionEvent describing the changes made to the puzzle, or null if no changes were
     * made
     */
	private TechniqueEvent exploreRecipientCombos(List<Fact> srcCombo){
		//For each recipient combo derivable from srcCombo that disjoint, closely connected source combo
		for(List<Fact> recipientCombo : recipientCombinations(srcCombo)){
			
			//If the source and recipient combos make a valid sledgehammer scenario
			Set<Claim> claimsToSetFalse = areSledgehammerScenario(srcCombo, recipientCombo);
			if(claimsToSetFalse != null && !claimsToSetFalse.isEmpty()){
				return resolve(claimsToSetFalse, srcCombo, recipientCombo);
			}
		}
		return null;
	}
	
	private final Map<Integer,List<Fact>> distinctRulesAtSizeCache = new HashMap<>();
	
	private List<Fact> distinctRulesAtSize(int size){
		return distinctRulesOfTypeAtSize(size, distinctRulesAtSizeCache, distinctRulesBySledgehammerSize, ArrayList::new);
	}
	
	private final Map<Integer,Set<Fact>> distinctSourcesAtSizeCache = new HashMap<>();
	
	private Set<Fact> distinctSourcesAtSize(int size){
		return distinctRulesOfTypeAtSize(size, distinctSourcesAtSizeCache, distinctSourcesBySledgehammerSize, factUniverse()::back);
	}
	
	private static <C extends Collection<Fact>> C distinctRulesOfTypeAtSize(int size, Map<Integer,C> cache, Map<Integer,List<Fact>> rulesOfType, Supplier<C> supplier){
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
     * <p>Returns a collection of the Rules of {@code target} such that if there are any Rules in
     * {@code target} that are equal as {@code Set}s, then only one of them appears.</p>
     * @param target the Sudoku whose distinct Rules are identified
     * @return the distinct Rules found in {@code target}
     */
	private static Collection<Rule> distinctRules(Sudoku target){
		class RuleWrap{
			private final Rule wrapped;
			RuleWrap(Rule rule){
				this.wrapped = rule;
			}
			
			private Rule wrapped(){
				return wrapped;
			}
			
			@Override
			public int hashCode(){
				return wrapped.parallelStream()
				    .mapToInt(Object::hashCode)
				    .reduce(0, Integer::sum);
			}
			
			@Override
			public boolean equals(Object o){
				if(o instanceof RuleWrap){
					RuleWrap rw = (RuleWrap) o;
					return wrapped.size() == rw.wrapped.size() && wrapped.containsAll(rw.wrapped);
				}
				return false;
			}
		}
		return target.factStream()
				.map(Rule.class::cast)
				.map(RuleWrap::new)
				.distinct()
				.map(RuleWrap::wrapped)
				.collect(Collectors.toList());
	}
	
    /**
     * <p>Returns a set of all the Rules that intersect at least two of the Rules in
     * {@code sources}.</p> <p>The requirement that no source Rule should be considered for use as a
     * recipient Rule is accounted for implicitly by the disjointness testing this method performs
     * on the {@code sources}, accumulating a cloud of Rules visible to the {@code sources} and
     * making sure that none of the {@code sources} is in that cloud. That cloud is later used as
     * the basis of the collection of Rules sent to the constructor of the ComboGen that is
     * returned, except in cases where {@link #NO_COMBOS no recipient-combinations} should be
     * iterated over at all.</p>
     * @param sources a collection of Rules to be used as an originating combination for a
     * sledgehammer solution event
     * @param distinctRuleMask a collection of Rules that are allowed to be used in the returned
     * ComboGen
     * @return a set of all the Rules that intersect any of the Rules in {@code sources}, excluding
     * the Rules in {@code sources}.
     */
	private ComboGen<Fact> recipientCombinations(List<Fact> sources){
		
		List<Fact> unconnectedSources = new ArrayList<>(sources);
		int prevUnconSrcCount = unconnectedSources.size();
		
		//seed the "connected" area with a source Rule
		Map<Fact,Set<Fact>> rulesVisibleToConnectedSources = new HashMap<>();
		addVisibles(rulesVisibleToConnectedSources, unconnectedSources.remove(unconnectedSources.size()-1));
		
		//connect as many of the sources together as possible
		while(unconnectedSources.size() != prevUnconSrcCount){
			prevUnconSrcCount = unconnectedSources.size();
			
			//try to connect each currently unconnected source to the "connected" area
			for(Iterator<Fact> iter = unconnectedSources.iterator(); iter.hasNext();){
				Fact currentUnconnectedSource = iter.next();
				
				//No source Rule can be directly visible to another source Rule
				if(rulesVisibleToConnectedSources.containsKey(currentUnconnectedSource)){
					return NO_COMBOS;
				}
				
				//If the current Rule is 4 steps away from at least one other source Rule, 
				Set<Fact> visibleToCurrentRule = visibleCache.get(currentUnconnectedSource, sources.size());
				if(!Collections.disjoint(visibleToCurrentRule, rulesVisibleToConnectedSources.keySet())){
					addVisibles(rulesVisibleToConnectedSources, currentUnconnectedSource);
					iter.remove();
				}
			}
		}
		
		if(unconnectedSources.size() == 0){
			Set<Fact> recipientsVisibleToMultipleSources = new HashSet<>();
			List<Set<Fact>> sourcesSeenByRemainingRecipients = rulesVisibleToConnectedSources.keySet().stream()
					.filter((r) -> rulesVisibleToConnectedSources.get(r).size() >= MIN_SOURCE_COUNT_PER_RECIPIENT)
					.peek(recipientsVisibleToMultipleSources::add)
					.map(rulesVisibleToConnectedSources::get)
					.collect(Collectors.toList());
			recipientsVisibleToMultipleSources.retainAll(distinctRulesAtSize(sources.size()));
			Map<Fact,Integer> sourceCounts = Sets.countingUnion(sourcesSeenByRemainingRecipients);
			
			if(recipientsVisibleToMultipleSources.size() >= sources.size() 
					&& sourceCounts.size() >= sources.size() 
					&& sourceCounts.keySet().stream().allMatch((r) -> sourceCounts.get(r) >= MIN_RECIPIENT_COUNT_PER_SOURCE)){
				return new ComboGen<>(recipientsVisibleToMultipleSources, sources.size(), sources.size());
			}
		}
		return NO_COMBOS;
	}
	
	private static final ComboGen<Fact> NO_COMBOS = new ComboGen<>(Collections.emptyList(), 0, 0);
	
	private void addVisibles(Map<Fact,Set<Fact>> map, Fact seer){
		for(Fact visible : seer.visible()){
			if(map.containsKey(visible)){
				map.get(visible).add(seer);
			} else{
				Set<Fact> value = new HashSet<>();
				value.add(seer);
				map.put(visible, value);
			}
		}
	}
	
	private Set<Claim> areSledgehammerScenario(Collection<Fact> sources, Collection<Fact> recipients){
		Set<Claim> sourceClaims = sources.stream()
		    .map(HashSet<Claim>::new)
		    .reduce(
		        new HashSet<>(), 
		        Sets::mergeCollections);
		ToolSet<Claim> recipClaims = recipients.stream()
		    .map(ToolSet<Claim>::new)
		    .reduce(
		        new ToolSet<>(), 
		        Sets::mergeCollections);
		
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
   * @return a SolutionEvent describing the changes made to the puzzle, or null if no changes were
   * made
   */
	private static TechniqueEvent resolve(Set<Claim> claimsToSetFalse, Collection<? extends Fact> src, Collection<? extends Fact> recip){
		return new SolveEventSledgehammer(claimsToSetFalse, src, recip).falsifyClaims();
	}
	
  /**
   * <p>A {@link java.util.HashMap HashMap} whose {@link HashMap#get(Object) get} method
   * automatically checks for the argument's existence in the Map and adds a mapping from the
   * specified Rule to a Set of that Rule's {@link Rule#visibleRules() visible Rules} to the
   * Map.</p>
   * @author fiveham
   * @author fiveham
	 *
	 */
	private class VisibleCache extends HashMap<Pair<Fact,Integer>,Set<Fact>>{
		
		private static final long serialVersionUID = -8687502832663252756L;
		
		@Override
		public Set<Fact> get(Object o){
			if(containsKey(o)){
				return super.get(o);
			} else if(o instanceof Pair){
				Pair<?,?> pair = (Pair<?,?>)o;
				if(pair.getA() instanceof Rule && pair.getB() instanceof Integer){
					@SuppressWarnings("unchecked")
					Pair<Fact,Integer> pri = (Pair<Fact,Integer>) pair;
					Fact r = pri.getA();
					Integer i = pri.getB();
					
					Set<Fact> result = r.visible();
					result.retainAll(distinctRulesAtSize(i));
					
					super.put(pri, result);
					return result;
				}
			}
			
			throw new IllegalArgumentException("specified key must be a Pair<Rule,Integer>");
		}
		
		public Set<Fact> get(Fact rule, int size){
			Pair<Fact,Integer> pair = new Pair<>(rule,size);
			if(containsKey(pair)){
				return super.get(pair);
			} else{
				Set<Fact> result = pair.getA().visible();
				result.retainAll(distinctRulesAtSize(pair.getB()));
				
				super.put(pair, result);
				return result;
			}
		}
	}
	
  /**
   * <p>Represents an event (group) when a valid sledgehammer has been found and is being
   * resolved.</p>
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
		protected String toStringStart(){
			return "Sledgehammer scenario: "+src+" ARE "+recip;
		}
	}
	
	@Override
	public Sledgehammer apply(Sudoku sudoku){
		return new Sledgehammer(sudoku);
	}
}
