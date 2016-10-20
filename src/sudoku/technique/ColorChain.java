package sudoku.technique;

import common.ComboGen;
import common.Pair;
import common.graph.Graph;
import common.graph.Wrap;
import common.graph.BasicGraph;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
import sudoku.time.FalsifiedTime;
import sudoku.time.TechniqueEvent;

/**
 * <p>The color-chain technique exploits the fact that a Rule with 
 * only two connected Claims is analogous to a {@code xor} operation. 
 * A collection of interconnected two-Claim Rules, regardless of 
 * the size and shape of such a network, has only two possible 
 * solution-states.</p>
 * 
 * @author fiveham
 * 
 */
public class ColorChain extends AbstractTechnique {
	
	/**
	 * <p>Constructs a ColorChain that works to solve the 
	 * specified {@code target}.</p>
	 * @param target the Puzzle that this Technique works 
	 * to solve.
	 */
	public ColorChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	private static final List<BiFunction<ColorChain,Collection<Graph<ColorClaim>>,TechniqueEvent>> SUBTECHNIQUES = Arrays.asList(
			ColorChain::visibleColorContradiction, 
			ColorChain::bridgeCollapse, 
			ColorChain::xyChain);
	
	/**
	 * <p>Imitates the slide structure of {@link Solver#solve() Solver.solve()}, 
	 * with subtechniques for {@link #visibleColorContradiction(Collection) color-contradiction} 
	 * and {@link #bridgeCollapse(Collection) chain-chain interaction}. Subtechniques are applied 
	 * in sequence.</p>
	 * @return a TechniqueEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	@Override
	protected TechniqueEvent process(){
		Collection<Graph<ColorClaim>> chains = generateChains();
		for(BiFunction<ColorChain, Collection<Graph<ColorClaim>>,TechniqueEvent> test : SUBTECHNIQUES){
			TechniqueEvent result = test.apply(this,chains);
			if(result != null){
				return result;
			}
		}
		
		return null;
	}
	
	/**
	 * <p>Isolates those Rules in the target that have two Claims, makes 
	 * a Graph of their Claims, where two Claims share an edge if they 
	 * share a Rule as an element, and returns a collection of that Graph's 
	 * connected components, each of which is a xor-chain with two possible 
	 * solution-states.</p>
	 * 
	 * @return a collection of the xor-chains that exist in the target 
	 * at the time when this method is called
	 */
	private Collection<Graph<ColorClaim>> generateChains(){
		List<Fact> xorRules = target.nodeStream()
				.filter(Fact.class::isInstance)
				.map(Fact.class::cast)
				.filter(Fact::isXor)
				.collect(Collectors.toList());
		return new BasicGraph<ColorClaim>(Wrap.wrap(xorRules,ColorClaim::new))
				.addContractEventListenerFactory(colorSource)
				.connectedComponents();
	}

	private final ColorSource colorSource = new ColorSource();
	
	/**
	 * <p>A generator and manager of ints to be used as colors for Claims 
	 * in xor-chains.</p>
	 * 
	 * <p>Use {@code get()} to get the current color with the current sign, 
	 * use {@code nextColor()} when beginning to color a new xor-chain, and 
	 * use {@code invertColor()} to change the sign of the colors returned 
	 * by subsequent calls to {@code get()}.</p>
	 * @author fiveham
	 *
	 */
	private static class ColorSource implements Consumer<Set<ColorClaim>>, Supplier<Consumer<Set<ColorClaim>>>{
		
		public static final int INIT_COLOR = 1;
		
		private boolean positive;
		private int color;
		ColorSource(){
			this.color = INIT_COLOR;
			this.positive = true;
		}
		
		/**
		 * <p>Changes this ColorSource's color sign so that 
		 * subsequent calls to {@code get()} return a color 
		 * with the sign opposite of the sign returned by 
		 * previous calls to {@code get()}.</p>
		 */
		void invertColor(){
			positive = !positive;
		}
		
		/**
		 * <p>Returns the current color with the current sign.</p>
		 * @return the current color with the current sign
		 */
		int getColor(){
			return positive ? color : -color;
		}
		
