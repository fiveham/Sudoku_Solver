package sudoku;

/**
 * Represents a task of collapsing FactBags onto a smaller Rule that each 
 * Rule to be collapsed contains as a proper subset. These are set up by 
 * the FactBags themselves during initial population of the Puzzle with the 
 * initial values of the cells and registered with the Puzzle for later 
 * resolution.
 * @author fiveham
 *
 */
public abstract class Resolvable {
	
	/**
	 * 
	 * @return
	 */
	public abstract boolean resolve();
}
