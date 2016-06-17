package common.graph;

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
		this.contractEventListeners = new ArrayList<>(contractEvents);
		
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
	
	private final List<Consumer<Set<T>>> contractEventListeners;
	
	Set<T> contract(){
		triggerContractEventListeners();
		
		core.addAll(edge);
		edge = cuttingEdge;
		cuttingEdge = new HashSet<>(size - core.size() - edge.size());
		
		return core;
	}
	
	private void triggerContractEventListeners(){
		contractEventListeners.stream().forEach( (c)->c.accept(cuttingEdge) );
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
