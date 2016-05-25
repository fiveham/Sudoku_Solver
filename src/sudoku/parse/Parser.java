package sudoku.parse;

import java.util.List;

/**
 * <p>Parses a file into a sudoku puzzle.</p>
 * 
 * <p>The file must be parsed before any calls to any of this 
 * object's puzzle-pertinent instance methods return--preferably 
 * before the constructor returns.</p>
 * @author fiveham
 *
 */
public interface Parser {
	
	/**
	 * <p>The magnitude of the puzzle parsed from the file.</p>
	 * @return the magnitude of the puzzle parsed from the file
	 */
	public int mag();
	
	/**
	 * <p>Returns a list of the values of the non-empty cells parsed 
	 * from this Parser's parsed file.</p>
	 * @return
	 */
	public List<Integer> values();
}
