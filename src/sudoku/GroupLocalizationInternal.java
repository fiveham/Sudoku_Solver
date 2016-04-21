package sudoku;

import java.util.List;
import java.util.Set;

import common.ComboGen;

import java.util.HashSet;

/**
 * A technique for solving sudoku puzzles.
 * If a set of N cells in a region are the 
 * only cells that could contain N certain 
 * values, then those cells cannot hold any 
 * values outside that set of N values.
 * @author fiveham
 */
public class GroupLocalizationInternal extends Technique{
	
	/*
	 * This technique is probably unnecessary.
	 * 
	 * If cells A, B, and C are the only cells to
	 * contain the possibilities of 1, 2, and 3, then
	 * together cells D, E, F, G, H, and I have a 
	 * combined possible value set of 4, 5, 6, 7, 8, 
	 * and 9.  From that combination, we can propagate
	 * based on the rules of GLE and mark all 6 of those
	 * values impossible in cells 1, 2, and 3.
	 */
	
	/** Least number of values that can lead to a valid analysis */
	public static final int MIN_VALUES_COUNT = 2;
	
	/** Greatest number of values that can lead to a valid analysis */
	public static final int MAX_VALUES_COUNT = 7;
	
	/**
	 * Constructor.
	 * @param target			The target to which this
	 * instance of this technique pertains.
	 */
	public GroupLocalizationInternal(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Look in all the regions. For each one, looks at all 
	 * the properly-sized combinations of values that haven't
	 * been solved for in that region.
	 * @return					Returns whether any changes 
	 * were made as the result of this analysis.
	 */
 	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		// iterate over the regions
		for(Region currentRegion : puzzle.getRegions())
			
			// iterate over the properly sized combinations of values
			for(List<Value> valueCombo : 
					new ComboGen<>(currentRegion.unsolvedValues(), 
							LineHatch.inBounds(0, MIN_VALUES_COUNT, currentRegion.unsolvedValues().size()), 
							LineHatch.inBounds(0, MAX_VALUES_COUNT, currentRegion.unsolvedValues().size()))){
				
				// See whether those values can be said to occur only in a 
				// certain subset of the cells in the region--a subset with
				// as many elements as the current value combination
				
				// get the list of cells in the region in which these values are possible
				Set<Cell> cellsList = getCellsSet(currentRegion, valueCombo);
				
				// Check that the number of cells that have any of
				// the values in question is equal to the 
				// count of the values in question.
				if(cellsList.size() == valueCombo.size())
					
					// check that propagation would do something
					//if( cellsHaveOtherPossibleValues(valueCombo, cellsList) ){
					//	resolve(valueCombo, cellsList);
					//	puzzleHasUpdated = true;
					//}
					puzzleHasUpdated |= resolve(valueCombo, cellsList);
			}
		
		return puzzleHasUpdated;
	}
 	
 	
 	private static boolean resolve(List<Value> values, Set<Cell> cells){
 		boolean returnValue = false;
 		
 		// iterate over the cells
 		for(Cell currentCell : cells)
 			// mark all the values other than those in the values list 
 			// impossible in the current cell
 			for( Value currentValue : Value.KNOWN_VALUES )
 				if( !values.contains(currentValue) )
 					returnValue |= currentCell.setImpossibleValue(currentValue);
 		
 		return returnValue;
 	}
 	
 	/* 
 	 * Returns the list of cells in the region in which these values are possible 
 	 */
 	private static Set<Cell> getCellsSet(Region region, List<Value> valuesSet){
 		Set<Cell> returnSet = new HashSet<Cell>();
 		
 		for(int i=0; i<valuesSet.size(); i++)
 			returnSet.addAll( region.cellsPossibleForValue(valuesSet.get(i)) );
 		
 		return returnSet;
 	}
 	
 	/*
 	 * Returns true if at least one cell in the passed region
 	 * other than the cells in the passed list of cells has 
 	 * at least one of the values in the passed list of values
 	 * not marked as impossible or certain.
 	 */
 	/*private static boolean cellsHaveOtherPossibleValues(Set<Value> values, Set<Cell> cells){
 		
 		//iterate over the cells in the list
 		for(int i=0; i<cells.size(); i++)
 			// iterate over all values
 			for(Value currentValue : Value.KNOWN_VALUES)
 	 			if(!values.contains(currentValue) && cells.get(i).isPossibleValue(currentValue))
 	 				return true;
 			
 		return false;
 	}/**/
	
 	/*
 	 * Returns a list of all combinations of values 
 	 * that are not solved-for in the parameter region.
 	 */
	/*private static Set<Set<Value>> getUnsolvedValueCombinations(Region region){
		
		// get list of un-solved-for values in the region
		Set<Value> unsolvedValues = new Set<Value>();
		for(Value currentValue : Value.KNOWN_VALUES)
			if(!region.isSolvedForValue(currentValue))
				unsolvedValues.add(currentValue);
		
		return Set.powerSet(unsolvedValues, MIN_VALUES_COUNT, Math.min(MAX_VALUES_COUNT, unsolvedValues.size()));
	}/**/
 	
	/*
	 * For each cell in the parameter cells list, marks each value
	 * not in the parameter values list impossible.
	 * 
	 * Returns whether any changes were made.
	 */
}
