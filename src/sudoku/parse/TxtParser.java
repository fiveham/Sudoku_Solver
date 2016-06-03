package sudoku.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import sudoku.Sudoku;

/**
 * <p>A utility class that tries to parse a target out of a text 
 * source via a specified Scanner that scans from that text.</p>
 * 
 * <p>Used by Puzzle to read a sudoku puzzle of unknown dimensions 
 * from a .txt file and to extract both the content of the puzzle and 
 * the {@link Sudoku#magnitude() magnitude} of the puzzle without 
 * requiring separate passes for each of those results.</p>
 * @author fiveham
 *
 */
public class TxtParser implements Parser{
	
	/**
	 * <p>The lowest possible magnitude of a sudoku puzzle that anyone 
	 * would want to subject to an automated solver: {@value}.</p>
	 * 
	 * <p>The only smaller size is 1, which has exactly 1 solution.</p>
	 */
	public static final int MIN_REASONABLE_SUDOKU_MAGNTITUDE = 2;
	
	private int mag = MIN_REASONABLE_SUDOKU_MAGNTITUDE;
	private final List<Integer> values;
	
	/**
	 * <p>Constructs a Parser that extracts and parses text via 
	 * the specified Scanner. {@code s} is closed by this constructor.</p>
	 * @param s the Scanner that sources the text that this 
	 * Parser analyses. {@code s} is closed by this constructor
	 */
	public TxtParser(Scanner s){
		this.values = parse(s);
		s.close();
	}
	
	/**
	 * <p>Returns the magnitude of the puzzle specified by the text 
	 * behind the Scanner sent to the {@link #Parser(Scanner) constructor}, 
	 * which is determined as a side-effect of the {@link #parse(Scanner) parsing} 
	 * process.</p>
	 * @return the magnitude of the puzzle specified by the text 
	 * behind the Scanner sent to the {@link #Parser(Scanner) constructor}
	 */
	@Override
	public int mag(){
		return mag;
	}
	
	/**
	 * <p>Returns a list of the integers present in the text of the 
	 * puzzle as read from the text source specified by the Scanner 
	 * sent to the {@link #Parser(Scanner) constructor}.</p>
	 * @return the initial values in the cells of this Parser's puzzle, 
	 * where 0 represents an empty cell
	 */
	@Override
	public List<Integer> values(){
		return values;
	}
	
	/**
	 * <p>Returns the radix to be used for {@link Integer#parseInt(String) parsing} 
	 * human-readable text integers into {@code int}s for internal use. The value 
	 * returned depends on the current value of {@link #mag mag}.</p>
	 * @return the radix to be used for parsing the human-readable values of the 
	 * cells specified in the text source for this target into {@code int}s for 
	 * internal use, depending on the current value of {@code mag}
	 */
	private int radix(){
		return mag*mag+1;
	}
	
	/**
	 * <p>Converts a human-readable integer in an unknown base, from a target of 
	 * unknown size, into an {@code int} while determining what base is appropriate 
	 * for parsing the current and remaining text into ints.</p>
	 * @param token the string to be parsed into an int
	 * @return the int parsed from the specified token
	 */
	private int parseInt(String token){
		Integer result = null;
		while( result == null ){
			try{
				result = Integer.parseInt(token, radix());
			} catch(NumberFormatException e){
				++mag;
			}
		}
		return result;
	}
	
	public static final int MAX_CELL_CONTENT_STRING_LEN = 1;
	
	/**
	 * <p>Gets tokens one-at-a-time from the specified Scanner, and 
	 * parses them into ints while determining the size of the target 
	 * represented in the text that the Scanner scans.</p>
	 * @param s the Scanner used to access the target's source text
	 * @return a list of integers each of which is the value of a 
	 * cell in the target; the mapping between cells and the list is 
	 * a snake starting in the upper left corner (low x,y), moving 
	 * right (increasing x), then wrapping around to the next y-level 
	 * until the snake reaches the lower right (high x,y)
	 */
	private List<Integer> parse(Scanner s){
		List<Integer> result = new ArrayList<>();
		
		while(s.hasNext() && mag*mag*mag*mag > result.size()){
			String token = s.next();
			if(token.length() > MAX_CELL_CONTENT_STRING_LEN){
				throw new IllegalArgumentException(token+" is more than a single char");
			}
			result.add(parseInt(token));
		}
		
		return result;
	}
}