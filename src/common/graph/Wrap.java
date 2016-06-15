package common.graph;

import common.ComboGen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import sudoku.technique.Sledgehammer;

/**
 * <p>This class wraps a non-Vertex object so that a network of those non-Vertex 
 * objects can be constructed and managed.</p>
 * 
 * <p>This is a utility class created to mitigate the combinatorial explosion that 
 * could result from accounting for the need to identify different types 
 * of nodes in a bipartite graph contract. By wrapping non-{@link common.graph.Vertex Vertex} nodes within 
 * the Graph itself (a WrappingGraph), adding the concept of a bipartite graph 
 * to the package would require adding an ordinary BipartiteGraph as well as a 
 * BipartiteWrappingGraph to manage bipartite graphs with nodes that do not 
 * implement Vertex.</p>
 * 
 * @author fiveham
 *
 * @param <W>
 * @param <N>
 */
public class Wrap<W> implements WrapVertex<W,Wrap<W>>{
	
	protected final W wrapped;
	protected final List<Wrap<W>> neighbors;
	
	/**
	 * <p>Constructs a Wrap that wraps {@code wrapped} and has 
	 * no neighbors.</p>
	 * @param wrapped the object being wrapped
	 */
	public Wrap(W wrapped) {
		this.wrapped = wrapped;
		this.neighbors = new ArrayList<>();
	}
	
	/**
	 * <p>Constructs a Wrap that wraps {@code wrapped} and has 
	 * the neighbors specified as elements of {@code c}.</p>
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
	 * <p>A utility method that wraps each element of {@code unwrapped} in a Wrap and 
	 * gives each resulting Wrap neighbors from among the resulting Wraps. 
	 * Two resulting Wraps are neighbors if {@code edgeDetector} 
	 * {@link BiPredicate#test(Object,Object) says so}.</p>
	 * @param unwrapped the collection of non-{@link common.graph.Vertex Vertex} nodes 
	 * to be wrapped
	 * @param edgeDetector outputs true if two elements of {@code unwrapped} are linked 
	 * by an edge, false otherwise
	 * @return a list of Wraps complete with populated neighbor lists
	 */
	public static <W> List<Wrap<W>> wrap(
			Collection<W> unwrapped, 
			BiPredicate<? super W, ? super W> edgeDetector){
		return wrap(unwrapped, edgeDetector, Wrap<W>::new);
	}
	
	public static final int VERTICES_PER_EDGE = 2;
	
	/**
	 * 
	 * @param unwrapped a collection of objects to be wrapped and connected
	 * @param edgeDetector a BiPredicate that tests any and every pair 
	 * of elements of {@code unwrapped} (after they've been wrapped by {@code wrapper}) 
	 * to see whether they're adjacent, and if so, they are connected
	 * @param wrapper a Function that accepts an element of {@code unwrapped} 
	 * and wraps it with some type of WrapVertex before outputting it
	 * @return a List of the elements of {@code unwrapped} wrapped via {@code wrapper} 
	 * and connected according to {@code edgeDetector}
	 */
	public static <T extends WrapVertex<W,T>, W> List<T> wrap(
			Collection<W> unwrapped, 
			BiPredicate<? super W, ? super W> edgeDetector, 
			Function<? super W,T> wrapper){
		List<T> result = unwrapped.stream().map(wrapper).collect(Collectors.toList());
		addEdges(result, edgeDetector);
		return result;
	}
	
	public static <T extends WrapVertex<W,T>, W> void addEdges(
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
	
	public static void addNodes(){
		
	}
	
	public static <T extends WrapVertex<W,T>, E extends Collection<W>, W> List<T> wrap(
			Collection<E> edges, 
			Function<? super W, T> wrapper){
		List<T> wrappedNodes = Sledgehammer.massUnion(edges).stream()
				.map(wrapper)
				.collect(Collectors.toList());
		addNodes(edges, wrappedNodes);
		return wrappedNodes;
	}
	
	public static <T extends WrapVertex<W,T>, E extends Collection<W>, W> void addNodes(
			Collection<E> edges, 
			List<T> wrappedNodes){
		Map<W,T> map = wrappedNodes.stream()
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
	
	public static <T extends WrapVertex<W,T>, E, W> List<T> wrap(
			Collection<E> edges, 
			Function<? super E, ? extends Collection<W>> nodeSource, 
			Function<? super W, T> wrapper){
		return wrap(edges.stream().map(nodeSource).collect(Collectors.toList()), wrapper);
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
