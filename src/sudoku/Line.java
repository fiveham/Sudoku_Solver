package sudoku;

/**
 * Represents a row or column in a sudoku target.
 * @author fiveham
 */
public final class Line extends Region{
    
	/* Specifies the orientation of the Line */
	private boolean isVertical;
	
	/*
	 * Constructs a Line object.
	 * @param lineIndex			The line's position within its target.
	 * Indicates x-coordinate if Line is a column; indicates 
	 * y-coordinate if Line is a row.
	 * @param cells				
	 * @param target
	 */
	/*
	 * Constructor is package-private to prevent external code from 
	 * arbitrarily creating new Lines.
	 */
    Line(Index lineIndex, Cell[] cells, Puzzle puzzle){
    	super(lineIndex, cells, puzzle);
    	isVertical = (cells[0].getX() == cells[1].getX());
    }
	
    /**
     * Returns whether the Line is a column.
     * @return				Returns whether the Line is a column.
     */
	public boolean isColumn(){
		return isVertical;
	}
	
	/**
	 * Returns whether the Line is a row.
	 * @return				Returns whether the Line is a row.
	 */
	public boolean isRow(){
		return !isVertical;
	}
	
	/**
	 * Returns a text representation of the Line, identifying it
	 * as a row or column and its position in its target.
	 * @return				Returns a text representation of the 
	 * Line, identifying it as a row or column and its position 
	 * in its target.
	 */
	public String toString(){
		
		String returnString = "";
		
		returnString += ((isVertical) ? "Column " : "Row ") + index.toInt();
		
		return returnString;
	}
	
	/**
	 * If the Line is a row, returns a textual representation of 
	 * the cells in the Line describing what values they hold or 
	 * are capable of holding.  Otherwise, returns an empty String.
	 * @param blockEdge				Character(s) to be placed between
	 * fields of values from cells from two different blocks.
	 * @param cellEdge				Character(s) to be placed between
	 * fields of values from cells from the same block.
	 * @return						If the Line is a row, returns 
	 * a textual representation of the cells in the Line describing 
	 * what values they hold or are capable of holding.  Otherwise, 
	 * returns an empty String.
	 */
	public String getPossibilitiesStringForEndDisplay(String blockEdge, String cellEdge){
		String returnString = "";
		
		//This method is meant to be called only for rows.
		if( isVertical )
			return returnString;
		
		/* 
		 * Values whose possibility has not been addressed in 
		 * a cell are represented by '-' in the returned String, 
		 * both being shorthand for -1 and being smaller characters 
		 * overall and thus slightly less noticeable.
		 */
		final char UNKNOWN = '-';
		
		/*
		 * Represents a value that has been determined 
		 * to be impossible in a cell in the return String.
		 */
		final char IMPOSSIBLE = '0';
		
		/*
		 * Represents a value known to be a cell's value 
		 * in the return String.
		 */
		final char CERTAIN = '1';
		
		/*
		 * Number of lines in the return String needed to display
		 * all nine possibility-characters
		 */
		final int possibilityRowsCount = 3;
		
		/*
		 * Number of columns of characters in the return String 
		 * needed to display all nine possibility-characters.
		 */
		final int possibilityColumnsCount = 3;
		
		/* 
		 * the first value in each cell for which a 
		 * possibility-character is needed for addtion to 
		 * the return String. When the current line being 
		 * added to the return String is the first line, 0, 
		 * add characters for 1, 2, and 3; add characters 
		 * for 4, 5, and 6 for the second row, 1, and so forth.
		 */
		final int[] initialValues = { 1, 4, 7 };
		
		for(int currentLine = 0; currentLine < possibilityRowsCount; currentLine++){
			
			returnString += blockEdge;
			
			for(Cell currentCell : cells){
				
				int initialValue = initialValues[currentLine];
				
				for(int value = initialValue; value < initialValue + possibilityColumnsCount; value++){
					Possibility possibility = currentCell.getPossibility(Value.fromInt(value));
					returnString += " " + ((possibility == Possibility.UNKNOWN) 
										? (UNKNOWN)
										: ((possibility == Possibility.IMPOSSIBLE) 
										? (IMPOSSIBLE) 
										: (CERTAIN) ));
				}
				returnString += " ";
				
				// add either cellEdge or blockEdge
				boolean cellIsAtBlockEdge = currentCell.getX().toInt() % Block.WIDTH == 0;
				returnString += cellIsAtBlockEdge ? blockEdge : cellEdge;
			}
			returnString += "\n";
		}
		return returnString;
	}
}