		/**
		 * <p>Increments the internal unsigned color and resets 
		 * the internal color-sign to positive.</p>
		 */
		void nextColor(){
			++color;
			positive = true;
		}
		
		@Override
		public void accept(Set<ColorClaim> cuttingEdge) {
			cuttingEdge.stream().forEach((e)->e.setColor(getColor()));
			invertColor();
		}
		
		@Override
		public Consumer<Set<ColorClaim>> get() {
			nextColor();
			return this;
		}
	}
	
	/**
	 * <p>Any claims in the {@code target} that {@link Claim#visibleClaims() can see} 
	 * Claims with opposite colors from the same chain must be false, no matter 
	 * which color from that chain is true and which is false.</p>
	 * @return a TechniqueEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	private TechniqueEvent visibleColorContradiction(Collection<Graph<ColorClaim>> chains){
		for(Graph<ColorClaim> chain : chains){
			TechniqueEvent result = visibleColorContradiction(chain);
			if( result != null ){
				return result;
			}
		}
		return null;
	}
	
	/**
	 * <p>Detects and {@link Claim#setFalse() sets false} Claims that 
	 * can see both of the colors in {@code concom}.</p>
	 * 
	 * <p>In a xor-chain, either all the positive-colored Claims are true 
	 * and all the negative-colored Claims are false or all the positive-
	 * colored Claims are false and all the negative-colored Claims are 
	 * true. </p>
	 * @param concom a xor-chain
	 * @return a TechniqueEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	private TechniqueEvent visibleColorContradiction(Graph<ColorClaim> concom){
		Set<Claim> visibleToPositives = concom.nodeStream()
				.filter(ColorClaim::posColor)
				.map(ColorClaim::wrapped)
				.map(Claim::visible)
				.collect(Sledgehammer.massUnionCollector());
		Set<Claim> visibleToNegatives = concom.nodeStream()
				.filter(ColorClaim::negColor)
				.map(ColorClaim::wrapped)
				.map(Claim::visible)
				.collect(Sledgehammer.massUnionCollector());
		
		Set<Claim> claimsToSetFalse = visibleToPositives;
		claimsToSetFalse.retainAll(visibleToNegatives);
		if(!claimsToSetFalse.isEmpty()){
			return new SolveEventColorContradiction(claimsToSetFalse).falsifyClaims();
		}
		
		return null;
	}

	/**
	 * <p>A time node encapsulating the events that occur in searching 
	 * for and falsifying Claims that see both colors of a xor-chain.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventColorContradiction extends TechniqueEvent{
		private SolveEventColorContradiction(Set<Claim> falseClaims){
			super(falseClaims);
		}
		
		@Override
		protected String toStringStart(){
			return "Visibile-color contradiction";
		}
	}
	
	public static final int CHAINS_FOR_BRIDGE = 2;
	
	private final Map<Graph<ColorClaim>, Set<Fact>> massUnionCache = new HashMap<>();
	
	private Set<Fact> cacheMassUnion(Graph<ColorClaim> chain){
		if(massUnionCache.containsKey(chain)){
			return massUnionCache.get(chain);
		} else{
			Set<Fact> result = Collections.unmodifiableSet(
					Sledgehammer.massUnion(
							chain.nodeStream()
									.map(ColorClaim::wrapped)
									.collect(Collectors.toList())));
			massUnionCache.put(chain, result);
			return result;
		}
	}
	
	public static final int RULES_FOR_BRIDGE = 2;
	
	/**
	 * <p>Returns a Pair containing a chain to be collapsed and the color of the claims in that 
	 * chain to be set false, or {@code null} if no even bridged chain is found.</p>
	 * 
	 * <p>This method tries to find two single-Rule bridges between the two specified chains and 
	 * determines the step length between the two bridge Rules in either chain. If one 
	 * chain has an even step-length between bridges while the other chain has an odd step 
	 * length between the two bridges, then the chain with the even step-length between bridges 
	 * can be completely collapsed, with the claims from the even-side chain with the color of 
	 * those even-side chain claims that were on the bridges all being set false.</p>
	 * 
	 * @return a Pair containing a chain to be collapsed and the color of the Claims in that 
	 * chain to be set False, or {@code null} if no even bridged chain is found
	 */
	private Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor(List<Graph<ColorClaim>> chains){
		Graph<ColorClaim> chain0 = chains.get(0);
		Graph<ColorClaim> chain1 = chains.get(1);
		
		Set<Fact> chainUnion0 = cacheMassUnion(chain0);
		Set<Fact> chainUnion1 = cacheMassUnion(chain1);
		
		Set<Fact> bridges = new HashSet<>(chainUnion0);
		bridges.retainAll(chainUnion1);
		
		for(List<Fact> bridge : new ComboGen<>(bridges, RULES_FOR_BRIDGE, RULES_FOR_BRIDGE)){
			Fact lane0 = bridge.get(0);
			Fact lane1 = bridge.get(1);
			
			boolean evenDist0 = evenDist(lane0, lane1, chain0);
			boolean evenDist1 = evenDist(lane0, lane1, chain1);
			if( evenDist0 && !evenDist1 ){
				return new Pair<Graph<ColorClaim>,Integer>(chain0, bridgeColor(chain0, lane0, lane1));
			} else if(evenDist1 && !evenDist0){
				return new Pair<Graph<ColorClaim>,Integer>(chain1, bridgeColor(chain1, lane0, lane1));
			}
		}
		return null;
	}

