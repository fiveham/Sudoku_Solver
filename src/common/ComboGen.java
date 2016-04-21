package common;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Collection;

public class ComboGen<T> implements Iterable<List<T>>{
	
	public static final int MIN_COMBO_SIZE = 0;
	
	private List<T> list;
	private int minSize;
	private int maxSize;
	
	public ComboGen(Collection<T> source, int minSize){
		this(source, minSize, source.size()-1);
	}
	
	public ComboGen(Collection<T> source, int minSize, int maxSize){
		this.list = new ArrayList<>(source);
		this.minSize = minSize;
		this.maxSize = maxSize;
	}
	
	public Iterator<List<T>> iterator(){
		return new ComboIterator();
	}
	
	/**
	 * A combination-navigating iterator for this ComboGen's underlying 
	 * collection.
	 * 
	 * Produces collections of varying sizes from the underlying 
	 * collection, starting from a size of minMag and increasing to 
	 * maxMag.
	 */
	private class ComboIterator implements Iterator<List<T>>{
		
		private List<T>	nextCombo;
		private int 	comboSize;
		
		/**
		 * <p>Constructs a ComboIterator.</p>
		 */
		public ComboIterator() {
			comboSize = minSize;
			nextCombo = new ArrayList<T>();
			nextCombo = firstComboAtSize();
		}
		
		private List<T> NO_MORE_COMBOS_AVAILABLE(){
			return null;
		}
		
		@Override
		public List<T> next(){
			if(nextCombo == NO_MORE_COMBOS_AVAILABLE()){
				throw new NoSuchElementException();
			}
			
			List<T> retVal = new ArrayList<T>(nextCombo);
			
			nextCombo = generateNextCombo();
			
			return retVal;
		}
		
		private List<T> generateNextCombo(){
			BinRep br = new BinRep();
			
			int index = br.getSwapIndex();
			
			if(index < list.size()){
				br.swap(index);
				
				List<T> retList = new ArrayList<T>();
				for(int i = 0; i<list.size(); ++i){
					if(br.testBit(i)){
						nextCombo.add(list.get(i));
					}
				}
				return retList;
			} else if(comboSize < maxSize){
				++comboSize;
				return firstComboAtSize();
			} else{
				return NO_MORE_COMBOS_AVAILABLE();
			}
		}
		
		/**
		 * <p>Returns a list containing the last N elements from <tt>list</tt>.</p>
		 * 
		 * <p>Normally this would return a List<T> with the first N elements, 
		 * but the bit-manipulation used for iterating over combinations 
		 * is more easily described if the first combination for each combo-size has 
		 * elements with the highest indices instead of with the lowest indices.</p>
		 * @return a list containing the last N elements from baseList
		 */
		private List<T> firstComboAtSize(){
			List<T> retVal = new ArrayList<>();
			
			for(int i = list.size()-comboSize; i<list.size(); ++i){
				retVal.add( list.get(i) );
			}
			
			if(retVal.isEmpty() & comboSize!=0){
				retVal = NO_MORE_COMBOS_AVAILABLE();
			}
			
			return retVal;
		}
		
		@Override
		public boolean hasNext(){
			return nextCombo != NO_MORE_COMBOS_AVAILABLE();
		}
		
		/**
		 * <p>Stores a binary representation of the relationship between 
		 * the base <tt>list</tt> and the {@link #next() next combo}.<p>
		 * 
		 * <p>For any index pertaining to <tt>list</tt>, if the next combo contains 
		 * list.get(index) then the bit at that index in the binary representation 
		 * is 1, otherwise that bit is 0.</p>
		 */
		private class BinRep{
			
			private BigInteger bitstring;
			
			/**
			 * <p>Constructs a binary representation of the relationship between 
			 * baseList and nextCombo.</p>
			 */
			public BinRep(){
				bitstring = BigInteger.ZERO;
				for(int i=0; i<list.size(); i++){
					bitstring = nextCombo.contains(list.get(i))
							? bitstring.setBit(i)
							: bitstring.clearBit(i);
				}
			}
			
			/**
			 * <p>Returns true if a swap can be executed at the specified 
			 * index, false otherwise.</p>
			 * 
			 * <p>Determines whether a swap can be executed by determining 
			 * whether the bit in bitstring at the specified <tt>index</tt> and 
			 * the bit at <tt>index-1</tt> are 1 and 0 respectively.</p>
			 * 
			 * <p>In the case of <tt>index == 0</tt>, where there is no bit in 
			 * bitstring for <tt>index-1</tt>, <tt>false</tt> is returned.</p>
			 * 
			 * @param index the index in bitstring at which a swap-test is 
			 * being performed
			 * 
			 * @return true if the bit in bitstring at the specified 
			 * <tt>index</tt> and the bit at <tt>index-1</tt> are 1 
			 * and 0 respectively, false otherwise
			 */
			public boolean canSwap(int index){
				return index>0 && bitstring.testBit(index) && !bitstring.testBit(index-1);
			}
			
			/**
			 * <p>Returns the lowest index for which a swap can be executed, 
			 * or returns <tt>list.size()</tt> if no swap can be executed.</p>
			 * @return the lowest index for which a swap can be executed, 
			 * or <tt>list.size()</tt> if no swap can be executed
			 */
			public int getSwapIndex(){
				int i=0;
				while(i<list.size()){
					if(canSwap(i++)){
						break;
					}
				}
				return i;
			}
			
			/**
			 * <p>Flips <tt>bitstring</tt>'s bit at the specified index as well as the bit 
			 * at <tt>index-1</tt>. In the preferred use, these two bits will be complementary
			 * <tt>(bit(index) ^ bit(index-1) == true)</tt>.</p>
			 * 
			 * <p>Then, determines the number of {@link BigInteger#setBit(int) set bits} in 
			 * bitstring at indices lower than <t>index-1</tt>, and sets that many bits, 
			 * starting at <tt>index-2</tt>.
			 * 
			 * <tt>Then, {@link BigInteger#clearBit(int) clears} all bits at indices lower 
			 * than the last one from the previous step.</tt>
			 * @param index
			 */
			public void swap(int index){
				
				bitstring = bitstring.flipBit(index).flipBit(index-1);
				
				int ones = getLowerOnes(index-1);
				
				for( int i = index-2; i>index-ones-2; i--){
					bitstring = bitstring.setBit(i);
				}
				
				for( int i = index-ones-2; i>=0; i--){
					bitstring = bitstring.clearBit(i);	
				}
			}
			
			/**
			 * <p>Returns true if the bit in bitstring at the specified index
			 * is {@link BigInteger#setBit(int) set}, false otherwise.</p>
			 * @param index the index in <tt>bitstring</tt> whose bit is tested
			 * @return true if the bit in bitstring at the specified index
			 * is {@link BigInteger#setBit(int) set}, false otherwise
			 */
			public boolean testBit(int index){
				return bitstring.testBit(index);
			}
			
			/**
			 * <p>Returns the number of {@link BigInteger#setBit(int) set bits} in 
			 * <tt>bitstring</tt> at indices lower than <tt>index</tt>.
			 * @param index the position in <tt>bitstring</tt> below which 
			 * {@link BigInteger#setBit(int) set bits} are counted
			 * @return the number of {@link BigInteger#setBit(int) set bits} in 
			 * <tt>bitstring</tt> at indices lower than <tt>index</tt>
			 */
			private int getLowerOnes(int index){
				int retVal = 0;
				for(int i=index-1; i>=0; i--){
					if(bitstring.testBit(i)){
						retVal++;
					}
				}
				return retVal;
			}
			
		}
	}
}
