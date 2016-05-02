package common.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import java.util.*;

class ConnectedComponent<T extends Vertex<T>> {
	
	private final Map<Partition,Set<T>> map;
	
	@SafeVarargs
	ConnectedComponent(int size, Consumer<Set<T>>... contractEvents) {
		this.contractEventListeners = new ArrayList<>(contractEvents.length);
		Collections.addAll(contractEventListeners, contractEvents);
		
		this.map = new HashMap<>(Partition.values().length);
		map.put(Partition.CORE, new HashSet<>(size));
		map.put(Partition.EDGE, new HashSet<>(size));
		map.put(Partition.CUTTINGEDGE, new HashSet<>(size));
	}
	
	ConnectedComponent(int size, Collection<Consumer<Set<T>>> contractEvents) {
		this.contractEventListeners = new ArrayList<>(contractEvents);
		
		this.map = new HashMap<>(Partition.values().length);
		map.put(Partition.CORE, new HashSet<>(size));
		map.put(Partition.EDGE, new HashSet<>(size));
		map.put(Partition.CUTTINGEDGE, new HashSet<>(size));
	}
	
	Set<T> core(){
		return map.get(Partition.CORE);
	}
	
	Set<T> edge(){
		return map.get(Partition.EDGE);
	}
	
	Set<T> cuttingEdge(){
		return map.get(Partition.CUTTINGEDGE);
	}
	
	private final List<Consumer<Set<T>>> contractEventListeners;
	
	Set<T> contract(){
		triggerContractEventListeners();
		
		core().addAll(edge());
		edge().clear();
		
		edge().addAll(cuttingEdge());
		cuttingEdge().clear();
		
		return core();
	}
	
	private void triggerContractEventListeners(){
		contractEventListeners.parallelStream().forEach( (c)->c.accept(cuttingEdge()) );
	}
	
	void add(T vertex){
		if( !core().contains(vertex) && !edge().contains(vertex) ){
			cuttingEdge().add(vertex);
		}
	}
	
	void addAll(Collection<? extends T> vertices){
		for(T vertex : vertices){
			add(vertex);
		}
	}
	
	private enum Partition{
		CORE, EDGE, CUTTINGEDGE, UNACCOUNTED;
	}
}
