package sudoku;

import common.Sets;
import common.ToolSet;
import common.graph.Vertex;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>A set that's a node in {@link Puzzle a graph representation of a sudoku puzzle}, such that a 
 * NodeSet itself is a collection of its {@link Vertex#neighbors() neighbors}.</p>
 * 
 * <p>The NodeSet class serves as a pool to unite certain operations performed identically by both 
 * Claim and Fact. Most importantly, NodeSet serves to enforce, in one consistent location, the rule
 * that any node's neighbors must know that that node in question is one of their neighbors: 
 * Neighbor status must be symmetrical. Additionally, NodeSet being superclass to both Claim and 
 * Rule enables Claim and Fact to be elements of the same collection, allowing a Puzzle's graph  to 
 * be backed by a single {@literal Collection<NodeSet<?, ?>} rather than two Collections of 
 * different parameter-types.</p>
 * 
 * @author fiveham
 * @param <T> The type of the elements of this set.
 * @param <S> The type of this set
 */
public abstract class NodeSet<T extends NodeSet<S, T>, S extends NodeSet<T, S>> 
    extends ToolSet<T> 
    implements Vertex<NodeSet<?, ?>>{
	
	private static final long serialVersionUID = 6938429068342291749L;
	
	protected final Puzzle puzzle;
	protected final int hashCode;
	
	/**
	 * <p>Constructs a NodeSet that belongs to {@code puzzle} and has {@code hash} as its 
	 * {@link #hashCode() hashcode}.</p>
	 * @param puzzle the puzzle to which this NodeSet belongs
	 * @param hash this NodeSet's hashcode
	 * @see #hashCode()
	 */
	protected NodeSet(Puzzle puzzle, int hash){
		this.puzzle = puzzle;
		this.hashCode = hash;
	}
	
	/**
	 * <p>Constructs a </p>
	 * @param puzzle the puzzle to which this NodeSet belongs
	 * @param initialCapacity the initial capacity of this set
	 * @param hash this NodeSet's hashcode
   * @see #hashCode()
	 */
	protected NodeSet(Puzzle puzzle, int initialCapacity, int hash) {
		super(initialCapacity * Sets.JAVA_UTIL_HASHSET_SIZE_FACTOR);
		this.puzzle = puzzle;
		this.hashCode = hash;
	}
	
  /**
   * <p>Returns the puzzle to which this NodeSet belongs.</p>
   * @return the puzzle to which this NodeSet belongs
   */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/**
	 * <p>Links this node with {@code otherNode}.</p>
	 * @return true if the added connection was not present before the call, false otherwise
	 */
	@Override
	@SuppressWarnings("unchecked")
	public final boolean add(T unlinkedNode){
		boolean change = super.add(unlinkedNode);
		if(change){
			unlinkedNode.add((S)this);
		}
		return change;
	}
	
	/**
	 * <p>Links this node with all the nodes in {@code otherNodes}.</p>
	 */
	@Override
	public final boolean addAll(Collection<? extends T> unlinkedNodes){
		return unlinkedNodes.stream()
		    .map(this::add)
		    .reduce(false, Boolean::logicalOr);
	}
	
	/**
	 * <p>Removes the edge linking this node with the {@code otherNode} if {@code otherNode} is a 
	 * node.</p>
	 */
	@Override
	public final boolean remove(Object linkedNode){
		boolean change = super.remove(linkedNode);
		if(change){
			((NodeSet<?, ?>) linkedNode).remove(this);
		}
		return change;
	}
	
	/**
	 * <p>Removes from this set all elements of {@code otherNodes}. Since this class of set is meant 
	 * to be used as a node in a graph, in practice a call to this method will be an instruction for 
	 * this node and all nodes contained in {@code c} to remove the edge linking this node with each 
	 * of those nodes.</p>
	 * @param otherNodes nodes to be disconnected from this node
	 */
	@Override
	public final boolean removeAll(Collection<?> otherNodes){
		boolean change = false;
		for(Object otherNode : otherNodes){
			change |= remove(otherNode);
		}
		return change;
	}
	
	/**
	 * <p>Removes from this set all of its elements that are not found in {@code otherNodes}. Since 
	 * this class is meant to be used as a node in a graph, in practice a call to method will be an 
	 * instruction for this node to delete all its edges that don't connect it with nodes in 
	 * {@code otherNodes}.</p>
	 * @param otherNodes the nodes connections to which will not be broken while all other connections 
	 * are broken
	 */
	@Override
	public final boolean retainAll(Collection<?> otherNodes){
		boolean change = false;
		Iterator<T> linkedNodes = super.iterator();
		for(T linkedNode; linkedNodes.hasNext();){
			if(!otherNodes.contains(linkedNode = linkedNodes.next())){
				remove(linkedNode);
				change = true;
			}
		}
		return change;
	}
	
	/**
	 * <p>Removes all edges connecting to this node.</p>
	 */
	@Override
	public final void clear(){
		Iterator<T> linkedNodes = iterator();
		while(linkedNodes.hasNext()){
			linkedNodes.next();
			linkedNodes.remove();
		}
	}
	
	@Override
	public final Iterator<T> iterator(){
		return new SafeRemovingIterator();
	}
	
  /**
   * <p>An Iterator whose {@link Iterator#remove() remove()} method calls 
   * {@link Collection#remove() remove(this)} on the removed element to remove this NodeSet from 
   * the removed element, ensuring that the edge linking this node with the removed element is 
   * completely removed.</p>
   * @author fiveham
	 */
	private class SafeRemovingIterator implements Iterator<T>{
	  
		private Iterator<T> wrapped = NodeSet.super.iterator();
		private T lastResult = null;
		
		@Override
		public void remove(){
			if(lastResult != null){
				wrapped.remove();
				lastResult.remove(NodeSet.this);
				lastResult = null;
			} else{
				throw new IllegalStateException("Previous next() element already removed.");
			}
		}
		
		@Override
		public T next(){
			return lastResult = wrapped.next();
		}
		
		@Override
		public boolean hasNext(){
			return wrapped.hasNext();
		}
	}
	
	@Override
	public final Collection<NodeSet<?, ?>> neighbors(){
		return this.stream().map(NodeSet.class::cast).collect(Collectors.toList());
	}
	
  /**
   * <p>Returns a set of vertices that share at least one {@link #neighbors() neighbor} in common 
   * with this vertex and which are not directly connected to this vertex.</p>
   * @return a set of vertices that share at least one {@link #neighbors() neighbor} in common with 
   * this vertex and which are not directly connected to this vertex
   */
	public Set<S> visible(){
		Set<S> pool = stream()
		    .map(HashSet<S>::new)
		    .reduce(
		        new HashSet<S>(), 
		        Sets::mergeCollections);
		pool.remove(this);
		return pool;
	}
	
  /**
   * <p>Returns the toString() content for this NodeSet as if it were only a HashSet.</p>
   * @return {@link HashSet#toString() super.toString()}
   */
	public String contentString(){
		return super.toString();
	}
	
	/**
	 * <p>Returns this NodeSet's hashcode, which is a constant value that does not change with changes 
	 * in the content of the set.</p>
	 * 
	 * <p>Because a NodeSet contains (references to) its neighbors as elements, a NodeSet's elements all
   * contain (references to) that NodeSet itself. Nodeset extends HashSet; so, the implementation of 
   * hashCode available to NodeSet without overriding the method returns the sum of the hashcodes of 
   * the elements of the set, but since a NodeSet just refers to other NodeSets, no concrete values 
   * can ever be extracted to begin summing; rather, a call for one NodeSet's hashcode will result in 
   * a loop of calls to {@code hashCode()} on one NodeSet after another, ending in stack overflow.</p>
   * 
   * <p>A NodeSet is used as an element of other NodeSets; so, a NodeSet's hashcode needs to remain 
   * consistent and be independent of the NodeSet's content. Otherwise the process of mutual 
   * dissociation when an edge is removed won't work. If NodeSet did not override {@code hashCode()} 
   * and used HashSet's implementation, then the process of removing an edge between {@code A} and 
   * {@code B} by calling {@code A.remove(B)} would go as follows:
   * <ol>
   * <li>B's hashcode is determined</li>
   * <li>B's entry in A is found and removed</li>
   * <li>B.remove(A) is called</li>
   * <li>A's hashcode is determined</li>
   * <li>A's entry in B is not found, even though it is present, because A's hashcode is different 
   * than it was when the entry was put in</li>
   * </ol></p>
	 */
	@Override
	public final int hashCode(){
		return hashCode;
	}
	
  /**
   * <p>The specified {@code x}, {@code y}, and {@code z} coordinates are combined into a single 
   * unique integer equal to the concatenation of {@code x}, {@code y}, and {@code z} in 
   * base-{@code sideLength} if {@code x}, {@code y}, and {@code z} are all less than 
   * {@code sideLength}.</p>
   * @param x the coordinate given the highest digital significance
   * @param y the coordinate given the second-highest digital significance
   * @param z the coordinate given the lowest digital significance.
   * @param sideLength the side-length of the {@link #getPuzzle puzzle} to which the NodeSet whose
   * coordinates are being linearized belongs.
   * @return an int that encodes {@code x}, {@code y}, and {@code z} as base-{@code sideLength} 
   * digits
   */
	public static int linearizeCoords(int x, int y, int z, int sideLength){
		return x * sideLength * sideLength + y * sideLength + z;
	}
}
