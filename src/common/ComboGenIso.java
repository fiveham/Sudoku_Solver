package common;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.BiPredicate;

/**
 * <p>Given a starting collection, this class produces every allowed combination 
 * of elements from that collection. Every combination is allowed by default 
 * unless one or more of its elements has been {@link #remove() removed} from 
 * the source collection.</p>
 * @author fiveham
 *
 * @param <T>
 */
public class ComboGenIso<T> implements Iterable<List<T>>{
	
	private List<T> list;
	private int minSize;
	private int maxSize;
	private Direction direction;
	
	/**
	 * <p>Constructs a ComboGenIso that produces combinations of the 
	 * elements in <tt>source</tt> with a minimum size of 
	 * <tt>minSize</tt>, a maximum size of <tt>source.size()</tt>, 
	 * and an increasing <tt>Direction</tt>.</p>
	 * @param source the initial collection of elements combinations 
	 * of which are produced by this ComboGenIso
	 * @param minSize the minimum size of combinations produced by 
	 * this ComboGenIso
	 */
	public ComboGenIso(Collection<? extends T> source, int minSize){
		this(source, minSize, source.size(), Direction.INCREASE);
	}
	
	/**
	 * <p>Constructs a ComboGenIso that produces combinations of the 
	 * elements in <tt>source</tt> with a minimum size of 
	 * <tt>minSize</tt>, a maximum size of <tt>maxSize</tt>, 
	 * and an increasing <tt>Direction</tt>.</p>
	 * @param source the initial collection of elements combinations 
	 * of which are produced by this ComboGenIso
	 * @param minSize the minimum size of the combinations produced 
	 * by this ComboGenIso
	 * @param maxSize the maximum size of the combinations produced 
	 * by this ComboGenIso
	 */
	public ComboGenIso(Collection<? extends T> source, int minSize, int maxSize){
		this(source, minSize, maxSize, Direction.INCREASE);
	}
	
	/**
	 * <p>Constructs a ComboGenIso that produces combinations of the 
	 * elements in <tt>source</tt> with a minimum size of 
	 * <tt>minSize</tt>, a maximum size of <tt>maxSize</tt>, 
	 * and the specified <tt>direction</tt>.</p>
	 * @param source the initial collection of elements combinations 
	 * of which are produced by this ComboGenIso
	 * @param minSize the minimum size of the combinations produced 
	 * by this ComboGenIso
	 * @param maxSize the maximum size of the combinations produced 
	 * by this ComboGenIso
	 * @param direction the {@link ComboGenIso.Direction Direction} 
	 * of motion that this ComboGenIso takes in combination-space
	 */
	public ComboGenIso(Collection<? extends T> source, int minSize, int maxSize, Direction direction){
		this.list = new ArrayList<>(source);
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.direction = direction;
	}
	
	/**
	 * <p>Sets this ComboGenIso's direction to the specified <tt>direction</tt>.</p>
	 * @param direction the new direction of motion that this ComboGenIso takes 
	 * in combination-space
	 */
	public void setDirection(Direction direction){
		this.direction = direction;
	}
	
	/**
	 * <p>Returns a {@link ComboGenIso.ComboIterator ComboIterator} that 
	 * outputs combinations of elements from this ComboGenIso's underlying 
	 * collection of elements.</p>
	 * @return a ComboIterator that outputs combinations of elements from 
	 * this ComboGenIso's underlying collection of elements
	 */
	@Override
	public ComboIterator<T> iterator(){
		return new ComboIterator<>(this);
	}
	
	/**
	 * <p>A combination-navigating iterator for this ComboGen's underlying 
	 * collection.</p>
	 * 
	 * <p>Produces collections of varying sizes from the underlying 
	 * collection, starting from a size of <tt>minSize</tt> and increasing 
	 * to <tt>maxSize</tt>.</p>
	 */
	public static class ComboIterator<T> implements Iterator<List<T>>{
		
		private final ComboGenIso<T> owner;
		
		private int 	   comboSize;
		private BigInteger disallowed;
		private BigInteger prevResult;
		
		private final GenNextCache gnc;
		
