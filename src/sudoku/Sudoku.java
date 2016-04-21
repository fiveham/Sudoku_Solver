package sudoku;

import common.graph.Graph;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * <p>A subgraph of a sudoku target, including an intact initial 
 * sudoku target.</p>
 * @author fiveham
 *
 */
public interface Sudoku extends Graph<NodeSet<?,?>>{
	
	public boolean isSolved();
	
	public Stream<Fact> factStream();
	
	public Stream<Claim> claimStream();
	
	public boolean addRemovalListener(Consumer<NodeSet<?,?>> listener);
	
	public boolean removeRemovalListener(Consumer<NodeSet<?,?>> listener);
	
	//public TimeBuilder timeBuilder();
	
	public boolean removeNode(NodeSet<?,?> node);
	
	public int magnitude();
	
	public int sideLength();
}
