package sudoku.technique;

import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.time.SolutionEvent;

/**
 * <p>Represents a technique used for solving sudoku puzzles.</p>
 * @author fiveham
 */
public abstract class AbstractTechnique implements Technique{
	
	/**
	 * <p>The Sudoku graph that this Technique tries to solve.</p>
	 */
	protected Sudoku target; //Privacy is protected to allow access from subclasses
	
	/**
	 * <p>Constructs a Technique object. Specifies the target
	 * to which this technique pertains.</p>
	 * @param target The target to which this 
	 * instance of this solution technique pertains.
	 */
	protected AbstractTechnique(Sudoku puzzle){
		this.target = puzzle;
	}
	
	/**
	 * <p>Performs this Technique's analysis on the underlying sudoku graph 
	 * if and only if the underlying sudoku graph is not already 
	 * {@link sudoku.Sudoku#isSolved() solved}.</p>
	 */
	@Override
	final public SolutionEvent digest(){
		return target.isSolved() ? null : process();
	}
	
	/**
	 * <p>Performs this Technique's analysis on the underlying sudoku graph.</p>
	 * <p>As soon as any change is made, the method returns.</p>
	 * @return a SolutionEvent describing the modification made to the underlying 
	 * graph, and whose {@link common.time.Time#children() subordinate Time nodes} 
	 * describe subordinate steps of the solution event, such as 
	 * {@link Rule#validateFinalState() automatic resolution} of Rule-subset 
	 * scenarios, or returns null of no changes were made
	 */
	protected abstract SolutionEvent process();
}


