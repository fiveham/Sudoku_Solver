package common.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <p>A graph whose connection data is stored in adjacency list form, each {@link Vertex vertex} 
 * keeping track of with which other vertices it shares an edge.</p>
 * @author fiveham
 * @param <T>
 */
public interface Graph<T extends Vertex<T>> extends Iterable<T>{
	
  /**
   * <p>Returns the number of vertices in this Graph.</p>
   * @return the number of vertices in this Graph
   */
	public int size();
	
    /**
     * <p>Returns the number of edges to be traversed to move on the Graph from {@code t1} to
     * {@code t2}, or -1 if there is no path connecting {@code t1} and {@code t2}.</p>
     * @param t1 a vertex in this Graph
     * @param t2 a vertex in this Graph
     * @return the number of edges to be traversed to move on the Graph from {@code t1} to
     * {@code t2}, or -1 if there is no path connecting {@code t1} and {@code t2}
     */
	public int distance(T t1, T t2);
	
  /**
   * <p>Returns a stream based on the underlying collection of nodes.</p>
   * @return a stream backed by this Graph's underlying collection of nodes
   */
	public Stream<T> nodeStream();
	
    /**
     * <p>Adds the specified event-listener-supplier to this Graph then returns this Graph.</p>
     * @param newEL an event-listener-supplier to be added
     * @return this Graph
     */
	public Graph<T> addGrowthListenerFactory(Supplier<Consumer<Set<T>>> newEL);
	
    /**
     * <p>Returns a list of all the registered event-listeners for a contraction event during the
     * construction of connected components.</p>
     * @return a list of all the registered event-listeners for a contraction event during the
     * construction of connected components
     */
	public List<Consumer<Set<T>>> growthListeners();
	
    /**
     * <p>Returns a collection of all the connected components of this Graph.</p>
     * @param eventListeners event-listeners that respond to the contractions that occur in
     * connected components while they build
     * @param seedSrc a function that selects an element from a list of nodes in this Graph from
     * which to begin building a given connected component
     * @return a collection of all the connected components of this Graph
     */
	public Collection<Graph<T>> connectedComponents(List<Consumer<Set<T>>> eventListeners, Function<List<T>,T> seedSrc);
	
    /**
     * <p>Returns a collection of all the connected components of this Graph. This is a convenience
     * method supplying default arguments to
     * {@link #connectedComponents(List<Consumer<Set<T>>>, Function<List<T>,T>) connectedComponents(List<Consumer<Set<T>>>, Function<List<T>,T>)}.
     * The default list of event-listeners supplied is the list returned by
     * {@link contractEventListeners() contractEventListeners()}. The default {@code seedSrc}
     * supplied is a function that removes and returns the last element of the list it is given.</p>
     * @return
     */
	public Collection<Graph<T>> connectedComponents();
	
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
	public Graph<T> component(List<T> unassignedNodes, Function<List<T>,T> seedSrc, List<Consumer<Set<T>>> eventListeners);
	
    /**
     * <p>Returns a list of the vertices to be traversed to travel through this Graph from t1 to t2.
     * The listed vertices are in the proper order. {@code t1} and {@code t2} are the first and last
     * elements of the list, respectively.</p>
     * @param t1
     * @param t2
     * @return
     * @throws IllegalArgumentException if there is no path between {@code t1} and {@code t2} in
     * this Graph or if either {@code t1} or {@code t2} is not part of this Graph
     */
	public List<T> path(T t1, T t2);
}