		/**
		 * <p>Constructs a ComboIterator.</p>
		 * 
		 * <p>Adds all elements of the specified collection to a list.
		 * Creates the first combo, with minMag elements, from 
		 * the last elements from the specified collection.</p>
		 * @param collection A collection from which elements are 
		 * drawn.
		 */
		public ComboIterator(ComboGenIso<T> owner) {
			this.owner = owner;
			this.comboSize  = owner.direction.initComboSize(owner);
			this.disallowed = BigInteger.ZERO;
			this.prevResult = BigInteger.ZERO;
			this.gnc = new GenNextCache();
		}
		
		@Override
		public ComboIterator<T> clone(){
			ComboIterator<T> result = new ComboIterator<>(owner);
			result.comboSize = this.comboSize;
			result.disallowed = this.disallowed;
			result.prevResult = this.prevResult;
			
			return result;
		}
		
		@Override
		public boolean hasNext(){
			return genNext(prevResult, owner) != null;
		}
		
		@Override
		public List<T> next(){
			return getCombo(prevResult = genNext(prevResult, owner), comboSize, owner.list);
		}
		
		private static <T> List<T> getCombo(BigInteger bi, int comboSize, List<T> ownerList){
			if(bi==null){
				throw new NoSuchElementException("No more combinations available.");
			}
			
			List<T> result = new ArrayList<>(comboSize);
			
			for(int i=0; i<ownerList.size(); ++i){
				if(bi.testBit(i)){
					result.add(ownerList.get(i));
				}
			}
			
			return result;
		}
		
		private BigInteger genNext(BigInteger previousResult, ComboGenIso<T> owner){
			return gnc.apply(previousResult, owner);
		}
		
		private class GenNextCache{
			BigInteger previousResult;
			ComboGenIso<T> owner;
			Direction direction;
			BigInteger output;
			private GenNextCache(){
				this.previousResult = null;
				this.owner = null;
				this.output = null;
			}
			public BigInteger apply(BigInteger previousResult, ComboGenIso<T> owner){
				if(previousResult==this.previousResult && owner==this.owner && owner.direction == this.direction){
					return this.output;
				} else{
					BigInteger output = genNext3(previousResult, owner);
					this.previousResult = previousResult;
					this.owner = owner;
					this.direction = owner.direction;
					this.output = output;
					return output;
				}
			}
		}
		
		private BigInteger genNext3(BigInteger previousResult, ComboGenIso<T> owner){
			BigInteger result = previousResult;
			while(!( (result = genNext2(result, owner))==null || result.and(disallowed).equals(BigInteger.ZERO) ));
			return result;
		}
		
		private BigInteger genNext2(BigInteger previousResult, ComboGenIso<T> owner){
			int index = getSwapIndex(previousResult, owner.list.size());
			if(index < owner.list.size()){ //transitions within a comboSize
				return swap(index, previousResult);
			} else if( owner.direction.comboSizeChangeTest.test(owner,comboSize)){ //index == owner.list.size() //transitions between comboSizes
				this.comboSize += owner.direction.increment();
				return firstComboAtSize();
			} else{ //end of iteration
				return null;
			}
		}
		
		private BigInteger firstComboAtSize(){
			BigInteger result = BigInteger.ZERO;
			
			for(int i = owner.list.size()-comboSize; i<owner.list.size(); ++i){
				result = result.setBit(i);
			}
			
			return result;
		}
		
		/**
		 * <p>Disallows all the elements of the previously produced nextResult 
		 * from being elements of any subsequently produced results. Equivalent 
		 * to calling {@link #remove(Object) remove(T)} for each individual 
		 * element of the last combo returned by {@link #next() next()}.</p>
		 */
		@Override
		public void remove(){
			if(disallowed.equals(disallowed = disallowed.or(prevResult))){
				throw new IllegalStateException("remove() has already been called since the last next() call.");
			}
		}
		
		/**
		 * <p>Prevents <tt>t</tt> from appearing in any subsequent combinations  
		 * returned by {@link #next() next()}.</p>
		 * @param t an element of this ComboGenIso's backing collection
		 */
		public void remove(T t){
			int i = owner.list.indexOf(t);
			if(i>=0){
				BigInteger mask = BigInteger.ONE.shiftLeft(i);
				disallowed = disallowed.or(mask);
			}
		}
		
