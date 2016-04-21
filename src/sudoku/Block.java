package sudoku;

import java.util.ArrayList;

/**
 * Represents a 3x3 block in a sudoku target, 
 * also known as a box.
 * @author fiveham
 */
public class Block extends Region{
	
	/** The height of a block measured in cells */
	public static final int HEIGHT = 3;
	
	/** The width of a block measured in cells */
	public static final int WIDTH = 3;
	
	/** The x-coordinate of the left edge of the block */
	private Index minX;
	
	/** The x-coordinate of the right edge of the block */
	private Index maxX;
	
	/** The y-coordinate of the top edge of the block */
	private Index minY;
	
	/** The y-coordinate of the bottom edge of the block */
	private Index maxY;
	
	/**
	 * Constructor. Makes sure the parameter cells have the 
	 * 3-by-3 arrangement needed for a block and are positioned
	 * in the appropriate location for a block that has the  
	 * parameter index as its index.
	 * @param index					Position-indicating index
	 * of this block. Indices begin in the top-left of the 
	 * target and increment left-to-right then top-to-bottom.
	 * @param cells					The cells in this block.
	 * @param target				The target to which this 
	 * block belongs.
	 */
	/*
	 * Constructor is package-private to prevent external code from 
	 * arbitrarily creating Blocks.
	 */
	Block(Index index, Cell[] cells, Puzzle puzzle){
		super(index, cells, puzzle);
		
		// Check arrangement of parameter cells in their target of origin
		if( !hasProperArragement(cells) )
			throw new IllegalArgumentException("Bad cells array");
		
		// set the min and max X and Y
		minX = properMinXForBlockIndex(index);
		maxX = Index.fromInt( WIDTH + minX.toInt() - 1 );
		// depending on the blockIndex
		minY = properMinYForBlockIndex(index);
		maxY = Index.fromInt( HEIGHT + minY.toInt() - 1 );
	}
	
	/*
	 * Returns whether the parameter cells have the proper
	 * arrangement for a block with this block's index.
	 * 
	 * This method is private and only called by the constructor
	 * and relies on this block's index having already been 
	 * set by a call to the superclass's constructor.
	 * @param cells				The cells to be checked for 
	 * a proper arrangement for a block with this block's
	 * index.
	 * @return					Returns whether the 
	 * parameter cells have the proper arrangement 
	 * for a block with this block's index.
	 */
	private boolean hasProperArragement(Cell[] cells){
		
		final int CELLS_PER_COORD_PAIR = 1;
		
		// get min and max X and Y
		int minX = Index.MAXIMUM.toInt() + 1;
		int maxX = Index.MINIMUM.toInt() - 1;
		int minY = Index.MAXIMUM.toInt() + 1;
		int maxY = Index.MINIMUM.toInt() - 1;
		for( Cell currentCell : cells ){
			int currentX = currentCell.getX().toInt();
			int currentY = currentCell.getY().toInt();
			
			minX = (currentX < minX)?currentX:minX;
			maxX = (maxX < currentX)?currentX:maxX;
			minY = (currentY < minY)?currentY:minY;
			maxY = (maxY < currentY)?currentY:maxY;
		}
		
		// return false if cells don't fit properly into a 3x3 box like they should
		if( maxX - minX + 1 != WIDTH )
			return false;
		if( maxY - minY + 1 != HEIGHT )
			return false;
		
		// return false if the min x and y are not proper values (1,4,7)
		// has to be the correct 1, 4, or 7 depending on the block index
		if( minX != properMinXForBlockIndex(index).toInt() )
			return false;
		if( minY != properMinYForBlockIndex(index).toInt() )
			return false;
		
		for(int x = minX; x <= maxX; x++){
			for(int y = minY; y <= maxY; y++){
				
				/* get a list of cells with position x,y */
				ArrayList<Cell> cellsList = new ArrayList<Cell>();
				for(Cell currentCell : cells){
					if( currentCell.getX().toInt() == x && currentCell.getY().toInt() == y)
						cellsList.add(currentCell);
				}
				
				// If there are no cells or multiple cells in the array for the given x and y,
				// then the listed combination of cells cannot be valid for a block
				if(cellsList.size() != CELLS_PER_COORD_PAIR )
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the correct min x-coordinate for a block 
	 * with the specified index.
	 * @param blockIndex		The index for which 
	 * a block's proper min x-coordinate is to be returned.
	 * @return					Returns the correct min 
	 * x-coordinate for a block with the specified index.
	 */
	public static Index properMinXForBlockIndex(Index blockIndex){
		final int[] minXByBlockIndex = {1, 4, 7, 1, 4, 7, 1, 4, 7};
		return Index.fromInt(minXByBlockIndex[blockIndex.toInt() - 1]);
	}
	
	/**
	 * Returns the correct min y-coordinate for a block the 
	 * index of which is the parameter index.
	 * @param index				The index for which 
	 * a block's proper min y-coordinate is to be returned.
	 * @return					Returns the correct min
	 * y-coordinate for a block the index of which is 
	 * the parameter index.
	 */
	public static Index properMinYForBlockIndex(Index index){
		final int[] minYByBlockIndex = {1, 1, 1, 4, 4, 4, 7, 7, 7};
		return Index.fromInt(minYByBlockIndex[index.toInt() - 1]);
	}
	
	/**
	 * Returns the min x-coordinate of this block.
	 * @return					Returns the min 
	 * x-coordinate of this block.
	 */
	public Index getMinX(){
		return minX;
	}
	
	/**
	 * Returns the max x-coordinate of this block.
	 * @return					Returns the max 
	 * x-coordinate of this block.
	 */
	public Index getMaxX(){
		return maxX;
	}
	
	/**
	 * Returns the min y-coordinate of this block.
	 * @return					Returns the min 
	 * y-coordinate of this block.
	 */
	public Index getMinY(){
		return minY;
	}
	
	/**
	 * Returns the max y-coordinate of this block.
	 * @return					Returns the max 
	 * y-coordinate of this block.
	 */
	public Index getMaxY(){
		return maxY;
	}
	
	/**
	 * Returns a textual representation of this block
	 * identifying that this is a block and its index.
	 * @return					Returns a textual 
	 * representation of this block identifying that 
	 * this is a block and its index.
	 */
	public String toString(){
		return "Box " + index.toInt();
	}
}
