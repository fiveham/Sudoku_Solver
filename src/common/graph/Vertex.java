package common.graph;

import java.util.Collection;

/**
 * A vertex in a graph
 * @author fiveham
 *
 */
public interface Vertex<N> {
	
	public Collection<? extends N> neighbors();
	/*
	public <N extends Vertex> boolean addNeighbor(N newNeighbor);
	
	public <N extends Vertex> boolean removeNeighbor(N oldNeighbor);*/
}
