package common.graph;

import common.ComboGen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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
	
	private final W wrapped;
	private final List<Wrap<W>> neighbors;
	
	/**
	 * <p>Constructs a Wrap that wraps <tt>wrapped</tt> and has 
	 * no neighbors.</p>
	 * @param wrapped the object being wrapped
	 */
	public Wrap(W wrapped) {
		this.wrapped = wrapped;
		this.neighbors = new ArrayList<>();
	}
	
	/**
	 * <p>Constructs a Wrap that wraps <tt>wrapped</tt> and has 
	 * the neighbors specified as elements of <tt>c</tt>.</p>
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
	 * <p>A utility method that wraps each element of <tt>c</tt> in a Wrap and 
	 * gives each resulting Wrap neighbors from among the resulting Wraps. 
	 * Two resulting Wraps are neighbors if <tt>edgeDetector</tt> 
	 * {@link BiPredicate#test(Object,Object) says so}.</p>
	 * @param c the collection of non-{@link common.graph.Vertex Vertex} nodes 
	 * to be wrapped
	 * @param edgeDetector outputs true if two elements of <tt>c</tt> are linked 
	 * by an edge, false otherwise
	 * @return a list of Wraps complete with populated neighbor lists
	 */
	public static <W> List<Wrap<W>> wrap(Collection<? extends W> c, BiPredicate<? super W, ? super W> edgeDetector){
		List<Wrap<W>> result = c.stream().map((w)->new Wrap<W>(w)).collect(Collectors.toList());
		for(List<Wrap<W>> pair : new ComboGen<>(result, 2,2)){
			Wrap<W> wn1 = pair.get(0);
			Wrap<W> wn2 = pair.get(1);
			if(edgeDetector.test(wn1.wrapped, wn2.wrapped)){
				wn1.neighbors.add(wn2);
				wn2.neighbors.add(wn1);
			}
		}
		return result;
	}
}
