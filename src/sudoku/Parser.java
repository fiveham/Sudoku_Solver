package sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * <p>A utility class that tries to parse a target out of a text 
 * source via a specified Scanner that scans from that text.</p>
 * @author fiveham
 *
 */
public class Parser{
	
	private int mag = 2;
	private final List<Integer> values;
	
	public Parser(Scanner s){
		this.values = parse(s);
		s.close();
	}
	
	public int mag(){
		return mag;
	}
	
	public List<Integer> values(){
		return values;
	}
	
	/**
	 * <p>Returns the radix to be used for {@link Integer#parseInt(String) parsing} 
	 * human-readable text integers into <tt>int</tt>s for internal use. The value 
	 * returned depends on the current value of {@link #mag mag}.</p>
	 * @return the radix to be used for parsing the human-readable values of the 
	 * cells specified in the text source for this target into <tt>int</tt>s for 
	 * internal use, depending on the current value of <tt>mag</tt>
	 */
	private int radix(){
		return mag*mag+1;
	}
	
	/**
	 * <p>Converts a human-readable integer in an unknown base, from a target of 
	 * unknown size, into an <tt>int</tt> while determining what base is appropriate 
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
			if(token.length() > 1){
				throw new IllegalArgumentException(token+" is more than a single char");
			}
			result.add(parseInt(token));
		}
		
		return result;
	}
}
