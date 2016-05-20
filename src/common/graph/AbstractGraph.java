package common.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
 *
 * @param <T> the type of the vertices of this Graph
 */
public abstract class AbstractGraph<T extends Vertex<T>> implements Graph<T>{
	
	/**
	 * <p>The backing collection of vertices in this Graph.</p>
	 */
	protected final ArrayList<T> nodes;
	
	/**
	 * <p>The list of connected-component contraction event-listeners for 
	 * this Graph.</p>
	 */
	protected final List<Supplier<Consumer<Set<T>>>> contractEventListenerFactories;
	
	/**
	 * <p>Constructs an AbstractGraph with an empty list of vertices 
	 * and an empty list of connected-component contraction event-listeners.</p>
	 */
	public AbstractGraph() {
		nodes = new ArrayList<>();
		contractEventListenerFactories = new ArrayList<>();
	}
	
	/**
	 * <p>Constructs an AbstractGraph whose backing collection of 
	 * vertices is initialized empty but with a capacity of {@code size}.</p>
	 * @param size the capacity which the backing collection of 
	 * vertices will have
	 */
	public AbstractGraph(int size) {
		nodes = new ArrayList<>(size);
		contractEventListenerFactories = new ArrayList<>();
	}
	
	/**
	 * <p>Constructs an AbstractGraph having the elements of {@code coll} 
	 * as vertices.</p>
	 * @param coll vertices for this Graph
	 */
	public AbstractGraph(Collection<? extends T> coll){
		nodes = new ArrayList<>(coll);
		contractEventListenerFactories = new ArrayList<>();
	}
	
	/**
	 * <p>Constructs an AbstractGraph having the elements of {@code coll} 
	 * as vertices and having all the elements of {@code factories} as 
	 * event-listener sources.</p>
	 * @param coll vertices for this Graph
	 * @param factories connected-component contraction event-listers for 
	 * this Graph
	 */
	public AbstractGraph(Collection<? extends T> coll, List<Supplier<Consumer<Set<T>>>> factories) {
		nodes = new ArrayList<>(coll);
		contractEventListenerFactories = new ArrayList<>(factories);
	}
	