	/**
	 * <p>Returns the number of steps between {@code lane0} and {@code lane1} 
	 * in {@code chain}.</p>
	 * @param lane0 a bridge-lane that intersects {@code chain}
	 * @param lane1 a bridge-lane that intersects {@code chain}
	 * @param chain a xor-chain intersecting {@code lane0} and {@code lane1}
	 * @throws IllegalArgumentException if any of the lanes does not intersect {@code chain}
	 * @return the number of steps between {@code lane0} and {@code lane1} 
	 * in {@code chain}
	 */
	private boolean evenDist(Fact lane0, Fact lane1, Graph<ColorClaim> chain){
		int intersection0 = getBridgeIntersection(lane0, chain).color;
		return intersection0 == getBridgeIntersection(lane1, chain).color;
	}

	/**
	 * <p>Identifies the Claim where {@code lane} and {@code chain} intersect.</p>
	 * @param lane a non-xor Rule that intersects {@code chain}
	 * @param chain a xor-chain
	 * @throws IllegalArgumentException if {@code lane} does not intersect {@code chain}
	 * @return the vertex from {@code chain} where {@code lane} and {@code chain} 
	 * intersect
	 */
	private static ColorClaim getBridgeIntersection(Fact lane, Graph<ColorClaim> chain){
		for(ColorClaim ccFromChain : chain){
			if(lane.contains(ccFromChain.wrapped())){
				return ccFromChain;
			}
		}
		throw new IllegalArgumentException("Specified chain and bridge-lane do not intersect.");
	}

	/**
	 * <p>The number ({@value}) of colors that a xor-chain collapseable as the result 
	 * of chain-chain interaction has on the bridge that mediates that chain-chain 
	 * interaction.</p>
	 * 
	 * <p>A bridge between two chains is constituted by a pair of non-{@code xor} 
	 * Rules each of which has a Claim (neighbor) in each of the two bridged chains.</p>
	 */
	public static final int COLLAPSEABLE_CHAIN_BRIDGE_COLORS = 1;
	
	/**
	 * <p>Returns the int color of the Claims belonging to {@code chain} 
	 * that are on the bridge made of {@code lane0} and {@code lane1}.</p>
	 * @param chain the xor-chain the color of whose Claims on the bridge 
	 * made of {@code lane0} and {@code lane1} is returned
	 * @param lane0 one of the lanes of the bridge
	 * @param lane1 one of the lanes of the bridge
	 * @throws IllegalStateException if {@code chain} has more than one color 
	 * on the bridge, which means {@code chain} has an odd step-length between 
	 * {@code lane0} and {@code lane1}
	 * @return the int color of the Claims belonging to {@code chain} 
	 * that are on the bridge made of {@code lane0} and {@code lane1}
	 */
	private int bridgeColor(Graph<ColorClaim> chain, Fact lane0, Fact lane1){
		Map<Claim,ColorClaim> map = claimToColorMap(chain);
		
		Set<Claim> allBridgeClaims = new HashSet<>(lane0);
		allBridgeClaims.addAll(lane1);
		
		Set<Integer> colorsOnBridge = new HashSet<>();
		for(Claim c : allBridgeClaims){
			if(map.containsKey(c)){
				colorsOnBridge.add(map.get(c).getColor());
			}
		}
		
		if(colorsOnBridge.size() == COLLAPSEABLE_CHAIN_BRIDGE_COLORS){
			return colorsOnBridge.iterator().next();
		}
		throw new IllegalStateException("Should have one color on bridge, but instead have "+colorsOnBridge.size());
	}

