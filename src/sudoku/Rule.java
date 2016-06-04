package sudoku;

import common.time.Time;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import sudoku.Puzzle.RuleType;
import sudoku.Puzzle.IndexInstance;

public class Rule extends Fact{
	
	public static final Predicate<NodeSet<?,?>> IS_RULE = (n) -> n instanceof Rule;
	
	public static final Function<NodeSet<?,?>,Rule> AS_RULE = (n) -> (Rule)n;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2191355198146127036L;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule that 
	 * is a {@code xor}.  A Rule is a {@code xor} if it has two Claims, 
	 * because exactly one of them is true and the other is false. Given 
	 * that we know that the Rule is satisfied (is {@code true}), the 
	 * Claims, as inputs to the Rule, make such a Rule a {@code xor} 
	 * operation on its neighbors.</p>
	 */
	public static final int SIZE_WHEN_XOR = 2;
	
	private final Puzzle.RuleType type;
	private final IndexInstance dimA, dimB;
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle and 
	 * having only the specified {@code types}.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 */
	public Rule(Puzzle puzzle, RuleType type, IndexInstance dimA, IndexInstance dimB){
		super(puzzle);
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
		this.hashCode = genHashCode(puzzle, type, dimA, dimB);
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified {@code types}, and containing 
	 * all the elements of {@code c}.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param c a collection whose elements this Rule will also 
	 * have as elements
	 */
	public Rule(Puzzle puzzle, RuleType type, Collection<Claim> c, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, c);
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
		this.hashCode = genHashCode(puzzle, type, dimA, dimB);
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified {@code types}, with the 
	 * specified initial capacity.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param initialCapacity the initial capacity of this Rule
	 */
	public Rule(Puzzle puzzle, RuleType type, int initialCapacity, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, initialCapacity);
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
		this.hashCode = genHashCode(puzzle, type, dimA, dimB);
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified {@code types}, with the 
	 * specified initial capacity and load factor.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param initialCapacity the initial capacity of this Rule
	 * @param loadFactor the load factor for this Rule
	 */
	public Rule(Puzzle puzzle, RuleType type, int initialCapacity, float loadFactor, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, initialCapacity, loadFactor);
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
		this.hashCode = genHashCode(puzzle, type, dimA, dimB);
	}
	
	public Set<Rule> visibleRules(){
		Set<Rule> result = Sledgehammer.sideEffectUnion(this, false).stream()
				.filter(IS_RULE)
				.map(AS_RULE)
				.collect(Collectors.toSet());
		result.remove(this);
		return result;
	}
	
	@Override
	public int hashCode(){
		return hashCode;
	}
	
	private final int hashCode;
	
	private static int genHashCode(Puzzle puzzle, RuleType type, IndexInstance dimA, IndexInstance dimB){
		Puzzle.IndexValue[] a = puzzle.decodeXYZ(dimA, dimB);
		return Claim.linearizeCoords(a[Puzzle.X_DIM].intValue(), a[Puzzle.Y_DIM].intValue(), a[Puzzle.Z_DIM].intValue(), puzzle.sideLength()) 
				+ (int)Math.pow(puzzle.sideLength(), Puzzle.DIMENSION_COUNT) 
				* type.ordinal() ;
	}
	
	@Override
	public String toString(){
		return "Rule: " + type.descriptionFor(dimA,dimB) /*+ " ["+size()+"]"*/;
	}
	
	public RuleType getType(){
		return type;
	}
	
