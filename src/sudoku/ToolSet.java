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
	 * <p>Constructs a ToolSet containing all the elements of {@code c}.</p>
	 * @param c the collection whose elements will be contained by this ToolSet
	 */
	public ToolSet(Collection<? extends T> c) {
		super(c);
	}
	
	/**
	 * <p>Constructs an empty ToolSet whose capacity is initially set to 
	 * {@code initialCapacity}.</p>
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
	 * <p>Returns true if this set intersects the {@code otherSet}, 
	 * false otherwise.</p>
	 * @param otherSet another set
	 * @return  true if this set intersects the {@code otherSet}, 
	 * false otherwise
	 */
	public boolean intersects(Collection<? extends T> otherSet){
		return !Collections.disjoint(this, otherSet);
	}
	
	public ToolSet<T> intersection(Collection<? extends T> otherSet){
		ToolSet<T> result = new ToolSet<>(this);
		result.retainAll(otherSet);
		return result;
	}
	
	/**
	 * <p>Returns a list of elements from this set not found in 
	 * {@code otherCollection}.</p>
	 * @param otherCollection
	 * @return
	 */
	public List<T> complement(Collection<T> otherCollection){
		return stream().filter((e)->!otherCollection.contains(e)).collect(Collectors.toList());
	}
	
	/**
	 * <p>Returns true if {@code otherSet} is a proper subset of {@code this}, false otherwise.</p>
	 * @param otherSet another set
	 * @return true if {@code otherSet} is a proper subset of {@code this}, false otherwise
	 */
	public boolean hasProperSubset(Set<T> otherSet){
		return size() > otherSet.size() && containsAll(otherSet);
	}
	
	/**
	 * <p>Returns true if {@code otherSet} is a subset of {@code this}, false otherwise.</p>
	 * @param otherSet another set
	 * @return true if {@code otherSet} is a proper subset of {@code this}, false otherwise
	 */
	public boolean hasSubset(Set<T> otherSet){
		return size() >= otherSet.size() && containsAll(otherSet);
	}
}
