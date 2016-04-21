package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents a portion of a sudoku target that has
 * nine cells and houses one of each of the nine 
 * values.
 * @author fiveham
 */
public abstract class Region{
	
	/*
	 * This class should remain public to ensure its 
	 * methods are accessible to external code, should 
	 * any external code need to access certain methods 
	 * of regions in, for example, designing a new 
	 * analysis technique.
	 */
	
	/** The number of cells in a region */
	public static final int CELL_COUNT = 9;
	
	protected Cell[] cells;
	protected Puzzle puzzle;
	protected Index index;
	
	/*
	 * Constructor. Verifies that the correct number of 
	 * cells was supplied and that all parameter cells 
	 * belong to the parameter target. Validates the 
	 * parameter index.
	 * @param index					Positional index of
	 * the region. For rows and columns, indicates Y or
	 * X coordinates respectively. For blocks, indicates
	 * position starting from top-left, moving 
	 * left-to-right, top-to-bottom.
	 * @param cells					Cells in the region.
	 * @param target				The target to which 
	 * the region belongs.
	 */
	/*
	 * Constructor privacy is set package-private (default (no modifier)) 
	 * to permit access to existing subclasses Line and Box which share 
	 * this package while restricting access to any externally-created 
	 * new kinds of regions.
	 */
	Region(Index index, Cell[] cells, Puzzle puzzle){
		
		// Check cell count
		if( cells.length != CELL_COUNT )
		throw new IllegalArgumentException("Bad cell count");
		
		// Check passed cells versus passed target
		for( Cell currentCell : cells )
			if( currentCell.getPuzzle() != puzzle )
				throw new IllegalArgumentException("Cell not contained in target");
		
		// Check IndexValue
		if(index == Index.UNKNOWN)
			throw new IllegalArgumentException("UNKNOWN index");
		
		this.cells = cells;
		this.puzzle = puzzle;
		this.index = index;
	}
	
	/**
	 * Returns all the unsolved cells in the region
	 * @return
	 */
	public Set<Cell> unsolvedCells(){
		Set<Cell> returnSet = new HashSet<Cell>();
		
		for(Cell c : cells)
			if( !c.isSolved() )
				returnSet.add(c);
		
		return returnSet;
	}
	
	/**
	 * Returns all the solved cells in the region
	 * @return
	 */
	public Set<Cell> solvedCells(){
		Set<Cell> returnSet = new HashSet<Cell>();
		
		for(Cell c : cells)
			if( c.isSolved() )
				returnSet.add(c);
		
		return returnSet;
	}
	
	/**
	 * Returns a set of values for which this region 
	 * is unsolved
	 * @return
	 */
	public Set<Value> unsolvedValues(){
		Set<Value> retVal = new HashSet<>();
		
		for(Value v : Value.KNOWN_VALUES)
			if(isSolvedForValue(v))
				retVal.add(v);
		
		return retVal;
	}
	
	/**
	 * Reports which of the region's cells are not unable 
	 * to contain the parameter Value.
	 * @param value					The value for which a list
	 * of cells that could have that value as their value
	 * is returned.	
	 * @return						A list of the cells in the region
	 * that could contain the parameter Value.
	 */
	public Set<Cell> cellsPossibleForValue(Value value){
		Set<Cell> returnList = new HashSet<>();
		
		for(Cell currentCell : cells)
			if(currentCell.isPossibleValue(value))
				returnList.add(currentCell);
		
		return returnList;
	}
	
	public <T extends Collection<Value>> Set<Cell> cellsPossibleForValues(T collection){
		Set<Cell> returnSet = new HashSet<>();
		
		for(Value val : collection){
			returnSet.addAll( cellsPossibleForValue(val) );
		}
		
		return returnSet;
	}
	
	/**
	 * Marks the parameter Value as impossible in those 
	 * cells in this region that are not in the parameter
	 * Region.
	 * @param value				The Value to be marked 
	 * impossible in those cells in this Region that are
	 * not also in the parameter Region.
	 * @param region			The Region whose cells 
	 * that are also in this region will not have the
	 * parameter Value marked impossible in them.
	 * @return					Returns whether any 
	 * changes were made to this region.
	 */
	public boolean propagateValueOutsideRegion(Value value, Region region){
		boolean retVal = false;
		for(Cell c : cells)
			if( !region.contains(c))
				retVal |= c.setImpossibleValue(value);
		return retVal;
	}
	
