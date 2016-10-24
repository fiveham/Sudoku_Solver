package sudoku.technique;

import common.graph.Graph;
import common.graph.Wrap;
import common.graph.BasicGraph;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
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
			ColorChain::xyChain);
	
	/**
	 * <p>Applies {@link #SUBTECHNIQUES sub-techniques} for 
	 * {@link #visibleColorContradiction(Collection) color-contradiction} 
	 * and {@link #bridgeCollapse(Collection) chain-chain interaction} 
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
		List<Fact> xorRules = target.factStream()
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
		
		Set<Claim> claimsToSetFalse = ColorClaim.COLOR_SIGNS.stream()
				.map((test) -> concom.nodeStream()
						.filter(test)
						.map(ColorClaim::wrapped)
						.map(Claim::visible)
						.collect(Sledgehammer.massUnionCollector()))
				.collect(massIntersectionCollector());
		
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
	
	public static <T extends Collection<E>, E> Collector<T,?,Set<E>> massIntersectionCollector(){
		
		class Intersection<Z> extends HashSet<Z>{
			
			/**
			 * 
			 */
			private static final long serialVersionUID = -1768519565525064860L;
			
			private boolean used = false;
			
			public Intersection(){
				super();
			}
			
			public Intersection(Collection<? extends Z> coll){
				super(coll);
			}
			
			public Intersection<Z> intersect(Collection<? extends Z> coll){
				if(used){
					retainAll(coll);
				}else{
					addAll(coll);
					used = true;
				}
				return this;
			}
		}
		
		return Collector.of(
				Intersection<E>::new, 
				Intersection::intersect, 
				Intersection::intersect, 
				Intersection<E>::new, 
				Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
	}
	
	private TechniqueEvent xyChain(Collection<Graph<ColorClaim>> chains){
		
		for(Graph<ColorClaim> chain : chains){
			
			Set<Claim> falseIntersection = ColorClaim.COLOR_SIGNS.stream()
					.map((test) -> getFalsifiedClaimsForTrueColor(
							chain.nodeStream()
									.filter(test)
									.map(ColorClaim::wrapped)
									.collect(Collectors.toSet())))
					.collect(massIntersectionCollector());
			
			if(!falseIntersection.isEmpty()){
				return new SolveEventXYChain(
						falseIntersection, 
						chain.nodeStream()
								.map(ColorClaim::wrapped)
								.collect(Sledgehammer.massUnionCollector())
								.stream()
								.filter(Fact::isXor)
								.collect(Collectors.toList()))
				.falsifyClaims();
			}
		}
		
		return null;
	}
	
	private static Set<Claim> getFalsifiedClaimsForTrueColor(Set<Claim> initialTrue){
		
		Set<Claim> trueClaims = new HashSet<>();
		Set<Claim> falseClaims = new HashSet<>();
		
		Set<Claim> newTrue = new HashSet<>(initialTrue);
		
		while(!newTrue.isEmpty()){
			Set<Claim> newFalse = newFalse(newTrue, falseClaims);
			
			trueClaims.addAll(newTrue);
			falseClaims.addAll(newFalse);
			
			newTrue = newTrue(falseClaims, trueClaims, newFalse);
		}
		
		return falseClaims;
	}
	
	/**
	 * <p.Determines which Claims, in addition to those already known to 
	 * be conditionally false, must be conditionally false.</p>
	 * <p>A Claim is conditionally false if it would have to be false 
	 * given that the Claim currently {@code assertedTrue} in 
	 * {@code getFalsifiedClaims} is asserted to be true.</p>
	 * @param newTrue
	 * @param f
	 * @return
	 */
	private static Set<Claim> newFalse(Set<Claim> newTrue, Set<Claim> f){
		Set<Claim> result = new HashSet<>();
		for(Claim newlyVerified : newTrue){
			result.addAll(newlyVerified.visible());
		}
		result.removeAll(f);
		return result;
	}
	
	/**
	 * <p>Returns a set of the Claims that must be true because all other Claims of some 
	 * Rule have already been determined to be false.</p>
	 * @param newFalse
	 * @return
	 */
	private static Set<Claim> newTrue(Set<Claim> falseClaims, Set<Claim> trueClaims, Set<Claim> newFalse){
		
		Set<Fact> visibleRules = visibleRules(newFalse);
		Set<Claim> result = new HashSet<>(visibleRules.size());
		for(Fact rvisibleRule : visibleRules){
			Set<Claim> copyOfVisibleRule = new HashSet<>(rvisibleRule);
			copyOfVisibleRule.removeAll(falseClaims);
			if(copyOfVisibleRule.size() == Fact.SIZE_WHEN_SOLVED){
				result.add(rvisibleRule.iterator().next());
			}
		}
		
		result.removeAll(trueClaims);
		
		return result;
	}
	
	/**
	 * <p>Returns a set of the Facts visible (adjacent) to at least one of 
	 * the Claims in {@code newFalse}.</p>
	 * @param newFalse
	 * @return
	 */
	private static Set<Fact> visibleRules(Set<Claim> newFalse){
		return Sledgehammer.massUnion(newFalse);
	}
	
	public class SolveEventXYChain extends TechniqueEvent{
		
		private final Collection<Fact> xorEntity;
		
		public SolveEventXYChain(Set<Claim> falsified, Collection<Fact> xorEntity) {
			super(falsified);
			this.xorEntity = xorEntity;
		}
		
		@Override
		protected String toStringStart() {
			return "XYChain based on the xor-entity "+xorEntity.toString();
		}
	}
	
	/**
	 * <p>Wraps a Claim and decorates it with an int color.</p>
	 * @author fiveham
	 *
	 */
	private static class ColorClaim implements WrapVertex<Claim,ColorClaim>{
		
		public static final List<Predicate<ColorClaim>> COLOR_SIGNS;
		static{
			COLOR_SIGNS = new ArrayList<>(2); //MAGIC
			COLOR_SIGNS.add(ColorClaim::posColor);
			COLOR_SIGNS.add(ColorClaim::negColor);
		}
		
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
			if(this.color == 0){
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
}
