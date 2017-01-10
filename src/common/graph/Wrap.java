package common.graph;

import common.ComboGen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * <p>This class wraps a non-Vertex object so that a network of those non-Vertex objects can be
 * constructed and managed.</p> <p>This is a utility class created to mitigate the combinatorial
 * explosion that could result from accounting for the need to identify different types of nodes in
 * a bipartite graph contract. By wrapping non-{@link common.graph.Vertex Vertex} nodes within the
 * Graph itself (a WrappingGraph), adding the concept of a bipartite graph to the package would
 * require adding an ordinary BipartiteGraph as well as a BipartiteWrappingGraph to manage bipartite
 * graphs with nodes that do not implement Vertex.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <W>
 * @param <W>@param <W>
 * @param <W>@param <N>
 */
public class Wrap<W> implements WrapVertex<W,Wrap<W>>{
	
	protected final W wrapped;
	protected final List<Wrap<W>> neighbors;
	
  /**
   * <p>Constructs a Wrap that wraps {@code wrapped} and has no neighbors.</p>
   * @param wrapped the object being wrapped
   */
	public Wrap(W wrapped) {
		this.wrapped = wrapped;
		this.neighbors = new ArrayList<>();
	}
	
  /**
   * <p>Constructs a Wrap that wraps {@code wrapped} and has the neighbors specified as elements
   * of {@code c}.</p>
   * @param wrapped the object being wrapped
   * @param c a collection of this Wrap's neighbors
   */
	public Wrap(W wrapped, Collection<? extends Wrap<W>> c) {
		this.wrapped = wrapped;
		this.neighbors = new ArrayList<>(c);
	}
	
	@Override
	public W wrapped(){
		return wrapped;
	}
	
	@Override
	public List<Wrap<W>> neighbors(){
		return neighbors;
	}
	
  /**
   * <p>A utility method that wraps each element of {@code unwrapped} in a Wrap and gives each
   * resulting Wrap neighbors from among the resulting Wraps. Two resulting Wraps are neighbors if
   * {@code edgeDetector} {@link BiPredicate#test(Object,Object) says so}.</p> <p>This method
   * connects the nodes in {@code O(n^2)} time where {@code n = unwrapped.size()} by testing all
   * possible pairs of nodes.</p> <p>The generated edges are bidirectional.</p>
   * @param unwrapped raw nodes
   * @param edgeDetector outputs true if two raw nodes from {@code unwrapped} share an edge, false
   * otherwise
   * @return a list of Wraps complete with populated neighbor lists
   */
	public static <W> List<Wrap<W>> wrap(
			Collection<W> unwrapped, 
			BiPredicate<? super W, ? super W> edgeDetector){
		return wrap(unwrapped, edgeDetector, Wrap<W>::new);
	}
	
	public static final int VERTICES_PER_EDGE = 2;
	
  /**
   * <p>A utility method that wraps each element of {@code unwrapped} in a {@code T} specified by
   * {@code wrapper} and gives each resulting {@code T} neighbors from among the resulting
   * {@code T}s. Two resulting {@code T}s are neighbors if {@code edgeDetector}
   * {@link BiPredicate#test(Object,Object) says so}.</p> <p>This method connects the nodes in
   * {@code O(n^2)} time where {@code n = unwrapped.size()} by testing all possible pairs of
   * nodes.</p> <p>The generated edges are bidirectional.</p>
   * @param unwrapped raw nodes
   * @param edgeDetector outputs true if and only if two raw nodes from {@code unwrapped} share an
   * edge, false otherwise
   * @param wrapper wraps an element of {@code unwrapped} with some type of WrapVertex
   * @return a List of the elements of {@code unwrapped} wrapped via {@code wrapper} and connected
   * according to {@code edgeDetector}
   */
	public static <T extends WrapVertex<W, T>, W> List<T> wrap(
			Collection<W> unwrapped, 
			BiPredicate<? super W, ? super W> edgeDetector, 
			Function<? super W, T> wrapper){
		List<T> result = unwrapped.stream().map(wrapper).collect(Collectors.toList());
		addEdges(result, edgeDetector);
		return result;
	}
	
  /**
   * <p>A utility method that connects the nodes in {@code wrapped} by brute force, trying every
   * pair of them and testing the pair using {@code edgeDetector}.</p> <p>This method operates in
   * {@code O(n^2)} time, where {@code n = wrapped.size()}.</p>
   * @param wrapped the nodes of a graph, wrapped in some type of WrapVertex
   * @param edgeDetector bidirectionally links two nodes from {@code wrapped} if and only if their
   * raw nodes test true
   */
	public static <T extends WrapVertex<W, T>, W> void addEdges(
			Collection<T> wrapped, 
			BiPredicate<? super W, ? super W> edgeDetector){
		
		for(List<T> pair : new ComboGen<>(wrapped, VERTICES_PER_EDGE, VERTICES_PER_EDGE)){
			T wn1 = pair.get(0);
			T wn2 = pair.get(1);
			if(edgeDetector.test(wn1.wrapped(), wn2.wrapped())){
				wn1.neighbors().add(wn2);
				wn2.neighbors().add(wn1);
			}
		}
	}
	
  /**
   * <p>A utility method that extracts a graph's nodes from its {@code edges} and wraps those
   * nodes in {@code Wrap}s.</p>
   * @param edges a {@link Collection} of edges of the graph of whose interconnected nodes a list
   * is returned
   * @return a list of the nodes of the graph whose edges are specified in {@code edges}
   * @throws IllegalArgumentException if any of the {@code edges} has a size other than
   * {@value #VERTICES_PER_EDGE}
   */
	public static <E extends Collection<W>, W> List<Wrap<W>> wrap(Collection<E> edges){
		return wrap(edges, Wrap<W>::new);
	}
	
