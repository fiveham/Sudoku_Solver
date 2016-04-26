package sudoku;

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
	
	public static final byte TYPES = (byte)0;
	public static final int CAPACITY = 1;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3700799253734450539L;
	
	public Init(Puzzle puzzle) {
		super(puzzle, TYPES, CAPACITY);
	}
}
