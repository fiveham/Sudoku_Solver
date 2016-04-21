package sudoku;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Represents a 9x9 Sudoku target, with
 * distinct subcomponents representing each cell,
 * column, row, and block.
 * @author fiveham
 */
public class Puzzle {
	
	/** Width of a target measured in cells */
	public static final int	WIDTH = 9;
	
	/** Height of a target measured in cells */
	public static final int	HEIGHT = 9;
	
	private Cell[][] 		cells;
	private Line[] 			rows;
	private Line[] 			columns;
	private Block[] 		blocks;
	private boolean 		isSolved;
	
	/**
	 * Constructs a target from a String containing the values of 
	 * the cells in the target as the first 81 tokens.
	 * @param puzzleAsString	String representation 
	 * of a sudoku target, having the values of the cells as its 
	 * first 81 tokens.
	 * @throws IllegalArgumentException
	 */
	public Puzzle(String puzzleAsString) throws IllegalArgumentException{
		constructor(puzzleAsString);
	}
	
	/**
	 * Constructs a target from a plaintext source file having the 
	 * values of the cells of the target as the first 81 tokens.
	 * @param file				The file from which to extract the 
	 * initial values of the target's cells.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public Puzzle(File file) throws FileNotFoundException, IOException, IllegalArgumentException{
		constructor( fileContent(file) );
	}
	
	/**
	 * Constructs a target with all empty cells except for the 
	 * cells from the parameter set.
	 * @param cells
	 */
	public Puzzle(Set<Cell> cells){
		
		final String defaultEntry = "0 ";
		
		ArrayList<Integer> indices = new ArrayList<>();
		ArrayList<Value> values = new ArrayList<>();
		for(Cell c : cells){
			int x = c.getX().toInt();
			int y = c.getY().toInt();
			int index = ((y-1)*WIDTH+(x-1))*defaultEntry.length();
			indices.add(new Integer(index));
			values.add(c.getValue());
		}
		
		//Get a String representation of the target based on the 
		//parameter cells and the assumption that all other cells 
		//are to be empty
		String puzzleAsString = "";
		for(int i=0; i<defaultEntry.length()*WIDTH*HEIGHT; i+=defaultEntry.length()){
			//at each position,
			//if there is a cell whose value needs to be represented,
			//represent it instead of the default 0
			
			Integer possibleEntry = new Integer(i);
			puzzleAsString += indices.contains(possibleEntry) 
					? values.get(
							indices.indexOf(possibleEntry)
							).toInt()
							+ (( i%18 == 16 ) //Last digit on a line is at an index whose mod-18 is 16
									? "\n"
									: " ")
					: ( i%18==16 
							? "0\n" 
							: defaultEntry);
					
		}
		
		constructor(puzzleAsString);
	}
	
	/**
	 * Constructs a target that's a copy of a pre-existing target 
	 * except that one of its blocks is transplanted from one 
	 * corner of some target to the opposite corner of this target.
	 * This transplanted block is a different instance, but its cells
	 * are the same as those in the parameter block; so any analysis 
	 * operations performed on it will be visible in the original 
	 * target as well.
	 * 
	 * This is meant to construct puzzles on the corners of 
	 * multi-puzzles which have five puzzles interlocked.
	 */
	public Puzzle(Puzzle originalPuzzle, Block sharedBlock){
		Index oldIndex = sharedBlock.getIndex();
		Index newBlockIndex = null;
		if( oldIndex == Index.INDEX_1 ){
			newBlockIndex = Index.INDEX_9;
		}
		else if( oldIndex == Index.INDEX_3 ){
			newBlockIndex = Index.INDEX_7;
		}
		else if( oldIndex == Index.INDEX_7 ){
			newBlockIndex = Index.INDEX_3;
		}
		else if( oldIndex == Index.INDEX_9 ){
			newBlockIndex = Index.INDEX_1;
		}
		else{
			throw new IllegalArgumentException("Box must be from a corner of its original target.");
		}
		
		cells		= originalPuzzle.cells;
		rows		= originalPuzzle.rows;
		columns		= originalPuzzle.columns;
		blocks		= originalPuzzle.blocks;
		isSolved	= originalPuzzle.isSolved;
		
		Block newBlock = new Block(newBlockIndex, sharedBlock.getCells(), this);
		
		blocks[ newBlockIndex.toInt()-1 ] = newBlock;
		
		isSolved = isSolved && newBlock.isSolved();
	}
	