	/**
	 * <p>Returns a {@link Map Map} from the Claims in {@code chain} to those 
	 * Claims' colors.</p>
	 * @param chain a xor-chain whose Claims and whose Claims' colors are to 
	 * be mapped
	 * @return a {@link Map Map} from the Claims in {@code chain} to those 
	 * Claims' colors
	 */
	private static Map<Claim,ColorClaim> claimToColorMap(Graph<ColorClaim> chain){
		Map<Claim,ColorClaim> result = new HashMap<>(chain.size());
		
		for(ColorClaim cc : chain){
			result.put(cc.wrapped(), cc);
		}
		
		return result;
	}
	
	/**
	 * <p>Checks pairs of xor-chains for bridge-based interactions that force 
	 * one of the chains to collapse.</p>
	 * 
	 * <p>If, given two chains, there exist two non-xor Rules that each contain 
	 * one Claim belonging to each of the two given chains, then one of the 
	 * chains may be completely collapseable. If the distance in one chain between 
	 * its claims belonging to the bridge Rules is odd and the distance in the 
	 * other chain between its claims belonging to the bridge Rules is even, then 
	 * the chain with the even distance can be collapsed, with the Claims having 
	 * the color of its Claims on the bridge all being false.</p>
	 * 
	 * @return a TechniqueEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	private TechniqueEvent bridgeCollapse(Collection<Graph<ColorClaim>> chains){
		for(List<Graph<ColorClaim>> chainPair : new ComboGen<>(chains, CHAINS_FOR_BRIDGE, CHAINS_FOR_BRIDGE)){
			Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor = evenSideAndFalseColor(chainPair);
			if(evenSideAndFalseColor != null){
				return setChainFalseForColor(evenSideAndFalseColor.getA(), evenSideAndFalseColor.getB());
			}
		}
		
		return null;
	}
	
	/**
	 * <p>Sets false all the Claims in this chain decorated with the specified 
	 * {@code color}.</p>
	 * @param time the contextual time node which needs to store a collection of 
	 * the Claims being set false by this method call 
	 * @param chain a xor-chain of connected two-Claim Rules 
	 * @param color the color of the Claims being set false
	 * @return {@code time}, after the Claims of the specified {@code color} from 
	 * the specified xor-{@code chain} are added to its pool of 
	 * {@link sudoku.time.FalsifiedTime#falsified() falsified Claims}
	 */
	private TechniqueEvent setChainFalseForColor(Graph<ColorClaim> chain, final int color){
		Set<Claim> setFalse = chain.nodeStream()
				.filter((cc)->cc.getColor()==color)
				.map(ColorClaim::wrapped)
				.collect(Collectors.toSet());
		return new SolveEventBridgeCollapse(setFalse).falsifyClaims();
	}
	
	/**
	 * <p>A Time node describing two xor-chains' collapse due 
	 * to {@link #bridgeCollapse(Collection) bridge} interaction.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventBridgeCollapse extends TechniqueEvent{
		private SolveEventBridgeCollapse(Set<Claim> falsified){
			super(falsified);
		}
		
		@Override
		protected String toStringStart(){
			return "Bridge-collapse";
		}
	}
	
	/**
	 * <p>Wraps a Claim and decorates it with an int color.</p>
	 * @author fiveham
	 *
	 */
	private static class ColorClaim implements WrapVertex<Claim,ColorClaim>{
		
