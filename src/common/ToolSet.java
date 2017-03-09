package common;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>A utility class providing certain set-manipulation tools, in the form of instance methods,
 * that some sets used in the sudoku target-solving techniques provided in this package.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T>
 * @param <T>@param <T>
 */
public class ToolSet<T> extends HashSet<T> {
	
	private static final long serialVersionUID = 7562530465837262993L;
	
  /**
   * <p>Constructs an empty ToolSet.</p>
   */
	protected ToolSet() {
	}
	
  /**
   * <p>Constructs a ToolSet containing all the elements of {@code c}.</p>
   * @param c the collection whose elements will be contained by this ToolSet
   */
	private ToolSet(Collection<? extends T> c) {
		super(c);
	}
	
  /**
   * <p>Constructs an empty ToolSet whose capacity is initially set to {@code initialCapacity}.</p>
   * @param initialCapacity the initial capacity of this ToolSet
   */
	protected ToolSet(int initialCapacity) {
		super(initialCapacity);
	}
	
	public ToolSet<T> intersection(Collection<? extends T> otherSet){
		ToolSet<T> result = new ToolSet<>(this);
		result.retainAll(otherSet);
		return result;
	}
	
    /**
     * <p>Returns a list of elements from this set not found in {@code otherCollection}.</p>
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
