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
	protected Puzzle puzzle; //Privacy is protected to allow access from subclasses
	
	/**
	 * <p>Constructs a Technique object. Specifies the target
	 * to which this technique pertains.</p>
	 * @param target The target to which this 
	 * instance of this solution technique pertains.
	 */
	protected Technique(Puzzle puzzle){
		this.puzzle = puzzle;
	}
	
	/**
	 * <p>Performs this technique's analysis on the underlying target.</p>
	 * <p>If opportunities to set values in any cells or to mark values
	 * impossible in any cells arise, such an opportunity must be 
	 * exploited before the method returns.<p>
	 * @return true if any changes were made
	 * to the underlying target, false otherwise
	 */
	final public boolean digest(){
		//target.timeBuilder().push(techTime());
		
		boolean result = puzzle.isSolved() ? false : process();
		
		//target.timeBuilder().pop();
		return result;
	}
	
	/**
	 * <p>Performs this technique's analysis on the underlying target.</p>
	 * <p>If opportunities to make changes to the target arise, they are 
	 * exploited before the method returns.</p>
	 * @return true if changes were made to the target, false otherwise
	 */
	protected abstract boolean process();
	
	/**
	 * <p>Returns the target to which this instance of 
	 * this technique pertains.</p>
	 * @return the target to which this instance of this 
	 * technique pertains.
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
}


