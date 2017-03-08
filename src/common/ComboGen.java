package common;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>Generates combinations of the elements in a given combination supplied to the constructor.</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T> the type of the elements in the combinations that this 
 * @param <T> the type of the elements in the combinations that this @param <T> the type of the
 * @param <T> the type of the elements in the combinations that this elements in the combinations
 * @param <T> the type of the elements in the combinations that this that this class produces
 */
public class ComboGen<T> implements Iterable<List<T>>{
	
  /**
   * <p>The minimum possible size ({@value}) of a combination.</p>
   */
	public static final int MIN_COMBO_SIZE = 0;
	
  /**
   * <p>The internal list from which elements are chosen for the combinations this class
   * produces.</p>
   */
	private final List<T> source;
	private final int minSize;
	private final int maxSize;
	
  /**
   * <p>Constructs a ComboGen that produces combinations of elements from {@code source} that have
   * a size at least {@value #MIN_COMBO_SIZE} and at most {@code source.size()}.</p>
   * @param source a collection of elements combinations of which are produced by this ComboGen
   */
	public ComboGen(Collection<? extends T> source, int minSize, int maxSize){
		this.source = new ArrayList<>(source);
		
		if(minSize < 0){
			throw new IllegalArgumentException("minSize " + minSize + " < 0");
		} else if(maxSize < 0){
			throw new IllegalArgumentException("maxSize " + maxSize + " < 0");
		}
		
		this.minSize = minSize < MIN_COMBO_SIZE 
				? MIN_COMBO_SIZE 
				: minSize;
		
		this.maxSize = maxSize > this.source.size() 
				? this.source.size() 
				: maxSize;
	}
	
	public ComboGen(Collection<? extends T> source, int minSize){
		this(source, minSize, source.size());
	}
	
	public ComboGen(Collection<? extends T> source){
		this(source, MIN_COMBO_SIZE, source.size());
	}
	
	@Override
	public Iterator<List<T>> iterator(){
		return new ComboIterator();
	}
	
  /**
   * <p>A combination-navigating iterator for this ComboGen's underlying collection.</p>
   * <p>Produces collections of varying sizes from this ComboGen's underlying collection, starting
   * from a size of minMag and increasing to maxMag.</p>
   */
	private class ComboIterator implements Iterator<List<T>>{
		
		private int currentSize;
		private BigInteger combo;
		
		private ComboIterator(){
			this.currentSize = minSize;
			if(sizeInRange()){
				this.combo = firstCombo(currentSize);
			}
		}
		
		@Override
		public boolean hasNext(){
			return sizeInRange();
		}
		
		private boolean sizeInRange(){
			return minSize <= currentSize && currentSize <= maxSize;
		}
		
		@Override
		public List<T> next(){
			if(!sizeInRange()){
				throw new NoSuchElementException();
			}
			List<T> result = genComboList(combo);
			updatePosition();
			return result;
		}
		
		private List<T> genComboList(BigInteger combo){
			List<T> result = new ArrayList<>(currentSize);
			for(int i = 0; i < source.size(); ++i){
				if(combo.testBit(i)){
					result.add(source.get(i));
				}
			}
			return result;
		}
		
		private void updatePosition(){
			if(finalCombo(currentSize).equals(combo)){ //maximum lateral position at this height
				currentSize++; //move to next height
				if(currentSize <= maxSize){ //this height is legal
					combo = firstCombo(currentSize);
				}
			} else{ //nonmaximum lateral position. proceed to next lateral position
				combo = comboAfter(combo);
			}
		}
		
    /**
     * <p>Returns a BigInteger {@link #genComboList(BigInteger) pointing} to the first
     * {@code size} elements from {@code list}.</p>
     * @param size the size of the combo whose backing bitstring is returned
     * @return a BigInteger {@link #genComboList(BigInteger) pointing} to the first {@code size}
     * elements from {@code list}
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
		
		private final Map<Integer, BigInteger> leastComboCache = new HashMap<>();
		
    /**
     * <p>Returns a BigInteger {@link #genComboList(BigInteger) pointing} to the last
     * {@code size} elements from {@code list}.</p>
     * @param size the size of the combo whose backing bitstring is returned
     * @return a BigInteger {@link #genComboList(BigInteger) pointing} to the last {@code size}
     * elements from {@code list}
     */
		private BigInteger firstCombo(int size){
			return greatestCombo(size);
		}
		
