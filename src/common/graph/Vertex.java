package common.graph;

import java.util.Collection;

/**
 * A vertex in a graph
 * @author fiveham
 *
 */
public interface Vertex<N> {
	
	public Collection<N> neighbors();
}
