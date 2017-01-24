package common.graph;

/**
 * <p>Denotes a Vertex that merely wraps around a non-Vertex object that is the "real" vertex in the
 * graph.</p>
 * @author fiveham
 * @param <W> the type of the wrapped non-Vertex object
 * @param <N> the type of the Vertex neighbors
 */
public interface WrapVertex<W, N> extends Vertex<N> {
	public W wrapped();
}
