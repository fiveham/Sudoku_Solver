package sudoku;

import common.time.Time;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import sudoku.Puzzle.RuleType;
import sudoku.time.FalsifiedTime;
import sudoku.Puzzle.IndexInstance;

public class Rule extends Fact{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2703809294481675542L;
	
	private final Puzzle.RuleType type;
	private final IndexInstance dimA, dimB;
	
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
		super(puzzle, c, linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength()));
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Rule){
			Rule r = (Rule) o;
			return type == r.type && dimA.equals(r.dimA) && dimB.equals(r.dimB); 
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "Rule: " + type.descriptionFor(dimA,dimB) /*+ " ["+size()+"]"*/;
	}
	
	public RuleType getType(){
		return type;
	}
	
	public IndexInstance dimA(){
		return dimA;
	}
	
	public IndexInstance dimB(){
		return dimB;
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
	public void validateState(FalsifiedTime time){
		if(isSolved()){
			Claim c = iterator().next(); //there is only one Claim
			
			FalsifiedTime solve;
			try{
				solve = new TimeTotalLocalization(time, c.visible(), this);
			} catch(FalsifiedTime.NoUnaccountedClaims e){
				return;
			}
			
			time.addChild(solve);
			solve.falsified().stream()
					.forEach((falsifiedClaim) -> falsifiedClaim.setFalse(solve));
		} else if( shouldCheckForValueClaim() ){
			findAndAddressValueClaim(time);
		} else if( isEmpty() ){
			throw new IllegalStateException("A Rule is not allowed to be empty. this.toString(): "+toString());
		}
	}

	/**
	 * <p>A time node denoting an {@link #verifyFinalState automatic collapse} 
	 * and encapsulating subordinate automatic collapses.</p>
	 * @author fiveham
	 *
	 */
	public static abstract class AutoResolve extends FalsifiedTime{
		private AutoResolve(Time parent, Set<Claim> falseClaims){
			super(parent, falseClaims);
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
		protected String toStringStart(){
			return "Total localization from "+src+src.contentString();
		}
	}
	
	/**
	 * <p>Finds value-claim collapse scenarios and resolves them 
	 * if found. These are scenarios where one Rule is a subset 
	 * of another.</p>
	 */
	private void findAndAddressValueClaim(FalsifiedTime time){
		visible().stream()
				.filter(Rule.class::isInstance)
				.map(Rule.class::cast)
				.filter((r) -> r.type.canClaimValue(type) && r.hasProperSubset(this))
				.forEach((r) -> {
					Set<Claim> falsified = new HashSet<>(r);
					falsified.removeAll(this);
					
					TimeValueClaim newTime;
					try{
						newTime = new TimeValueClaim(time, falsified, this, r);
					} catch(FalsifiedTime.NoUnaccountedClaims e){
						return;
					}
					
					time.addChild(newTime);
					newTime.falsified().stream().forEach((claim) -> claim.setFalse(newTime));
				});
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
		protected String toStringStart(){
			return "Value-claim "+sup+" subsumes "+sub;
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
}
