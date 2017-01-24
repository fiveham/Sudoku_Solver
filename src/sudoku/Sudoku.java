package sudoku;

import common.graph.Graph;
import java.util.stream.Stream;

/**
 * <p>A graph or subgraph of a sudoku puzzle.</p>
 * @author fiveham
 */
public interface Sudoku extends Graph<NodeSet<?,?>>{
	
  /**
   * <p>Returns true if this Sudoku is solved, false otherwise. A Sudoku is solved if all its
   * Facts contain only one truth-claim.</p>
   * @return true if this Sudoku is solved, false otherwise
   */
	public boolean isSolved();
	
  /**
   * <p>Returns a Stream of all the Facts in this Sudoku.</p>
   * @return a Stream of all the Facts in this Sudoku
   */
	public Stream<Fact> factStream();
	
  /**
   * <p>Returns a Stream of all the Claims in this Sudoku.</p>
   * @return a Stream of all the Claims in this Sudoku
   */
	public Stream<Claim> claimStream();
	
  /**
   * <p>Returns the fundamental order of this Sudoku. For a Puzzle, this is the square root of the 
   * {@link #sideLength() side length}.</p>
   * @return the fundamental order of this Sudoku
   */
	public int magnitude();
	
  /**
   * <p>Returns the square of the {@link #magnitude() magnitude}. For a Puzzle, this is length of a 
   * side of this puzzle.</p>
   * @return the square of the magnitude
   */
	public int sideLength();
}
