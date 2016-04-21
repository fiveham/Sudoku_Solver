package sudoku;

import java.util.Set;

/**
 * Represents a technique for solving sudoku puzzles.
 * If only one cell in any block, row, or column is 
 * not incapable of containing a given value, then 
 * that value must be that cell's value.
 * @author fiveham
 */
class OrganFailure extends Technique{
	
	/** 
	 * The number of cells in a given block that should be able to
	 * contain a given value in order to set the value in that cell.
	 */
	public static final int POSSIBLE_CELLS_COUNT = 1;
	
	/**
	 * Constructor.
	 * @param target			The target to which this 
	 * instance of this analysis technique pertains.
	 */
	public OrganFailure(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Looks in every block, row, and column, and iterates 
	 * over every value. If only one cell in the current 
	 * region can contain the current value, then that 
	 * cell's value is set to the current value.
	 * @return					Returns whether any changes 
	 * were made due to this analysis.
	 */
	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		// iterate over the regions: blocks, rows, columns
		for(Region currentRegion : puzzle.getRegions())
			
			// iterate over all valid values
			for(Value currentValue : Value.KNOWN_VALUES)
				
				// check whether the current region is solved for the current value
				if( !currentRegion.isSolvedForValue(currentValue) ){
					
					// create a list of cells that are neither known to
					// be unable to contain the current value
					// nor known to already contain the current value
					Set<Cell> possibleCellsList = currentRegion.cellsPossibleForValue(currentValue);
					
					// If there's only one cell in the current region that is capable of containing
					// the value in question, then that's the cell in the current region that holds that value;
					// so, propagate an OrganFailure based on that region, cell, and value.
					if( possibleCellsList.size() == OrganFailure.POSSIBLE_CELLS_COUNT )
						for(Cell onlyCell : possibleCellsList)
							puzzleHasUpdated |= onlyCell.setValue(currentValue);
				}
		
		return puzzleHasUpdated;
	}
}