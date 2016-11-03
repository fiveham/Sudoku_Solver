package common.graph;

import common.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class ConnectedComponent<T extends Vertex<T>> {
	
	private final int size;
	private final Set<T> core;
	private Set<T> edge, cuttingEdge;
	
	private final List<T> unassignedNodes;
	
	@SafeVarargs
	ConnectedComponent(int size, List<T> unassignedNodes, Consumer<Set<T>>... contractEvents) {
		this(size, unassignedNodes, Arrays.asList(contractEvents));
	}
	
	ConnectedComponent(int size, List<T> unassignedNodes, Collection<Consumer<Set<T>>> contractEvents) {
		this.size = size;
		this.growthListeners = new ArrayList<>(contractEvents);
		
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
	
	private final List<Consumer<Set<T>>> growthListeners;
	
	Set<T> contract(){
		triggerGrowthListeners();
		
		core.addAll(edge);
		edge = cuttingEdge;
		cuttingEdge = new HashSet<>((size - core.size() - edge.size()) * Sets.JAVA_UTIL_HASHSET_SIZE_FACTOR);
		
		return core;
	}
	
	private void triggerGrowthListeners(){
		growthListeners.stream().forEach( (c)->c.accept(cuttingEdge) );
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