		/**
		 * <p>Returns the lowest index for which a swap can be executed, 
		 * or returns list.size() if no swap can be executed.</p>
		 * @return the lowest index for which a swap can be executed, 
		 * or returns list.size() if no swap can be executed
		 */
		private static int getSwapIndex(BigInteger bi, int listSize){
			int i=0;
			for(; i<listSize; ++i){
				if(canSwap(i, bi)){
					break;
				}
			}
			return i; //returns list.size() if nextCombo is in its final state for its size.
		}
		
		/**
		 * Returns true if a swap can be executed at the specified 
		 * index, false otherwise.
		 * 
		 * Determines whether a swap can be executed by determining 
		 * whether the bit in bitstring at the specified <tt>index</tt> and 
		 * the bit at <tt>index-1</tt> are 1 and 0 respectively.
		 * 
		 * In the case of <tt>index == 0</tt>, where there is no bit in 
		 * bitstring for <tt>index-1</tt>, <tt>false</tt> is returned.
		 * @param index
		 * @return
		 */
		private static boolean canSwap(int index, BigInteger bi){
			return index>0 && bi.testBit(index) && !bi.testBit(index-1);
		}
		
		/**
		 * Flips bitstring's bit at the specified index as well as the bit 
		 * at index-1.  In the preferred use, these two bits will be complementary
		 * ( bit(index) ^ bit(index-1) == true ).
		 * 
		 * Then, determines the number of 1-bits in bitstring at indices lower 
		 * than index-1, and sets that many bits to 1, starting at index-2.
		 * 
		 * Then, sets all bits at indices lower than the last one from the previous 
		 * step to 0.
		 * @param index
		 */
		private static BigInteger swap(int index, BigInteger bi){
			bi = bi.flipBit(index).flipBit(index-1);
			int ones = getLowerOnes(index-1, bi);
			
			for( int i = index-2; i>index-ones-2; --i){
				bi = bi.setBit(i);
			}
			for( int i = index-ones-2; i>=0; --i){
				bi = bi.clearBit(i);	
			}
			
			return bi;
		}
		
		/**
		 * Returns the number of 1-bits in bitstring at indices lower 
		 * than the specified index.
		 * @param index
		 * @return
		 */
		private static int getLowerOnes(int index, BigInteger bi){
			int retVal = 0;
			for(int i=index-1; i>=0; --i){
				if(bi.testBit(i)){
					++retVal;
				}
			}
			return retVal;
		}
	}
	
	/**
	 * <p>Represents the direction of motion of a ComboGenIso in linear 
	 * combination-space in terms of the size of the combinations.</p>
	 * @author fiveham
	 *
	 */
	public static enum Direction{
		
		/**
		 * <p>Refers to motion in combination-space such that the 
		 * sizes of traversed combinations increase.</p>
		 */
		INCREASE( 1, (c)->c.minSize, (o,i)->i<=o.maxSize), 
		
		/**
		 * <p>Refers to motion in combination-space such that the 
		 * sizes of traversed combinations decrease.</p>
		 */
		DECREASE(-1, (c)->c.maxSize, (o,i)->o.minSize<=i);
		
		private final int increment;
		private final Function<ComboGenIso<?>,Integer> ics;
		private final BiPredicate<ComboGenIso<?>,Integer> comboSizeChangeTest;
		
		private Direction(int increment, Function<ComboGenIso<?>,Integer> ics, BiPredicate<ComboGenIso<?>,Integer> comboSizeChangeTest){
			this.increment = increment;
			this.ics = ics;
			this.comboSizeChangeTest = comboSizeChangeTest;
		}
		
		/**
		 * <p>Returns the combination-space position-difference for 
		 * this Direction.</p>
		 * @return the combination-space position-difference for 
		 * this Direction
		 */
		public int increment(){
			return increment;
		}
		
		/**
		 * <p>Returns the initial size of the combinations for a ComboGenIso 
		 * having this Direction.</p>
		 * <p>For a typical, increasing ComboGenIso, the initial combo size 
		 * is <tt>minSize</tt>, but for a ComboGenIso initialized with the 
		 * DECREASE Direction, the initial combo size is <tt>maxSize</tt>.</p>
		 * @param cgi
		 * @return
		 */
		public int initComboSize(ComboGenIso<?> cgi){
			return ics.apply(cgi) - increment; //TODO why is there a minus here?
		}
	}
}