		public static final List<Predicate<ColorClaim>> SIGNS = Arrays.asList(
				ColorClaim::posColor, 
				ColorClaim::negColor);
		
		private int color = 0;
		private Claim claim;
		private final List<ColorClaim> neighbors;
		
		ColorClaim(Claim claim){
			this.claim = claim;
			this.neighbors = new ArrayList<>();
		}
		
		@Override
		public Claim wrapped(){
			return claim;
		}
		
		@Override
		public List<ColorClaim> neighbors(){
			return neighbors;
		}
		
		/**
		 * <p>Returns the color of the wrapped Claim.</p>
		 * @return the color of the wrapped Claim
		 */
		int getColor(){
			return color;
		}
		
		public boolean posColor(){
			return color > 0;
		}
		
		public boolean negColor(){
			return color < 0;
		}
		
		/**
		 * <p>Sets the color to {@code color{@code .</p>
		 * @param color the new color
		 */
		void setColor(int color){
			if(this.color==0){
				this.color = color;
			} else{
				throw new IllegalStateException("Cannot change color to "+color+" because color has already been set to "+this.color);				
			}
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof ColorClaim){
				ColorClaim cc = (ColorClaim) o;
				return cc.color == this.color && cc.claim == this.claim;
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return claim.hashCode();
		}
		
		@Override
		public String toString(){
			return "ColorClaim pairing " + color + " with " + claim;
		}
	}
	
	private TechniqueEvent xyChain(Collection<Graph<ColorClaim>> chains){
		
		/*
		 * "False push" refers to a Claim, if true, forcing its visibles to be false 
		 * (pushing falseness to them). All Claims that can see each other have this 
		 * relationship.
		 * 
		 * "True push" refers to a Claim, if false, forcing its visibles to be true 
		 * (pushing trueness to them). Claims have this relationship with Claims that 
		 * they can see through a xor Fact.
		 * 
		 * "Mix push" refers to a push graph (a directed graph) such that nodes of one 
		 * color point to the nodes onto which they push state A (false or true) and 
		 * the nodes of the opposite color point to the nodes onto which they push 
		 * state B (true or false, respectively).
		 */
		
		//Isolate all xor Rules and pack their Claims in one set
		List<Fact> xorFacts = target.factStream()
				.filter(Fact::isXor)
				.collect(Collectors.toList());
		Set<Claim> xorClaims = Sledgehammer.massUnion(xorFacts);
		
		//connect those Claims together based on their sharing a Rule
		Graph<ColorClaim> allFalsePushes = new BasicGraph<>(Wrap.wrap(xorClaims, (c1,c2) -> !Collections.disjoint(c1, c2), ColorClaim::new))
				.addContractEventListenerFactory(new ColorSource());
		
		//reversed relationships from Claim to ColorClaim 
		Map<Claim,ColorClaim> falseMap = Wrap.rawToWrap(allFalsePushes);
		Map<Claim,ColorClaim> trueMap = trueMap(falseMap, xorFacts);
		
		for(Graph<ColorClaim> falsePush : allFalsePushes.connectedComponents()){
			
			List<Pair<Predicate<ColorClaim>, Pair<Graph<ColorClaim>, Map<Claim,ColorClaim>>>> mixPushes = ColorClaim.SIGNS.stream()
					.map((sign) -> new Pair<>(sign, mixPushGraphAndMap(falsePush, sign, falseMap, trueMap)))
					.collect(Collectors.toList());
			
			for(Set<Claim> xorChain : xorChainsInXYChain(falsePush, trueMap)){
				Claim representative = xorChain.iterator().next();
				
				Set<Claim> and = mixPushes.stream()
						.map((pair) -> consequences(pair.getA(), representative, pair.getB().getA(), pair.getB().getB()))
						.collect(Collector.of(
								HashSet::new, 
								Set::addAll, 
								(left, right) -> {
									left.retainAll(right);
									return left;
								}));
				
				if(!and.isEmpty()){
					try{
						return new SolveEventXYChain(and).falsifyClaims();
					} catch(FalsifiedTime.NoUnaccountedClaims e){
						//do nothing
					}
				}
				
				/*
				 * Choosing which one is true first in each scenario just means choosing which 
				 * color performs which kind of push.
				 * 
				 * option-specific directed graphs can be constructed:
				 * If red is true, then reds get their neighbors from the pushFalse graph 
				 * and blues get their neighbors from the pushTrue graph
				 * 
				 * If blue is true, then blues get their neighbors form the pushFalse graph 
				 * and reds their their neighbors from the pushTrue graph
				 */
			}
		}
		
		return null;
	}
	
