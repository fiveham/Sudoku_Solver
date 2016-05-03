package common;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import sudoku.Debug;

/**
 * <p>Generates combinations of the elements in a given combination 
 * supplied to the constructor.</p>
 * @author fiveham
 *
 * @param <T> the type of the elements in the combinations that this 
 * class produces
 */
public class ComboGen<T> implements Iterable<List<T>>{
	
	/**
	 * <p>The minimum possible size ({@value}) of a combination.</p>
	 */
	public static final int MIN_COMBO_SIZE = 0;
	
	/**
	 * <p>The internal list from which elements are chosen for the 
	 * combinations this class produces.</p>
	 */
	private final List<T> source;
	private final int minSize;
	private final int maxSize;
	
	/**
	 * <p>Constructs a ComboGen that produces combinations of elements from 
	 * <tt>source</tt> that have a size at least {@value #MIN_COMBO_SIZE} and at 
	 * most <tt>source.size()</tt>.</p>
	 * @param source a collection of elements combinations of which are 
	 * produced by this ComboGen
	 */
	public ComboGen(Collection<? extends T> source, int minSize, int maxSize){
		this.source = new ArrayList<>(source);
		
		if(minSize < MIN_COMBO_SIZE){
			throw new IllegalArgumentException("minSize "+minSize);
		}
		this.minSize = minSize;
		
		if(maxSize > source.size()){
			throw new IllegalArgumentException("maxSize "+maxSize);
		}
		this.maxSize = maxSize;
		
		if(maxSize<minSize){
			throw new IllegalArgumentException("max size must be greater than min size: " + maxSize + " < " + minSize);
		}
		
		//Debug.log("ComboGen min size "+minSize + ", max size "+maxSize); //DEBUG
	}
	
	public ComboGen(Collection<? extends T> source, int minSize){
		this(source, minSize, source.size());
	}
	
	public ComboGen(Collection<? extends T> source){
		this(source, MIN_COMBO_SIZE, source.size());
	}
	
	private void test(){
		ComboIterator ci = new ComboIterator();
		
		/*System.out.println("Testing firstCombo and finalCombo");
		for(int size = 0; size <= source.size(); size++){ //note: starts at 0 and test including equality with source size
			BigInteger first = ci.firstCombo(size);
			BigInteger finla = ci.finalCombo(size);
			
			System.out.println("size "+size);
			System.out.println("first: "+first.toString(2));
			System.out.println("final: "+finla.toString(2));
		}*/
		
		System.out.println("Testing comboAfter");
		BigInteger bi;
		int lo, ob;
		

		bi = BigInteger.ZERO;
		lo = ci.lowerableOne(bi);
		ob = ci.onesBelow(lo, bi);
		System.out.println(bi.toString(2));
		System.out.println(lo + " " + ob);
		System.out.println();
		
		for(int i=0; i<25; i++){
			bi = ci.comboAfter(bi);
			lo = ci.lowerableOne(bi);
			ob = ci.onesBelow(lo, bi);
			System.out.println(bi.toString(2));
			System.out.println(lo + " " + ob);
			System.out.println();
		}
	}
	
	/**
	 * <p>Returns an IsoIterator wrapping this ComboGen's normal 
	 * iterator, allowing elements from the underlying element 
	 * pool to be excluded from combos produced  by subsequent 
	 * calls to <tt>next()</tt>.</p>
	 * 
	 * @return an IsoIterator wrapping this ComboGen's normal 
	 * iterator
	 */
	@Override
	public IsoIterator<T> iterator(){
		return new IsoIterator<>(new ComboIterator());
	}
	
	/**
	 * <p>A combination-navigating iterator for this ComboGen's underlying 
	 * collection.</p>
	 * 
	 * <p>Produces collections of varying sizes from this ComboGen's underlying 
	 * collection, starting from a size of minMag and increasing to 
	 * maxMag.</p>
	 */
	private class ComboIterator implements Iterator<List<T>>{
		
		private int size;
		private BigInteger combo;
		
		private ComboIterator(){
			this.size = minSize;
			this.combo = firstCombo(size);
		}
		
		@Override
		public boolean hasNext(){
			return minSize <= size && size <= maxSize;
		}
		
		@Override
		public List<T> next(){
			if(!hasNext()){
				throw new NoSuchElementException();
			}
			List<T> result = genComboList(combo);

			updatePosition();
			return result;
		}
		
		private List<T> genComboList(BigInteger combo){
			List<T> result = new ArrayList<>(size);
			for(int i=0; i<source.size(); ++i){
				if(combo.testBit(i)){
					result.add(source.get(i));
				}
			}
			return result;
		}
		
		private void updatePosition(){
			if(combo.equals(finalCombo(size))){
				combo = firstCombo(++size);
			} else{
				combo = comboAfter(combo);
			}
		}
		
		/**
		 * <p>Returns a BigInteger {@link #genComboList(BigInteger) pointing} to 
		 * the first <tt>size</tt> elements from <tt>list</tt>.</p>
		 * 
		 * @param size the size of the combo whose backing bitstring is returned
		 * @return a BigInteger {@link #genComboList(BigInteger) pointing} to 
		 * the first <tt>size</tt> elements from <tt>list</tt>
		 */
		private BigInteger finalCombo(int size){
			return leastCombo(size);
		}
		
