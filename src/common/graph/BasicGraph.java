package common.graph;

import java.util.Collection;

/**
 * <p>A graph.</p>
 * @author fiveham
 * @param <T> the type of the vertices of this Graph
 */
public class BasicGraph<T extends Vertex<T>> extends AbstractGraph<T>{
	
  /**
   * <p>Constructs a BasicGraph with no vertices.</p>
   */
	public BasicGraph() {
		super();
	}
	
  /**
   * <p>Constructs a BasicGraph having the vertices contained in {@code coll}.</p>
   * @param coll vertices for the graph
   */
	public BasicGraph(Collection<? extends T> coll){
		super(coll);
	}
}
