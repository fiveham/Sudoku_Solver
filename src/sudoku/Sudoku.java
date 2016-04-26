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
	
	public Stream<Fact> factStream();
	
	public Stream<Claim> claimStream();
	
	public int magnitude();
	
	public int sideLength();
}