	/**
	 * <p>Add to {@code contractEventListenerFactories} the specified object that supplies a 
	 * list of event-listeners for when a ConnectedComponent {@link ConnectedComponent#contract() contracts}.</p>
	 * @param newEL an event-listener
	 * @return this Graph
	 */
	@Override
	public Graph<T> addContractEventListenerFactory(Supplier<Consumer<Set<T>>> newEL){
		contractEventListenerFactories.add(newEL);
		return this;
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
	
	private final Function<List<T>,T> STD_CONCOM_SEED_SRC = (unassigned)->unassigned.remove(unassigned.size()-1);
	
	@Override
	public Collection<Graph<T>> connectedComponents(){
		return connectedComponents(contractEventListeners(), STD_CONCOM_SEED_SRC);
	}
	
	@Override
	public Collection<Graph<T>> connectedComponents(List<Consumer<Set<T>>> contractEventListenerSrc, 
			Function<List<T>,T> seedSrc){
		
		List<Graph<T>> result = new ArrayList<>();
		
		List<T> unassignedNodes = new ArrayList<>(nodes);
		
		while( !unassignedNodes.isEmpty() ){
			Graph<T> component = component(unassignedNodes, seedSrc, contractEventListenerSrc);
			result.add(component);
		}
		
		return result;
	}
	
	@Override
	public Graph<T> component(List<T> unassignedNodes, Function<List<T>,T> seedSrc, List<Consumer<Set<T>>> contractEventListeners){
		
		ConnectedComponent<T> newComponent = new ConnectedComponent<T>(nodes.size(), unassignedNodes, contractEventListeners);
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
	
	/**
	 * <p>Returns a list of objects that will respond to a ConnectedComponent {@link ConnectedComponent#contract() contracting} 
	 * to move its vertices inward toward its core.  The returned list contains all the contents of all the 
	 * lists produced by the individual event-listener factories in {@code contractEventListenerFactories}.</p>
	 * @return
	 */
	@Override
	public List<Consumer<Set<T>>> contractEventListeners(){
		List<Consumer<Set<T>>> result = new ArrayList<>();
		
		for(Supplier<Consumer<Set<T>>> contractEventListenerFactory : contractEventListenerFactories){
			result.add(contractEventListenerFactory.get());
		}
		
		return result;
	}
	
	@Override
	public int distance(T v1, T v2){
		int index1 = nodes.indexOf(v1);
		int index2 = nodes.indexOf(v2);
		if( index1<0 || index2<0 ){
			throw new IllegalArgumentException("At least one of the specified nodes is not in this graph.");
		}
		
		if(index1==index2){
			return 0;
		}
		
		final boolean[][] adjacencyMatrix = new boolean[nodes.size()][nodes.size()];
		for(int i=0; i<nodes.size(); ++i){
			for(int j=0; j<i; ++j){
				adjacencyMatrix[i][j] = adjacencyMatrix[j][i] = nodes.get(i).neighbors().contains(nodes.get(j));
			}
			adjacencyMatrix[i][i] = false;
		}
		
		boolean[][] adjPower = new boolean[nodes.size()][nodes.size()];
		Arrays.fill(adjPower[0], false);
		Arrays.fill(adjPower, adjPower[0]); //TODO make sure this doesn't make changes to adjPower[0] write through to all the corresponding indices of adjPower
		for(int i=0; i<adjPower.length; ++i){
			adjPower[i][i] = true;
		}
		
		for(int n=0; n<nodes.size(); ++n){
			if(adjPower[index1][index2]){
				return n;
			} else{
				adjPower = times(adjPower,adjacencyMatrix);
			}
		}
		return -1;
	}
	
	/**
	 * <p>Matrix-multiplies the specified boolean matrices under the assumption 
	 * that they are both symmetrical about the main diagonal and that they are 
	 * both square and both have the same dimensions, all of which will be true 
	 * for all calls to this method because method is private and is called from 
	 * only one place, where those assumptions holds true.</p>
	 * 
	 * <p>Since the elements of the matrices involved are boolean values instead 
	 * of numbers, the operations of (scalar) multiplication and addition used 
	 * in combining the elements of matrices whose elements are numbers when 
	 * multiplying those matrices are replaced here with {@code and} (&&) and 
	 * {@code or} (||).</p>
	 * 
	 * @param a a square symmetrical matrix of boolean values
	 * @param b a square symmetrical matrix of boolean values
	 * @return a square symmetrical matrix of boolean values corresponding to 
	 * the matrix-product of {@code a} and {@code b}
	 */
	private boolean[][] times(boolean[][] a, boolean[][] b){
		boolean[][] result = new boolean[a.length][a.length];
		
		for(int i=0; i<nodes.size(); ++i){
			for(int j=0; j<=i; ++j){
				result[i][j] = result[j][i] = dot(a[j],b[i]);
			}
		}
		
		return result;
	}
	
	/**
	 * <p>Returns the dot product of two vectors. Implicitly assumes 
	 * that {@code a} and {@code b} are the same length. The 
	 * operations of multiplication and addition used in calculating 
	 * the dot product of two vectors of numbers are replaced with 
	 * {@code and} (&&) and {@code or} (||).</p>
	 * @param a a vector
	 * @param b a vector
	 * @return the boolean dot product of two boolean vectors
	 */
	private boolean dot(boolean[] a, boolean[] b){
		for(int i=0; i<a.length; ++i){
			if( a[i]&&b[i] ){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <p>Determines the distance between vertices {@code t1} and {@code t2} 
	 * if both are in this graph and in the same connected component by building 
	 * a connected component around {@code t1}.</p>
	 * @param t1 a vertex in this Graph
	 * @param t2 a vertex in this Graph
	 * @return the distance between {@code t1} and {@code t2} in this Graph, 
	 * or -1 if there is no path connecting them
	 */
	public int distance2(final T t1, final T t2){
		if( !nodes.contains(t1) || !nodes.contains(t2) ){
			throw new IllegalArgumentException("At least one of the specified Nodes is not in this graph.");
		}
		
		if(t1==t2){
			return 0;
		}
		
		class DistFinder implements Consumer<Set<T>>{
			int dist = 0;
			boolean foundTarget = false;
			
			@Override
			public void accept(Set<T> cuttingEdge){
				if(!foundTarget){
					if(cuttingEdge.contains(t2)){
						foundTarget = true;
					} else{
						++dist;
					}
				}
			}
		};
		DistFinder distFinder = new DistFinder();
		
		component(new ArrayList<>(nodes), (list)->t1, Collections.singletonList(distFinder));
		
		return distFinder.dist;
	}
}
