package sudoku.technique;

import common.ComboGen;
import common.Pair;
import common.graph.Graph;
import common.graph.BasicGraph;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
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
	
	private final ColorSource colorSource = new ColorSource();
	
	/**
	 * <p>Constructs a ColorChain that works to solve the 
	 * specified {@code target}.</p>
	 * @param target the Puzzle that this Technique works 
	 * to solve.
	 */
	public ColorChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Imitates the slide structure of {@link Solver#solve() Solver.solve()}, 
	 * with subtechniques for {@link #visibleColorContradiction(Collection) color-contradiction} 
	 * and {@link #bridgeCollapse(Collection) chain-chain interaction}. Subtechniques are applied 
	 * in sequence.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	@Override
	protected TechniqueEvent process(){
		List<Function<Collection<Graph<ColorClaim>>,TechniqueEvent>> actions = new ArrayList<>(SUBTECHNIQUE_COUNT);
		Collections.addAll(actions, 
				this::visibleColorContradiction, 
				this::bridgeCollapse, 
				this::bridgeJoin);
		
		Collection<Graph<ColorClaim>> chains = generateChains();
		for(Function<Collection<Graph<ColorClaim>>,TechniqueEvent> test : actions){
			TechniqueEvent result = test.apply(chains);
			if(result != null){
				return result;
			}
		}
		
		return null;
	}
	
	public static final int SUBTECHNIQUE_COUNT = 3;
	
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
	 * @return a SolutionEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	//TODO create a graph where each xor-chain is a node, connected to each other chain-node with which the chain shares some rules
	private TechniqueEvent bridgeCollapse(Collection<Graph<ColorClaim>> chains){
		for(List<Graph<ColorClaim>> chainPair : new ComboGen<>(chains, CHAINS_FOR_BRIDGE, CHAINS_FOR_BRIDGE)){
			Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor = evenSideAndFalseColor(chainPair);
			if(evenSideAndFalseColor != null){
				Graph<ColorClaim> falseEvenSideChain = evenSideAndFalseColor.getA();
				return setChainFalseForColor(falseEvenSideChain, evenSideAndFalseColor.getB());
			}
		}
		
		return null;
	}
	
	/**
	 * <p>Examines pairs of xor-chains to find sledgehammerable bridges between chains. 
	 * A bridge is sledgehammerable if the distances from one lane to the other in 
	 * either connected chain is even. In such a sledgehammer situation, </p>
	 * @param chains
	 * @return
	 */
	private TechniqueEvent bridgeJoin(Collection<Graph<ColorClaim>> chains){
		for(List<Graph<ColorClaim>> chainPair : new ComboGen<>(chains, CHAINS_FOR_BRIDGE, CHAINS_FOR_BRIDGE)){
			Pair<Collection<Fact>,Collection<Fact>> sledgehammer = chainSledgehammer(chainPair);
			if(sledgehammer != null){
				Collection<Fact> sources = sledgehammer.getA();
				Collection<Fact> recipients = sledgehammer.getB();
				
				Set<Claim> falsified = Sledgehammer.sideEffectUnion(recipients, false);
				falsified.removeAll(Sledgehammer.sideEffectUnion(sources, false));
				
				return Sledgehammer.resolve(falsified, sources, recipients, SolveEventBridgeJoin::new);
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Pair<Collection<Fact>,Collection<Fact>> chainSledgehammer(List<Graph<ColorClaim>> chains){
		Graph<ColorClaim> chain0 = chains.get(0);
		Graph<ColorClaim> chain1 = chains.get(1);
		
		Set<Fact> chainUnion0 = cacheMassUnion(chain0);
		Set<Fact> chainUnion1 = cacheMassUnion(chain1);
		
		Set<Fact> lanes = new HashSet<>(chainUnion0);
		lanes.retainAll(chainUnion1);
		
		for(List<Fact> bridge : new ComboGen<>(lanes, RULES_FOR_BRIDGE, RULES_FOR_BRIDGE)){
			Fact lane0 = bridge.get(0);
			Fact lane1 = bridge.get(1);
			
			List<ColorClaim> pathC0 = chain0.path(chainRuleIntersection(chain0, lane0), chainRuleIntersection(chain0, lane1));
			List<Fact> path0 = IntStream.range(1,pathC0.size())
					.mapToObj((int i) -> pathC0.get(i-1).wrapped().intersection(pathC0.get(i).wrapped()).iterator().next())
					.collect(Collectors.toList());
			List<ColorClaim> pathC1 = chain1.path(chainRuleIntersection(chain1, lane0), chainRuleIntersection(chain1, lane1));
			List<Fact> path1 = IntStream.range(1, pathC1.size())
					.mapToObj((int i) -> pathC1.get(i-1).wrapped().intersection(pathC1.get(i).wrapped()).iterator().next())
					.collect(Collectors.toList());
			
			if(path0.size()%2==1 && path1.size()%2==1){
				List<Fact> recip = new ArrayList<>((path0.size()+path1.size())/2+1);
				Collections.addAll(recip, lane0, lane1);
				List<Fact> src = new ArrayList<>((path0.size()+path1.size())/2+1);
				
				for(List<Fact> path : new List[]{path0, path1}){
					for(int i = 0; i<path.size(); ++i){
						(i%2==0 ? src : recip).add(path.get(i));
					}
				}
				
				return new Pair<>(src, recip);
			}
		}
		return null;
	}
	
	private static ColorClaim chainRuleIntersection(Graph<ColorClaim> graph, Set<Claim> set){
		return graph.nodeStream().filter((cc) -> set.contains(cc.wrapped())).findFirst().get();
	}
	
	public static class SolveEventBridgeJoin extends TechniqueEvent{
		private SolveEventBridgeJoin(Set<Claim> falsified, Collection<? extends Fact> src, Collection<? extends Fact> recip){
			super(falsified);
		}
	}
	
	public static final int CHAINS_FOR_BRIDGE = 2;
	
	/**
	 * <p>Any claims in the {@code target} that {@link Claim#visibleClaims() can see} 
	 * Claims with opposite colors from the same chain must be false, no matter 
	 * which color from that chain is true and which is false.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or 
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
	 * @return a SolutionEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	private TechniqueEvent visibleColorContradiction(Graph<ColorClaim> concom){
		Set<Claim> visibleToPositives = concom.nodeStream()
				.filter((cc) -> cc.color > 0)
				.map(ColorClaim::wrapped)
				.map(Claim::visibleClaims)
				.collect(MASS_UNION);
		Set<Claim> visibleToNegatives = concom.nodeStream()
				.filter((cc) -> cc.color < 0)
				.map(ColorClaim::wrapped)
				.map(Claim::visibleClaims)
				.collect(MASS_UNION);
		
		Set<Claim> claimsToSetFalse = visibleToPositives;
		claimsToSetFalse.retainAll(visibleToNegatives);
		if(!claimsToSetFalse.isEmpty()){
			TechniqueEvent result = new SolveEventColorContradiction(claimsToSetFalse);
			claimsToSetFalse.stream().forEach((c)->c.setFalse(result));
			return result;
		}
		
		return null;
	}
	
	public static final BinaryOperator<Set<Claim>> MERGE_CLAIM_SETS = (r1,r2) -> {r1.addAll(r2); return r1;};
	
	public static final Collector<Set<Claim>,?,Set<Claim>> MASS_UNION = Collector.of(
			HashSet::new, 
			Set::addAll, 
			MERGE_CLAIM_SETS);
	
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
		return new BasicGraph<ColorClaim>(link(xorRules))
				.addContractEventListenerFactory(colorSource)
				.connectedComponents();
	}
	
	/**
	 * <p>Wraps the Claims of the {@code xorRules} in {@code ColorClaim}s and 
	 * adds bidirectional edges linking those ColorClaims that wrap Claims that 
	 * are both connected to a common element of {@code xorRules}.</p>
	 * @param claims a set of Claims to be wrapped by ColorClaims and linked 
	 * by edges
	 * @return a list of connected ColorClaim each of which wraps a Claim 
	 * from {@code claims} and tags that Claim with a color
	 */
	private static List<ColorClaim> link(Collection<Fact> xorRules){
		Map<Claim,ColorClaim> map = new HashMap<>();
		List<ColorClaim> colorClaims = Sledgehammer.sideEffectUnion(xorRules, false).stream()
				.map(ColorClaim::new)
				.peek((colorClaim) -> map.put(colorClaim.wrapped(), colorClaim))
				.collect(Collectors.toList());
		
		xorRules.stream()
				.map(ArrayList<Claim>::new)
				.forEach((edge) -> {
					ColorClaim cc0 = map.get(edge.get(0));
					ColorClaim cc1 = map.get(edge.get(1));
					cc0.neighbors().add(cc1);
					cc1.neighbors().add(cc0);
				});
		
		return colorClaims;
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
				.filter((cc)->cc.color()==color)
				.map( ColorClaim::wrapped )
				.collect(Collectors.toSet());
		TechniqueEvent time = new SolveEventBridgeCollapse(setFalse);
		setFalse.stream().forEach((c)->c.setFalse(time));
		return time;
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
		public String toString(){
			return "Color-chain bridge contradiction" + super.toString();
		}
	}
	
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
			
			int dist0 = dist(lane0, lane1, chain0);
			int dist1 = dist(lane0, lane1, chain1);
			if( dist0%2==0 && dist1%2==1 ){
				return new Pair<Graph<ColorClaim>,Integer>(chain0, bridgeColor(chain0, lane0, lane1));
			} else if(dist1%2==0 && dist0%2==1){
				return new Pair<Graph<ColorClaim>,Integer>(chain1, bridgeColor(chain0, lane0, lane1));
			}
		}
		return null;
	}
	
	public static final int RULES_FOR_BRIDGE = 2;
	
	private final Map<Graph<ColorClaim>, Set<Fact>> massUnionCache = new HashMap<>();
	
	private Set<Fact> cacheMassUnion(Graph<ColorClaim> chain){
		if(massUnionCache.containsKey(chain)){
			return massUnionCache.get(chain);
		} else{
			Set<Fact> result = Sledgehammer.sideEffectUnion( chain.nodeStream().map(ColorClaim::wrapped).collect(Collectors.toList()), false);
			massUnionCache.put(chain, result);
			return result;
		}
	}
	
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
				colorsOnBridge.add(map.get(c).color());
			}
		}
		
		if(colorsOnBridge.size() == COLORS_ON_BRIDGE_FOR_COLLAPSEABLE_CHAIN){
			for(Integer i : colorsOnBridge){
				return i;
			}
		}
		throw new IllegalStateException("Should have one color on bridge, but instead have "+colorsOnBridge.size());
	}
	
	/**
	 * <p>The number ({@value}) of colors that a xor-chain collapseable as the result 
	 * of chain-chain interaction has on the bridge that mediates that chain-chain 
	 * interaction.</p>
	 * 
	 * <p>A bridge between two chains is constituted by a pair of non-{@code xor} 
	 * Rules each of which has a Claim (neighbor) in each of the two bridged chains.</p>
	 */
	public static final int COLORS_ON_BRIDGE_FOR_COLLAPSEABLE_CHAIN = 1;
	
	/**
	 * <p>Returns a {@link Map Map} from the Claims in {@code chain} to those 
	 * Claims' colors.</p>
	 * @param chain a xor-chain whose Claims and whose Claims' colors are to 
	 * be mapped
	 * @return a {@link Map Map} from the Claims in {@code chain} to those 
	 * Claims' colors
	 */
	private Map<Claim,ColorClaim> claimToColorMap(Graph<ColorClaim> chain){
		Map<Claim,ColorClaim> result = new HashMap<>(chain.size());
		
		for(ColorClaim cc : chain){
			result.put(cc.wrapped(), cc);
		}
		
		return result;
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
	private int dist(Fact lane0, Fact lane1, Graph<ColorClaim> chain){
		ColorClaim lane0Intersection = getBridgeIntersection(lane0, chain);
		ColorClaim lane1Intersection = getBridgeIntersection(lane1, chain);
		
		return chain.distance(lane0Intersection, lane1Intersection);
	}
	
	/**
	 * <p>Identifies the Claim where {@code lane} and {@code chain} intersect.</p>
	 * @param lane a non-xor Rule that intersects {@code chain}
	 * @param chain a xor-chain
	 * @throws IllegalArgumentException if {@code lane} does not interest {@code chain}
	 * @return the vertex from {@code chain} where {@code lane} and {@code chain} 
	 * intersect
	 */
	private ColorClaim getBridgeIntersection(Fact lane, Graph<ColorClaim> chain){
		for(ColorClaim wgccn : chain){
			if(lane.contains(wgccn.wrapped())){
				return wgccn;
			}
		}
		throw new IllegalArgumentException("Specified chain and bridge-lane do not intersect.");
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
		public String toString(){
			return "color-chain visibility contradiction " + super.toString();
		}
	}
	
	/**
	 * <p>Wraps a Claim and decorates it with an int color.</p>
	 * @author fiveham
	 *
	 */
	public static class ColorClaim implements WrapVertex<Claim,ColorClaim>{
		private int color = 0;
		private Claim wrapped;
		private final List<ColorClaim> neighbors;
		
		ColorClaim(Claim wrapped){
			this.wrapped = wrapped;
			this.neighbors = new ArrayList<>();
		}
		
		@Override
		public Claim wrapped(){
			return wrapped;
		}
		
		@Override
		public List<ColorClaim> neighbors(){
			return neighbors;
		}
		
		/**
		 * <p>Returns the color of the wrapped Claim.</p>
		 * @return the color of the wrapped Claim
		 */
		int color(){
			return color;
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
				return cc.color == this.color && cc.wrapped == this.wrapped;
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return wrapped.hashCode();
		}
	}
	
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
}
