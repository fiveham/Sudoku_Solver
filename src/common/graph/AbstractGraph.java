package common.graph;

import common.Universe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	
	private T stdSeed(List<T> list){
	  return list.remove(list.size() - 1);
	}
	
	@Override
	public Collection<Graph<T>> connectedComponents(){
		return connectedComponents(this::stdSeed);
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
	
	@Override
	public List<T> path(T t1, T t2){
		
		Branch implicitPath = findPath(t2, t1); //reverse args so parent-path goes in order from t1 to t2
		
		List<T> result = new ArrayList<>();
		for(Branch pointer = implicitPath; pointer != null; pointer = pointer.parent){
			result.add(pointer.wrapped);
		}
		
		return result;
	}
	
	/**
	 * <p>Finds a path from {@code from} to {@code to} in this graph.</p>
	 * @param to a node in this graph
	 * @param from a node in this graph
	 * @return a Branch describing the path from {@code from} to {@code to}
	 * @throws IllegalStateException if {@code to} and {@code from} are not both nodes in this graph
	 * @throws IllegalArgumentException if no path is found between {@code to} and {@code from} in 
	 * this graph
	 */
	private Branch findPath(T to, T from){
		if(!(nodes.contains(to) && nodes.contains(from))){
			throw new IllegalStateException(to + " and/or " + from + " not present in this graph");
		}
		
		Branch init = new Branch(to, null);
		if(to.equals(from)){
			return init;
		}
		
    Set<Branch> cuttingEdge = new HashSet<>();
    cuttingEdge.add(init);
		
    Universe<T> nodeUniv = new Universe<>(nodes);
		Set<T> unassigned = nodeUniv.back();
		unassigned.remove(to);
		
    Set<Branch> edge = new HashSet<>();
		while(!cuttingEdge.isEmpty()){
			//contract
			edge = cuttingEdge;
			cuttingEdge = new HashSet<>();
			
			//grow
			for(Branch b : edge){
				Set<T> n = nodeUniv.back(b.wrapped.neighbors());
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
		
		throw new IllegalArgumentException(
		    "Cannot find path between specified nodes: " + from + " and " + to);
	}
	
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