	/**
	 * <p>Identifies all the xor-chains (like those analysed in 
	 * {@link ColorChain}) that exist within the specified connected 
	 * component, {@code falsePush}, of the overall false-push graph, 
	 * represents each xor-chain as a set of its Claims, and returns 
	 * a list of those sets.</p>
	 * 
	 * <p>Most xor-chain sets will be two-element sets, but there can
	 * exist some more complex xor-chains as part of an XYChain 
	 * system. Any connected network of xor Facts has two solution 
	 * states; so, identifying all the xor-chains in an XYChain and 
	 * examining the consequences of the states of only one Claim 
	 * from each chain allows a full exploration of the implications 
	 * of the XYChain using the smallest number of iterations of the 
	 * second nested for-loop in {@link process()}.</p>
	 * 
	 * @param falsePush the connected component of the overall false-
	 * push graph that is being analysed
	 * @param trueMap the map from Claims to the ColorClaims that 
	 * @return a list of sets of the Claims belonging to the xor-
	 * chains found in {@code falsePush}
	 */
	private static List<Set<Claim>> xorChainsInXYChain(Graph<ColorClaim> falsePush, Map<Claim,ColorClaim> trueMap){
		return new BasicGraph<ColorClaim>(falsePush.nodeStream()
				.map(ColorClaim::wrapped)
				.map(trueMap::get)
				.collect(Collectors.toList()))
		.connectedComponents().stream()
				.map((graph) -> graph.nodeStream()
						.map(ColorClaim::wrapped)
						.collect(Collectors.toSet()))
				.collect(Collectors.toList());
	}
	
	/**
	 * <p>Returns a set of the Claims falsified if {@code xorElement} takes 
	 * on the state (true or false) that {@code state} outputs when given 
	 * as input the {@code ColorClaim} in {@code mixPush} that wraps 
	 * {@code xorElement}.</p>
	 * @param state indicates whether {@code xorElement} is to be treated as 
	 * true or false
	 * @param xorElement a claim from a xor-chain subgraph of {@code mixPush}
	 * @param mixPush a directed graph where one claim points to another claim 
	 * if the first claim, by having a certain state, forces the other claim 
	 * to have the opposite state. The forcing relationship only works for one 
	 * of the two possible states of the first claim; if the first claim has 
	 * the wrong state, it does not force the other claim's state.
	 * @return a set of the Claims falsified if {@code xorElement} takes 
	 * on the state (true or false) that {@code state} outputs when given 
	 * as input the {@code ColorClaim} in {@code mixPush} that wraps 
	 * {@code xorElement}
	 */
	private static Set<Claim> consequences(
			Predicate<ColorClaim> state, 
			Claim xorElement, 
			Graph<ColorClaim> mixPush,
			Map<Claim,ColorClaim> mixPushMap){
		ColorClaim seed = mixPushMap.get(xorElement);
		
		Graph<ColorClaim> component = mixPush.component(
				mixPush.nodeStream().collect(Collectors.toList()), 
				(list) -> seed, 
				Collections.emptyList());
		
		return component.nodeStream()
				.filter(state) //these ones falsify Claims visible() to them
				.map(ColorClaim::wrapped)
				.map(Claim::visible)
				.collect(Sledgehammer.massUnionCollector());
	}
	
