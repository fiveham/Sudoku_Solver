package sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ToolSet<T> extends HashSet<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7950470421023736025L;

	public ToolSet() {
	}

	public ToolSet(Collection<T> c) {
		super(c);
	}

	public ToolSet(int initialCapacity) {
		super(initialCapacity);
	}

	public ToolSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}
	
	public boolean intersects(Set<T> otherSet){
		Set<T> copy = new HashSet<>(otherSet);
		copy.retainAll(this);
		return !copy.isEmpty();
	}
	
	/**
	 * Returns a list of elements from this set not found in 
	 * <tt>otherCollection</tt>.
	 * @param otherCollection
	 * @return
	 */
	public List<T> complement(Collection<T> otherCollection){
		return stream().filter((e)->!otherCollection.contains(e)).collect(Collectors.toList());
	}
	
	/**
	 * returns true if fb is a proper subset of this, false otherwise
	 * @param fb
	 * @return
	 */
	public boolean hasProperSubset(Set<Claim> fb){
		return size() > fb.size() && containsAll(fb);
	}
	
	public boolean hasSubset(Set<Claim> fb){
		return size() >= fb.size() && containsAll(fb);
	}
}