	private void constructor(String puzzleAsString){
		
		// Check that the first 81 tokens are integers
		Scanner intChecker = new Scanner(puzzleAsString);
		for(int i=0; i<WIDTH*HEIGHT; i++){
			if(!intChecker.hasNextInt()){
				intChecker.close();
				throw new IllegalArgumentException("too few uninterrupted int tokens ("+i+"); 81 required.");
			}
			intChecker.next();
		}
		intChecker.close();
		
		// Create the cells, populating them with the 81 verified integers
		createCells(puzzleAsString);
		
		// Create the rows, populating them with their cells
		createRows();
		
		// create the columns, populating them with their cells
		createColumns();
		
		// create the blocks, populating them with their cells
		createBlocks();
		
		
		// propagate values beyond their cells
		// for each cell, propagate the value of the cell in 
		// the block, the column, and the row to which that
		// cell belongs, if that value is known
		initialPropagation();
		
		
		
		isSolved = false;
	}
	
	private void initialPropagation(){
		for(Cell[] currentCellArray : cells ){
			for(Cell currentCell : currentCellArray ){
				Value currentValue = currentCell.getValue();
				if(currentValue != Value.UNKNOWN){
					for(Cell neighborCell : neighbors(currentCell)){
						neighborCell.setImpossibleValue(currentValue);
					}
				}
			}
		}
	}
	
	private void createBlocks(){
		blocks = new Block[Index.INDEX_COUNT];
		for( Index currentBlockIndex : Index.KNOWN_VALUES )
			blocks[currentBlockIndex.toInt() - 1] = 
					new Block(currentBlockIndex, 
							cellsForBlock(currentBlockIndex), this);
	}
	
	private void createColumns(){
		columns = new Line[Index.INDEX_COUNT];
		for(int x = Index.MINIMUM.toInt(); x <= Index.MAXIMUM.toInt(); x++){
			columns[x-1] = new Line(Index.fromInt(x), cells[x-1], this);
		}
	}
	
	private void createRows(){
		rows = new Line[Index.INDEX_COUNT];
		for(int y = Index.MINIMUM.toInt(); y <= Index.MAXIMUM.toInt(); y++){
			Cell[] tempCells = new Cell[Index.INDEX_COUNT];
			for(int x = Index.MINIMUM.toInt(); x <= Index.MAXIMUM.toInt(); x++)
				tempCells[x-1] = cells[x-1][y-1];
			rows[y-1] = new Line(Index.fromInt(y), tempCells, this);
		}
	}
	
	private void createCells(String puzzleAsString){
		cells = new Cell[Index.INDEX_COUNT][Index.INDEX_COUNT];
		Scanner valueGetter = new Scanner(puzzleAsString);
		for( Index y : Index.KNOWN_VALUES ){
			Scanner lineReader = new Scanner(valueGetter.nextLine());
			for( Index x : Index.KNOWN_VALUES )
				cells[x.toInt()-1][y.toInt()-1] = new Cell(x,y,Value.fromInt(lineReader.nextInt()),this);
			lineReader.close();
		}
		valueGetter.close();
	}
	
	/*
	 * Extracts the contents of the parameter file and 
	 * returns it as a String.
	 */
	private String fileContent(File file) throws FileNotFoundException, IOException{
		
		if(!file.canRead())
			throw new IOException("cannot read "+file.getName());
		
		String returnString = "";
		Scanner scanner = new Scanner(file);
		
		returnString += scanner.nextLine();
		while(scanner.hasNextLine()){
			returnString += "\n" + scanner.nextLine();
		}
		scanner.close();
		return returnString;
	}
	
