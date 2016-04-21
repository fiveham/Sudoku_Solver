package sudoku;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import common.graph.BasicGraph;
import common.graph.Graph;
import common.time.Root;
import common.time.TimeBuilder;

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
	
	/**
	 * <p>The root of this target's time-tree for solution-events.</p>
	 */
	protected final TimeBuilder root; //TODO should this be a new Root every time or be the same root passed on from the Puzzle (or an intermediary time node not the root)?
	
	public SudokuNetwork(int magnitude) {
		this.removalListeners = new ArrayList<>();
		this.root = new Root();
		
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	public SudokuNetwork(int magnitude, Graph<NodeSet<?,?>> connectedComponent){
		this.nodes.addAll(connectedComponent.nodeStream().collect(Collectors.toList()));
		this.removalListeners = new ArrayList<>();
		this.root = new Root();
		
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	@Override
	public boolean isSolved(){
		return factStream().allMatch((f)->f.size() == Fact.SIZE_WHEN_SOLVED);
	}
	
	/**
	 * <p>Returns a Stream of those nodes of this Puzzle's underlying graph 
	 * that are of the type <tt>Rule</tt>. The nodes returned are cast as 
	 * Rule, as well.</p>
	 * @return a Stream of all the <tt>Rule</tt>-type nodes from this Puzzle's 
	 * underlying graph.
	 */
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
	
	/**
	 * <p>A list of event-listeners that respond when a node 
	 * {@link #removeNode(NodeSet) is removed} from this target.</p>
	 */
	private final List<Consumer<NodeSet<?,?>>> removalListeners;
	
	/**
	 * <p>Adds <tt>listener</tt> to this target's list of 
	 * {@link #removalListeners removal-listeners}.</p>
	 * @param listener a removal-event listener
	 * @return true if the list of removal-event listeners was 
	 * changed by calling this method, false otherwise
	 */
	@Override
	public boolean addRemovalListener(Consumer<NodeSet<?,?>> listener){
		return removalListeners.add(listener);
	}
	
	/**
	 * <p>Removes <tt>listener</tt> from this target's list of 
	 * {@link #removalListeners removal-listeners}.</p>
	 * @param listener a removal-event listener
	 * @return true if the list of removal-event listeners was 
	 * changed by calling this method, false otherwise
	 */
	@Override
	public boolean removeRemovalListener(Consumer<NodeSet<?,?>> listener){
		return removalListeners.remove(listener);
	}
	
	/**
	 * <p>Removes the specified <tt>node</tt> from this Puzzle's underlying graph.</p>
	 * @see #removalListeners
	 * @param node the Rule or Claim to be removed
	 */
	@Override
	public boolean removeNode(NodeSet<?,?> node){
		if(nodes.remove(node)){
			node.losePuzzle();
			notifyRemovalListeners(node);
			return true;
		}
		return false;
	}
	
	private void notifyRemovalListeners(NodeSet<?,?> node){
		removalListeners.stream().forEach((listener)->listener.accept(node));
	}
	
	/* *
	 * <p>Returns the root of this Puzzle's resolution time-tree.</p>
	 * @return the root of this Puzzle's resolution time-tree
	 */
	/*@Override
	public TimeBuilder timeBuilder(){
		return root;
	}*/
}
