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
	
	public Stream<Claim> claimStream();
	
	public int magnitude();
	
	public int sideLength();
}
