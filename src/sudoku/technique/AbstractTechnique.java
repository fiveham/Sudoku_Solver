package sudoku.technique;

import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

/**
 * <p>Represents a technique used for solving sudoku puzzles.</p>
 * @author fiveham
 */
public abstract class AbstractTechnique<T extends Technique<T>> implements Technique<T>{
	
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
	final public TechniqueEvent digest(){
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
	protected abstract TechniqueEvent process();
	
	public abstract T apply(Sudoku sudoku);
}


