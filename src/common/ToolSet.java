package common;

import java.util.HashSet;

/**
 * <p>A utility class providing certain set-manipulation tools, in the form of instance methods,
 * that some sets used in the sudoku target-solving techniques provided in this package.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T>
 * @param <T>@param <T>
 */
public class ToolSet<T> extends HashSet<T> {
	
	private static final long serialVersionUID = 7562530465837262993L;
	
  /**
   * <p>Constructs an empty ToolSet.</p>
   */
	protected ToolSet() {
	}
	
  /**
   * <p>Constructs an empty ToolSet whose capacity is initially set to {@code initialCapacity}.</p>
   * @param initialCapacity the initial capacity of this ToolSet
   */
	protected ToolSet(int initialCapacity) {
		super(initialCapacity);
	}
}
