package sudoku;

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
	
	/**
	 * <p>Returns true if this SudokuNetwork is solved, false otherwise. A 
	 * SudokuNetwork is solved iff all of its {@code Fact}s each have only 
	 * one {@code Claim}.</p>
	 * @return true if this SudokuNetwork is solved, false otherwise
	 */
	@Override
	public boolean isSolved(){
		return factStream().allMatch(Fact::isSolved);
	}
	
	@Override
	public Stream<Fact> factStream(){
		return nodes.stream().filter(Fact.IS_FACT).map(Fact.AS_FACT);
	}
	
	/**
	 * <p>Returns a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.</p>
	 * @return a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.
	 */
	@Override
	public Stream<Claim> claimStream(){
		return nodes.stream().filter(Claim.IS_CLAIM).map(Claim.AS_CLAIM);
	}
	
	@Override
	public int magnitude(){
		return magnitude;
	}
	
	@Override
	public int sideLength(){
		return sideLength;
	}
}
