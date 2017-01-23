package common.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <p>A base class for implementations of the Graph interface.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T> the type of the vertices of this Graph
 * @param <T> the type of the vertices of this Graph@param <T> the type of the vertices of this
 * @param <T> the type of the vertices of this GraphGraph
 */
public abstract class AbstractGraph<T extends Vertex<T>> implements Graph<T>{
	
    /**
     * <p>The backing collection of vertices in this Graph.</p>
     */
	protected final ArrayList<T> nodes;
	
    /**
     * <p>Constructs an AbstractGraph with an empty list of vertices and an empty list of
     * connected-component contraction event-listeners.</p>
     */
	public AbstractGraph() {
		nodes = new ArrayList<>();
	}
	
    /**
     * <p>Constructs an AbstractGraph whose backing collection of vertices is initialized empty but
     * with a capacity of {@code size}.</p>
     * @param size the capacity which the backing collection of vertices will have
     */
	public AbstractGraph(int size) {
		nodes = new ArrayList<>(size);
	}
	
    /**
     * <p>Constructs an AbstractGraph having the elements of {@code coll} as vertices.</p>
     * @param coll vertices for this Graph
     */
	public AbstractGraph(Collection<? extends T> coll){
		nodes = new ArrayList<>(coll);
	}
	
    /**
     * <p>Constructs an AbstractGraph having the elements of {@code coll} as vertices and having all
     * the elements of {@code factories} as event-listener sources.</p>
     * @param coll vertices for this Graph
     * @param factories connected-component contraction event-listers for this Graph
     */
	public AbstractGraph(Collection<? extends T> coll, List<Supplier<Consumer<Set<T>>>> factories) {
		nodes = new ArrayList<>(coll);
	}
	
	@Override
	public int size(){
		return nodes.size();
	}
	
	@Override
	public Iterator<T> iterator(){
		return nodes.iterator();
	}
	
	@Override
	public Stream<T> nodeStream(){
		return nodes.stream();
	}
	
	@Override
	public Collection<Graph<T>> connectedComponents(Function<List<T>,T> seedSrc){
		
		List<Graph<T>> result = new ArrayList<>();
		
		List<T> unassignedNodes = new ArrayList<>(nodes);
		
		while( !unassignedNodes.isEmpty() ){
			Graph<T> component = component(unassignedNodes, seedSrc);
			result.add(component);
		}
		
		return result;
	}
	
	@Override
	public Graph<T> component(List<T> unassignedNodes, Function<List<T>,T> seedSrc){
		
		ConnectedComponent<T> newComponent = new ConnectedComponent<T>(nodes.size(), unassignedNodes);
		{
			int initUnassignedCount = unassignedNodes.size();
			T seed = seedSrc.apply(unassignedNodes);
			
			if(unassignedNodes.size() == initUnassignedCount){
				unassignedNodes.remove(seed);
			}
			
			newComponent.add(seed); //seed now in cuttingEdge
		}
		
		while( !newComponent.cuttingEdgeIsEmpty() ){
			newComponent.contract();	//move cuttingEdge into edge and old edge into core
			newComponent.grow();		//add unincorporated nodes to cuttingEdge
		}
		
		return new BasicGraph<T>(newComponent.contract());
	}
	
	@Override
	public int hashCode(){
		return nodeStream()
				.map(Object::hashCode)
				.reduce(0, Integer::sum);
	}
	
	@Override
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append(getClass()).append(" size ").append(size()).append(System.lineSeparator()).append(nodes).append(System.lineSeparator());
		
		for(T node : nodes){
			out.append(node).append(": ").append(System.lineSeparator()).append(node.neighbors()).append(System.lineSeparator());
		}
		
		return out.toString();
	}
}