	/* 
	 * Returns the cells that need to be sent to a block 
	 * with the parameter index.
	 * 
	 * Purely for use in constructing the target.
	 */
	private Cell[] cellsForBlock(Index blockIndex){
		Cell[] returnArray = new Cell[Block.CELL_COUNT];
		
		int minX = Block.properMinXForBlockIndex(blockIndex).toInt();
		int minY = Block.properMinYForBlockIndex(blockIndex).toInt();
		
		int pointer = 0;
		for(int y = minY; y < minY + Block.HEIGHT; y++ )
			for( int x = minX; x < minX + Block.WIDTH; x++ )
				returnArray[ pointer++ ] = cells[x-1][y-1];
			
		return returnArray;
	}
	
	/**
	 * Returns a 2D array of all the cells in this target, 
	 * arranged so that cells[i][j] is the same cell 
	 * returned by getCell( i-1, j-1 ). X increases to the 
	 * right, and Y increases going down.
	 * @return					Returns a 2D array of 
	 * all the cells in this target, arranged so that 
	 * cells[i][j] is the same cell returned by 
	 * getCell( i-1, j-1 ). X increases to the right, 
	 * and Y increases going down.
	 */
	public Cell[][] getCells(){
		return cells;
	}
	
	/**
	 * Returns an array of all the rows in this target.
	 * @return					Returns an array of all 
	 * the rows in this target.
	 */
	public Line[] getRows(){
		return rows;
	}
	
	/**
	 * Returns an array of all the columns in this target.
	 * @return					Returns an array of all 
	 * the columns in this target.
	 */
	public Line[] getColumns(){
		return columns;
	}
	
	/**
	 * Returns a list of all the regions (blocks, rows, 
	 * and columns) of this target.
	 * @return					Returns a list of all 
	 * the regions (blocks, rows, and columns) of this 
	 * target.
	 */
	public ArrayList<Region> getRegions(){
		ArrayList<Region> returnList = new ArrayList<Region>();
		
		for(Block currentBlock : blocks)
			returnList.add( (Region) currentBlock);
		
		for(Line currentRow : rows)
			returnList.add( (Region) currentRow);
		
		for(Line currentColumn : columns)
			returnList.add( (Region) currentColumn);
		
		return returnList;
	}
	
	/**
	 * Returns an array of the regions to which the parameter cell 
	 * belongs in this target.
	 * @param cell				The cell in this target for 
	 * which its regions should be returned.
	 * @return					Returns an array of the regions 
	 * to which the parameter cell belongs in this target.
	 */
	public Region[] getRegions(Cell cell){
		if(!contains(cell))
			throw new IllegalArgumentException("That cell does not belong to this target.");
		
		Region[] returnArray = new Region[Cell.REGION_COUNT];
		
		returnArray[0] = cell.getBlock();
		returnArray[1] = cell.getRow();
		returnArray[2] = cell.getColumn();
		
		return returnArray;
	}
	
	/**
	 * Returns the cell at coordinates x,y in this target.
	 * @param x					The x-coordinate of the cell
	 * to be returned.
	 * @param y					The y-coordinate of the cell
	 * the be returned.
	 * @return					Returns the cell at 
	 * coordinates x,x in this target.
	 */
	public Cell getCell(Index x, Index y){
		try{
			return cells[x.toInt()-1][y.toInt()-1];
		}
		catch( ArrayIndexOutOfBoundsException e){	//IndexValue.UNKNOWN would create an ArrayIndexOutOfBoundsException
			throw new IllegalArgumentException("bad x or y ("+e.getMessage()+")");
		}
	}
	
	/**
	 * Returns the column from this target with the parameter index.
	 * @param columnIndex		The index for which a column from this
	 * target is to be returned.
	 * @return					Returns the column from this target 
	 * with the parameter index.
	 */
	public Line getColumn(Index columnIndex){
		for( Line currentColumn : columns )
			if( currentColumn.getIndex() == columnIndex )
				return currentColumn;
		throw new IllegalArgumentException("unknown column index");
	}
	
	/**
	 * Returns the row from this target with the parameter index.
	 * @param rowIndex			The index for which a row from 
	 * this target is to be returned..
	 * @return					Returns the row from this target 
	 * with the parameter index.
	 */
	public Line getRow(Index rowIndex){
		for( Line currentRow : rows )
			if( currentRow.getIndex() == rowIndex )
				return currentRow;
		throw new IllegalArgumentException("unknown row index");
	}
	
