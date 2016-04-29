package sudoku;

/**
 * <p>An expression of a specific value of a cell in a sudoku 
 * puzzle.</p>
 * 
 * <p>A {@link Rule Rule} is equivalent to a specific true statement 
 * about its puzzle, indicating that exactly one of its neighbors is 
 * true and all its other neighbor Claims are false. The collection 
 * of all the Rules of a Puzzle is an expression of a system of 
 * equations. To solve this system, specific values are needed, akin 
 * to the boundary or initial values needed to solve a system of 
 * differential equations. An Init is such a value, and the collection 
 * of initial values provided for a sudoku puzzle is typically 
 * sufficient to specify a unique solution of the entire system of 
 * equations that the puzzle's Rules constitute.</p>
 * @author fiveham
 *
 */
public class Init extends Fact {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3700799253734450539L;
	
	private Claim claim;
	
	/**
	 * <p>Constructs an Init belonging to the specified <tt>puzzle</tt>, 
	 * with an initial capacity of {@value #CAPACITY}.</p>
	 * @param puzzle
	 */
	public Init(Puzzle puzzle) {
		super(puzzle, SIZE_WHEN_SOLVED);
	}
	
	@Override
	public boolean add(Claim e){
		return super.add(this.claim = e);
	}
	
	/**
	 * <p>Returns the last Claim {@link #add(Claim) added} to this 
	 * Init. Under ordinary circumstances, the Claim returned is 
	 * the sole element of this set.</p>
	 * @return the last Claim {@link #add(Claim) added} to this 
	 * Init
	 */
	public Claim claim(){
		return this.claim;
	}
}
