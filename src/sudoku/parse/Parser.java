package sudoku.parse;

import java.util.List;

/**
 * <p>Parses a file into a sudoku puzzle.</p>
 * 
 * <p>A Parser should accept a File or another reference to a 
 * file's contents, such as a Scanner or a String, and determine 
 * the magnitude of the specified puzzle and the initial contents 
 * of that puzzle.</p>
 * 
 * <p>The file must be parsed before any calls to any of this 
 * object's puzzle-pertinent instance methods return--preferably 
 * before the constructor returns.</p>
 * @author fiveham
 *
 */
public interface Parser {
	
	public static final int MAX_RADIX = 36;
	
	/**
	 * <p>The magnitude of the puzzle parsed from the file. A 
	 * Puzzle's {@link sudoku.Sudoku#magnitude() magnitude} is 
	 * the square root of that puzzle's side-length.</p>
	 * @return the magnitude of the puzzle parsed from the file
	 */
	public int mag();
	
	/**
	 * <p>Returns a list of the values of the cells parsed 
	 * from this Parser's file.</p>
	 * @return
	 */
	public List<Integer> values();
}
