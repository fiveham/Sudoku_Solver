package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ToolSet<T> extends HashSet<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7950470421023736025L;

	public ToolSet() {
		// TODO Auto-generated constructor stub
	}

	public ToolSet(Collection<T> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	public ToolSet(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public ToolSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Returns a list of elements from this set not found in 
	 * <tt>otherCollection</tt>.
	 * @param otherCollection
	 * @return
	 */
	public List<T> complement(Collection<T> otherCollection){
		List<T> retVal = new ArrayList<>(this);
		retVal.removeAll(otherCollection);
		return retVal;
	}
	
	/**
	 * returns true if fb is a proper subset of this, false otherwise
	 * @param fb
	 * @return
	 */
	public boolean hasProperSubset(Set<Claim> fb){
		return containsAll(fb) && size() > fb.size();
	}
	
	/**
	 * 
	 * @throws NoSuchElementException if this set is empty
	 * @return
	 */
	public T sample(){
		return iterator().next();
	}
	
}
