package sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import common.graph.BasicGraph;
import common.graph.Graph;

/**
 * <p>A bipartite graph of Rules and Claims.</p>
 * @author fiveham
 *
 */
public class SudokuNetwork extends BasicGraph<NodeSet<?,?>> implements Sudoku{
	
	/**
	 * <p>Returns true if and only if the specified {@code NodeSet<?,?>} is 
	 * a {@code Rule}.</p>
	 */
	public static final Predicate<NodeSet<?,?>> IS_FACT  = (ns)->ns instanceof Fact;
	
	/**
	 * <p>Returns true if and only if the specified {@code NodeSet<?,?>} is 
	 * a {@code Rule}.</p>
	 */
	public static final Predicate<NodeSet<?,?>> IS_CLAIM = (ns)->ns instanceof Claim;
	
	/**
	 * <p>The fundamental order of the target to which the nodes of this graph pertain, 
	 * equal to the square root of the {@link #sideLength side length}.</p>
	 */
	protected final int magnitude;
	
	/**
	 * <p>The number of coordinates along a dimension of the target to which the nodes 
	 * of this graph belong.</p>
	 */
	protected final int sideLength;
	
	public SudokuNetwork(int magnitude) {
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	public SudokuNetwork(int magnitude, Graph<NodeSet<?,?>> connectedComponent){
		this.nodes.addAll(connectedComponent.nodeStream().collect(Collectors.toList()));
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	@Override
	public boolean isSolved(){
		return factStream().allMatch((f)->f.size() == Fact.SIZE_WHEN_SOLVED);
	}
	
	@Override
	public Stream<Fact> factStream(){
		return nodes.stream().filter(IS_FACT).map((ns)->(Fact)ns);
	}
	
	/**
	 * <p>Returns a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.</p>
	 * @return a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.
	 */
	@Override
	public Stream<Claim> claimStream(){
		return nodes.stream().filter(IS_CLAIM).map((ns)->(Claim)ns);
	}
	
	@Override
	public int magnitude(){
		return magnitude;
	}
	
	@Override
	public int sideLength(){
		return sideLength;
	}
	
	/**
	 * <p>A utility class that tries to parse a target out of a text 
	 * source via a specified Scanner that scans from that text.</p>
	 * 
	 * <p>Used by Puzzle to read a sudoku puzzle of unknown dimensions 
	 * from a text file and both extract the content of the puzzle and 
	 * also extract the {@link Sudoku#magnitude() order} of the puzzle 
	 * as a side-effect, without requiring further after-the-fact 
	 * analysis.</p>
	 * @author fiveham
	 *
	 */
	protected static class Parser{
		
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
		 * the specified Scanner.</p>
		 * @param s the Scanner that sources the text that this 
		 * Parser analyses
		 */
		Parser(Scanner s){
			this.values = parse(s);
			s.close();
		}
		
		/**
		 * <p>Returns the order of the puzzle specified by the text 
		 * behind the Scanner sent to the {@link #Parser(Scanner) constructor}, 
		 * which is determined as a side-effect of the {@link #parse(Scanner) parsing} 
		 * process.</p>
		 * @return the order of the puzzle specified by the text 
		 * behind the Scanner sent to the {@link #Parser(Scanner) constructor}
		 */
		int mag(){
			return mag;
		}
		
		/**
		 * <p>Returns a list of the integers present in the text of the 
		 * puzzle as read from the text source specified by the Scanner 
		 * sent to the {@link #Parser(Scanner) constructor}.</p>
		 * @return
		 */
		List<Integer> values(){
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
				if(token.length() > 1){ //MAGIC
					throw new IllegalArgumentException(token+" is more than a single char");
				}
				result.add(parseInt(token));
			}
			
			return result;
		}
	}
}
