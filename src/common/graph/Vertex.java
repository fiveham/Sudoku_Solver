package common.graph;

import java.util.Collection;

/**
 * <p>A vertex in a {@code Graph graph}.</p>
 * @author fiveham
 */
public interface Vertex<N> {
	
  /**
   * <p>Returns the vertices to which this vertex is connected by edges.</p>
   * @return the vertices to which this vertex is connected by edges
   */
	public Collection<N> neighbors();
}
