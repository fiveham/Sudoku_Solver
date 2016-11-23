package common.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>A graph.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T> the type of the vertices of this Graph
 * @param <T> the type of the vertices of this Graph@param <T> the type of the vertices of this
 * @param <T> the type of the vertices of this GraphGraph
 */
public class BasicGraph<T extends Vertex<T>> extends AbstractGraph<T>{
	
    /**
     * <p>Constructs a BasicGraph with no vertices and no contraction-event-listener sources.</p>
     */
	public BasicGraph() {
		super();
	}
	
    /**
     * <p>Constructs a BasicGraph having the vertices contained in {@code coll}.</p>
     * @param coll vertices for the graph being constructed
     */
	public BasicGraph(Collection<? extends T> coll){
		super(coll);
	}
	
    /**
     * <p>Constructs a BasicGraph having the specified vertices and contraction-event-listener
     * sources</p>
     * @param coll
     * @param factories
     */
	public BasicGraph(Collection<? extends T> coll, List<Supplier<Consumer<Set<T>>>> factories) {
		super(coll, factories);
	}
}
