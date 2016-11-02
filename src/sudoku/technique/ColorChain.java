package sudoku.technique;

import common.BackedSet;
import common.Pair;
import common.Sets;
import common.Universe;
import common.graph.Graph;
import common.graph.Wrap;
import common.graph.BasicGraph;
import common.graph.WrapVertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
		this.claimUniverse = new Universe<>(target.claimStream().collect(Collectors.toList()));
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
	private static Collection<Graph<ColorClaim>> generateChains(Sudoku target, ColorSource colorSource){
		List<Fact> xorRules = target.factStream()
				.filter(Fact::isXor)
				.collect(Collectors.toList());
		return new BasicGraph<ColorClaim>(Wrap.wrap(xorRules,ColorClaim::new))
				.addGrowthListenerFactory(colorSource)
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
	private static class ColorSource implements Supplier<Consumer<Set<ColorClaim>>>{
		
		public static final int INIT_COLOR = 1;
		
		private int color;
		
		ColorSource(){
			this.color = INIT_COLOR;
		}
		
		@Override
		public Consumer<Set<ColorClaim>> get() {
			return new Colorizer(color++);
		}
		
		class Colorizer implements Consumer<Set<ColorClaim>>{
			
			private final int col;
			private boolean positive;
			
			Colorizer(int col){
				this.col = col;
				this.positive = true;
			}
			
			@Override
			public void accept(Set<ColorClaim> cuttingEdge) {
				cuttingEdge.stream().forEach((e)->e.setColor(getColor()));
				invertColor();
			}
			
			/**
			 * <p>Changes this Colorizer's color sign so that 
			 * subsequent calls to {@code getColor()} return a color 
			 * with the sign opposite of the sign returned by 
			 * previous calls to {@code getColor()}.</p>
			 */
			private void invertColor(){
				positive = !positive;
			}
			
			/**
			 * <p>Returns the current color with the current sign.</p>
			 * @return the current color with the current sign
			 */
			private int getColor(){
				return positive ? col : -col;
			}
		}
	}
	
	private static final List<Function<ColorChain,TechniqueEvent>> SUBTECHNIQUES = Arrays.asList(
			ColorChain::subsumedFacts,
			ColorChain::xyChain, 
			ColorChain::implicationIntersection);
	
	@Override
	public TechniqueEvent process(){
		try{
			return SUBTECHNIQUES.stream()
					.map((st) -> st.apply(this))
					.filter((result) -> result != null)
					.findFirst().get();
		} catch(NoSuchElementException e){
			return null;
		}
	}
	
	private static final int MIN_IMPLICATION_INTERSECTION_SIZE = 3;
	
	/**
	 * <p>Explores the consequences of each individual Claim of a 
	 * given Rule (for each Rule of each size greater than 2) being 
	 * true and falsifies those Claims that would have to be false 
	 * regardless of which Claim of the Rule currently in focus is 
	 * true.</p>
	 * @return
	 */
	private TechniqueEvent implicationIntersection(){
		for(int size = MIN_IMPLICATION_INTERSECTION_SIZE; size <= target.sideLength(); ++size){
			
			Collection<Fact> rules;
			{
				final int size2 = size;
				rules = target.factStream()
						.filter((f) -> f.size() == size2)
						.collect(Collectors.toList());
			}
			
			for(Fact rule : rules){
				
				Set<Claim> externalFalseIntersection = getFalseIntersection(rule, size);
				
				if(!externalFalseIntersection.isEmpty()){
					return new SolveEventImplicationIntersection(
							externalFalseIntersection, 
							rule)
							.falsifyClaims();
				}
			}
		}
		
		return null;
	}
	
	public static class SolveEventImplicationIntersection extends TechniqueEvent{
		
		private final Fact rule;
		
		SolveEventImplicationIntersection(Set<Claim> falsifiedClaims, Fact rule){
			super(falsifiedClaims);
			this.rule = rule;
		}
		
		@Override
		protected String toStringStart() {
			return "An intersection of the Claims that would be falsified by the verification of any of the Claims of "+rule;
		}
	}
	
	private static Set<Claim> getFalseIntersection(Fact rule, int size){
		
		Iterator<Claim> claimAsserter = rule.iterator();
		
		if(!claimAsserter.hasNext()){
			StringBuilder sb = new StringBuilder("This Iterator doesn't have a first element. ");
			sb.append("Size: ").append(rule.size());
			sb.append("Intended size: ").append(size);
			throw new IllegalStateException(sb.toString());
		}
		
		Set<Claim> falseIntersection = getFalsifiedClaims(claimAsserter.next());
		
		while(claimAsserter.hasNext() && !rule.containsAll(falseIntersection)){
			falseIntersection.retainAll(
					getFalsifiedClaims(
							claimAsserter.next()));
		}
		
		falseIntersection.removeAll(rule);
		return falseIntersection;
	}
	
	private TechniqueEvent xyChain(){
		for(Graph<ColorClaim> chain : generateChains(target, colorSource)){ //for each chain
			Set<Claim> falseIntersection = ColorClaim.COLOR_SIGNS.stream()
					.map((test) -> chain.nodeStream()
							.filter(test)
							.map(ColorClaim::wrapped)
							.collect(Collectors.toSet()))
					.map(ColorChain::getFalsifiedClaims)
					.collect(Sets.massIntersectionCollector()); //get the overlap of the implied falsifications of both solutions
			
			if(!falseIntersection.isEmpty()){
				List<Fact> xorChain = chain.nodeStream()
						.map(ColorClaim::wrapped)
						.collect(Sets.massUnionCollector())
						.stream()
						.filter(Fact::isXor)
						.collect(Collectors.toList());
				return new SolveEventColorChain(falseIntersection, xorChain)
						.falsifyClaims();
			}
		}
		
		return null;
	}
	
	private static Set<Claim> getFalsifiedClaims(Claim... initialTrue){
		return getFalsifiedClaims(
				Arrays.stream(initialTrue)
				.collect(Collectors.toSet()));
	}
	
	private static Set<Claim> getFalsifiedClaims(Set<Claim> initialTrue){
		
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
		return Sets.massUnion(newFalse);
	}
	
	public class SolveEventColorChain extends TechniqueEvent{
		
		private final Collection<Fact> xorEntity;
		
		public SolveEventColorChain(Set<Claim> falsified, Collection<Fact> xorEntity) {
			super(falsified);
			this.xorEntity = xorEntity;
		}
		
		@Override
		protected String toStringStart() {
			return "Either-solution propagation from the "+xorEntity.size()+"-Rule xor-chain "+xorEntity.toString();
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
			COLOR_SIGNS.add((colorClaim) -> colorClaim.color > 0);
			COLOR_SIGNS.add((colorClaim) -> colorClaim.color < 0);
		}
		
		private int color = 0;
		private final Claim claim;
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
	
	@Override
	public ColorChain apply(Sudoku sudoku){
		return new ColorChain(sudoku);
	}
	
	/**
	 * <p>Finds a unary Fact in {@code target} and sets that Fact's 
	 * one Claim neighbor true.</p>
	 * @return an Initialization describing the verification of 
	 * an Init's sole Claim neighbor and any and all resulting 
	 * automatic resolution events, or null if no Init is found
	 */
	private TechniqueEvent subsumedFacts(){
		Optional<Pair<Fact,Set<Fact>>> i = target.factStream()
				.map((fact) -> new Pair<Fact,Set<Fact>>(
						fact, 
						fact.visible().stream()
								.filter((vis) -> vis.containsAll(fact))
								.collect(Collectors.toSet())))
				.filter((pair) -> !pair.getB().isEmpty())
				.findFirst();
		if(i.isPresent()){
			Pair<Fact,Set<Fact>> p = i.get();
			return new SubsumedFact(p.getA(), p.getB()).falsifyClaims();
		}
		
		return null;
	}
	
	/**
	 * <p>Describes the verification of a Claim known to be true 
	 * because one or more of its Fact neighbors contains only that 
	 * Claim as an element..</p>
	 * @author fiveham
	 *
	 */
	public static class SubsumedFact extends TechniqueEvent{
		
		private final Fact src;
		private final Set<Fact> supersets;
		
		private SubsumedFact(Fact solved, Set<Fact> supersets){
			super(solved.iterator().next().visible());
			this.src = solved;
			this.supersets = supersets;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof SubsumedFact){
				SubsumedFact se = (SubsumedFact) o;
				return super.equals(se) && (src == null ? se.src == null : src.equals(se.src));
			}
			return false;
		}
		
		@Override
		protected String toStringStart(){
			return src+"is subsumed by "+supersets.stream()
					.map(Object::toString)
					.collect(Collectors.joining(", "));
		}
	}
	
	/*
	 * TODO incorporate use of subsumedFacts via its method into xyChain's analysis
	 * and incorporate xyChain and subsumedFacts into implicationIntersection.
	 * 
	 * Currently, SubsumedFacts analysis (in the form of checks for fully localized 
	 * Facts, not including partially localized facts) is implicitly incorporated into 
	 * xyChain analysis.  I'd like to incorporate it in an explicit, formal manner, 
	 * in order to add the partial-overlap functionality and also to avoid code duplication.
	 * 
	 * Additionally, I'd like to incorporate subsumedFacts and xyChain into 
	 * implicationIntersection in order to begin unifying ColorChain and Sledgehammer.
	 * 
	 * There should be a method that accepts an arbitrary int between 1 and sideLength 
	 * along with a trueMask and a falseMask--sets that describe modifications to the 
	 * Puzzle--and output some results describing the discoveries it has made w/r/t the 
	 * implications of the state of the puzzle described by the passed parameters in 
	 * concert with the established existing state of the Puzzle.
	 * 
	 * Using it should be equivalent to asking the question "Given the puzzle as it is, 
	 * except that these Claims are true and these Claims are false, what can we 
	 * say about the truth or falsehood of other Claims?" and getting back an answer 
	 * like "These other Claims would be false and these other Claims would be true."
	 */
	
	/**
	 * <p>Tries to find an overlap among the consequences of each 
	 * of the Claims of a given Fact in the puzzle hypothetically 
	 * being true, starting from the smallest Facts in the puzzle
	 * and increasing in Fact size from there.</p>
	 * @return a TechniqueEvent describing the Fact whose Claims' 
	 * consequences led to progress in solving the puzzle and the 
	 * Claims that were falsified in that step of progress, or 
	 * {@code null} if no progress was made
	 */
	private TechniqueEvent implications(){
		
		for(Fact f : target.factStream()
				.sorted((small,large) -> Integer.compare(large.size(), small.size()))
				.collect(Collectors.toList())){
			TechniqueEvent result = implications(f);
			if(result != null){
				return result;
			}
		}
		return null;
	}
	
	/**
	 * <p>Tries to find an overlap among the consequences of each 
	 * of the Claims of {@code f} in the puzzle hypothetically 
	 * being true.</p>
	 * @return a TechniqueEvent describing the Fact whose Claims' 
	 * consequences led to progress in solving the puzzle and the 
	 * Claims that were falsified in that step of progress, or 
	 * {@code null} if no progress was made
	 */
	private TechniqueEvent implications(Fact f){
		Implications implications = new Implications(f);
		
		while(implications.intersection().isEmpty() && implications.isDepthAvailable()){ //TODO while (that && the implications can be explored deeper)
			implications.enhance();
		}
		
		Consequences con = implications.intersection();
		if(!con.isEmpty()){
			return new SolveEventImplications(f, con.falseMask()).falsifyClaims();
		}
		return null;
	}
	
	public static class SolveEventImplications extends TechniqueEvent{
		
		private final Fact initFact;
		
		SolveEventImplications(Fact f, Set<Claim> falsifiedClaims){
			super(falsifiedClaims);
			this.initFact = f;
		}

		@Override
		protected String toStringStart() {
			return "Exploration of the consequences of the possible solutions of " + initFact;
		}
	}
	
	/**
	 * <p>A map from a Claim to some consequences that follow logically if 
	 * that Claim is true.</p>
	 * @author fiveham
	 *
	 */
	private class Implications extends HashMap<Claim,Consequences>{
		
		Implications(Fact f){
			super();
			for(Claim c : f){
				Consequences con = new Consequences();
				con.addTrue(c);
				con.addFalse(c.visible());
				put(c, con);
			}
		}
		
		/**
		 * <p>Explores further implications and incorporates their consequences.</p>
		 */
		void enhance(){
			//TODO stub
		}
		
		/**
		 * <p>Return true if there are implications that have yet to be explored 
		 * and accounted for as a result of the Claims this obect tracks being 
		 * hypothetically true, false otherwise.</p>
		 * @return true if there are implications that have yet to be explored 
		 * and accounted for as a result of the Claims this obect tracks being 
		 * hypothetically true, false otherwise
		 */
		boolean isDepthAvailable(){
			//TODO stub
			return false;
		}
		
		Consequences intersection(){
			Consequences intersection = new Consequences();
			intersection.addTrue(values().stream()
					.map(Consequences::trueMask)
					.collect(Sets.massIntersectionCollector()));
			intersection.addFalse(values().stream()
					.map(Consequences::falseMask)
					.collect(Sets.massIntersectionCollector()));
			return intersection;
		}
		
	}
	
	private final Universe<Claim> claimUniverse;
	
	private class Consequences{
		
		private final Set<Claim> trueMask;
		private final Set<Claim> falseMask;
		
		Consequences(){
			trueMask = new BackedSet<>(claimUniverse);
			falseMask = new BackedSet<>(claimUniverse);
		}
		
		Set<Claim> trueMask(){
			return trueMask;
		}
		
		Set<Claim> falseMask(){
			return falseMask;
		}
		
		boolean addTrue(Collection<? extends Claim> c){
			return trueMask.addAll(c);
		}
		
		boolean addFalse(Collection<? extends Claim> c){
			return falseMask.addAll(c);
		}
		
		boolean addTrue(Claim c){
			return trueMask.add(c);
		}
		
		boolean addFalse(Claim c){
			return falseMask.add(c);
		}
		
		boolean addAll(Consequences c){
			return addTrue(c.trueMask) || addFalse(c.falseMask); 
		}
		
		boolean isEmpty(){
			return trueMask.isEmpty() && falseMask.isEmpty();
		}
	}
}
