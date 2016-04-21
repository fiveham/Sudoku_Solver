package sudoku;

import java.util.*;

/**
 * Represents a technique for solving sudoku puzzles
 * where a cell is determined to hold a certain 
 * value if it has been determined that no other 
 * values can be that cell's value.
 * @author fiveham
 */
public class CellDeath extends Technique{
	
	/**
	 * The number of values possible for a given
	 * cell in order to set its value.
	 */
	public static final int POSSIBLE_VALUES_COUNT = 1;
	
	/**
	 * Constructor. Passes the target to be analysed 
	 * to the superclass constructor.
	 * @param target				The target to be 
	 * analysed.
	 */
	public CellDeath(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Looks at every cell in the target; if 
	 * any cell can only hold one value, sets
	 * the value of that cell to that value.
	 * @return					Returns whether 
	 * any changes to the target have been made 
	 * as a result of this analysis.
	 */
	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		for(Cell currentCell : puzzle.getCells()){
			List<Value> possibleValues = currentCell.getPossibleValues();
			if(possibleValues.size() == POSSIBLE_VALUES_COUNT)
				for(Value value : possibleValues)
					puzzleHasUpdated |= currentCell.setValue( value );
		}
		return puzzleHasUpdated;
	}
}