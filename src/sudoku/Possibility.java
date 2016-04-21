package sudoku;

/**
 * Represents the possibility of a given 
 * value being the value of a given cell 
 * in a sudoku target.
 * @author fiveham
 */
public enum Possibility {
	
	/*
	 * This enum needs to remain public so that external code 
	 * may make reference to its contents in creating new 
	 * solution technique classes.
	 */
	
	/**
	 * -1 means that it is not yet clear whether a 
	 * certain value is possible or not for the cell.
	 */
	UNKNOWN (-1),
	
	/**
	 * 0 means that the pertinent value is known to 
	 * not be the value of the cell.
	 */
	IMPOSSIBLE (0),
	
	/**
	 * 1 means that the pertinent value is the value 
	 * of the cell.
	 */
	CERTAIN (1);
	
	/**
	 * The values that should be found in a solved 
	 * sudoku target.
	 */
	public static Possibility[] FINAL_VALUES = { IMPOSSIBLE, CERTAIN };
	
	/* 
	 * The difference between the value of a 
	 * Possibility instance and the position 
	 * of that Possibility in the values() array.
	 */
	private static int OFFSET = 1;
	
	private int possibility;
	
	private Possibility(int possibility){
		this.possibility = possibility;
	}
	
	/**
	 * Returns the int value of this instance.
	 * @return					Returns the int 
	 * value of this instance.
	 */
	public int toInt(){
		return possibility;
	}
	
	/**
	 * Returns the Possibility instance pertaining to the 
	 * parameter input.
	 * @param input				The int value for which 
	 * the Possibility that has that value as its 
	 * private field should be returned.
	 * @return					Returns the Possibility 
	 * instance pertaining to the parameter input.
	 */
	public static Possibility fromInt(int input){
		try{
			return values()[input + OFFSET];
		}
		catch(ArrayIndexOutOfBoundsException e){
			throw new IllegalArgumentException("invalid possibility (a -1, 0, or 1 is needed)");
		}
	}
}
