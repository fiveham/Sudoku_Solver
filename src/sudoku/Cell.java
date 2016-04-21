package sudoku;

import java.util.*;

/**
 * Represents a cell in a sudoku target.
 * @author fiveham
 */
public final class Cell{
	
	/*
	 * The class Cell needs to remain visible to all 
	 * outside code (public) but the constructor 
	 * should not be visible to outside code (protected) 
	 * because no code other than the Puzzle class 
	 * should need to create Cell objects but outside 
	 * code should have access to the methods of the 
	 * Cell class in order to be able to make and use 
	 * arbitrary new analysis techniques.
	 */
	
	/** The number of regions that contain any one cell */
	public static final int REGION_COUNT = 3;
	
	/*
	 *  An array of the correct block indices of a cell 
	 *  with coordinates x,y
	 */
	private static final int[][] BLOCK_INDEX_INT = 
		{{ 1,1,1,4,4,4,7,7,7 }, { 1,1,1,4,4,4,7,7,7 }, { 1,1,1,4,4,4,7,7,7 },
		 { 2,2,2,5,5,5,8,8,8 }, { 2,2,2,5,5,5,8,8,8 }, { 2,2,2,5,5,5,8,8,8 },
		 { 3,3,3,6,6,6,9,9,9 }, { 3,3,3,6,6,6,9,9,9 }, { 3,3,3,6,6,6,9,9,9 }};
	
	private final Index x;
	private final Index y;
	private final Puzzle puzzle;
	private final Possibility[] possibilities;
	
	private Value value;
	
	private Symbol symbol = null;
	
	/*
	 * Constructor.
	 * @param x				x-coordinate of this cell
	 * @param y				y-coordinate of this cell
	 * @param value			value for this cell
	 * @param target		the target to which this cell belongs
	 */
	public Cell(Index x, Index y, Value value, Puzzle puzzle){
		
		this.x = x;
		this.y = y;
		this.puzzle = puzzle;
		this.value = value;
		
		possibilities = new Possibility[Value.KNOWN_VALUES.length];
		if( value == Value.UNKNOWN ){
			Arrays.fill(possibilities, Possibility.UNKNOWN);
		}
		else{
			solvePossibilities(value);
		}
	}
	
	/*
	 * sets the cell's possibility for the parameter value
	 * to "certain" and sets all other possibilities to "impossible"
	 */
	private void solvePossibilities(Value value){
		for( Value currentValue : Value.KNOWN_VALUES )
			possibilities[currentValue.intValue()-1] = (currentValue == value) 
			? Possibility.CERTAIN 
			: Possibility.IMPOSSIBLE;
	}
	
	/**
	 * Sets the value of this cell to the parameter value
	 * if that value isn't marked impossible in this cell
	 * and this cell's value is currently unknown. Next, 
	 * all the possibilities in this cell are set to 
	 * impossible, except the one pertaining to the 
	 * newly-set value, which is set to certain. Then, 
	 * all the cells that share this cell's block, row, 
	 * or column have their possibility for the parameter 
	 * value set to impossible.
	 * 
	 * Returns whether any changes to this cell were made.
	 * @param value				The value to which this 
	 * cell's value is to be set.
	 * @return					Returns whether any changes 
	 * to this cell were made.
	 */
	public boolean setValue(Value value){
		boolean returnValue = false;
		
		if( isImpossibleValue(value) )
			return returnValue;
		
		// If the cell's value is unknown, the new value will be set.
		if(this.value == Value.UNKNOWN){
			this.value = value;
			solvePossibilities(value);
			for( Cell currentNeighbor : neighbors() )
				currentNeighbor.setValueImpossible(value);
			returnValue = true;
		}
		
		return returnValue;
	}
	
	/**
	 * Sets the possibility for the parameter value
	 * to "impossible" if and only if the possibility 
	 * associated with that value in the cell was 
	 * previously unassessed.
	 * @param value				The value to be marked 
	 * impossible in this cell.
	 * @return					Returns whether any change 
	 * was made to this cell.
	 */
	public boolean setValueImpossible(Value value){
		boolean returnValue = false;
		int index = value.intValue()-1;
		if(possibilities[index] == Possibility.UNKNOWN){
			possibilities[index] = Possibility.IMPOSSIBLE;
			returnValue = true;
		}
		return returnValue;
	}
	
