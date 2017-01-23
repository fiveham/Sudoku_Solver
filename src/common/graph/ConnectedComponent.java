package common.graph;

import common.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ConnectedComponent<T extends Vertex<T>> {
	
	private final int size;
	private final Set<T> core;
	private Set<T> edge, cuttingEdge;
	
	private final List<T> unassignedNodes;
	
	ConnectedComponent(int size, List<T> unassignedNodes) {
		this.size = size;
		this.unassignedNodes = unassignedNodes;
		
		this.cuttingEdge = new HashSet<>(size);
		this.edge = new HashSet<>(size);
		this.core = new HashSet<>(size);
	}
	
	boolean cuttingEdgeIsEmpty(){
		return cuttingEdge.isEmpty();
	}
	
	void grow(){
		for(T edgeNode : edge){
			addAll(edgeNode.neighbors());
		}
		unassignedNodes.removeAll(cuttingEdge);
	}
	
	Set<T> contract(){
		core.addAll(edge);
		edge = cuttingEdge;
		cuttingEdge = new HashSet<>((size - core.size() - edge.size()) * Sets.JAVA_UTIL_HASHSET_SIZE_FACTOR);
		
		return core;
	}
	
	void add(T vertex){
		if( !core.contains(vertex) && !edge.contains(vertex) ){
			cuttingEdge.add(vertex);
		}
	}
	
	void addAll(Collection<? extends T> vertices){
		for(T vertex : vertices){
			add(vertex);
		}
	}
}
