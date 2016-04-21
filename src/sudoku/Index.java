package sudoku;

/**
 * Represents various indices that needs to vary
 * between lower and upper limits of 1 and 9 
 * (inclusive) respectively.
 * @author fiveham
 */
public enum Index {
	
	/*
	 * This enum needs to remain public so that external code 
	 * may make reference to its contents in creating new 
	 * solution technique classes.
	 */
	
	/**
	 * 0 means that the index of a region or the value 
	 * of an x or y coordinate in a cell is uncertain.
	 */
	UNKNOWN (0),
	
	/**
	 * 1 through 9 indicate that the index of a region 
	 * or the x or y coordinate of a cell is that value.
	 */
	INDEX_1 (1),
	INDEX_2 (2),
	INDEX_3 (3),
	INDEX_4 (4),
	INDEX_5 (5),
	INDEX_6 (6),
	INDEX_7 (7),
	INDEX_8 (8),
	INDEX_9 (9);
	
	/** Minimum value of index */
	public static final Index MINIMUM = INDEX_1;
	
	/** Maximum value of index */
	public static final Index MAXIMUM = INDEX_9;
	
	/** Total number of values that indicate an index is known */
	public static final int INDEX_COUNT = 9;
	
	/** all values in the enum that don't pertain to not knowing the actual value */
	public static final Index[] KNOWN_VALUES = 
		{INDEX_1, INDEX_2, INDEX_3, INDEX_4, INDEX_5, INDEX_6, INDEX_7, INDEX_8, INDEX_9};
	
	private final int index;
	
	private Index(int index){
		this.index = index;
	}
	
	/**
	 * Returns the internal value field for this index.
	 * @return					Returns the internal 
	 * value field for this index.
	 */
	public int intValue(){
		return index;
	}
	
	/**
	 * Returns the enum instance with its instance field
	 * equal to the parameter input.
	 * @param input				int to be used to 
	 * determine which enum instance to return
	 * @return					Returns the enum 
	 * instance with its instance field
	 * equal to the parameter input.
	 */
	public static Index fromInt(int input){
		try{
			return values()[input];
		}
		catch(ArrayIndexOutOfBoundsException e){
			throw new IllegalArgumentException("invalid index (an integer 0 to 9 inclusive is needed)");
		}
	}
}