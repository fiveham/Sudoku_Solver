package sudoku;

import java.util.function.*;

/**
 * Represents a technique used in solving sudoku puzzles.
 * @author fiveham
 */

public abstract class Technique {
	
	/*
	 * The Technique class should remain publicly visible, but
	 * the constructor should only be visible to subclasses.
	 */
	
	/** The target to which this instance of this technique pertains */
	protected Puzzle puzzle;			//Privacy is protected to allow access from subclasses
	
	/**
	 * Constructs a Technique object. Specifies the target
	 * to which this technique pertains
	 * @param target			The target to which this 
	 * instance of this solution technique pertains.
	 */
	protected Technique(Puzzle puzzle){
		this.puzzle = puzzle;
	}
	
	/**
	 * Performs this technique's analysis on the underlying target.
	 * If opportunities to set values in any cells or to mark values
	 * impossible in any cells arise, such an opportunity must be 
	 * exploited before the method returns.
	 * @return				Returns whether any changes were made
	 * to the underlying target.
	 */
	public abstract boolean digest();
	
	/**
	 * Returns a reference to the target to which this instance of 
	 * this technique pertains.
	 * @return			Returns a reference to the target to which 
	 * this instance of this technique pertains.
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/* *
	 * Returns whether the parameter cells are considered
	 * to connect for the purposes of the technique.
	 * 
	 * The default is that cells don't connect at all.
	 * @param cell1			First cell	
	 * @param cell2			Second cell
	 * @return 				Returns whether the parameter cells are considered
	 * to connect for the purposes of the technique.
	 */
	/*public boolean connect(Cell cell1, Cell cell2){
		return false;
	}*/
	
	public static final BiPredicate<Cell,Cell> DEFAULT_CONNECTION = (c1,c2) -> false;
	
	public BiPredicate<Cell,Cell> connection(){
		return DEFAULT_CONNECTION;
	}
}
