package common.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
 * @param <T> the type of the vertices of this Graph
 */
public abstract class AbstractGraph<T extends Vertex<T>> implements Graph<T>{
	
  /**
   * <p>The backing collection of vertices in this Graph.</p>
   */
	protected final ArrayList<T> nodes;
	
    /**
     * <p>The list of connected-component contraction event-listeners for this Graph.</p>
     */
	protected final List<Supplier<Consumer<Set<T>>>> contractEventListenerFactories;
	
  /**
   * <p>Constructs an AbstractGraph with no vertices.</p>
   */
	public AbstractGraph() {
		nodes = new ArrayList<>();
		contractEventListenerFactories = new ArrayList<>();
	}
	
  /**
   * <p>Constructs an AbstractGraph whose backing collection of vertices is initialized empty but
   * with a capacity of {@code size}.</p>
   * @param size the capacity which the backing collection of vertices will have
   */
	public AbstractGraph(int size) {
		nodes = new ArrayList<>(size);
		contractEventListenerFactories = new ArrayList<>();
	}
	
  /**
   * <p>Constructs an AbstractGraph having the elements of {@code coll} as vertices.</p>
   * @param coll vertices for this Graph
   */
	public AbstractGraph(Collection<? extends T> coll){
		nodes = new ArrayList<>(coll);
		contractEventListenerFactories = new ArrayList<>();
	}
	
    /**
     * <p>Constructs an AbstractGraph having the elements of {@code coll} as vertices and having all
     * the elements of {@code factories} as event-listener sources.</p>
     * @param coll vertices for this Graph
     * @param factories connected-component contraction event-listers for this Graph
     */
	public AbstractGraph(Collection<? extends T> coll, List<Supplier<Consumer<Set<T>>>> factories) {
		nodes = new ArrayList<>(coll);
		contractEventListenerFactories = new ArrayList<>(factories);
	}
	
