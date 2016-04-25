package common.time;

import java.util.List;

/**
 * <p>Represents either an atomic event or a non-atomic grouping of 
 * events. This interface is used to express time as a tree whose 
 * leaves can be traversed forward or backward.</p>
 * @author fiveham
 *
 */
public interface Time extends Iterable<Time>{
	
	/**
	 * <p>Returns true if this Time defers to another Time (typically one 
	 * of its children), false otherwise.</p>
	 * @return true if this Time defers to another Time, false otherwise
	 */
	public boolean defers();
	
	/**
	 * <p>Returns the TIme to which this Time {@link #defers() defers}, or 
	 * <tt>this</tt> if this Time does not defer to another Time.</p>
	 * @return the TIme to which this Time {@link #defers() defers}, or 
	 * <tt>this</tt> if this Time does not defer to another Time
	 */
	public Time focus();
	
	/**
	 * <p>Returns a list of the children of this Time.</p>
	 * @return a list of the children of this Time
	 */
	public List<Time> children();
	
	public boolean hasChildren();
	
	/**
	 * <p>Returns this Time's parent.</p>
	 * @return this Time's parent
	 */
	public Time parent();
	
	public boolean hasParent();
	
	/**
	 * <p>Returns the Time to which this Time ultimately defers. That is 
	 * the Time to which a chain of Times point by having the next Time 
	 * in the chain as their focus.</p>
	 */
	public Time currentTime();
	
	/**
	 * <p>Returns a list of Times starting with this Time and ending with 
	 * {@link #currentTime() the current Time} which includes all the 
	 * intermediate parents in between.</p>
	 * @return a list of Times starting with this Time and ending with 
	 * {@link #currentTime() the current Time} which includes all the 
	 * intermediate parents in between
	 */
	public List<Time> currentTrail();
	
	/**
	 * <p>Adds a child Time to this Time's list of {@link #children() children} and 
	 * returns true if that list was changed by the operation, and returns false 
	 * otherwise.</p>
	 * @param child
	 * @return true if this Time's collection of {@link #children() children} was 
	 * changed by calling this method, false otherwise
	 */
	public boolean addChild(Time child);
	
	/**
	 * <p>Updates subordinate Times in order to change the {@link #currentTime() current time} 
	 * to {@link #successor() the next time}, then returns that Time.</p>
	 * @return the current time after {@link #children() subordinate Times} have been updated 
	 * to {@link #focus() point} to the immediately subsequent Time.
	 */
	public Time nextTime();
	
	/**
	 * <p>Returns true if this Time has a next Time after the {@link #currentTime() current Time}, 
	 * false otherwise.</p>
	 * @see #successor()
	 * @return true if this Time has a next time after the current time, false otherwise.
	 */
	public boolean hasNextTime();
	
	/**
	 * <p>Returns true if this Time has a previous Time before the {@link #currentTime() current Time}, 
	 * false otherwise.</p>
	 * @see #predecessor()
	 * @return true if this Time has a previous Time before the current time, false otherwise.
	 */
	public boolean hasPrevTime();
	
	/**
	 * <p>Updates subordinate Times in order to change the {@link #currentTime() current time} 
	 * to {@link #predecessor() the previous time}, then returns that Time.</p>
	 * @return the current time after {@link #children() subordinate Times} have been updated 
	 * to {@link #focus() point} to the immediately prior Time.
	 */
	public Time prevTime();
	
	/**
	 * <p>Moves focus from current child in focus to next child. Behavior is 
	 * undefined if a call to hasNextChild() returns false immediately prior 
	 * to calling this method.</p>
	 * @return the {@link #children() child Time} subsequent to the 
	 * {@link #focus() current child}.
	 */
	public Time nextChild();
	
	/**
	 * <p>Returns true if there is a next child Time available, false otherwise.</p>
	 * @return true if there is a next child Time available, false otherwise.
	 */
	public boolean hasNextChild();
	
	/**
	 * <p>Returns true if there is a previous child Time available, false otherwise.</p>
	 * @return true if there is a previous child Time available, false otherwise.
	 */
	public boolean hasPrevChild();
	
	/**
	 * <p>Moves focus from current child in focus to previous child. Behavior is 
	 * undefined if a call to hasPrevChild() returns false immediately prior 
	 * to calling this method.</p>
	 * @return the {@link #children() child Time} prior to the 
	 * {@link #focus() current child}.
	 */
	public Time prevChild();
	
	/**
	 * <p>If this Time is a leaf node, returns the next leaf node Time after this 
	 * one. If this Time is a branch node, returns the first leaf node of the 
	 * {@link #nextChild() next child} of this Time. Behavior is undefined if 
	 * a call to hasSuccessor() returns false immediately prior to this method 
	 * being called.</p>
	 * @return the next leaf node Time after this one if this Time is a leaf node, or 
	 * the  first leaf node of the {@link #nextChild() next child} of this Time if 
	 * this Time is a branch node.
	 */
	public Time successor();
	
	/**
	 * <p>Returns true if this Time has a {@link #successor() successor}, false 
	 * otherwise.</p>
	 * @return true if this Time has a {@link #successor() successor}, false 
	 * otherwise.
	 */
	public boolean hasSuccessor();
	
	/**
	 * <p>Returns true if this Time has a {@link #predecessor() predecessor}, false 
	 * otherwise.</p>
	 * @return true if this Time has a {@link #predecessor() predecessor}, false 
	 * otherwise.
	 */
	public boolean hasPredecessor();
	
	/**
	 * <p>If this Time is a leaf node, returns the previous leaf node Time after this one. 
	 * If this Time is a branch node, returns the first leaf node of the 
	 * {@link #prevChild() previous child} of this Time. Behavior is undefined if 
	 * a call to hasPredecessor() returns false immediately prior to this method 
	 * being called.</p>
	 * @return the previous leaf node Time before this one if this Time is a leaf node, or 
	 * the  first leaf node of the {@link #prevChild() previous child} of this Time if 
	 * this Time is a branch node.
	 */
	public Time predecessor();
	
	/**
	 * <p>Sets the focus of this Time and all its nth children to the Time's first child Time, 
	 * unless the Time doesn't defer.</p>
	 */
	public void toStart();
	
	/**
	 * <p>Sets the focus of this Time and all its nth children to the Time's last child Time, 
	 * unless the Time doesn't defer.</p>
	 */
	public void toEnd();
}
