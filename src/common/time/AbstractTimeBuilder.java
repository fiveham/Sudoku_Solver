package common.time;

/**
 * <p>A base class for the TimeBuilder interface.</p>
 * @author fiveham
 *
 */
public abstract class AbstractTimeBuilder extends AbstractTime implements TimeBuilder{
	
	protected Time top;
	
	/**
	 * <p>Constructs an AbstractTimeBuilder whose {@link Time#parent() parent} 
	 * is <tt>parent</tt>.</p>
	 * @param parent the parent of this Time
	 */
	public AbstractTimeBuilder(Time parent) {
		super(parent);
		this.top = this;
	}
	
	/**
	 * <p>Returns the top of the implicit time-stack, the Time to which new time 
	 * nodes are added as children by calling <tt>push()</tt> with a new time 
	 * node as a parameter.</p>
	 */
	@Override
	public Time top(){
		return top;
	}
	
	/**
	 * <p>Removes the current top element from the implicit stack, making the 
	 * new top element the {@link Time#parent() parent} of the current top 
	 * element.</p>
	 */
	@Override
	public void pop(){
		top = top.parent();
	}
	
	/**
	 * <p>Adds <tt>time</tt> to the implicit time stack, making it the new 
	 * {@link #top() top} time.</p>
	 */
	@Override
	public void push(Time time){
		top.addChild(time);
		top = time;
	}
	
}