	/**
	 * <p>Returns a Pair containing the mix-push graph for the specified 
	 * connected component, {@code falsePush}, of the overall false-push 
	 * graph and a map from the Claims of that graph to that graph's 
	 * ColorClaims.</p>
	 * 
	 * <p>A mix-push graph is a mixture of a false-push graph and a true-
	 * push graph such that Claims of one color point to the Claims they 
	 * falsify (neighbors analogous to that Claim's neighbors in the 
	 * false-push graph) and Claims of the other color point to the Claims 
	 * they verify (neighbors analogous to that Claim's neighbors in the 
	 * true-push graph). As such, there are two mix-push graphs: one where 
	 * positive colors push false and negatives push true, and; one where 
	 * negative colors push false and positives push true.</p>
	 * 
	 * <p>In these mix-push graphs, xor-chains are islands on which 
	 * traversal from one Claim to any other in the xor-chain is possible, 
	 * since Claims in xor-chains push true as well as false, causing 
	 * forcing links within any xor to be bidirectional. Links between such 
	 * islands are one-directional.</p>
	 * @param falsePush a connected component of the overal false-push graph
	 * @param state outputs true if an applied ColorClaim has a color with 
	 * the appropriate sign
	 * @param falseMap a map from the Claims of {@code falsePush} to its 
	 * ColorClaims
	 * @param trueMap a map from the Claims to {@code falsePush} to the 
	 * ColorClaims of its corresponding true-push graph
	 * @return a Pair containing the mix-push graph for the specified 
	 * connected component, {@code falsePush}, of the overall false-push 
	 * graph and a map from the Claims of that graph to that graph's 
	 * ColorClaims
	 */
	private static Pair<Graph<ColorClaim>, Map<Claim,ColorClaim>> mixPushGraphAndMap(
			Graph<ColorClaim> falsePush, 
			Predicate<ColorClaim> state, 
			Map<Claim,ColorClaim> falseMap, 
			Map<Claim,ColorClaim> trueMap){
		List<ColorClaim> pushGraphNodes = falsePush.nodeStream()
				.map((cc) -> {
					ColorClaim lambdaResult = new ColorClaim(cc.wrapped());
					lambdaResult.setColor(cc.getColor());
					return lambdaResult;
				})
				.collect(Collectors.toList());
		
		Map<Claim,ColorClaim> pushGraphNodeMap = Wrap.rawToWrap(pushGraphNodes);
		
		//connect elements of pushGraphNodes to one another
		for(ColorClaim cc : pushGraphNodes){
			Map<Claim,ColorClaim> structureMap = state.test(cc) ? falseMap : trueMap;
			List<ColorClaim> neighbors = structureMap.get(cc.wrapped()).neighbors().stream()
					.map(ColorClaim::wrapped)
					.map(pushGraphNodeMap::get)
					.collect(Collectors.toList());
			cc.neighbors().addAll(neighbors);
		}
		
		return new Pair<>(
				new BasicGraph<>(pushGraphNodes), 
				Wrap.rawToWrap(pushGraphNodes));
	}
	
	/**
	 * <p>Returns a map from the Claims in the Facts of {@code xorFacts} to 
	 * those Claims' ColorClaims in a graph (which is temporarily created 
	 * in implicit form within this method) in which Claims are linked if 
	 * one, by being false, forces the other to be true.</p>
	 * @param falseMap a Map from the Claims in the Facts of 
	 * {@code xorFacts} to their ColorClaims in a graph of 
	 * false-push relationships. Used for those ColorClaims' color data
	 * @param xorFacts edges linking Claims in the true-push graph
	 * @return a map from the Claims in the Facts of {@code xorFacts} to 
	 * those Claims' ColorClaims in a graph (which is temporarily created 
	 * in implicit form within this method) in which Claims are linked if 
	 * one, by being false, forces the other to be true
	 */
	private static Map<Claim,ColorClaim> trueMap(
			Map<Claim,ColorClaim> falseMap, 
			Collection<Fact> xorFacts){
		List<ColorClaim> truePushW = Wrap.wrap(xorFacts, ColorClaim::new);
		truePushW.stream().forEach((cc) -> cc.setColor(falseMap.get(cc.wrapped()).getColor()));
		return Wrap.rawToWrap(truePushW);
	}
	
	public static class SolveEventXYChain extends TechniqueEvent{
		private SolveEventXYChain(Set<Claim> falsified){
			super(falsified);
		}
		
		@Override
		protected String toStringStart(){
			return "XYChain scenario";
		}
	}
}
