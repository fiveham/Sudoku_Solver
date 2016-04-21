package sudoku;

import common.ComboGen;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a technique for solving sudoku puzzles.
 * If there exists a set of N cells in a region (box, 
 * row, or column)  which together have a collective 
 * set of exactly N possible values, then no other 
 * cells in the region can hold any of those values.
 * @author fiveham
 */
public class GroupLocalizationExternal extends Technique{
	
	/** Least number of cells that can be used to take action */
	public static final int MIN_CELL_COUNT = 2;
	
	/** Greatest number of cells that can be used to take action */
	public static final int MAX_CELL_COUNT = 7;
	
	/**
	 * Constructor.
	 * @param target				The sudoku target 
	 * to which this instance of this solution 
	 * technique pertains.
	 */
	public GroupLocalizationExternal(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Looks in every region and looks at every validly 
	 * sized combination of unsolved cells in that region
	 * and checks them each for a combined set of possible
	 * values the same size as the set of cells. If this 
	 * is found, then check whether setting those values 
	 * to impossible in the other cells in the region 
	 * would change any possibility values. If some values 
	 * would get changed, then set all the values in the 
	 * current value set to impossible in all the cells 
	 * in the region other than those in the current cell 
	 * set.
	 * @return					Returns whether any changes 
	 * were made to the target as a result of this analysis.
	 */
	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		// iterate over the regions
		for(Region currentRegion : puzzle.getRegions()){
			
			Set<Cell> unsolveds = currentRegion.unsolvedCells();
			
			if(unsolveds.size() >= MIN_CELL_COUNT){
			// iterate over the properly sized combinations of unsolved cells
				for( List<Cell> currentCombination : 
						new ComboGen<>(unsolveds, 
								LineHatch.inBounds(0, MIN_CELL_COUNT, unsolveds.size()), 
								LineHatch.inBounds(0, MAX_CELL_COUNT, unsolveds.size()))){
				
					// check that the size of the combined set of candidate values
					// from all the cells in the current combination is
					// equal to the size of the current combination
					// e.g. four cells with a total of four unassigned values
					Set<Value> values = getValuesSet(currentCombination);
					
					if(values.size() == currentCombination.size())
						
						// This seems to mean that propagation is in order
						// (unless it wouldn't change any possibilities)
						
						// check that propagation would do something
						//if( regionHasOtherCellsPossibleForValues(currentRegion, currentCombination, values) )
						if( !currentCombination.equals(currentRegion.cellsPossibleForValues(values) ))
							puzzleHasUpdated |= resolve(currentRegion, currentCombination, values);
				}
			}
		}
		return puzzleHasUpdated;
	}
	
	/*
	 * returns a set containing all the combinations of
	 * unsolved cells in a region.  Each combination is
	 * contained as a set, as well.
	 * @param region
	 * @return
	 */
	/*private Set<Set<Cell>> getUnsolvedCellCombinations(Region region){
		
		// get the unsolved cells in the region
		Set<Cell> unsolvedCells = new Set<Cell>();
		for(Cell currentCell : region.getCells())
			if(!currentCell.isSolved())
				unsolvedCells.add(currentCell);
		
		//return the partial power set of the set of unsolved cells
		return Set.powerSet(unsolvedCells, MIN_CELL_COUNT, Math.min(MAX_CELL_COUNT, unsolvedCells.size()));
	}/**/
	
	/*
	 * Returns a list of all the values that are possible 
	 * values of the cells from a parameter cell list.
	 */
	private Set<Value> getValuesSet(List<Cell> cells){
		Set<Value> allValues = new HashSet<Value>();
		
		for( int i=0; i<cells.size(); i++ ){
			Cell currentCell = cells.get(i);
			for(Value currentValue : Value.KNOWN_VALUES)
				if(currentCell.isPossibleValue(currentValue))
					allValues.add(currentValue);
		}
		
		return allValues;
	}
	
	/*
	 * Returns true if at least one cell in the parameter region
	 * other than the cells in the parameter cells list has 
	 * at least one of the values in the parameter values list
	 * not marked as impossible or certain.
	 * @param region
	 * @param cells
	 * @param values
	 * @return
	 */
	/*private static boolean regionHasOtherCellsPossibleForValues(Region region, 
			Set<Cell> cells, Set<Value> values){
		
		Set<Cell> possibleCells = new Set<Cell>();
		for(Value value : values)
			possibleCells.addAll(region.cellsPossibleForValue(value));
		
		if( possibleCells.minus(cells).size() > 0)
			return true;
		return false;
	}/**/
	
	/*
	 * Marks all the values in the values list as impossible 
	 * in each of the cells in the cells list.
	 * 
	 * Returns whether any changes were made.
	 */
	private static boolean resolve(Region region, List<Cell> cells, Set<Value> values){
		boolean returnValue = false;
		
		// iterate over the cells in the region other than 
		// those in the cells list
		for(Cell currentCell : region.getCells())
			if(!cells.contains(currentCell))
				
				// mark all the entries in the values list
				// as impossible in the current cell
				for(Value currentValue : values)
					returnValue |= currentCell.setImpossibleValue(currentValue);
		return returnValue;
	}
}
