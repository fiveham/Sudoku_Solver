package sudoku;

import java.util.Collection;

/**
 * <p>An expression of a specific value of a cell in a sudoku 
 * puzzle.</p>
 * <p>A {@link Rule Rule} is equivalent to a specific true statement 
 * about its puzzle, indicating that exactly one of its neighbors is 
 * true and all its other neighbor Claims are false. The collection 
 * of all the Rules of a Puzzle is an expression of a system of 
 * equations. To solve this system, specific values are needed, such 
 * as the boundary or initial values needed to solve a system of 
 * differential equations. An Init is such a value, and the collection 
 * of initial values provided for a sudoku puzzle is typically 
 * sufficient to specify a unique solution of the entire system.</p>
 * @author fiveham
 *
 */
public class Init extends Fact {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3700799253734450539L;
	
	public Init(Puzzle puzzle) {
		super(puzzle);
		// TODO Auto-generated constructor stub
	}
	
	public Init(Puzzle puzzle, Collection<Claim> c) {
		super(puzzle, c);
		// TODO Auto-generated constructor stub
	}
	
	public Init(Puzzle puzzle, int initialCapacity) {
		super(puzzle, initialCapacity);
		// TODO Auto-generated constructor stub
	}
	
	public Init(Puzzle puzzle, int initialCapacity, float loadFactor) {
		super(puzzle, initialCapacity, loadFactor);
		// TODO Auto-generated constructor stub
	}
}
