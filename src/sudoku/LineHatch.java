package sudoku;

import common.ComboGen;
import java.util.*;

/**
 * Represents a technique for solving sudoku puzzles.
 * When N Lines each have up to N cells capable of housing 
 * a certain value and all up-to-N^2 of those cells line 
 * up in N Lines of the opposite orientation, only 
 * those cells at the intersection points of the 
 * first set of lines and the complementary set 
 * of lines of opposite orientation can, out of 
 * all the cells in the lines of opposite 
 * orientation, house the value in question.
 * @author fiveham
 */
public class LineHatch extends Technique{
	
	/** 
	 * Minimum number of lines of each orientation 
	 * needed to have this analysis technique work.
	 */
	public static final int MIN_LINE_COUNT = 2;
	
	/** 
	 * Maximum number of lines of each orientation 
	 * that leaves this analysis technique able to work.
	 */
	public static final int MAX_LINE_COUNT = 7;
	
	/**
	 * Constructor.
	 * @param target			The target to which this 
	 * instace of this technique pertains.
	 */
	public LineHatch(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * For each value for which the target is not solved, 
	 * check each combination of rows and each 
	 * combination of columns for the arrangement of 
	 * cells possible for the current value that allows 
	 * this analysis technique to take action.
	 */
	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		// If the target is already solved, skip digestion
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		//iterate over the values
		for(Value currentValue : Value.KNOWN_VALUES){
			
			// check that the target is not solved for the current value 
			if( !puzzleIsSolvedForValue(puzzle, currentValue) ){
				
				// iterate over the combinations of rows and combinations of columns
				
				//Create a set of individual master sets.
				//One element is the master set of rows not solved for the current value,
				//and the other is the master set of columns not solved for the current value.
				List<List<Line>> multiUnsolved = new ArrayList<>();
				multiUnsolved.add(rowsNotSolvedForValue(currentValue));
				multiUnsolved.add(columnsNotSolvedForValue(currentValue));
				
				//Iterate over this set of sets
				for( List<Line> unsolveds : multiUnsolved ){
					if( unsolveds.size() >= MIN_LINE_COUNT ){
						for( List<Line> currentOriginatingCombination : new ComboGen<>(unsolveds, 
								inBounds(ComboGen.MIN_COMBO_SIZE, unsolveds.size(), MIN_LINE_COUNT), 
								inBounds(ComboGen.MIN_COMBO_SIZE, unsolveds.size(), MAX_LINE_COUNT))){
							puzzleHasUpdated |= digestEachLineSet(currentValue, currentOriginatingCombination);
						}
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	private boolean digestEachLineSet(Value currentValue, List<Line> currentOriginatingCombination){
		
		boolean puzzleHasUpdated = false;
		
		// get a list of complementary regions--columns or rows that intersect the
		// rows or columns (respectively) in the currentCombination at cells that 
		// have the currentValue still possible
		List<Line> complement = getComplement(puzzle, currentValue, currentOriginatingCombination);
		
		if( !complement.isEmpty() )
			// check whether propagating based on this xwing/swordfish/jellyfish/etc would do anything 
			if( propagationWouldHaveEffect(currentValue, currentOriginatingCombination, complement) )
				puzzleHasUpdated |= resolve( currentValue, currentOriginatingCombination, complement );
		
		return puzzleHasUpdated;
	}
	
	private List<Line> rowsNotSolvedForValue(Value value){
		List<Line> rows = new ArrayList<>();
		
		for(Line currentRow : puzzle.getRows())
			if( !currentRow.isSolvedForValue(value) )
				rows.add( currentRow);
		
		return rows;
	}
	
	private List<Line> columnsNotSolvedForValue(Value value){
		List<Line> columns = new ArrayList<>();
		
		for(Line currentRow : puzzle.getRows())
			if( !currentRow.isSolvedForValue(value) )
				columns.add( currentRow);
		
		return columns;
	}
	
	/*
	 * Returns whether marking the parameter value impossible in 
	 * those cells in the regions in the parameter complement that 
	 * are also not in the regions in the parameter originator 
	 * would change the possibility-states of any of those cells.
	 */
	private static boolean propagationWouldHaveEffect(Value value, 
			List<Line> originator, List<Line> complement){
		
		for(Line currentRecipient : complement )
			for( Cell currentCell : currentRecipient.getCells())
				if( !regionsContainCell(originator, currentCell) )		//If currentCell is not in any of the originator regions
					if( currentCell.isPossibleValue(value) )
						return true;
		return false;
	}
	
	/*
	 * Returns whether the parameter cell is contained in any of the 
	 * parameter regions.
	 */
	private static boolean regionsContainCell(List<Line> regions, Cell cell){
		for( Line currentLine : regions)
			if(currentLine.contains(cell))
				return true;
		return false;
	}
	
	/*
	 * If the Lines in originator together 
	 * have cells containing the parameter value as a possibility 
	 * at exactly N positions (N x-values for N rows or 
	 * N y-values for N columns), then a set of the N columns or 
	 * rows for those indices in the target is returned. If the 
	 * originator set does not have exactly N indices at which 
	 * the cells of its Lines can house the parameter value, 
	 * then an empty list is returned.
	 */
	private static List<Line> getComplement(Puzzle puzzle, Value value, 
			List<Line> originator){
		
		List<Line> complement = new ArrayList<Line>();
		
		// get a list of the coordinates for which the originating regions
		// have at least one cell that could hold the value in question
		List<Index> ableCellCoords = new ArrayList<Index>();
		for( Index currentCoordinate : Index.KNOWN_VALUES )
			for( Line currentOriginatingRegion : originator)
				if( currentOriginatingRegion.getCell(currentCoordinate).isPossibleValue(value) )
					ableCellCoords.add(currentCoordinate);
		
		// if there are too few or too many, return the empty list.
		if(ableCellCoords.size() != originator.size())
			return complement;
		
		// add all the complementary regions to the complement list
		if(originator.get(0).isRow()){							//originator is made of rows
			for( Index index : ableCellCoords )
				complement.add( puzzle.getColumn(index) );		//complement is made of columns
		}
		else{													//originator is made of columns
			for( Index index : ableCellCoords )
				complement.add( puzzle.getRow(index) );			//complement is made of rows
		}
		
		return complement;
	}
	
	/*
	 * Returns whether all nine instances of the parameter value 
	 * have been located in the parameter target.
	 */
	private static boolean puzzleIsSolvedForValue(Puzzle puzzle, Value value){
		
		for(Box currentBlock : puzzle.getBlocks())
			if( !currentBlock.isSolvedForValue(value) )
				return false;
		return true;
	}
	
	/*
	 * Marks the parameter value list impossible in those cells in the 
	 * parameter recipient regions that are not also in the parameter 
	 * originator regions.
	 * 
	 * Returns whether changes were made to any of the recipient regions.
	 * @param value				The value to be marked impossible in 
	 * certain cells in the regions in the parameter recipient.
	 * @param originator		A set of rows or columns to be used in 
	 * determining which cells in the regions in the parameter 
	 * recipients are not to have the parameter value marked impossible.
	 * @param recipient			A set of columns or rows that are to 
	 * have the parameter value marked impossible in certain cells.
	 * @return					Returns whether changes were made to 
	 * any of the recipient regions.
	 */
	private static boolean resolve(Value value, List<Line> originators, List<Line> recipients){
		boolean returnValue = false;
		
		for( Line currentRecipient : recipients )
			returnValue |= currentRecipient.propagateValueOutsideRegions(value, originators);
		
		return returnValue;
	}
}
