package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.math.BigInteger;

/**
 * <p>A Set implementation that can only contain elements included in a predefined list and refers 
 * to those elements by their index in that list.</p>
 * @see Universe
 * @see java.util.EnumSet
 * @author fiveham
 */
public class BackedSet<E> implements Set<E>, Cloneable{
	
	private final Universe<E> universe;
	private BigInteger mask;
	
	/**
	 * <p>Constructs a BackedSet backed by {@code universe} and initially containing no elements.</p>
	 * @param universe the Universe backing this BackedSet
	 */
	public BackedSet(Universe<E> universe) {
		this.universe = universe;
		this.mask = BigInteger.ZERO;
	}
	
	/**
	 * <p>Constructs a BackedSet backed by {@code universe} and initially containing exactly the 
	 * elements of {@code c}.</p>
	 * @param universe the Universe backing this BackedSet
	 * @param c a collection
	 * @throws OutOfUniverseException if any element of {@code c} is not in this BackedSet's backing 
   * Universe
	 */
	public BackedSet(Universe<E> universe, Collection<? extends E> c){
		this(universe);
		addAll(c);
	}
	
	/**
	 * <p>Constructs a BackedSet backed by {@link #universe() bs's Universe} and initially containing 
	 * exactly the elements of {@code bs}.</p>
	 * @param bs a BackedSet
	 */
	public BackedSet(BackedSet<E> bs){
		this(bs.universe, bs);
	}
	
	@Override
	public int size() {
		return mask.bitCount();
	}
	
	@Override
	public boolean isEmpty() {
		return mask.equals(BigInteger.ZERO);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o) {
		return universe.contains(o) && mask.testBit(universe.index((E) o));
	}
	
	@Override
	public Iterator<E> iterator() {
		class BSIterator implements Iterator<E>{
			
			BigInteger comparisonMask;
			int pointer;
			
			BSIterator(){
				comparisonMask = BackedSet.this.mask;
				pointer = comparisonMask.getLowestSetBit();
			}
			
			private void updatePointer(){
				while(pointer<universe.size()){
					if(comparisonMask.testBit(pointer)){
						return;
					}
					pointer++;
				}
			}
			
			private void concurrentModificationCheck(){
				if(!comparisonMask.equals(mask)){
					throw new ConcurrentModificationException();
				}
			}
			
			@Override
			public boolean hasNext() {
				concurrentModificationCheck();
				return comparisonMask.testBit(pointer);
			}
			
			@Override
			public E next() {
				concurrentModificationCheck();
				lastResult = universe.get(pointer);
				pointer++;
				updatePointer();
				return lastResult;
			}
			
			private E lastResult = null;
			
			@Override
			public void remove(){
				if(lastResult != null){
					concurrentModificationCheck();
					BackedSet.this.remove(lastResult);
					lastResult = null;
					comparisonMask = mask;
				} else{
					throw new IllegalStateException("Element already removed or not yet returned.");
				}
			}
		}
		
		return new BSIterator();
	}
	
	@Override
	public Object[] toArray() {
		return new ArrayList<>(this).toArray();
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		return new ArrayList<>(this).toArray(a);
	}
	
  /**
   * <p>Adds the specified object to this set as long as it is in this set's universe; otherwise,
   * throws an OutOfUniverseException.</p>
   * @param e the element to be added to this BackedSet
   * @return true if this set was modified by this operation, false otherwise
   * @throws OutOfUniverseException if {@code e} is not in this BackedSet's backing Universe
   */
	@Override
	public boolean add(E e) {
		if(universe.contains(e)){
			int index = universe.index(e);
			boolean old = mask.testBit(index);
			mask = mask.setBit(index);
			return old != mask.testBit(index);
		} else{
			throw new OutOfUniverseException("Cannot add the object because it is not in this set's universe.");
		}
	}
	
	/**
	 * <p>Indicates that an object is not in a given BackedSet's Universe.</p>
	 * @author fiveham
	 */
	public static class OutOfUniverseException extends RuntimeException{
    
		private static final long serialVersionUID = -4023554063682941699L;

		OutOfUniverseException(String s){
			super(s);
		}
	}
	
	@Override
	public boolean remove(Object o) {
		if(universe.contains(o)){
			@SuppressWarnings("unchecked")
			int index = universe.index((E) o);
			boolean old = mask.testBit(index);
			mask = mask.clearBit(index);
			return old != mask.testBit(index);
		}
		return false;
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				return this.mask.or(b.mask).equals(this.mask); 
			}
		}
		return c.stream().allMatch(this::contains);
	}
	
	/**
	 * @throws OutOfUniverseException if any element of {@code c} is not in this BackedSet's backing 
	 * Universe
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				BigInteger oldMask = mask;
				mask = mask.or(b.mask);
				return !oldMask.equals(mask);
			}
		}
		return c.stream()
				.map(this::add)
				.reduce(false, Boolean::logicalOr);
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				BigInteger oldMask = mask;
				mask = mask.and(b.mask);
				return !oldMask.equals(mask);
			}
		}
		
		boolean result = false;
		for(Iterator<E> iter = iterator(); iter.hasNext();){
			E e = iter.next();
			if(!c.contains(e)){
				iter.remove();
				result = true;
			}
		}
		return result;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				BigInteger oldMask = mask;
				mask = mask.andNot(b.mask);
				return !mask.equals(oldMask);
			}
		}
		
		return c.stream()
				.map(this::remove)
				.reduce(false, Boolean::logicalOr);
	}
	
	/**
	 * <p>Returns the Universe object backing the elements of this set.</p>
	 * @return the Universe object backing the elements of this set
	 */
	public Universe<E> universe(){
		return universe;
	}
	
	@Override
	public void clear() {
		mask = BigInteger.ZERO;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof BackedSet){
			BackedSet<?> b = (BackedSet<?>) o;
			return b.mask.equals(mask) && b.universe.equals(universe);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return universe.hashCode() + mask.hashCode();
	}
	
	@Override
	public BackedSet<E> clone(){
		BackedSet<E> result = new BackedSet<>(this.universe);
		result.mask = this.mask;
		return result;
	}
	
	/**
	 * <p>Returns true if {@code a} and {@code b} are disjoint, false otherwise.</p>
	 * <p>This method uses bitwise integer operations if {@code a} and {@code b} have the same 
	 * Universe.</p>
	 * @param a a BackedSet
	 * @param b a BackedSet
	 * @return true if {@code a} and {@code b} are disjoint, false otherwise
	 */
	public static boolean disjoint(BackedSet<?> a, BackedSet<?> b){
		return a.universe.equals(b.universe)
			? a.mask.xor(b.mask).equals(BigInteger.ZERO)
			: Collections.disjoint(a, b);
	}
}
