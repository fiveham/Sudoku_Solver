package sudoku;

import common.time.Time;
import java.util.Collection;
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
			
			//DEBUG
			if(src==null){
				System.out.println("WAT; it's null");
			}
			
			//return "Total localization from "+src+src.contentString(); //DEBUG
			return "Total localization from " + src + "[CONTENT STRING ABSENT FOR DEBUGGING]";
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
		protected String toStringStart(){
			return "Value-claim "+sup+" subsumes "+sub;
		}
	}
}