	/**
	 * <p>Detects the need for this Rule to collapse completely and 
	 * set its sole element true if it has only one element left or 
	 * the need for this Rule to collapse partially onto one of the 
	 * Rules it intersects if that rule is a subset of this one, or 
	 * throws an exception if this Rule is empty.</p>
	 * 
	 * @throws IllegalStateException if this Rule is empty
	 */
	@Override
	protected void validateFinalState(SolutionEvent time){
		if(size() == SIZE_WHEN_SOLVED){
			Claim c = iterator().next(); //there is only one Claim
			if( !c.setTrueInProgress() ){
				Set<Claim> falsify = c.visibleClaims();
				if(!falsify.isEmpty()){
					
					Time solve;
					try{
						solve = new TimeTotalLocalization(time.top(), falsify, this);
					} catch(FalsifiedTime.NoUnaccountedClaims e){
						return;
					}
					
					time.push(solve);
					//TODO rework methods that accept a SolutionEvent so they directly accept the 
					//FalsifiedTime onto which they should append new Time nodes
					c.setTrue(time);
					time.pop();
				}
			}
		} else if( shouldCheckForValueClaim() ){
			findAndAddressValueClaim(time);
		} else if( isEmpty() ){
			throw new IllegalStateException("A Rule is not allowed to be empty. this.toString(): "+toString());
		}
	}
	
	/**
	 * <p>A time node denoting the complete collapse of a Rule because 
	 * it has exactly one Claim and encapsulating subordinate events that 
	 * occur as {@link #verifyFinalState() an automatic result} of the 
	 * aforementioned collapse.</p>
	 * @author fiveham
	 *
	 */
	public static class TimeTotalLocalization extends AutoResolve{
		
		private final Fact src;
		
		private TimeTotalLocalization(Time parent, Set<Claim> falseClaims, Fact src){
			super(parent, falseClaims);
			this.src = src;
		}
		
		@Override
		public String toString(){
			return "Total localization from "+src+src.contentString() + super.toString();
		}
	}
	
	/**
	 * <p>Finds value-claim collapse scenarios and resolves them 
	 * if found. These are scenarios where one Rule is a subset 
	 * of another.</p>
	 */
	private void findAndAddressValueClaim(SolutionEvent time){
		for(Rule r : visibleRules()){
			if(r.type.canClaimValue(type) && r.hasProperSubset(this)){
				Set<Claim> falsified = new HashSet<>(r);
				falsified.removeAll(this);
				FalsifiedTime.clean(falsified, time.top());
				
				if(!falsified.isEmpty()){
					time.push(new TimeValueClaim(time.top(), falsified, this, r));
					falsified.stream().forEach((claim) -> claim.setFalse(time));
					time.pop();
				}
			}
		}
	}
	
	/**
	 * <p>A time node denoting a value-claim event where one Rule 
	 * is a subset of another and the superset collapses onto the 
	 * subset.</p>
	 * @author fiveham
	 *
	 */
	public static class TimeValueClaim extends AutoResolve{
		
		private final Fact sub, sup;
		
		private TimeValueClaim(Time parent, Set<Claim> falseClaims, Fact sub, Fact sup){
			super(parent, falseClaims);
			this.sub = sub;
			this.sup = sup;
		}
		
		@Override
		public String toString(){
			return "Value-claim "+sup+" subsumes "+sub + super.toString();
		}
	}
	
	/**
	 * <p>Returns true if this Rule is possibly contained in another 
	 * Rule as a subset of the other bag in a manner compatible with 
	 * the deprecated solution technique known as ValueClaim.</p>
	 * @return true if this Rule is not a {@link Puzzle.RuleType#CELL cell Rule} 
	 * and has {@code size() <= target.magnitude()}
	 */
	private boolean shouldCheckForValueClaim(){
		return type == RuleType.CELL 
				? false 
				: size() <= puzzle.magnitude();
	}
	
	/**
	 * <p>A time node denoting an {@link #verifyFinalState automatic collapse} 
	 * and encapsulating subordinate automatic collapses.</p>
	 * @author fiveham
	 *
	 */
	public static class AutoResolve extends FalsifiedTime{
		private AutoResolve(Time parent, Set<Claim> falseClaims){
			super(parent, falseClaims);
		}
	}
	
	public boolean isXor(){
		return size() == SIZE_WHEN_XOR;
	}
}
