package sudoku;

import common.graph.Graph;
import java.util.stream.Stream;

/**
 * <p>A subgraph of a sudoku puzzle.</p>
 * @author fiveham
 *
 */
public interface Sudoku extends Graph<NodeSet<?,?>>{
	
	public boolean isSolved();
	
	/**
	 * <p>Returns a <tt>Stream&lt;Fact&gt;</tt> providing access to all 
	 * the <tt>Fact<tt>s in this <tt>Sudoku</tt>'s underlying 
	 * {@link common.graph.Graph Graph}.</p>
	 * 
	 * <p>Note: Because Facts in a Sudoku must become equal to other Facts 
	 * in the same Sudoku in the process of solving a puzzle, it is possible 
	 * that the Stream returned by a call to this method may contain Facts 
	 * that are equal (as {@link java.util.Set Sets}) to each other. If the 
	 * Facts in the returned Stream need to be unique, for example for use 
	 * in generating the source combinations for the sledgehammer technique, 
	 * factStream() should be {@link Stream#collect(java.util.stream.Collector) collected} 
	 * as {@link java.util.stream.Collectors#toSet() a Set}.</p>
	 * 
	 * @return a <tt>Stream&lt;Fact&gt;</tt> providing access to all 
	 * the <tt>Fact<tt>s in this <tt>Sudoku</tt>
	 */
	public Stream<Fact> factStream();
	
	/**
	 * <p>Returns a Stream of the Claims in this Sudoku's underlying Graph.</p>
	 * @return
	 */
	public Stream<Claim> claimStream();
	
	/**
	 * <p>Returns the underlying order of this Puzzle, the  square root of 
	 * the side length.</p>
	 * @return the underlying order of this Puzzle, the square root of the 
	 * side length
	 */
	public int magnitude();
	
	/**
	 * <p>Returns the length of a side of this Puzzle, which is also the number 
	 * of rows, the number of columns, and the number of boxes.</p>
	 * @return the length of a side of this target
	 */
	public int sideLength();
}