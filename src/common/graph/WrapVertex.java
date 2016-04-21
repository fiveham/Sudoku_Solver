package common.graph;

public interface WrapVertex<W,N> extends Vertex<N> {
	public W wrapped();
}