    /**
     * <p>Add to {@code contractEventListenerFactories} the specified object that supplies a list of
     * event-listeners for when a ConnectedComponent
     * {@link ConnectedComponent#contract() contracts}.</p>
     * @param newEL an event-listener
     * @return this Graph
     */
	@Override
	public Graph<T> addGrowthListenerFactory(Supplier<Consumer<Set<T>>> newEL){
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
		return connectedComponents(growthListeners(), STD_CONCOM_SEED_SRC);
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
     * <p>Returns a list of objects that will respond to a ConnectedComponent
     * {@link ConnectedComponent#contract() contracting} to move its vertices inward toward its
     * core. The returned list contains all the contents of all the lists produced by the individual
     * event-listener factories in {@code contractEventListenerFactories}.</p>
     * @return
     */
	@Override
	public List<Consumer<Set<T>>> growthListeners(){
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
		
		//adjacency matrix raised to a power (initially the power of 1)
		boolean[][] adjPower = new boolean[nodes.size()][nodes.size()];
		for(int i=0; i<adjPower.length; ++i){
			adjPower[i] = Arrays.copyOf(adjacencyMatrix[i], adjacencyMatrix.length);
		}
		
		for(int n=1; n<nodes.size(); ++n){
			if(adjPower[index1][index2]){
				return n;
			} else{
				adjPower = times(adjPower,adjacencyMatrix);
			}
		}
		return NO_CONNECTION;
	}
	
	public static final int NO_CONNECTION = -1;
	
    /**
     * <p>Matrix-multiplies the specified boolean matrices under the assumption that they are both
     * symmetrical about the main diagonal and that they are both square and both have the same
     * dimensions, all of which will be true for all calls to this method because method is private
     * and is called from only one place, where those assumptions hold true.</p> <p>Since the
     * elements of the matrices involved are boolean values instead of numbers, the operations of
     * (scalar) multiplication and addition used in combining the elements of matrices whose
     * elements are numbers when multiplying those matrices are replaced here with {@code and (&&)}
     * and {@code or (||)}.</p>
     * @param a a square symmetrical matrix of boolean values
     * @param b a square symmetrical matrix of boolean values
     * @return a square symmetrical matrix of boolean values corresponding to the matrix-product of
     * {@code a} and {@code b}
     */
	private boolean[][] times(boolean[][] a, boolean[][] b){
		boolean[][] result = new boolean[a.length][a.length];
		
		for(int i=0; i<nodes.size(); ++i){
			for(int j=0; j<i; ++j){
				result[i][j] = result[j][i] = dot(a[j],b[i]);
			}
			result[i][i] = dot(a[i],b[i]);
		}
		
		return result;
	}
	
    /**
     * <p>Returns the dot product of two boolean vectors. Assumes that {@code a} and {@code b} are
     * the same length. The operations of multiplication and addition used in calculating the dot
     * product of two numerical vectors are replaced with {@code and (&&)} and {@code or (||)}
     * respectively.</p>
     * @param a a vector
     * @param b a vector
     * @return the boolean dot product of two boolean vectors
     */
	private boolean dot(boolean[] a, boolean[] b){
		for(int i=0; i<a.length; ++i){
			if( a[i] && b[i] ){
				return true;
			}
		}
		return false;
	}
	
    /**
     * <p>Determines the distance between vertices {@code t1} and {@code t2} if both are in this
     * graph and in the same connected component by building a connected component around
     * {@code t1}.</p>
     * @param t1 a vertex in this Graph
     * @param t2 a vertex in this Graph
     * @return the distance between {@code t1} and {@code t2} in this Graph, or -1 if there is no
     * path connecting them
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
	
	@Override
	public List<T> path(T t1, T t2){
		
		Branch implicitPath = findPath(t2, t1); //reverse args so parent-path goes in order from t1 to t2
		
		List<T> result = new ArrayList<>();
		for(Branch pointer = implicitPath; pointer != null; pointer = pointer.parent){
			result.add(pointer.wrapped);
		}
		
		return result;
	}
	
	private Branch findPath(T to, T from){
		
		if( !(nodes.contains(to) && nodes.contains(from)) ){
			throw new IllegalStateException(to + " and/or " + from + " not present in this graph");
		}
		
		Set<Branch> cuttingEdge = new HashSet<>();
		
		Branch init = new Branch(to, null);
		if(to.equals(from)){
			return init;
		} else{
			cuttingEdge.add(init);
		}
		
		Set<Branch> edge = new HashSet<>();
		Set<T> unassigned = new HashSet<>(nodes);
		unassigned.remove(to);
		
		while(!cuttingEdge.isEmpty()){
			
			//contract
			edge = cuttingEdge;
			cuttingEdge = new HashSet<>();
			
			//grow
			for(Branch b : edge){
				Set<? extends T> n = new HashSet<>(b.wrapped.neighbors());
				n.retainAll(unassigned);
				
				unassigned.removeAll(n);
				for(T t : n){
					Branch newBranch = new Branch(t, b);
					
					if(t.equals(from)){
						return newBranch;
					} else{
						cuttingEdge.add(newBranch);
					}
				}
			}
		}
		
		throw new IllegalArgumentException("Cannot find path between specified nodes: "+from+" and "+to);
	}
	
	/**
	 * <p>A Branch is a wrapper around a single node of this graph used while finding a path between 
	 * two nodes. A path between two nodes is found by growing all possible paths away from one of the 
	 * nodes until one of the paths, while being grown, finds the other node. In order to efficiently 
	 * find the way back from the destination node in order to return the path, Branches are used, 
	 * each wrapping a node and pointing to the node (via its wrapping Branch) from which the node was 
	 * reached during the search for a path.</p>
	 * @author fiveham
	 */
	private class Branch{
		private final T wrapped;
		private final Branch parent;
		
		private Branch(T wrapped, Branch parent){
			this.wrapped = wrapped;
			this.parent = parent;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof AbstractGraph.Branch){
				AbstractGraph<?>.Branch b = (AbstractGraph<?>.Branch)o; 
				return wrapped.equals(b.wrapped) && (parent == null ? b.parent == null : parent.equals(b.parent));
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return wrapped.hashCode();
		}
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
