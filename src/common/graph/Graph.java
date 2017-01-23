package common.graph;

import java.util.Collection;
import java.util.List;
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
     * @param eventListeners event-listeners that respond to the contractions that occur in
     * connected components while they build
     * @param seedSrc a function that selects an element from a list of nodes in this Graph from
     * which to begin building a given connected component
     * @return a collection of all the connected components of this Graph
     */
	public Collection<Graph<T>> connectedComponents(Function<List<T>,T> seedSrc);
	
	/**
   * <p>Returns a collection of all the connected components of this Graph. This is a convenience
   * method supplying default arguments to {@link #connectedComponents(List, Function)}. 
   * The default {@code seedSrc} supplied is a function that removes and returns the last element of
   * the list it is given.</p>
   * @return a collection of all the connected components of this Graph
   */
	public default Collection<Graph<T>> connectedComponents(){
	  return connectedComponents(Graph::stdSeed);
	}
	
	/**
	 * <p>Removes and returns the last element of {@code list}.</p>
	 * @param list a modifiable list from which elements can be removed and which has at least one 
	 * element
	 * @return the last element that {@code list} had when it was sent to this method
	 * @throws IndexOutOfBoundsException if {@code list} is empty
	 */
	static <T> T stdSeed(List<T> list){
	  return list.remove(list.size() - 1);
	}
	
    /**
     * <p>Returns the connected component of this Graph that contains the {@code T} output by
     * {@code seedSrc} when {@code unassignedNodes} is given to it as input.</p>
     * @param unassignedNodes nodes from this Puzzle for which a connected component will be built
     * @param seedSrc a function that specifies an element from {@code unassignedNodes} with which
     * to begin building the connected component that is returned
     * @param eventListeners a list of event-listeners that respond when the connected component
     * being built moves newly-added nodes inward and out of the outermost layer
     * @return the connected component of this Grapht hat contains the {@code T} output by
     * {@code seedSrc} when {@code unassignedNodes} is given to it as input
     */
	public Graph<T> component(List<T> unassignedNodes, Function<List<T>,T> seedSrc);
}
