package sudoku;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>A utility class providing certain set-manipulation tools, 
 * in the form of instance methods, that some sets used in the 
 * sudoku target-solving techniques provided in this package.</p>
 * 
 * @author fiveham
 *
 * @param <T>
 */
public class ToolSet<T> extends HashSet<T> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7950470421023736025L;
	
	/**
	 * <p>Constructs an empty ToolSet.</p>
	 */
	public ToolSet() {
	}
	
	/**
	 * <p>Constructs a ToolSet containing all the elements of <tt>c</tt>.</p>
	 * @param c the collection whose elements will be contained by this ToolSet
	 */
	public ToolSet(Collection<T> c) {
		super(c);
	}
	
	/**
	 * <p>Constructs an empty ToolSet whose capacity is initially set to 
	 * <tt>initialCapacity</tt>.</p>
	 * @param initialCapacity the initial capacity of this ToolSet
	 */
	public ToolSet(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * <p>Constructs a ToolSet with the specified initial capacity and 
	 * the specified load factor.</p>
	 * @param initialCapacity the initial capacity of this ToolSet
	 * @param loadFactor the load factor for this ToolSet
	 */
	public ToolSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * <p>Returns true if this set intersects the <tt>otherSet</tt>, 
	 * false otherwise.</p>
	 * @param otherSet another set
	 * @return  true if this set intersects the <tt>otherSet</tt>, 
	 * false otherwise
	 */
	public boolean intersects(Collection<? extends T> otherSet){
		return !Collections.disjoint(this, otherSet);
	}
	
	/**
	 * <p>Returns a list of elements from this set not found in 
	 * <tt>otherCollection</tt>.</p>
	 * @param otherCollection
	 * @return
	 */
	public List<T> complement(Collection<T> otherCollection){
		return stream().filter((e)->!otherCollection.contains(e)).collect(Collectors.toList());
	}
	
	/**
	 * <p>Returns true if <tt>otherSet</tt> is a proper subset of <tt>this</tt>, false otherwise.</p>
	 * @param otherSet another set
	 * @return true if <tt>otherSet</tt> is a proper subset of <tt>this</tt>, false otherwise
	 */
	public boolean hasProperSubset(Set<T> otherSet){
		return size() > otherSet.size() && containsAll(otherSet);
	}
	
	/**
	 * <p>Returns true if <tt>otherSet</tt> is a subset of <tt>this</tt>, false otherwise.</p>
	 * @param otherSet another set
	 * @return true if <tt>otherSet</tt> is a proper subset of <tt>this</tt>, false otherwise
	 */
	public boolean hasSubset(Set<T> otherSet){
		return size() >= otherSet.size() && containsAll(otherSet);
	}
}
