package common.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class ConnectedComponent<T extends Vertex<T>> {
	
	private final int size;
	private Set<T> core;
	private Set<T> edge;
	private Set<T> cuttingEdge;
	
	@SafeVarargs
	ConnectedComponent(int size, Consumer<Set<T>>... contractEvents) {
		this.size = size;
		this.contractEventListeners = new ArrayList<>(contractEvents.length);
		Collections.addAll(contractEventListeners, contractEvents);
		
		this.core = new HashSet<>();
		this.edge = new HashSet<>();
		this.cuttingEdge = new HashSet<>();
	}
	
	ConnectedComponent(int size, Collection<Consumer<Set<T>>> contractEvents) {
		this.size = size;
		this.contractEventListeners = new ArrayList<>(contractEvents);
		
		this.core = new HashSet<>();
		this.edge = new HashSet<>();
		this.cuttingEdge = new HashSet<>();
	}
	
	Set<T> core(){
		return core;
	}
	
	Set<T> edge(){
		return edge;
	}
	
	Set<T> cuttingEdge(){
		return cuttingEdge;
	}
	
	private final List<Consumer<Set<T>>> contractEventListeners;
	
	Set<T> contract(){
		triggerContractEventListeners();
		core.addAll(edge);
		edge = cuttingEdge;
		cuttingEdge = new HashSet<>(size-core.size()-edge.size());
		return core;
	}
	
	private void triggerContractEventListeners(){
		contractEventListeners.parallelStream().forEach( (c)->c.accept(cuttingEdge) );
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