    /**
     * </p>Returns a BigInteger having the greatest numerical value of any BigInteger
     * {@link #genComboList(BigInteger) pointing} to a combination of the current size. The
     * value returned is equal to {@code (2^(size+1) - 1) * 2^(source.size() - size)}, which
     * equals {@code 2^(source.size()+1) - 2^(source.size() - size)}.</p>
     * @param size the number of set bits in the BigIteger returned
     * @return a BigInteger having the greatest numerical value of any BigInteger
     * {@link #genComboList(BigInteger) pointing} to a combination of the current size
     */
		private BigInteger greatestCombo(int size){
			if(greatestComboCache.containsKey(size)){
				return greatestComboCache.get(size);
			}
			
			BigInteger result = BigInteger.ZERO;
			for(int i=source.size() - size; i < source.size(); ++i){
				result = result.setBit(i);
			}
			
			greatestComboCache.put(size, result);
			return result;
		}
		
		private final Map<Integer, BigInteger> greatestComboCache = new HashMap<>();
		
		/** 
     * <p>This implementation, which pulls ones down to lower indices, is tied to the fact that the 
     * first combo is the greatest value and the final combo is the least value. If that 
     * relationship between combo precedence and the numerical size of the combo ever changes, this 
     * method needs to be adapted to the new relationship.</p>
     * <p>The combo after a given combo is determined by moving the lowest-indexed movable set bit 
     * to an index lower by 1. A set bit is movable if the bit at index 1 lower than the movable bit
     * is 0.</p>
     * @param combo a BigInteger whose bits encode a combination of the elements pertaining to this 
     * ComboGen
     * @return a BigInteger encoding the combination after {@code combo}
     */
		private BigInteger comboAfter(BigInteger combo){
			int swapIndex = lowerableOne(combo);
			int onesBelow = bitsSetToTheRight(swapIndex, combo);
			
			//swap the 1 with the 0 to the right of it
			BigInteger result = combo.clearBit(swapIndex);
			swapIndex--;
			result = result.setBit(swapIndex);
			swapIndex--;
			
			//move all the 1s from the right of the swapped 0 to a position immediately to the right of 
			//the swapped 0's initial position
			for(int onesSet = 0; onesSet < onesBelow; ++onesSet){
				result = result.setBit(swapIndex);
				swapIndex--;
			}
			
			//fill the space between the rightmost moved 1 and the ones' place of the BigInteger with 0s
			while(swapIndex >= 0){
				result = result.clearBit(swapIndex);
				swapIndex--;
			}
			
			return result;
		}
		
    /**
     * <p>Returns the lowest index in {@code combo} of a {@link BigInteger#testBit(int) 1} such
     * that the bit at the next lower index is 0. If no such bit exists in {@code combo}, then
     * {@code source.size()} is returned.</p>
     * @param combo the combo whose lowest-index 1 with a 0 immediately below it (in terms of
     * index) is returned
     * @return the lowest index in {@code combo} of a {@link BigInteger#testBit(int) 1} such
     * that the bit at the next lower index is 0, or {@code source.size()} if no such bit exists
     * in {@code combo}
     */
		private int lowerableOne(BigInteger combo){
			int i = 0;
			for(; i < source.size() - 1; ++i){
				if(!combo.testBit(i) && combo.testBit(i + 1)){
					break;
				}
			}
			return i + 1;
		}
		
    /**
     * <p>Returns the number of 1s in {@code combo} at indices less than {@code swapIndex}.</p>
     * @param swapIndex the index in {@code combo} below which 1s are counted
     * @param combo the BigInteger from which 1s are counted
     * @return the number of 1s in {@code combo} at indices less than
     */
		private int bitsSetToTheRight(int swapIndex, BigInteger combo){
			int result = 0;
			for(int i=swapIndex - 1; i >= 0; --i){
				if(combo.testBit(i)){
					++result;
				}
			}
			return result;
		}
	}
}
