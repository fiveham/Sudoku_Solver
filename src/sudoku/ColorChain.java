package sudoku;

import common.ComboGen;
import common.Pair;
import common.graph.Graph;
import common.graph.BasicGraph;
import common.graph.Wrap;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * <p>The color-chain technique exploits the fact that a Rule with 
 * only two connected Claims is a {@code xor} gate. A collection of 
 * interconnected two-Claim Rules, regardless of the size and shape 
 * of a connected network of these Rules, has only two possible 
 * solution-states.</p>
 * @author fiveham
 *
 */
/*
 * http://www.sadmansoftware.com/sudoku/colouring.php
 * 
 * If you can chain together a bunch of FactBags of size 2 
 * all pertaining to the same Symbol, then any cells outside 
 * that chain that are in a bag with one of the Claims in 
 * the chain and in another bag with another Claim of 
 * opposite truth-state in the Chain, then that outside-the-
 * chain cell can have the Symbol in question marked impossible 
 * in it.
 * 
 */
public class ColorChain extends Technique {
	
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
	 * with subtechniques for {@link #visibleColorContradiction() color-contradiction} 
	 * and {@link #bridge() chain-chain interaction}. Subtechniques are applied 
	 * in sequence.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or null 
	 * if no changes were made
	 */
	@Override
	protected SolutionEvent process(){
		List<Function<Collection<Graph<ColorClaim>>,SolutionEvent>> actions = new ArrayList<>(SUBTECHNIQUE_COUNT);
		Collections.addAll(actions, (graph)->visibleColorContradiction(graph), (graph)->bridge(graph));
		
		Collection<Graph<ColorClaim>> chains = generateChains();
		for(Function<Collection<Graph<ColorClaim>>,SolutionEvent> test : actions){
			SolutionEvent result = test.apply(chains);
			if(result != null){
				return result;
			}
		}
		
		return null;
	}
	
	public static final int SUBTECHNIQUE_COUNT = 2;
	
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
	private SolutionEvent bridge(Collection<Graph<ColorClaim>> chains){
		for(List<Graph<ColorClaim>> chainPair : new ComboGen<>(chains, CHAINS_FOR_BRIDGE, CHAINS_FOR_BRIDGE)){
			Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor = evenSideAndFalseColor(chainPair);
			if(evenSideAndFalseColor != null){
				Graph<ColorClaim> falseEvenSideChain = evenSideAndFalseColor.getA();
				return setChainFalseForColor(new SolveEventColorChainBridge(), falseEvenSideChain, evenSideAndFalseColor.getB());
			}
		}
		
		return null;
	}
	
	public static final int CHAINS_FOR_BRIDGE = 2;
	
	/**
	 * <p>Any claims in the {@code target} that {@link Claim#visibleClaims() can see} 
	 * Claims with opposite colors from the same chain must be false, no matter 
	 * which color from that chain is true and which is false.</p>
	 * @return a SolutionEvent describing the changes made to the puzzle, or 
	 * null if no changes were made
	 */
	private SolutionEvent visibleColorContradiction(Collection<Graph<ColorClaim>> chains){
		for(Graph<ColorClaim> chain : chains){
			SolutionEvent result = visibleColorContradiction(chain);
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
	private SolutionEvent visibleColorContradiction(Graph<ColorClaim> concom){
		Set<Claim> visibleToPositives = concom.nodeStream()
				.filter((cc) -> cc.color > 0)
				.map((cc) -> cc.wrapped())
				.collect(CLAIMS_TO_VISIBLE_CLAIMS);
		Set<Claim> visibleToNegatives = concom.nodeStream()
				.filter((cc) -> cc.color < 0)
				.map((cc) -> cc.wrapped())
				.collect(CLAIMS_TO_VISIBLE_CLAIMS);
		
		Set<Claim> claimsToSetFalse = visibleToPositives;
		claimsToSetFalse.retainAll(visibleToNegatives);
		if(!claimsToSetFalse.isEmpty()){
			SolutionEvent result = new SolveEventColorChainVisibleContradiction(claimsToSetFalse);
			claimsToSetFalse.stream().forEach((c)->c.setFalse(result));
			return result;
		}
		
		return null;
	}
	
	public static final Collector<Claim,?,Set<Claim>> CLAIMS_TO_VISIBLE_CLAIMS = Collector.of(
			HashSet::new, 
			(r,t) -> r.addAll(t.visibleClaims()), 
			(r1,r2) -> {r1.addAll(r2); return r1;});
	
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
		Set<Rule> xors = target.nodeStream()
				.filter((ns)->(ns.size()==Rule.SIZE_WHEN_XOR && ns instanceof Rule))
				.map((ns)->(Rule)ns)
				.collect(Collectors.toSet());
		Set<Claim> claimsInXors = Sledgehammer.sideEffectUnion(xors, false);
		
		Graph<ColorClaim> wg = new BasicGraph<ColorClaim>(link(claimsInXors, (c1,c2)->c1.intersects(c2)));
		
		Consumer<Set<ColorClaim>> painter = (cuttingEdge) -> {
			cuttingEdge.stream().forEach((e)->e.setColor(colorSource.get()));
			colorSource.invertColor();
		};
		
		wg.addContractEventListenerFactory(()->colorSource.nextColor(painter));
		return wg.connectedComponents();
	}
	
	/**
	 * <p>Works just like 
	 * {@link common.graph.Wrap#wrap(Collection,BiPredicate,Function) Wrap.wrap()}
	 * except that this method returns a {@code List<ColorClaim>} instead of 
	 * a {@code List<Wrap<Claim>>}.</p>
	 * @param claims
	 * @param edgeDetector
	 * @return a list of ColorClaim each of which wraps a Claim from {@code claims} 
	 * and tags that Claim with a color
	 */
	//TODO redesign Wrap.wrap() so that this method's action can be achieved by a call to Wrap.wrap(), including 
	//the stipulation that the returned List have ColorClaim as its parameterized type.
	public static List<ColorClaim> link(Collection<Claim> claims, BiPredicate<Claim,Claim> edgeDetector){
		List<ColorClaim> result = claims.stream().map(ColorClaim::new).collect(Collectors.toList());
		
		for(List<ColorClaim> pair : new ComboGen<>(result, Wrap.VERTICES_PER_EDGE, Wrap.VERTICES_PER_EDGE)){
			ColorClaim cc0 = pair.get(0);
			ColorClaim cc1 = pair.get(1);
			if(edgeDetector.test(cc0.wrapped(), cc1.wrapped())){
				cc0.neighbors().add(cc1);
				cc1.neighbors().add(cc0);
			}
		}
		
		return result;
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
	 * {@link FalsifiedTime#falsified() falsified Claims}
	 */
	private SolutionEvent setChainFalseForColor(SolutionEvent time, Graph<ColorClaim> chain, final int color){
		List<Claim> setFalse = chain.nodeStream()
				.filter((cc)->cc.color()==color)
				.map( (cc)->cc.wrapped() )
				.collect(Collectors.toList());
		time.falsified().addAll(setFalse);
		
		setFalse.stream().filter(Claim.CLAIM_IS_BEING_SET_FALSE.negate()).forEach((c)->c.setFalse(time));
		
		return time;
	}
	
	/**
	 * <p>A Time node describing two xor-chains' collapse due 
	 * to {@link #bridge(Collection) bridge} interaction.</p>
	 * @author fiveham
	 *
	 */
	public static class SolveEventColorChainBridge extends SolutionEvent{
		private SolveEventColorChainBridge(){
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
		
		Set<Fact> chainUnion0 = Sledgehammer.sideEffectUnion( chain0.nodeStream().map(UNWRAP_TO_CLAIM).collect(Collectors.toList()), false);
		Set<Fact> chainUnion1 = Sledgehammer.sideEffectUnion( chain1.nodeStream().map(UNWRAP_TO_CLAIM).collect(Collectors.toList()), false);
		
		Set<Fact> bridges = chainUnion0;
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
	
	/**
	 * <p>A Function mapping a ColorClaim to its {@link ColorClaim#wrapped() wrapped} Claim.</p>
	 */
	public static final Function<ColorClaim,Claim> UNWRAP_TO_CLAIM = (n)->n.wrapped();
	
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
	public static class SolveEventColorChainVisibleContradiction extends SolutionEvent{
		private SolveEventColorChainVisibleContradiction(Collection<Claim> falseClaims){
			falsified().addAll(falseClaims);
		}
	}
	
	/**
	 * <p>Wraps a Claim and decorates it with an int color.</p>
	 * @author fiveham
	 *
	 */
	public static class ColorClaim implements WrapVertex<Claim,ColorClaim>{//TODO move .wrap() from Wrap to WrapVertex
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
	private static class ColorSource{
		
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
		int get(){
			return positive ? color : -color;
		}
		
		/**
		 * <p>Increments the internal unsigned color and resets 
		 * the internal color-sign to positive.</p>
		 */
		<T> T nextColor(T t){
			++color;
			positive = true;
			return t;
		}
	}
}
