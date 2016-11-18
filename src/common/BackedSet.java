package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.math.BigInteger;

/**
 * <p>A {@code Set} implementation capable of containing only 
 * elements included in an initially-specified list that backs 
 * the contents of the Set in a manner like the way that the 
 * content of a String is backed by a backing {@code CharSequence} 
 * array which may be much longer than the String in question.</p>
 * @author fiveham
 *
 */
public class BackedSet<E> implements Set<E>, Cloneable{
	
	private final Universe<E> universe;
	private final Set<Integer> indices;
	private BigInteger mask;
	
	public BackedSet(Universe<E> universe) {
		this.universe = universe;
		this.indices = new HashSet<>();
		this.mask = BigInteger.ZERO;
	}
	
	public BackedSet(Universe<E> universe, Collection<? extends E> c){
		this(universe);
		addAll(c);
	}
	
	public BackedSet(BackedSet<E> bs){
		this(bs.universe, bs);
	}
	
	@Override
	public int size() {
		return indices.size();
	}
	
	@Override
	public boolean isEmpty() {
		return mask.equals(BigInteger.ZERO);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o) {
		return universe.contains(o) && mask.testBit(universe.index((E)o));
	}
	
	@Override
	public Iterator<E> iterator() {
		class BSIterator implements Iterator<E>{
			Iterator<Integer> indexIterator = indices.iterator();
			Integer lastNext = null;
			
			@Override
			public boolean hasNext(){
				return indexIterator.hasNext();
			}
			
			@Override
			public E next(){
				return universe.get(lastNext = indexIterator.next());
			}
			
			@Override
			public void remove(){ //removes index from indices
				if(lastNext == null){
					throw new IllegalStateException("Last result not available or already removed.");
				} else{
					mask = mask.clearBit(lastNext);
					indexIterator.remove();
					lastNext = null;
				}
			}
		}
		
		return new BSIterator();
	}
	
	@Override
	public Object[] toArray() {
		return asList().toArray();
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		return asList().toArray(a);
	}
	
	private List<E> asList(){
		return new ArrayList<>(this);
	}
	
	/**
	 * <p>Adds the specified object to this set as long as it is 
	 * in this set's universe; otherwise, throws an 
	 * OutOfUniverseException.</p>
	 * @param e the element to be added to this BackedSet
	 * @return true if this set was modified by this operation, 
	 * false otherwise
	 * @throws OutOfUniverseException if {@code e} is not in this 
	 * BackedSet's backing Universe.
	 */
	@Override
	public boolean add(E e) {
		if(universe.contains(e)){
			int index = universe.index(e);
			mask = mask.setBit(index);
			return indices.add(index);
		} else{
			throw new OutOfUniverseException("Cannot add the object because it is not in this set's universe.");
		}
	}
	
	public static class OutOfUniverseException extends RuntimeException{
		/**
		 * 
		 */
		private static final long serialVersionUID = -4023554063682941699L;

		OutOfUniverseException(String s){
			super(s);
		}
	}
	
	@Override
	public boolean remove(Object o) {
		if(universe.contains(o)){
			@SuppressWarnings("unchecked")
			int index = universe.index((E)o);
			mask = mask.clearBit(index);
			return indices.remove(index);
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
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				BigInteger oldMask = mask;
				mask = mask.or(b.mask);
				c.stream()
						.map(universe::index)
						.forEach(indices::add);
				return !oldMask.equals(mask);
			}
		}
		return c.stream()
				.map(this::add) //adds index to indices
				.reduce(false, Boolean::logicalOr);
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				mask = mask.and(b.mask);
				return indices.retainAll(b.indices);
			}
		}
		
		boolean result = false;
		for(Iterator<E> iter = iterator(); iter.hasNext();){
			E e = iter.next();
			if(!c.contains(e)){
				iter.remove(); //removes index from indices
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
				mask = mask.andNot(b.mask);
				return indices.removeAll(b.indices);
			}
		}
		
		return c.stream()
				.map(this::remove) //removes index from indices
				.reduce(false, Boolean::logicalOr);
	}
	
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
		result.indices.addAll(this.indices);
		return result;
	}
}
