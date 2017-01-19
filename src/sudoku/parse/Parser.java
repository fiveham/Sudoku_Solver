package sudoku.parse;

import java.util.List;

/**
 * <p>Parses a sudoku puzzle out of a file, identifying the initial {@link #values() values} held by 
 * the cells of the puzzle.</p>
 * <p>A Parser should accept a File or another reference
 * to a file's contents, such as a Scanner or a String, and determine the magnitude and initial 
 * cell values of the puzzle.</p>
 * <p>The file must be parsed before any calls to any of this object's puzzle-pertinent instance 
 * methods return--preferably before the constructor returns.</p>
 * @author fiveham
 */
public interface Parser {
	
  /**
   * <p>The largest allowed numerical base for human-readable representations of a Puzzle.</p>
   * <p>Equal to 36 because that is the number of digits 0-9 plus the number of letters in the 
   * English alphabet. Coincidentally, this is also the appropriate base for printing a puzzle with 
   * a magnitude of 6.</p>
   */
	public static final int MAX_RADIX = 36;
	
  /**
   * <p>The {@link Sudoku#magnitude() magnitude} of the puzzle parsed from the file, equal to the 
   * square root of the puzzle's side-length.</p>
   * @return the magnitude of the puzzle parsed from the file
   */
	public int mag();
	
  /**
   * <p>Returns a list of the values of the cells parsed from the file parsed by this Parser. The 
   * values are listed in encounter order.</p>
   * @return a list of the values of the cells parsed from the file parsed by this Parser in 
   * encounter order
   */
	public List<Integer> values();
}
