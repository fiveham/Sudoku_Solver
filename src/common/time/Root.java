package common.time;

/**
 * <p>A Time which has no parent (<tt>parent() == null</tt>) and serves 
 * as the root of the tree structure for which the Time interface is 
 * intended.</p>
 * @author fiveham
 *
 */
public final class Root extends AbstractTimeBuilder{
	public Root() {
		super(null);
	}
}