		private BigInteger leastCombo(int size){
			if(leastComboCache.containsKey(size)){
				return leastComboCache.get(size);
			}
			
			BigInteger result = BigInteger.ZERO;
			for(int i=0; i < size; ++i){
				result = result.setBit(i);
			}
			
			leastComboCache.put(size, result);
			return result;
		}
		
		private final Map<Integer,BigInteger> leastComboCache = new HashMap<>();
		
		/**
		 * <p>Returns a BigInteger {@link #genComboList(BigInteger) pointing} to 
		 * the last <tt>size</tt> elements from <tt>list</tt>.</p>
		 * 
		 * @param size the size of the combo whose backing bitstring is returned
		 * @return a BigInteger {@link #genComboList(BigInteger) pointing} to 
		 * the last <tt>size</tt> elements from <tt>list</tt>
		 */
		private BigInteger firstCombo(int size){
			BigInteger result = greatestCombo(size);
			
			//Debug.log("first combo at size "+size+": "+result.toString(2)); //DEBUG
			
			return result;
		}
		
		/**
		 * </p>Returns a BigInteger having the greatest numerical value of any 
		 * BigInteger {@link #genComboList(BigInteger) pointing} to a 
		 * combination of the current size. The value returned is equal to 
		 * <tt>(2^(size+1) - 1) * 2^(source.size() - size)</tt>, which equals 
		 * <tt>2^(source.size()+1) - 2^(source.size() - size)</tt>.</p>
		 * 
		 * @param size the number of set bits in the BigIteger returned
		 * @return a BigInteger having the greatest numerical value of any 
		 * BigInteger {@link #genComboList(BigInteger) pointing} to a 
		 * combination of the current size
		 */
		private BigInteger greatestCombo(int size){
			if(greatestComboCache.containsKey(size)){
				return greatestComboCache.get(size);
			}
			
			BigInteger result = BigInteger.ZERO;
			for(int i=source.size()-size; i < source.size(); ++i){
				result = result.setBit(i);
			}
			
			greatestComboCache.put(size, result);
			return result;
		}
		
		private final Map<Integer,BigInteger> greatestComboCache = new HashMap<>();
		
		/* 
		 * This implementation, pulling 1s down to lower indices, is 
		 * tied to the fact that the first combo is the greatest value 
		 * and the final combo is the least value. If that relationship 
		 * between combo precedence and the numerical size of the combo 
		 * ever changes, this method needs to be adapted to the new 
		 * relationship.
		 */
		private BigInteger comboAfter(BigInteger combo){
			int swapIndex = lowerableOne(combo);
			int onesBelow = onesBelow(swapIndex, combo);
			
			//swap the 1 with the 0 below it
			BigInteger result = combo.clearBit(swapIndex);
			swapIndex--;
			result = result.setBit(swapIndex);
			swapIndex--;
			
			//move all the 1s from below the swapped 0 to a position 
			//immediately below the swapped 0's initial position
			for(int onesSet = 0; onesSet < onesBelow; ++onesSet){
				result = result.setBit(swapIndex);
				swapIndex--;
			}
			
			//fill the space between the lowest moved 1 and the bottom of 
			//the BigInteger with 0s
			while(swapIndex>=0){
				result = result.clearBit(swapIndex);
				swapIndex--;
			}
			
			return result;
		}
		
		/**
		 * <p>Returns the lowest index in <tt>combo</tt> of a 
		 * {@link BigInteger#testBit(int) 1} such that the bit at the 
		 * next lower index is 0. If no such bit exists in <tt>combo</tt>, 
		 * then <tt>source.size()</tt> is returned.</p>
		 * 
		 * @param combo the combo whose lowest-index 1 with a 0 
		 * immediately below it (in terms of index) is returned
		 * @return the lowest index in <tt>combo</tt> of a 
		 * {@link BigInteger#testBit(int) 1} such that the bit at the 
		 * next lower index is 0, or <tt>source.size()</tt> if no such 
		 * bit exists in <tt>combo</tt>
		 */
		private int lowerableOne(BigInteger combo){
			int i=0;
			for(;i<source.size()-1; ++i){
				if(!combo.testBit(i) && combo.testBit(i+1)){
					break;
				}
			}
			return i+1;
		}
		
		/**
		 * <p>Returns the number of 1s in <tt>combo</tt> at indices less 
		 * than <tt>swapIndex</tt>.</p>
		 * @param swapIndex the index in <tt>combo</tt> below which 1s 
		 * are counted
		 * @param combo the BigInteger from which 1s are counted
		 * @return the number of 1s in <tt>combo</tt> at indices less 
		 * than <tt>swapIndex
		 */
		private int onesBelow(int swapIndex, BigInteger combo){
			int result = 0;
			for(int i=swapIndex-1; i>=0; --i){
				if(combo.testBit(i)){
					++result;
				}
			}
			return result;
		}
	}
	
	/**
	 * <p>For testing</p>
	 * @param args
	 */
	public static void main(String[] args){
		Integer[] array = new Integer[20];
		Arrays.fill(array, 5);
		List<Integer> src = Arrays.asList(array);
		
		ComboGen<Integer> cg = new ComboGen<>(src);
		cg.test();
	}
}
