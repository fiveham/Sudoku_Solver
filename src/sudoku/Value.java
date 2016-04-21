package sudoku;

/**
 * Represents the values that a cell in a sudoku target 
 * may take on.
 * @author fiveham
 */
public enum Value {
	
	/*
	 * This enum needs to remain public so that external code 
	 * may make reference to its contents in creating new 
	 * solution technique classes.
	 */
	
	/** 0 means the cell's value is unknown. */
	UNKNOWN (0),
	
	/** 1 through 9 mean the cell's value is that number. */
	VALUE_1 (1),
	VALUE_2 (2),
	VALUE_3 (3),
	VALUE_4 (4),
	VALUE_5 (5),
	VALUE_6 (6),
	VALUE_7 (7),
	VALUE_8 (8),
	VALUE_9 (9);
	
	/** The minimum value for a cell with a known value */
	public static final Value MINIMUM = VALUE_1;
	
	/** The maximum value for a cell with a known value */
	public static final Value MAXIMUM = VALUE_9;
	
	/** All values that don't pertain to not knowing a cell's actual value */
	public static final Value[] KNOWN_VALUES = 
		{VALUE_1, VALUE_2, VALUE_3, VALUE_4, VALUE_5, VALUE_6, VALUE_7, VALUE_8, VALUE_9};
	
	private final int value;
	
	private Value(int value){
		this.value = value;
	}
	
	/**
	 * Returns the int value of this instance of the enum.
	 * @return					Returns the int value of 
	 * this instance of the enum.
	 */
	public int intValue() {
		return value;
	}
	
	/**
	 * Returns the instance of this enum that pertains to 
	 * the parameter inputValue.
	 * @param inputValue		The int for which the pertinent 
	 * instance of this enum should be returned.
	 * @return					Returns the instance of 
	 * this enum that pertains to the parameter inputValue.
	 */
	public static Value fromInt(int inputValue) {
		try{
			return values()[inputValue];
		}
		catch(ArrayIndexOutOfBoundsException e){
			throw new IllegalArgumentException("invalid value (an integer 0 to 9 inclusive is needed)");
		}
	}
}
