package sudoku;

import common.ComboGen;
import common.Pair;
import common.graph.Graph;
import common.graph.BasicGraph;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>The color-chain technique exploits the fact that a Rule with 
 * only two connected Claims is a <tt>xor</tt> gate. A collection of 
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
	 * specified <tt>target</tt>.</p>
	 * @param target the Puzzle that this Technique works 
	 * to solve.
	 */
	public ColorChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	/* *
	 * <p>Imitates the slide structure of {@link Solver#solve() Solver.solve()}, 
	 * with subtechniques for {@link #internal() chain self-interaction}, 
	 * {@link #bridge() chain-chain interaction}, and 
	 * {@link #external chain-Claim interaction}.</p>
	 * 
	 * <p>Subtechniques are applied in sequence until all have been executed in a 
	 * row without making further changes to the target. If any one of these 
	 * subtechniques changes the target, then after that subtechnique finishes, 
	 * the list of subtechniques is reset and subtechniques are applied starting 
	 * from the beginning of the list again.</p>
	 * @return true if changes were made to the target, false otherwise
	 */
	@Override
	protected SolutionEvent process(){
		List<Supplier<SolutionEvent>> actions = new ArrayList<>();
		actions.add(()->internal());
		actions.add(()->bridge());
		actions.add(()->external());
		
		for(Supplier<SolutionEvent> test : actions){
			SolutionEvent result = test.get();
			if(result != null){
				return result;
			}
		}
		
		return null;
	}
	
	/*
	 * <p>Checks the {@link #generateChains() chains} in the <tt>target</tt> for 
	 * internal contradictions and resolves each chain in which internal contradictions 
	 * are encountered.</p>
	 * 
	 * <p>A chain contradicts itself if there exist two Claims of the same color 
	 * neighboring the same Rule. Only one Claim neighbor of a Rule can be true, 
	 * and all the claims of the same color in a chain have the same truth-state; 
	 * so the entire chain must collapse due to this self-interaction.</p>
	 * 
	 * <p>There is not really any contradiction, but it is simpler to refer to the 
	 * self-interaction as a contradiction than to describe a framework of chain 
	 * interactions and to describe the self-interaction in terms of that framework.</p>
	 * 
	 * @return true if any changes to the <tt>target</tt> were made by this call 
	 * to this method
	 */
	private SolutionEvent internal(){
		for(Graph<ColorClaim> chain : generateChains()){
			SolutionEvent result = contradictionInternal(chain);
			if( result != null ){
				return result;
			}
		}
		return null;
	}
	
	/* *
	 * <p>Checks pairs of chains for bridge-based interactions that force 
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
	 * @return true if calling this method made changes to <tt>target</tt>, 
	 * false otherwise
	 */
	private SolutionEvent bridge(){
		for(List<Graph<ColorClaim>> chainPair : new ComboGen<>(generateChains(), CHAINS_FOR_BRIDGE, CHAINS_FOR_BRIDGE)){
			Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor = evenSideAndFalseColor(chainPair);
			if(evenSideAndFalseColor != null){
				Graph<ColorClaim> falseEvenSideChain = evenSideAndFalseColor.getA();
				return setChainFalseForColor(new SolveEventColorChainBridge(), falseEvenSideChain, evenSideAndFalseColor.getB());
			}
		}
		
		return null;
	}
	
	public static final int CHAINS_FOR_BRIDGE = 2;
	
	/* *
	 * <p>Any claims in the target that {@link ToolSet#intersects(Set<T>) can see} 
	 * Claims with opposite colors from the same chain must be false, no matter 
	 * which color from that chain is true and which is false.</p>
	 * @return true if calling this method made changes to <tt>target</tt>, 
	 * false otherwise
	 */
	private SolutionEvent external(){
		for(Graph<ColorClaim> chain : generateChains()){
			SolutionEvent result = contradictionExternal(chain);
			if( result != null ){
				return result;
			}
		}
		return null;
	}
	
	/* *
	 * <p>Detects and {@link Claim#setFalse() sets false} Claims that 
	 * can see Claims from <tt>concom</tt> with opposite colors.</p>
	 * @param concom a xor-chain
	 * @return true if changes were made to the target, false otherwise
	 */
	private SolutionEvent contradictionExternal(Graph<ColorClaim> concom){
		List<Claim> claimsToSetFalse = target.claimStream().filter((claim)->claimContradictsChain(claim,concom)).collect(Collectors.toList());
		if(!claimsToSetFalse.isEmpty()){
			SolutionEvent result = new SolveEventColorChainExternal(claimsToSetFalse);
			claimsToSetFalse.stream().forEach((c)->c.setFalse(result));
			return result;
		}
		
		return null;
	}
	
	/**
	 * <p>Isolates those Rules in the target which have two Claims, makes 
	 * a Graph of them, where two Rules share an edge if they share a Claim 
	 * as an element, and returns a collection of that Graph's connected 
	 * components, each of which is a xor-chain with two possible solution-states.</p>
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
		
		wg.addContractEventListenerFactory(()->nextColorReturnList(painter));
		return wg.connectedComponents();
	}
	
	public static List<ColorClaim> link(Collection<Claim> claims, BiPredicate<Claim,Claim> edgeDetector){
		List<ColorClaim> result = claims.stream().map(ColorClaim::new).collect(Collectors.toList());
		
		for(List<ColorClaim> pair : new ComboGen<>(result,2,2)){
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
	 * <p>Calls <tt>colorSource.nextColor()</tt> and returns <tt>list</tt>.</p>
	 * 
	 * <p>This method allows the automatic updating from one color to the next 
	 * needed for proper chain-coloring.</p>
	 * @param list the list that is passed through unaltered
	 * @return <tt>list</tt>
	 */
	private Consumer<Set<ColorClaim>> nextColorReturnList(Consumer<Set<ColorClaim>> list){
		colorSource.nextColor();
		return list;
	}
	
	/**
	 * <p>Checks for chain-contradictions, wherein a xor-chain interacts with itself 
	 * and collapses because two Claims with the same color share a Rule, and collapses 
	 * <tt>chain</tt> if it is found to have such a self-interaction. When two 
	 * Claims share a Rule, at most, one of them can be true, but they also both be 
	 * false, and any Claims with the same color have to have the same truth-state; 
	 * so, only one solution-state is available: both are false, in which case all 
	 * claims of that color are false: The entire chain collapses.</p>
	 * 
	 * <p>Returns true if the target was changed by the call to this method, false otherwise.</p>
	 * @param chain the xor-chain being analysed
	 * @return true if the target was changed by the call to this method, false otherwise
	 */
	private SolutionEvent contradictionInternal(Graph<ColorClaim> chain){
		List<ColorClaim> chainList = chain.nodeStream().collect(Collectors.toList());
		for(int i=0; i<chainList.size(); ++i){
			ColorClaim cc1 = chainList.get(i);
			for(int j=0; j<i; ++j){
				ColorClaim cc2 = chainList.get(j);
				if(cc1.color() == cc2.color() && cc1.wrapped().intersects(cc2.wrapped())){
					return setChainFalseForColor(new SolveEventColorChainInternal(), chain, cc1.color());
				}
			}
		}
		return null;
	}
	
	/* *
	 * <p>A time node encapsulating events that occur while checking 
	 * a xor-chain for a collapse-initiating self-interaction.</p>
	 * @author fiveham
	 *
	 */
	public class SolveEventColorChainInternal extends SolutionEvent{
		private SolveEventColorChainInternal(){
		}
	}
	
	/* *
	 * <p>Sets all the Claims in this chain false if they are decorated with the 
	 * specified <tt>color</tt>.</p>
	 * @param time the contextual time node which needs to store a collection of 
	 * the Claims to be set false by this method call 
	 * @param chain a xor-chain of connected two-Claim Rules 
	 * @param color the color of the Claims being set false
	 * @return true if the target was changed by the call to this method, false otherwise
	 */
	private SolutionEvent setChainFalseForColor(SolutionEvent time, Graph<ColorClaim> chain, final int color){
		List<Claim> setFalse = chain.nodeStream()
				.filter((cc)->cc.color()==color)
				.map( (cc)->cc.wrapped() )
				.collect(Collectors.toList());
		time.falsified().addAll(setFalse);
		
		for(Claim c : setFalse){
			c.setFalse(time);
		}
		
		return time;
	}
	
	/* *
	 * <p>A time node encapsulating the events occurring during a period of 
	 * analysis where two xor-chains are checked for collapse due to 
	 * bridge interaction.</p>
	 * @author fiveham
	 *
	 */
	public class SolveEventColorChainBridge extends SolutionEvent{
		private SolveEventColorChainBridge(){
		}
	}
	
	/**
	 * <p>Returns a Pair containing a chain to be collapsed and the color of the claims in that 
	 * chain to be set false, or <tt>null</tt> if no even bridged chain is found.</p>
	 * 
	 * <p>This method tries to find two single-Rule bridges between the two specified chains and 
	 * determines the step length between the two bridge Rules in either chain. If one 
	 * chain has an even step-length between bridges while the other chain has an odd step 
	 * length between the two bridges, then the chain with the even step-length between bridges 
	 * can be completely collapsed, with the claims from the even-side chain with the color of 
	 * those even-side chain claims that were on the bridges all being set false.</p>
	 * @return a Pair containing a chain to be collapsed and the color of the Claims in that 
	 * chain to be set False, or <tt>null</tt> if no even bridged chain is found
	 */
	private Pair<Graph<ColorClaim>,Integer> evenSideAndFalseColor(List<Graph<ColorClaim>> chains){
		Graph<ColorClaim> chain0 = chains.get(0);
		Graph<ColorClaim> chain1 = chains.get(1);
		
		Set<Fact> chainUnion0 = Sledgehammer.sideEffectUnion( chain0.nodeStream().map(UNWRAP_TO_CLAIM).collect(Collectors.toList()), false);
		Set<Fact> chainUnion1 = Sledgehammer.sideEffectUnion( chain1.nodeStream().map(UNWRAP_TO_CLAIM).collect(Collectors.toList()), false);
		
		Set<Fact> bridges = new HashSet<>(chainUnion0);
		bridges.retainAll(chainUnion1);
		
		for(List<Fact> bridge : new ComboGen<>(bridges, 2, 2)){
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
	
	/**
	 * <p>A Function mapping a ColorClaim to its {@link ColorClaim#wrapped() wrapped} Claim.</p>
	 */
	public static final Function<ColorClaim,Claim> UNWRAP_TO_CLAIM = (n)->n.wrapped();
	
	/**
	 * <p>Returns the int color of the Claims belonging to <tt>chain</tt> 
	 * that are on the bridge made of <tt>lane0</tt> and <tt>lane1</tt>.</p>
	 * @param chain the xor-chain the color of whose Claims on the bridge 
	 * made of <tt>lane0</tt> and <tt>lane1</tt> is returned
	 * @param lane0 one of the lanes of the bridge
	 * @param lane1 one of the lanes of the bridge
	 * @throws IllegalStateException if <tt>chain</tt> has more than one color 
	 * on the bridge, which means <tt>chain</tt> has an odd step-length between 
	 * <tt>lane0</tt> and <tt>lane1</tt>
	 * @return the int color of the Claims belonging to <tt>chain</tt> 
	 * that are on the bridge made of <tt>lane0</tt> and <tt>lane1</tt>
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
	 * <p>A bridge between two chains is constituted by a pair of non-<tt>xor</tt> 
	 * Rules each of which has a Claim (neighbor) in each of the two bridged chains.</p>
	 */
	public static final int COLORS_ON_BRIDGE_FOR_COLLAPSEABLE_CHAIN = 1;
	
	/**
	 * <p>Returns a {@link Map Map} from the Claims in <tt>chain</tt> to those 
	 * Claims' colors.</p>
	 * @param chain a xor-chain whose Claims and whose Claims' colors are to 
	 * be mapped
	 * @return a {@link Map Map} from the Claims in <tt>chain</tt> to those 
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
	 * <p>Returns the number of steps between <tt>lane0</tt> and <tt>lane1</tt> 
	 * in <tt>chain</tt>.</p>
	 * @param lane0 a bridge-lane that intersects <tt>chain</tt>
	 * @param lane1 a bridge-lane that intersects <tt>chain</tt>
	 * @param chain a xor-chain intersecting <tt>lane0</tt> and <tt>lane1</tt>
	 * @throws IllegalArgumentException if any of the lanes does not intersect <tt>chain</tt>
	 * @return the number of steps between <tt>lane0</tt> and <tt>lane1</tt> 
	 * in <tt>chain</tt>
	 */
	private int dist(Fact lane0, Fact lane1, Graph<ColorClaim> chain){
		ColorClaim lane0Intersection = getBridgeIntersection(lane0, chain);
		ColorClaim lane1Intersection = getBridgeIntersection(lane1, chain);
		
		return chain.distance(lane0Intersection, lane1Intersection);
	}
	
	/**
	 * <p>Identifies the Claim where <tt>lane</tt> and <tt>chain</tt> intersect.</p>
	 * @param lane a non-xor Rule that intersects <tt>chain</tt>
	 * @param chain a xor-chain
	 * @throws IllegalArgumentException if <tt>lane</tt> does not interest <tt>chain</tt>
	 * @return the vertex from <tt>chain</tt> where <tt>lane</tt> and <tt>chain</tt> 
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
	
	/* *
	 * <p>A time node encapsulating the events that occur in searching 
	 * for and falsifying Claims that see both colors in a xor-chain.</p>
	 * @author fiveham
	 *
	 */
	public class SolveEventColorChainExternal extends SolutionEvent{
		private SolveEventColorChainExternal(Collection<Claim> falseClaims){
			falsified().addAll(falseClaims);
		}
	}
	
	/**
	 * <p>Determines whether <tt>claim</tt> should be set false on account of 
	 * <tt>concom</tt>.</p>
	 * @param claim
	 * @param concom
	 * @return true if <tt>claim</tt> sees a positive-colored Claim in <tt>concom</tt> 
	 * and a negative-colored Claim in <tt>concom</tt>, false otherwise
	 */
	private boolean claimContradictsChain(Claim claim, Graph<ColorClaim> concom){
		boolean hasPosColorNeighbor = false;
		boolean hasNegColorNeighbor = false;
		
		for(Iterator<ColorClaim> iterator = concom.iterator(); iterator.hasNext() && !(hasPosColorNeighbor && hasNegColorNeighbor); ){
			ColorClaim cc = iterator.next();
			if( claim.intersects(cc.wrapped()) ){
				if(cc.color() > 0){
					hasPosColorNeighbor = true;
				} else if(cc.color() < 0){
					hasNegColorNeighbor = true;
				} else{
					throw new IllegalStateException("This node has a 0 color: "+cc.toString());
				}
			}
		}
		
		return hasPosColorNeighbor && hasNegColorNeighbor;
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
		 * <p>Sets the color to <tt>color<tt>.</p>
		 * @param color the new color
		 */
		void setColor(int color){
			/*if(this.color==0){*/
				this.color = color;
			/*} else{
				throw new IllegalStateException("Cannot change color to "+color+" because color has already been set to "+this.color);				
			}*/
		}
	}
	
	/**
	 * <p>A generator and manager of ints to be used as colors for Claims 
	 * in xor-chains.</p>
	 * 
	 * <p>Use <tt>get()</tt> to get the current color with the current sign, 
	 * use <tt>nextColor()</tt> when beginning to color a new xor-chain, and 
	 * use <tt>invertColor()</tt> to change the sign of the colors returned 
	 * by subsequent calls to <tt>get()</tt>.</p>
	 * @author fiveham
	 *
	 */
	private static class ColorSource{
		private boolean positive;
		private int color;
		ColorSource(){
			this.color = 1;
			this.positive = true;
		}
		
		/**
		 * <p>Changes this ColorSource's color sign so that 
		 * subsequent calls to <tt>get()</tt> return a color 
		 * with the sign opposite of the sign returned by 
		 * previous calls to <tt>get()</tt>.</p>
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
		void nextColor(){
			++color;
			positive = true;
		}
	}
}