	/**
	 * Returns whether the cell is solved.
	 * @return					Returns whether this 
	 * cell is solved.
	 */
	public boolean isSolved() {
		return value != Value.UNKNOWN;
	}
	
	/**
	 * Returns whether the parameter value is marked impossible 
	 * in this cell.
	 * @param value				The value to be tested for 
	 * whether it is marked impossible in this cell.
	 * @return					Returns whether the parameter 
	 * value is marked impossible in this cell.
	 */
	public boolean isImpossibleValue(Value value){
		return possibilities[value.intValue()-1] == Possibility.IMPOSSIBLE;
	}
	
	/**
	 * Returns whether the parameter value is marked unknown 
	 * in this cell.
	 * @param value				The value to be tested for 
	 * whether it is marked unknown in this cell.
	 * @return					Returns whether the parameter 
	 * value is marked unknown in this cell.
	 */
	public boolean isPossibleValue(Value value){
		return possibilities[value.intValue()-1] == Possibility.UNKNOWN;
	}
	
	/*
	 * Returns whether this cell shares a region 
	 * (block, row, or column) with the parameter 
	 * cell.
	 * 
	 * This method is used by XYWing and RemotePairs 
	 * to narrow down their list of cells to look 
	 * at, but it's not philosophically sound enough
	 * to make available to the general public.
	 * @param cell				The cell to be 
	 * tested for whether it shares a region with
	 * this cell.
	 * @return					Returns whether 
	 * this cell shares a region (block, row, or 
	 * column) with the parameter cell.
	 */
	public boolean sharesRegionWith(Cell cell){

		if(puzzle.getBlock( getBlockIndex() ).contains(cell))
			return true;
		if(puzzle.getRow(y).contains(cell))
			return true;
		if(puzzle.getColumn(x).contains(cell))
			return true;
		
		return false;
	}
	
	/**
	 * Returns the index that a block that contains this cell 
	 * must have.
	 * @return					Returns the index that a 
	 * block that contains this cell must have.
	 */
	public Index getBlockIndex(){
		return Index.fromInt(BLOCK_INDEX_INT[x.intValue()-1][y.intValue()-1]);
	}
	
	/**
	 * Returns a list of the values that could be this 
	 * cell's value.
	 * @return					Returns a list of the 
	 * values that could be this cell's value.
	 */
	public List<Value> getPossibleValues(){
		List<Value> returnSet = new ArrayList<Value>();
		
		for(Value value : Value.KNOWN_VALUES)
			if( possibilities[ value.intValue()-1 ] == Possibility.UNKNOWN )
				returnSet.add( value );
		
		return returnSet;
	}
	
	/**
	 * Returns a list of cells that this cell must 
	 * propagate impossibilities to when its value 
	 * is set.
	 * @return					Returns a list of 
	 * cells that this cell must propagate 
	 * impossibilities to when its value is set.
	 */
	public ArrayList<Cell> neighbors(){
		return puzzle.neighbors(this);
	}
	
	/**
	 * Returns this cell's x-coordinate.
	 * @return					Returns this cell's x-coordinate.
	 */
	public Index getX(){
		return x;
	}
	
	/**
	 * Returns this cell's y-coordinate.
	 * @return					Returns this cell's y-coordinate.
	 */
	public Index getY(){
		return y;
	}
	
	/**
	 * Returns a new int array describing this cell's 
	 * possibilities array at the time this method was called.
	 * @return					Returns a new int array 
	 * describing this cell's possibilities array at 
	 * the time this method was called.
	 */
	public int[] getPossibilities(){
		int[] returnArray = new int[possibilities.length];
		for(int i=0; i<returnArray.length; i++){
			returnArray[i] = possibilities[i].toInt();
		}
		return returnArray;
	}
	
	/**
	 * Returns the possibility of the parameter value 
	 * in this cell.
	 * @param value				The value for which 
	 * the possibility in this cell is to be returned.
	 * @return					Returns the possibility 
	 * of the parameter value in this cell.
	 */
	public Possibility getPossibility(Value value) {
		return possibilities[value.intValue()-1];
	}
	
