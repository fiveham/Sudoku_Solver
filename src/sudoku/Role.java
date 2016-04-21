package sudoku;

import java.util.*;

/**
 * A role played by a symbol in a sudoku target, such as 
 * "the 7 in row 5", "the 1 in box 8", "the 6 in column 3", or 
 * POSSIBLY "the value in cell x,y"
 * 
 * The fact that each number in a sudoku target plays several 
 * roles is the key to solving the target, and representing 
 * those roles is the first step toward describing the solution 
 * process in terms of simple logical statements.
 * @author fiveham
 *
 */
public class Role {

	private final Puzzle puzzle;
	private final Value  value;
	private final Region region;
	
	private Symbol symbol = null;
	
	public Role(Puzzle p, Value v, Region r){
		value = v;
		puzzle = p;
		region = r;
	}
	
}
