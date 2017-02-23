package sudoku;

import common.graph.Graph;
import java.util.stream.Stream;

/**
 * <p>A graph or subgraph of a sudoku puzzle.</p>
 * @author fiveham
 */
public interface Sudoku extends Graph<NodeSet<?,?>>{
	
  /**
   * <p>Returns true if this Sudoku is solved, false otherwise. A Sudoku is solved if all its Facts 
   * contain only one truth-claim.</p>
   * @return true if this Sudoku is solved, false otherwise
   */
  public default boolean isSolved(){
    return factStream()
        .allMatch(Fact::isSolved);
  }
	
  /**
   * <p>Returns a Stream of the Facts in this Sudoku's underlying Graph.</p>
   * @return a Stream of the Facts in this Sudoku's underlying Graph
   */
	public default Stream<Fact> factStream(){
	  return nodeStream()
	      .filter(Fact.class::isInstance)
	      .map(Fact.class::cast);
	}
	
  /**
   * <p>Returns a Stream of the Claims in this Sudoku's underlying Graph.</p>
   * @return a Stream of the Claims in this Sudoku's underlying Graph
   */
	public default Stream<Claim> claimStream(){
	  return nodeStream()
	      .filter(Claim.class::isInstance)
	      .map(Claim.class::cast);
	}
	
  /**
   * <p>Returns the fundamental order of this Sudoku. For a Puzzle, this is the square root of the 
   * {@link #sideLength() side length}.</p>
   * @return the fundamental order of this Sudoku
   */
	public int magnitude();
	
  /**
   * <p>Returns the length of a side of this puzzle, which is also the number of rows, the number
   * of columns, and the number of boxes.</p>
   * @return the length of a side of this puzzle
   */
	public default int sideLength(){
	  int m = magnitude();
	  return m * m;
	}
}