	/**
	 * Returns the value of this cell.
	 * @return					Returns the value of this cell.
	 */
	public Value getValue(){
		return value;
	}
	
	/**
	 * Returns a reference to the target to which
	 * this cell belongs.
	 * @return					Returns a reference 
	 * to the target to which this cell belongs.
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/**
	 * Returns a reference to the block to which 
	 * this cell belongs.
	 * @return					Returns a reference 
	 * to the block to which this cell belongs.
	 */
	public Box getBlock(){
		return puzzle.getBlock(getBlockIndex());
	}
	
	/**
	 * Returns a reference to the row to which this 
	 * cell belongs.
	 * @return					Returns a reference 
	 * to the row to which this cell belongs.
	 */
	public Line getRow(){
		return puzzle.getRow(y);
	}
	
	/**
	 * Returns a reference to the column to which this
	 * cell belongs.
	 * @return					Returns a reference to 
	 * the column to which this cell belongs.
	 */
	public Line getColumn(){
		return puzzle.getColumn(x);
	}
	
	/**
	 * Returns a textual representation of the cell,
	 * in the form of the cell's x and y coordinates 
	 * separated by a comma followed by a semicolon 
	 * separating them from this cell's value, followed 
	 * by a semicolon separating it from a sequence of 
	 * nine characters describing this cell's possibilties:
	 * '-' for unknown; '0' for impossible, and; '1' 
	 * for certain.
	 * @return					Returns a textual 
	 * representation of the cell,
	 * in the form of the cell's x and y coordinates 
	 * separated by a comma followed by a semicolon 
	 * separating them from this cell's value, followed 
	 * by a semicolon separating it from a sequence of 
	 * nine characters describing this cell's possibilties:
	 * '-' for unknown; '0' for impossible, and; '1' 
	 * for certain.
	 */
	public String toString(){
		String returnString = x.intValue()+","+y.intValue()+";";
		
		returnString += value.intValue() + ";";
		for(int i=0; i<possibilities.length; i++)
			returnString += (Integer.toString(possibilities[i].toInt())).charAt(0);
		
		return returnString;
	}
	
	/**
	 * Returns whether the cell has multiple values
	 * marked as the cell's value, has all values 
	 * marked impossible, or has at least one value 
	 * marked unknown at the same time as at least 
	 * one value is marked as the cell's value.
	 * @return					Returns whether 
	 * the cell has multiple values
	 * marked as the cell's value, has all values 
	 * marked impossible, or has at least one value 
	 * marked unknown at the same time as at least 
	 * one value is marked as the cell's value.
	 */
	public boolean hasError(){
		
		final int MAX_CERTAINS = 1, MAX_IMPOSSIBLES = 8;
		
		int impossibleCount = 0;
		int certainCount = 0;
		
		for(Possibility currentPossibility : possibilities){
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
		if(certainCount != 0 && impossibleCount != possibilities.length - certainCount)
			return true;
		
		return false;
	}
	
	/**
	 * Returns a textual representation of any errors in the cell.
	 * @return					Returns a textual representation 
	 * of any errors in the cell.
	 */
	public String error(){

		final int MAX_CERTAINS = 1, MAX_IMPOSSIBLES = 8;
		
		int impossibleCount = 0;
		int certainCount = 0;
		
		for(Possibility currentPossibility : possibilities){
			if(currentPossibility == Possibility.IMPOSSIBLE){
				impossibleCount++;
			}
			else if(currentPossibility == Possibility.CERTAIN){
				certainCount++;
			}
		}
		
		if(certainCount > MAX_CERTAINS)
			return certainCount+" CERTAINs for Cell "+toString();
		if(impossibleCount > MAX_IMPOSSIBLES)
			return impossibleCount+" IMPOSSIBLEs for Cell "+toString();
		if(certainCount != 0 && impossibleCount != possibilities.length - certainCount)
			return "CERTAINs and UNKNOWNs coexisting for Cell "+toString();
		
		return "";
	}
}
