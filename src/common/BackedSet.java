package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
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
public class BackedSet<E> implements Set<E> {
	
	private final Universe<E> universe;
	private BigInteger mask;
	
	public BackedSet(Universe<E> universe) {
		this.universe = universe;
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
		return mask.bitCount();
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
			
			final BigInteger originalMask;
			int pointer = 0;
			
			BSIterator(){
				originalMask = BackedSet.this.mask;
				updatePointer();
			}
			
			private void updatePointer(){
				while(pointer<universe.size()){
					if(originalMask.testBit(pointer)){
						return;
					}
					pointer++;
				}
			}
			
			private void concurrentModificationCheck(){
				if(!originalMask.equals(mask)){
					throw new ConcurrentModificationException();
				}
			}
			
			@Override
			public boolean hasNext() {
				concurrentModificationCheck();
				return originalMask.testBit(pointer);
			}
			
			@Override
			public E next() {
				concurrentModificationCheck();
				E result = universe.get(pointer);
				pointer++;
				updatePointer();
				return result;
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
		List<E> asList = new ArrayList<>();
		for(int i=0; i<universe.size() && asList.size()<size(); ++i){
			if(mask.testBit(i)){
				asList.add(universe.get(i));
			}
		}
		return asList;
	}
	
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
	
	@Override
	public boolean remove(Object o) {
		if(universe.contains(o)){
			@SuppressWarnings("unchecked")
			int index = universe.index((E)o);
			boolean old = mask.testBit(index);
			mask = mask.clearBit(index);
			return old != mask.testBit(index);
		} else{
			throw new OutOfUniverseException("The object is not in this set's universe.");
		}
	}
	
	@Override
	public boolean containsAll(Collection<?> c) {
		if(sharesUniverse(c)){
			BackedSet<?> bs = (BackedSet<?>) c;
			return bs.mask.andNot(mask).equals(BigInteger.ZERO);
		} else{
			throw new UniverseMismatchException("The collection does not share this set's universe.");
		}
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		if(sharesUniverse(c)){
			BackedSet<?> bs = (BackedSet<?>) c;
			BigInteger oldMask = mask;
			mask = mask.or(bs.mask);
			return !oldMask.equals(mask);
		} else{
			throw new UniverseMismatchException("The collection does not share this set's universe.");
		}
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		if(sharesUniverse(c)){ //FIXME constructor from Collection relies on bulkop methods, which rely on Collection already being a BackedSet
			BackedSet<?> bs = (BackedSet<?>) c;
			BigInteger oldMask = mask;
			mask = mask.and(bs.mask);
			return !oldMask.equals(mask);
		} else{
			throw new UniverseMismatchException("The collection does not share this set's universe.");
		}
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		if(sharesUniverse(c)){
			BackedSet<?> bs = (BackedSet<?>) c;
			BigInteger oldMask = mask;
			mask = mask.andNot(bs.mask);
			return !oldMask.equals(mask);
		} else{
			throw new UniverseMismatchException("The collection does not share this set's universe.");
		}
	}
	
	private boolean sharesUniverse(Collection<?> c){
		if(c instanceof BackedSet){
			BackedSet<?> bs = (BackedSet<?>) c;
			return universe.equals(bs.universe);
		}
		return false;
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
	
	public static class UniverseMismatchException extends RuntimeException{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5875338960922977561L;

		UniverseMismatchException(String s){
			super(s);
		}
	}
	
	public Universe<E> universe(){
		return universe;
	}
	
	@Override
	public void clear() {
		mask = BigInteger.ZERO;
	}
}
