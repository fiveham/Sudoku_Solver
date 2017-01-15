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
 * <p>A set that's a node in {@link Puzzle a graph representation of a sudoku target}, such that the
 * NodeSet itself is a collection of its {@link Vertex#neighbors() neighbors}.</p> <p>The NodeSet
 * class serves as a pool to unite certain operations performed identically by both Claim and Rule.
 * Most importantly, NodeSet serves to enforce, in one consistent location, the rule that any node's
 * neighbors must know that that node in question is one of their neighbors: neighbor status must be
 * symmetrical. Additionally, NodeSet being superclass to both Claim and Rule enables Claim and Rule
 * to be elements of the same collection, so that a bipartite graph like Puzzle doesn't need to be
 * described as a bipartite graph explicitly via a BipartiteGraph contract, as that would feed into
 * combinatorial explosion, given the need for WrappingGraphs as well.</p>
 * @author fiveham
 * @param <T> The type of the elements of this Set.
 * @param <S> The type of the Set elements of this Set. This should also be a proxy for this type
 * itself.
 */
public abstract class NodeSet<T extends NodeSet<S, T>, S extends NodeSet<T, S>> 
    extends ToolSet<T> 
    implements Vertex<NodeSet<?, ?>>{
	
	private static final long serialVersionUID = 6938429068342291749L;
	
	protected final Puzzle puzzle;
	protected final int hashCode;
	
	protected NodeSet(Puzzle puzzle, int hash){
		this.puzzle = puzzle;
		this.hashCode = hash;
	}
	
	protected NodeSet(Puzzle puzzle, int initialCapacity, int hash) {
		super(initialCapacity * Sets.JAVA_UTIL_HASHSET_SIZE_FACTOR);
		this.puzzle = puzzle;
		this.hashCode = hash;
	}
	
  /**
   * <p>Returns the target to which this NodeSet belongs.</p>
   * @return the target to which this NodeSet belongs
   */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final boolean add(T e){
		boolean result = super.add(e);
		if(result){
			e.add((S)this);
		}
		return result;
	}
	
	@Override
	public final boolean addAll(Collection<? extends T> c){
		return c.stream()
		    .map(this::add)
		    .reduce(false, Boolean::logicalOr);
	}
	
	@Override
	public final boolean remove(Object o){
		return remove_internal(o);
	}
	
  /**
   * <p>Removes {@code o} and removes {@code this} from {@code o} without
   * {@link #validateFinalState() validating the set afterwards}.</p> <p>Used internally so that
   * bulk operations can validate the set's state only after they have completed instead of
   * numerous times throughout the bulk operation and so that only one removal method needs to
   * ensure mutual element-removal. If bulk operations were subject to final-state validation
   * checks by calling remove(Object) repeatedly, then any bulk removal operation that removes all
   * but one of the elements of this set could force a Rule to trigger a value-claim event in the
   * middle of the operation even though such a Rule really ought to wait until the end to
   * validate, which allows just one automatic resolution event to trigger.</p>
   * @param o the object being removed
   * @return true if this set has been changed by the operation, false otherwise
   */
	private boolean remove_internal(Object o){
		boolean result = super.remove(o);
		if(result){
			((NodeSet<?,?>)o).remove(this);
		}
		return result;
	}
	
	@Override
	public final boolean removeAll(Collection<?> c){
		boolean result = false;
		for(Object o : c){
			result |= remove_internal(o);
		}
		return result;
	}
	
	@Override
	public final boolean retainAll(Collection<?> c){
		boolean result = false;
		Iterator<T> iter = super.iterator();
		for(T t; iter.hasNext();){
			if(!c.contains(t = iter.next())){
				remove_internal(t);
				result = true;
			}
		}
		return result;
	}
	
	@Override
	public final void clear(){
		Iterator<T> iter = iterator();
		while(iter.hasNext()){
			iter.next();
			iter.remove();
		}
	}
	
	@Override
	public final Iterator<T> iterator(){
		return new SafeRemovingIterator();
	}
	
  /**
   * <p>An Iterator whose {@link Iterator#remove() remove()} method calls the
   * {@link Collection#remove() remove(Object)} method of the removed element so as to remove this
   * NodeSet from the element that was removed from this NodeSet, guaranteeing symmetry of
   * connections in the graph.</p>
   * @author fiveham
   * @author fiveham
	 *
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
     * <p>Returns a set of the vertices visible to this vertex. A vertex is visible to this one if
     * that vertex and this one share at least one {@link #neighbors() neighbor} in common.</p>
     * @return a set of the vertices that share at least one {@link #neighbors() neighbor} in common
     * with this vertex
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
	
	@Override
	public final int hashCode(){
		return hashCode;
	}
	
    /**
     * <p>Returns an int that encodes the specified {@code x}, {@code y}, and {@code z} coordinates
     * as if they belong to a Claim whose {@link #getPuzzle puzzle} has the specified
     * {@code sideLength}.</p> <p>The coordinates are concatenated as digits in a number system with
     * a base equal to {@code sideLength + 1}, with the first digit being the x-coordinate, followed
     * by the y-coordinate, followed by the z-coordinate.</p>
     * @param x the coordinate given the highest digital significance
     * @param y the coordinate given the second-highest digital significance
     * @param z the coordinate given the lowest digital significance.
     * @param sideLength the side-length of the {@link #getPuzzle puzzle} to which the NodeSet whose
     * coordinates are being linearized belongs.
     * @return an int that encodes the specified {@code x}, {@code y}, and {@code z} coordinates as
     * if they belong to a Claim whose {@link #getPuzzle puzzle} has the specified
     * {@code sideLength}
     */
	public static int linearizeCoords(int x, int y, int z, int sideLength){
		return x * sideLength * sideLength + y * sideLength + z;
	}
}
