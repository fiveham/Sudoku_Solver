package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
			
			BigInteger comparisonMask;
			int pointer = 0;
			
			BSIterator(){
				comparisonMask = BackedSet.this.mask;
				updatePointer();
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
				concurrentModificationCheck();
				BackedSet.this.remove(lastResult);
				comparisonMask = mask;
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
		return c.stream().allMatch((e) -> contains(e));
	}
	
	public Stream<E> stream(){
		return StreamSupport.stream(spliterator(), false);
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c) { //TODO unify addAll, containsAll and other methods with this internal structure
		if(c instanceof BackedSet<?>){
			BackedSet<?> b = (BackedSet<?>) c;
			if(universe.equals(b.universe)){
				BigInteger oldMask = mask;
				mask = mask.or(b.mask);
				return !oldMask.equals(mask);
			}
		}
		return c.stream()
				.map((e) -> add(e))
				.collect(MASS_OR);
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
	
	private static final BinaryOperator<Boolean> OR = (a,b) -> a || b;
	private static final Collector<Boolean,?,Boolean> MASS_OR = Collectors.reducing(false, OR);
	
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
				.map((o) -> remove(o))
				.collect(MASS_OR);
	}
	
	public Universe<E> universe(){
		return universe;
	}
	
	@Override
	public void clear() {
		mask = BigInteger.ZERO;
	}
}
