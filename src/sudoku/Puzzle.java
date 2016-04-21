package sudoku;

import java.util.*;
import java.io.*;
import java.util.function.*;

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
	
	private Cell[]	 	cells;
	private Line[] 		rows;
	private Line[] 		columns;
	private Box[] 		boxes;
	private boolean 	isSolved;
	
	private List<Symbol> symbols;
	private List<Role>   roles;
	
	/**
	 * Constructs a target from a String containing the values of 
	 * the cells in the target as the first 81 tokens.
	 * @param puzzleAsString	String representation 
	 * of a sudoku target, having the values of the cells as its 
	 * first 81 tokens.
	 * @throws IllegalArgumentException
	 */
	public Puzzle(String puzzleAsString) throws IllegalArgumentException{
		verifyInts(puzzleAsString);
		createCells(puzzleAsString);
		createRows();
		createColumns();
		createBoxes();
		initialPropagation();
		isSolved = false;
		
		symbols = new ArrayList<Symbol>(WIDTH*HEIGHT);
		for(Value val : Value.KNOWN_VALUES){
			for(int i=0; i<9; i++){
				symbols.add( new Symbol(val, this) );
			}
		}
		
		roles = new ArrayList<>(9*9*3);	//one for each value in each region
		for(Region region : getRegions()){
			for(Value value : Value.KNOWN_VALUES){
				roles.add( new Role(this, value, region) );
			}
		}
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
		this( fileContent(file) );
	}
	
	private static void verifyInts(String puzzleAsString){
		Scanner intChecker = new Scanner(puzzleAsString);
		for(int i=0; i<WIDTH*HEIGHT; i++){
			if(!intChecker.hasNextInt()){
				intChecker.close();
				throw new IllegalArgumentException("too few uninterrupted int tokens ("+i+"); 81 required.");
			}
			intChecker.next();
		}
		intChecker.close();
	}
	
	private void createCells(String puzzleAsString){
		cells = new Cell[Index.INDEX_COUNT*Index.INDEX_COUNT];
		Scanner valueGetter = new Scanner(puzzleAsString);
		for( Index y : Index.KNOWN_VALUES ){
			Scanner lineReader = new Scanner(valueGetter.nextLine());
			for( Index x : Index.KNOWN_VALUES ){
				cells[cellArrayIndexFromXY(x,y)] = new Cell(x,y,Value.fromInt(lineReader.nextInt()),this);
			}
			lineReader.close();
		}
		valueGetter.close();
	}
	
	private void initialPropagation(){
		for(Cell currentCell : cells ){
			Value currentValue = currentCell.getValue();
			if(currentValue != Value.UNKNOWN)
				for(Cell neighborCell : neighbors(currentCell))
					neighborCell.setValueImpossible(currentValue);
		}
	}
	
	private void createBoxes(){
		boxes = new Box[Index.INDEX_COUNT];
		for( Index currentBlockIndex : Index.KNOWN_VALUES )
			boxes[currentBlockIndex.intValue() - 1] = 
					new Box(currentBlockIndex, 
							cellsForBlock(currentBlockIndex), this);
	}
	
	/* 
	 * Returns the cells that need to be sent to a block 
	 * with the parameter index.
	 * 
	 * Purely for use in constructing the target.
	 */
	private Cell[] cellsForBlock(Index blockIndex){
		Cell[] returnArray = new Cell[Box.CELL_COUNT];
		
		Index minX = Box.minXForBlockIndex(blockIndex);
		Index minY = Box.minYForBlockIndex(blockIndex);
		
		for(int pointer = 0, 
				yLow    = minY.intValue(), 
				xLow    = minX.intValue(), 
				y       = yLow; 
				y < yLow + Box.HEIGHT; 
				y++ ){
			for( int x = xLow; x < xLow + Box.WIDTH; x++ ){
				returnArray[ pointer++ ] = getCell(minX, minY);
			}
		}
			
		return returnArray;
	}
	
	private void createColumns(){
		columns = createLines( c -> c.getX() );
	}
	
	private void createRows(){
		rows = createLines( c -> c.getY() );
	}
	
	private Line[] createLines( Function<Cell,Index> func ){
		Line[] retVal = new Line[9];
		
		for(Index i : Index.KNOWN_VALUES){
			retVal[i.intValue()-1] = new Line(i, cellsWhere( c -> func.apply(c) == i), this);
		}
		
		return retVal;
	}
	
	private Cell[] cellsWhere(Predicate<Cell> test){
		List<Cell> ret = new ArrayList<>();
		for(Cell c : cells){
			if(test.test(c)){
				ret.add(c);
			}
		}
		return ret.toArray( new Cell[]{} );
	}
	
	private int cellArrayIndexFromXY(Index x, Index y){
		return cellArrayIndexFromXY(x.intValue(), y.intValue());
	}
	
	private int cellArrayIndexFromXY(int x, int y){
		return (y-1)*8 + (x-1);
	}
	
	/*
	 * Extracts the contents of the parameter file and 
	 * returns it as a String.
	 */
	private static String fileContent(File file) throws FileNotFoundException, IOException{
		
		if(!file.canRead())
			throw new IOException("cannot read "+file.getName());
		
		StringBuilder returnString = new StringBuilder();
		Scanner scanner = new Scanner(file);
		
		for(int i=0; scanner.hasNextLine(); i++){
			if(i!=0)
				returnString.append("\n");
			returnString.append(scanner.nextLine());
		}
		
		scanner.close();
		return returnString.toString();
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
	public Cell[] getCells(){
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
	 * Returns a list of all the regions (boxes, rows, 
	 * and columns) of this target.
	 * @return					Returns a list of all 
	 * the regions (boxes, rows, and columns) of this 
	 * target.
	 */
	public ArrayList<Region> getRegions(){
		ArrayList<Region> returnList = new ArrayList<Region>();
		
		for(Box currentBlock : boxes)
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
		return cells[cellArrayIndexFromXY(x,y)];
	}
	
	public Cell getCell(int x, int y){
		return cells[cellArrayIndexFromXY(x,y)];
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
	 * Returns an array of all this target's boxes.
	 * @return					Returns an array of all this 
	 * target's boxes.
	 */
	public Box[] getBlocks(){
		return boxes;
	}
	
	/**
	 * Returns the block from this target that has 
	 * the parameter index.
	 * @param blockIndex		The index for which 
	 * a block is to be returned.
	 * @return					Returns the block 
	 * from this target that has the parameter index.
	 */
	public Box getBlock(Index blockIndex){
		return boxes[ blockIndex.intValue() - 1];
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
		for( int i = 0; i<cells.length; i++ )
			if( cells[i] == cell )
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
		
		for(Cell currentCell : cells)
			if(!currentCell.isSolved())
				return isSolved;
		
		return isSolved = true;
	}
	
	/*
	 * Returns a String representation of all the possibilities 
	 * of all the cells in the target, arranged in distinct cells 
	 * and boxes, like a target itself.
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
	@Override
	public String toString(){
		StringBuilder returnString = new StringBuilder();
		for(int y = Index.MINIMUM.intValue(); y <= Index.MAXIMUM.intValue(); y++){
			returnString.append( cells[cellArrayIndexFromXY(Index.MINIMUM,Index.fromInt(y))].getValue().intValue() );
			
			for(int x = Index.MINIMUM.intValue()+1; x <= Index.MAXIMUM.intValue(); x++)
				returnString.append(" ").append(cells[cellArrayIndexFromXY(Index.fromInt(x), Index.fromInt(y))].getValue().intValue());
			
			if(y != Index.MAXIMUM.intValue())
				returnString.append("\n");
		}
		return returnString.toString();
	}
}