package sudoku;

import java.util.Collections;
import sudoku.time.FalsifiedTime;

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
	private static final long serialVersionUID = 7730369667106322962L;
	
	private final Claim claim;
	
	/**
	 * <p>Constructs an Init belonging to the specified {@code puzzle}, 
	 * with an initial capacity of {@value #CAPACITY}.</p>
	 * @param puzzle
	 */
	public Init(Puzzle puzzle, Claim c) {
		super(puzzle, Collections.singletonList(c));
		this.claim = c;
	}
	
	/**
	 * <p>Returns the Claim that this Init marks true.</p>
	 * @return the Claim that this Init marks true
	 */
	public Claim claim(){
		return claim;
	}
	
	@Override
	public void validateState(FalsifiedTime time){
		Debug.log("Initial-value verifying "+claim);
		if(!claim.isKnownTrue()){
			claim.setTrue(time);
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("An Init verifying ");
		for(Claim c : this){
			sb.append(c);
		}
		return sb.toString();
	}
}
