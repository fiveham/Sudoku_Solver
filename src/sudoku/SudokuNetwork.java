package sudoku;

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
	 * <p>Returns true if and only if the specified <tt>NodeSet<?,?></tt> is 
	 * a <tt>Rule</tt>.</p>
	 */
	public static final Predicate<NodeSet<?,?>> IS_FACT  = (ns)->ns instanceof Fact;
	
	/**
	 * <p>Returns true if and only if the specified <tt>NodeSet<?,?></tt> is 
	 * a <tt>Rule</tt>.</p>
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
	 * <p>Returns a Stream of all the <tt>Claim</tt>-type nodes in this 
	 * Puzzle's underlying graph.</p>
	 * @return a Stream of all the <tt>Claim</tt>-type nodes in this 
	 * Puzzle's underlying graph.
	 */
	@Override
	public Stream<Claim> claimStream(){
		return nodes.stream().filter(IS_CLAIM).map((ns)->(Claim)ns);
	}
	
	/**
	 * <p>Returns the underlying order of this Puzzle, the 
	 * square root of the side length.</p>
	 * @return the underlying order of this Puzzle, the 
	 * square root of the side length
	 */
	@Override
	public int magnitude(){
		return magnitude;
	}
	
	/**
	 * <p>Returns the length of a side of this Puzzle, which is 
	 * also the number of rows, the number of columns, and the 
	 * number of boxes.</p>
	 * @return the length of a side of this target
	 */
	@Override
	public int sideLength(){
		return sideLength;
	}
}
