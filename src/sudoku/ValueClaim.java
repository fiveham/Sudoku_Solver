package sudoku;

import java.util.*;

/**
 * A technique for solving sudoku puzzles.
 * If all of the cells from region A (a 
 * block, row, or column) that could 
 * house a certain value are also in 
 * region B (a block, row, or column), 
 * then none of the cells in region B 
 * other than those that are also in 
 * region A could house the value in 
 * question.
 * @author fiveham
 */
public class ValueClaim extends Technique{

	/**
	 * Minimum number of cells from originating region 
	 * also in receiving region needed to mark cells 
	 * not in both regions as impossible for the value 
	 * in question.
	 */
	public static final int MIN_CELLS_COUNT = 2;
	
	/**
	 * Maximum number of cells from originating region 
	 * also in receiving region needed to mark cells 
	 * not in both regions as impossible for the value 
	 * in question.
	 */
	public static final int MAX_CELLS_COUNT = 3;
	
	private static final int SHARED_ROW = 1;
	private static final int SHARED_COLUMN = -1;
	private static final int NO_SHARED_LINE = 0;
	
	/**
	 * Constructor.
	 * @param target		The target to which this 
	 * instance of this technique pertains.
	 */
	public ValueClaim(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Skips analysis if the target is already solved.
	 * For each meaningful value, examines each block 
	 * to see if all its cells possible for the current 
	 * value are also in a common row or column, and 
	 * examines each row and each column to see whether 
	 * all its cell possible for the current value 
	 * are all in the same block.
	 * @return					Returns whether any 
	 * changes were made as a result of this analysis.
	 */
	public boolean digest(){
		
		//final int MIN_CELLS_TO_AFFECT = 1;
		
		boolean puzzleHasUpdated = false;
		
		// skip digestion if the target is already solved
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		// iterate over all the values 
		for(Value currentValue :  Value.KNOWN_VALUES){
			
			// iterate over the blocks
			for(Box currentBlock : puzzle.getBlocks()){
				
				// Check if the number of cells possible for the current value
				// is in the range to pertain to a claim
				List<Cell> possibleCellsList = currentBlock.cellsPossibleForValue(currentValue);
				if(MIN_CELLS_COUNT <= possibleCellsList.size() && possibleCellsList.size() <= MAX_CELLS_COUNT){
					
					// Check whether all the cells share a row or column, and determine which is the case if so.
					List<Line> lineBox = new ArrayList<>();		//Empty container to receive line in the method called on next line.
					int sharedLineType = sharedLineType(possibleCellsList, lineBox);
					
					//Extract the Line from the lineBox.
					Line sharedLine = null;
					for(Line l : lineBox)
						sharedLine = l;				//lineBox should only have one element; so, sharedLine contains the line that sharedLineType() determined to be the shared Line.
					
					switch(sharedLineType){
						case SHARED_ROW:
							puzzleHasUpdated |= sharedLine.propagateValueOutsideRegion(currentValue, currentBlock);
							break;
						case SHARED_COLUMN:
							puzzleHasUpdated |= sharedLine.propagateValueOutsideRegion(currentValue, currentBlock);
					}
				}
			}
		
		
			// iterate over all the rows and columns
			ArrayList<Region> rowsAndColumns = new ArrayList<>();
			for(Line currentRow : puzzle.getRows())
				rowsAndColumns.add( (Region) currentRow );
			for(Line currentColumn : puzzle.getColumns())
				rowsAndColumns.add( (Region) currentColumn );
			for(Region currentOriginator : rowsAndColumns){
				
				// Check if the number of cells possible for the current value
				// in the current region is in the range to pertain to a claim
				List<Cell> possibleCells = currentOriginator.cellsPossibleForValue(currentValue);
				if(MIN_CELLS_COUNT <= possibleCells.size() && possibleCells.size() <= MAX_CELLS_COUNT){
					
					// Check whether all the cells share a common block
					List<Box> blockBox = new ArrayList<>();
					if( shareCommonBlock(possibleCells, blockBox)){
						
						//Extract the Box from blockBox.
						Box receivingBlock = null;
						for( Box b : blockBox )
							receivingBlock = b;
						
						// propagate impossibilities in the receiving region
						puzzleHasUpdated |= receivingBlock.propagateValueOutsideRegion(currentValue, currentOriginator);
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	/**
	 * Returns whether the cells in the specified set share a Box.
	 * Stores that Box, if it exists, in the list blockBox.
	 * @param cells
	 * @param blockBox
	 * @return
	 */
	private static boolean shareCommonBlock(List<Cell> cells, List<Box> blockBox){
		List<Box> boxes = new ArrayList<>();
		
		for(Cell c : cells)
			boxes.add(c.getBlock());
		
		if(boxes.size() == 1){		//If they're all the same block, the set will end up with only one entry.
			blockBox.addAll(boxes);
			return true;
		}
		return false;
		
	}
	
	/**
	 * Returns an int value indicating what kind of line the 
	 * cells in the specified set share and adds this line to
	 * the specified set of lines.
	 * @param cells
	 * @param lineBox
	 * @return
	 */
	private static int sharedLineType(List<Cell> cells, List<Line> lineBox){
		List<Line> rows = new ArrayList<>();
		List<Line> columns = new ArrayList<>();
		
		for(Cell c : cells){
			rows.add(c.getRow());
			columns.add(c.getColumn());
		}
		
		if(rows.size()==1){
			for(Line r : rows)
				lineBox.add(r);
			return SHARED_ROW;
		}
		
		if(columns.size()==1){
			for(Line c : columns)
				lineBox.add(c);
			return SHARED_COLUMN;
		}
		
		return NO_SHARED_LINE;
	}
	
	/*
	 * Marks the parameter value impossible in all cells in the receiving 
	 * region other than those cells that are in the originating region.
	 */
	/*private static boolean resolve(Region claimOriginator, Region claimReceiver, Value value){
		return claimReceiver.propagateValueOutsideRegion(value, claimOriginator);
	}/**/
}