	/**
	 * Marks the parameter Value as impossible in those 
	 * cells in this region that are not in the Regions
	 * from the parameter collection of Regions.
	 * @param value				The Value to be marked 
	 * impossible in those cells in this Region that are
	 * not also in the parameter Regions.
	 * @param regions			The Regions whose cells 
	 * that are also in this region will not have the
	 * parameter Value marked impossible in them.
	 * @return					Returns whether any 
	 * changes were made to this region.
	 */
	public boolean propagateValueOutsideRegions(Value value, Collection<? extends Region> regions){
		
		//Get a set containing only the cells in this region that
		//are also not in any of the specified regions.
		Set<Cell> propCells = cells();
		for(Region r : regions)
			propCells.removeAll( r.cells() );
		
		//Propagate impossibility to all of those cells and report 
		//whether any changes were made as a result.
		boolean retVal = false;
		for(Cell c : propCells)
			retVal |= c.setImpossibleValue(value);
		return retVal;
	}
	
	/**
	 * Marks the parameter Values as impossible in those 
	 * cells in this region that are not in the Regions
	 * from the parameter collection of Regions.
	 * @param value				The Values to be marked 
	 * impossible in those cells in this Region that are
	 * not also in the parameter Regions.
	 * @param regions			The Regions whose cells 
	 * that are also in this region will not have the
	 * parameter Values marked impossible in them.
	 * @return					Returns whether any 
	 * changes were made to this region.
	 */
	public boolean propagateValuesOutsideRegions(Collection<Value> values, Collection<? extends Region> regions){
		boolean retVal = false;
		for(Value v : values)
			retVal |= propagateValueOutsideRegions(v, regions);
		return retVal;
	}

	/**
	 * Returns whether the parameter cell is one of 
	 * the cells in this Region.
	 * @param cell			Cell to be tested for
	 * whether it's in this region.
	 * @return				Returns whether the 
	 * parameter cell is one of the cells in this 
	 * Region.
	 */
	public boolean contains(Cell cell){
		for(Cell currentCell : cells)
			if(currentCell == cell)
				return true;
		return false;
	}
	
	/**
	 * Returns whether all the cells in the parameter cell 
	 * list are contained in this region.
	 * @param cellsList				List of cells to be checked 
	 * for membership in this region.
	 * @return						Returns whether all the cells
	 * in the parameter cell list are contained in this region.
	 */
	public boolean containsAll(ArrayList<Cell> cellsList){
		for(int i=0; i<cellsList.size(); i++)
			if(!contains(cellsList.get(i)))
				return false;
		return true;
	}
	
	/*
	 * Returns true if any of the parameter regions contain the
	 * parameter cell. Returns false otherwise.
	 * @param regions			Regions to be checked for
	 * possession of the parameter cell.
	 * @param cell				Cell whose possession is to 
	 * be checked for in the parameter regions.
	 * @return					Returns whether any of the 
	 * parameter Regions contain the parameter cell.
	 */
	/*private boolean regionsContainCell(Collection<? extends Region> regions, Cell cell){
		for(Region region : regions)
			if(region.contains(cell))
				return true;
		return false;
	}/**/
	
	/**
	 * Returns whether this region is solved for the 
	 * parameter Value
	 * @param currentValue
	 * @return
	 */
	public boolean isSolvedForValue(Value value){
		for(Cell currentCell: cells)
			if(currentCell.getValue() == value)
				return true;
		return false;
	}
	
	/**
	 * Returns whether the region has errors in it in terms
	 * of how many instances of any single value it has in it
	 * and how many cells in it could possibly hold a given
	 * value.
	 * @return				Returns whether the region has 
	 * errors in it in terms of how many instances of any 
	 * single value it has in it and how many cells in it 
	 * could possibly hold a given value.
	 */
	public boolean hasError(){
		
		final int MAX_CERTAINS = 1, MAX_IMPOSSIBLES = 8;
		
		for(Value currentValue : Value.KNOWN_VALUES){
			int impossibleCount = 0;
			int certainCount = 0;
			for(Cell currentCell : cells){
				Possibility currentPossibility = currentCell.getPossibility(currentValue);
				if(currentPossibility == Possibility.IMPOSSIBLE){
					impossibleCount++;
				}
				else if(currentPossibility == Possibility.CERTAIN){
					certainCount++;
				}
			}
			
			if(certainCount > MAX_CERTAINS)
				return true;
			if(impossibleCount > MAX_IMPOSSIBLES)
				return true;
			if(certainCount != 0 && impossibleCount != cells.length - certainCount)
				return true;
		}
		return false;
	}
	
