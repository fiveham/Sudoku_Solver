package common.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * <p>A graph whose connection data is stored in adjacency list form, each vertex keeping track of
 * with which other vertices it shares an edge.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T>
 * @param <T>@param <T>
 */
public interface Graph<T extends Vertex<T>> extends Iterable<T>{
	
    /**
     * <p>Returns the number of vertices in this Graph.</p>
     * @return the number of vertices in this Graph
     */
	public int size();
	
    /**
     * <p>Returns a stream based on the underlying collection of nodes.</p>
     * @return a stream backed by this Graph's underlying collection of nodes
     */
	public Stream<T> nodeStream();
	
  /**
   * <p>Returns a collection of all the connected components of this Graph.</p>
   * @param seedSrc a function that removes and returns an element from a set of nodes belonging to 
   * this Graph
   * @return a collection of all the connected components of this Graph
   */
	public Collection<Graph<T>> connectedComponents(Function<Set<T>, T> seedSrc);
	
	/**
   * <p>Returns a collection of all the connected components of this Graph. This is a convenience
   * method supplying default arguments to {@link #connectedComponents(Set, Function)}.</p>
   * @return a collection of all the connected components of this Graph
   */
	public default Collection<Graph<T>> connectedComponents(){
	  return connectedComponents(Graph::stdSeed);
	}
	
	/**
	 * <p>Removes and returns one element of {@code set}.</p>
	 * <p>This is the default seed source used by {@link #connectedComponents()}.</p>
	 * @param set a modifiable set from which elements can be removed and which has at least one 
	 * element
	 * @return the last element that {@code list} had when it was sent to this method
	 * @throws NoSuchElementException if {@code set} is empty
	 * @throws UnsupportedOperationException if {@code set}'s {@link Set#iterator() iterator} does not 
	 * support the optional {@link Iterator#remove() remove()} method
	 */
	static <T> T stdSeed(Set<T> set){
	  Iterator<T> i = set.iterator();
	  T result = i.next();
	  i.remove();
	  return result;
	}
}
