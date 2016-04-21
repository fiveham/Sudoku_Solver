package common.time;

/**
 * <p>Provides methods for building the time tree structure for which 
 * the Time interface is intended.</p>
 * 
 * <p>The methods this interface supplies for building a time tree 
 * borrow from the concept of the stack. By adding and removing Times 
 * to and from an implicit stack, a tree is built.</p>
 * 
 * <p>A Time is 
 * added to the tree by {@link #push(Time) pushing} it onto the 
 * implicit stack, which adds that Time as a child of the Time node 
 * currently being used as the site for the addition of new Time 
 * nodes and shifts the site for new additions to that newly added 
 * Time node. A Time is removed from the implicit stack by {@link #pop() popping} 
 * it off the stack, which leaves the most recently added Time attached 
 * to the time tree but returns control for the addition of further 
 * Time nodes to the {@link Time#parent() parent} of the Time node 
 * currently at the top of the implicit stack.</p>
 * 
 * <p>The site in the time 
 * tree currently used to add new Times to the implicit stack is the 
 * {@link #top() top} of the implicit stack.</p>
 * @author fiveham
 *
 */
public interface TimeBuilder extends Time{
	
	/**
	 * Removes the current {@link #top top} from the stack used to track 
	 * and build a tree network of Time nodes; the new top is the 
	 * {@link Time#parent() parent} of the current top.
	 */
	public void pop();
	
	/**
	 * Adds <tt>time</tt> as a {@link Time#addChild(Time) child} to the 
	 * Time returned by top().
	 * @param time
	 */
	public void push(Time time);
	
	/**
	 * Returns the Time at the top of the stack used by this TimeBuilder to 
	 * build a tree network of Time nodes.
	 * @return
	 */
	public Time top();
}