	/**
	 * Returns a String describing the errors in the region,
	 * e.g. whether any values have been marked impossible in
	 * all cells or any values have been marked as the value 
	 * of multiple cells.
	 * @return				Returns a String describing the 
	 * errors in the region, e.g. whether any values have 
	 * been marked impossible in all cells or any values 
	 * have been marked as the value of multiple cells.
	 */
	public String error(){
		String returnString = "";
		
		for(Value currentValue : Value.KNOWN_VALUES){
			
			String errorString = errorForValue(currentValue);
			if( !errorString.isEmpty() )
				returnString += "\n" + errorString;
		}
		
		return returnString;
	}
	
	/*
	 * Returns a String describing whether the parameter value
	 * has been marked as present in multiple cells or marked
	 * as impossible in all the region's cells.
	 * @param value				The value for which this method 
	 * seeks duplicates or total absence in this region.
	 * @return					Returns a String describing 
	 * whether the parameter value has been marked as present 
	 * in multiple cells or marked as impossible in all the 
	 * region's cells.
	 */
	private String errorForValue(Value value){
		
		final int MAX_CERTAINS = 1, MAX_IMPOSSIBLES = 8;
		
		int impossibleCount = 0;
		int certainCount = 0;
		for(Cell currentCell : cells){
			Possibility currentPossibility = currentCell.getPossibility(value);
			if(currentPossibility == Possibility.IMPOSSIBLE){
				impossibleCount++;
			}
			else if(currentPossibility == Possibility.CERTAIN){
				certainCount++;
			}
		}
		
		if(certainCount > MAX_CERTAINS)
			return certainCount+" CERTAINs for Value "+value.toInt();
		if(impossibleCount > MAX_IMPOSSIBLES)
			return impossibleCount+" IMPOSSIBLEs for Value "+value.toInt();
		if(certainCount != 0 && impossibleCount != cells.length - certainCount)
			return "CERTAINs and UNKNOWNs coexisting for Value "+value.toInt();
		
		return "";
	}
	
	/**
	 * Returns a reference to the target to which this region belongs.
	 * @return					Returns a reference to the target to 
	 * which this region belongs.
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/**
	 * Returns the position-indicating index of this region in its 
	 * target.  If this region is a column, the index is the 
	 * column's x-coordinate. If this region is a row, the index 
	 * is the row's y-coordinate. If this region is a block, the 
	 * index is what would be given by numbering the blocks one 
	 * horizontal line at a time, moving from left to right on 
	 * each horizontal line, with the first horizontal line placed 
	 * at the top of the target.
	 * @return					Returns the position-indicating 
	 * index of this region in its 
	 * target.  If this region is a column, the index is the 
	 * column's x-coordinate. If this region is a row, the index 
	 * is the row's y-coordinate. If this region is a block, the 
	 * index is what would be given by numbering the blocks one 
	 * horizontal line at a time, moving from left to right on 
	 * each horizontal line, with the first horizontal line placed 
	 * at the top of the target.
	 */
	public Index getIndex(){
		return index;
	}
	
	/**
	 * Returns a reference to the cells in this region.
	 * @return					Returns a reference to 
	 * the cells in this region.
	 */
	public Cell[] getCells(){
		return cells;
	}/**/
	
	public Set<Cell> cells(){
		Set<Cell> retSet = new HashSet<>();
		for(Cell c : cells)
			retSet.add(c);
		return retSet;
	}
	
	/**
	 * Returns a reference to the cell in this region indicated
	 * by the parameter index. If the region is a Line, the 
	 * index is the x or y coordinate of the cell. If the 
	 * region is a block, the index is that given by numbering 
	 * the cells in the block starting from the top left and 
	 * numbering along rows left to right and moving down to 
	 * the next row.
	 * @param index				The index in this region of the 
	 * cell to be returned.
	 * @return					Returns a reference to the cell 
	 * in this region indicated
	 * by the parameter index. If the region is a Line, the 
	 * index is the x or y coordinate of the cell. If the 
	 * region is a block, the index is that given by numbering 
	 * the cells in the block starting from the top left and 
	 * numbering along rows left to right and moving down to 
	 * the next row.
	 */
	public Cell getCell(Index index){
		return cells[index.toInt()-1];
	}
	
	/**
	 * Returns whether all the values for this region have been
	 * located.
	 * @return					Returns whether all the values 
	 * for this region have been located.
	 */
	public boolean isSolved(){
		for(Cell cell : cells)
			if( !cell.isSolved() )
				return false;
		return true;
	}

}
