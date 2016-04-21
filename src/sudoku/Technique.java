package sudoku;

/**
 * <p>Represents a technique used for solving sudoku puzzles.</p>
 * @author fiveham
 */

public abstract class Technique {
	
	/*
	 * The Technique class should remain publicly visible, but
	 * the constructor should only be visible to subclasses.
	 */
	
	/**
	 * <p>The target to which this instance of this technique pertains.</p>
	 */
	protected Sudoku target; //Privacy is protected to allow access from subclasses
	
	/**
	 * <p>Constructs a Technique object. Specifies the target
	 * to which this technique pertains.</p>
	 * @param target The target to which this 
	 * instance of this solution technique pertains.
	 */
	protected Technique(Sudoku puzzle){
		this.target = puzzle;
	}
	
	/**
	 * <p>Performs this technique's analysis on the underlying target.</p>
	 * <p>If opportunities to set values in any cells or to mark values
	 * impossible in any cells arise, such an opportunity must be 
	 * exploited before the method returns.<p>
	 * @return a Time node describing the solution event that was performed, 
	 * and whose subordinate Time nodes describe subordinate steps of the 
	 * solution event, such as {@link Rule#validateFinalState() automatic resolution} 
	 * of Rule-subset scenarios, or returns null of no changes were made
	 */
	final public SolutionEvent digest(){
		return target.isSolved() ? null : process();
	}
	
	/**
	 * <p>Performs this technique's analysis on the underlying target.</p>
	 * <p>As soon as any change is made, the method returns.</p>
	 * @return a Time node describing the solution event that was performed, 
	 * and whose subordinate Time nodes describe subordinate steps of the 
	 * solution event, such as {@link Rule#validateFinalState() automatic resolution} 
	 * of Rule-subset scenarios, or returns null of no changes were made
	 */
	protected abstract SolutionEvent process();
	
	/**
	 * <p>Returns the target to which this instance of 
	 * this technique pertains.</p>
	 * @return the target to which this instance of this 
	 * technique pertains.
	 */
	public Sudoku getPuzzle(){
		return target;
	}
}


