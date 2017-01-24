package common.graph;

import common.ComboGen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
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
public class Wrap<W> implements Vertex<Wrap<W>>{
	
	protected final W wrapped;
	protected final List<Wrap<W>> neighbors;
	
  /**
   * <p>Constructs a Wrap that wraps {@code wrapped} and has no neighbors.</p>
   * @param wrapped the object being wrapped
   */
	private Wrap(W wrapped) {
		this.wrapped = wrapped;
		this.neighbors = new ArrayList<>();
	}
	
	public W wrapped(){
		return wrapped;
	}
	
	@Override
	public List<Wrap<W>> neighbors(){
		return neighbors;
	}
	
	private static final int VERTICES_PER_EDGE = 2;
	
  /**
   * <p>A utility method that wraps each element of {@code unwrapped} and connects the resulting 
   * Wraps according to {@code edgeDetector}.</p>
   * <p>This method connects the nodes in {@code O(n^2)} time where {@code n = unwrapped.size()} by 
   * testing all possible pairs of nodes.</p>
   * <p>The generated edges are bidirectional.</p>
   * @param unwrapped raw nodes
   * @param edgeDetector when testing a pair of raw nodes from {@code unwrapped}, outputs true if 
   * and only if those two nodes share an edge
   * @return a List of the elements of {@code unwrapped} wrapped and connected according to 
   * {@code edgeDetector}
   */
	public static <W> List<Wrap<W>> wrap(
			Collection<W> unwrapped, 
			BiPredicate<? super W, ? super W> edgeDetector){
	  
		List<Wrap<W>> result = unwrapped.stream().map(Wrap<W>::new).collect(Collectors.toList());
		addEdges(result, edgeDetector);
		return result;
	}
	
  /**
   * <p>A utility method that connects the nodes in {@code wrapped} by brute force, trying every
   * pair of them and testing the pair using {@code edgeDetector}.</p>
   * <p>This method operates in {@code O(n^2)} time, where {@code n = wrapped.size()}.</p>
   * @param wrapped the wrapped nodes of a graph
   * @param edgeDetector bidirectionally links two nodes from {@code wrapped} if and only if their
   * raw nodes test true
   */
	private static <W> void addEdges(
			Collection<Wrap<W>> wrapped, 
			BiPredicate<? super W, ? super W> edgeDetector){
		
		for(List<Wrap<W>> pair : new ComboGen<>(wrapped, VERTICES_PER_EDGE, VERTICES_PER_EDGE)){
			Wrap<W> wn1 = pair.get(0);
			Wrap<W> wn2 = pair.get(1);
			if(edgeDetector.test(wn1.wrapped(), wn2.wrapped())){
				wn1.neighbors().add(wn2);
				wn2.neighbors().add(wn1);
			}
		}
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