  /**
   * <p>A utility method that extracts a graph's nodes from its {@code edges} and wraps those
   * nodes with {@code WrapVertex}s via {@code wrapper}.</p>
   * @param edges a {@link Collection} of edges of the graph of whose interconnected nodes a list
   * is returned
   * @param wrapper specifies the type of {@code WrapVertex} that wraps raw node elements of edges
   * from {@code edges}
   * @return a list of the nodes of the graph whose edges are specified in {@code edges}
   * @throws IllegalArgumentException if any of the specified {@code edges} has a size not equal
   * to {@value #VERTICES_PER_EDGE}
   */
	public static <T extends WrapVertex<W, T>, E extends Collection<W>, W> List<T> wrap(
			Collection<E> edges, 
			Function<? super W, T> wrapper){
	  
		List<T> wrappedNodes = edges.stream()
		    .collect(Collector.of(
          HashSet<W>::new, 
          Set::addAll, 
          (set1, set2) -> {
            set1.addAll(set2);
            return set1;
          }))
		    .stream()
				.map(wrapper)
				.collect(Collectors.toList());
		addNodes(edges, wrappedNodes);
		return wrappedNodes;
	}
	
  /**
   * <p>A utility method that adds elements of {@code wrappedNodes} as
   * {@link Vertex#neighbors() neighbors} of each other such that all the edges specified by
   * {@code edges} are accounted for.</p> <p>This method operates in {@code O(n)} time where
   * {@code n = edges.size()}.</p>
   * @param edges the edges of the graph whose nodes are contained in {@code wrappedNodes}
   * @param wrappedNodes the wrapped nodes of the graph whose edges are contained in {@code edges}
   * @throws IllegalArgumentException if any of the {@code edges} has a size not equal to
   * {@value #VERTICES_PER_EDGE}
   */
	public static <T extends WrapVertex<W, T>, E extends Collection<W>, W> void addNodes(
			Collection<E> edges, 
			List<T> wrappedNodes){
		Map<W, T> map = wrappedNodes.stream()
				.collect(Collectors.toMap(
						WrapVertex::wrapped, 
						Function.identity()));
		edges.stream()
				.forEach((edge) -> {
					if(edge.size() != VERTICES_PER_EDGE){
						throw new IllegalArgumentException("Edge not having exactly 2 nodes: "+edge);
					}
					Iterator<W> iter = edge.iterator();
					T t0 = map.get(iter.next());
					T t1 = map.get(iter.next());
					t0.neighbors().add(t1);
					t1.neighbors().add(t0);
				});
	}
	
  /**
   * <p>A utility method that wraps the raw nodes of a graph whose edges are explicitly specified
   * as {@code edges} and whose nodes are implicitly specified via the application of
   * {@code nodeSource} to the elements of {@code edges}. The extracted raw nodes are wrapped via
   * {@code wrapper}.</p>
   * @param edges the edges of the graph in a form that {@code nodeSource} can translate into a
   * size-{@value #VERTICES_PER_EDGE} Collection of raw nodes
   * @param nodeSource translates a raw edge to a size-{@value #VERTICES_PER_EDGE} Collection of
   * raw nodes
   * @param wrapper wraps the raw nodes that {@code nodeSource} extracts from the elements of
   * {@code edges}
   * @return a List of wrapped, connected nodes of the graph whose {@code edges} were provided
   * @throws IllegalArgumentException if any of the Collections produced by an application of
   * {@code nodeSource} has a size other than {@value #VERTICES_PER_EDGE}
   */
	public static <T extends WrapVertex<W, T>, E, W> List<T> wrap(
			Collection<E> edges, 
			Function<? super E, ? extends Collection<W>> nodeSource, 
			Function<? super W, T> wrapper){
		return wrap(edges.stream().map(nodeSource).collect(Collectors.toList()), wrapper);
	}
	
  /**
   * <p>Returns a Map from {@code graph}'s {@link WrapVertex#wrapped() raw nodes} to its wrapped
   * nodes.</p>
   * @param graph a graph of wrapped nodes
   * @return a Map from {@code graph}'s {@link WrapVertex#wrapped() raw nodes} to its wrapped
   * nodes
   */
	public static <T extends WrapVertex<W, T>, W> Map<W, T> rawToWrap(Graph<T> graph){
		return graph.nodeStream().collect(Collectors.toMap(WrapVertex::wrapped, Function.identity()));
	}
	
  /**
   * <p>Returns a Map from {@code wrappedNodes}'s elements' underlying raw nodes to those same
   * elements of {@code wrappedNodes}.</p>
   * @param wrappedNodes a collection of wrapped nodes
   * @return a Map from {@code wrappedNodes}'s elements' underlying raw nodes to those same
   * elements of {@code wrappedNodes}
   */
	public static <T extends WrapVertex<W, T>, W> Map<W, T> rawToWrap(Collection<T> wrappedNodes){
		return wrappedNodes.stream().collect(Collectors.toMap(WrapVertex::wrapped, Function.identity()));
	}
	
	@Override
	public String toString(){
		return "Wrap of " + wrapped;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Wrap){
			Wrap<?> w = (Wrap<?>) o;
			return wrapped.equals(w.wrapped);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return wrapped.hashCode();
	}
}