	/**
	 * Returns an array of all this target's blocks.
	 * @return					Returns an array of all this 
	 * target's blocks.
	 */
	public Block[] getBlocks(){
		return blocks;
	}
	
	/**
	 * Returns the block from this target that has 
	 * the parameter index.
	 * @param blockIndex		The index for which 
	 * a block is to be returned.
	 * @return					Returns the block 
	 * from this target that has the parameter index.
	 */
	public Block getBlock(Index blockIndex){
		return blocks[ blockIndex.toInt() - 1];
	}
	
	/* 
	 * Returns a list of the cells that would be affected 
	 * by setting the value in the parameter cell.
	 * 
	 * External code should use the neighbors() method 
	 * in the Cell class on a cell instance.
	 */
	ArrayList<Cell> neighbors(Cell cell){
		ArrayList<Cell> returnList = new ArrayList<Cell>();
		
		for( Region region : getRegions(cell) )
			for( Cell currentCell : region.getCells())
				if( !returnList.contains(currentCell) && currentCell != cell)
					returnList.add(currentCell);
		
		return returnList;
	}
	
	/**
	 * Returns whether the parameter cell is contained 
	 * in this target.
	 * @param cell				The cell to be tested 
	 * for membership in this target.
	 * @return					Returns whether the 
	 * parameter cell is contained in this target.
	 */
	public boolean contains(Cell cell){
		for( Cell[] currentCellArray : cells )
			for( Cell currentCell : currentCellArray )
				if( currentCell == cell )
					return true;
		return false;
	}
	
	/**
	 * Returns whether the target is solved.
	 * @return					Returns whether the target is 
	 * solved.
	 */
	public boolean isSolved(){
		if(isSolved)
			return isSolved;
		
		for(Cell[] currentArray : cells)
			for(Cell currentCell : currentArray)
				if(!currentCell.isSolved())
					return isSolved;
		
		isSolved = true;
		return isSolved;
	}
	
	/*
	 * Returns a String representation of all the possibilities 
	 * of all the cells in the target, arranged in distinct cells 
	 * and blocks, like a target itself.
	 */
	String possibilitiesAsString(){
		
		final String topLine = "\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2566\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2566\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2564\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557";
		
		final String cellBorder = "\u255F\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u256B\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u256B\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2562";
		
		final String blockBorder = "\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256C\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256C\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u256A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563";
		
		final String bottomLine = "\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2569\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2569\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2567\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D";
		
		final String blockEdge = "\u2551";
		
		final String cellEdge = "\u2502";
		
		final int FIRST_BLOCK_BOTTOM = 2;
		final int SECOND_BLOCK_BOTTOM = 5;
		
		String returnString = topLine + "\n";
		
		for(int i=0; i<rows.length; i++){
			returnString += rows[i].getPossibilitiesStringForEndDisplay(blockEdge, cellEdge);
			if( i==rows.length-1 ){
				returnString += bottomLine + "\n";
			}
			else if( i==FIRST_BLOCK_BOTTOM || i==SECOND_BLOCK_BOTTOM ){
				returnString += blockBorder + "\n";
			}
			else{
				returnString += cellBorder + "\n";
			}
		}
		
		return returnString;
	}
	
	/**
	 * Returns a String representation of the target, 
	 * specifying the values of all the cells, arranged 
	 * in rows and columns.
	 */
	public String toString(){
		String returnString = "";
		for(int y = Index.MINIMUM.toInt(); y <= Index.MAXIMUM.toInt(); y++){
			returnString += cells[Index.MINIMUM.toInt() - 1][y - 1].getValue().toInt();
			for(int x = Index.MINIMUM.toInt()+1; x <= Index.MAXIMUM.toInt(); x++)
				returnString += " " + cells[x-1][y-1].getValue().toInt();
			if(y != Index.MAXIMUM.toInt())
				returnString += "\n";
		}
		return returnString;
	}
}