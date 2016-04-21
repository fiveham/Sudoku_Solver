package common;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Manages an arbitrary number of independent options in an arbitrary 
 * number of independent collections. With two options from which to select, 
 * you'd have a rectangle, and with three a rectangular prism (cuboid). 
 * Accounting for a rectangle of options can be done with two nested 
 * for loops, and accounting for three can be done with three nested for 
 * loops, but to account for arbitrary and possibly unknown numbers of 
 * independent dimensions, something more adaptable than hard-coded loops 
 * is required. To that end, this class treats each possible position 
 * in an N-dimensional cuboid as a number in a variable-radix system. 
 * For example, in a 3-dimensional cuboid where the highest-order dimension 
 * has two options, the middle-order dimension has 8 options, and the lowest
 * -order dimension has 5 options, the digits of the equivalent number 
 * would be [1 to 2][1 to 8][1 to 5].  This abstract number is stored 
 * internally as an int array.
 * @author fiveham
 *
 */
public class NCuboid<T> implements Iterable<List<T>> {
	
	private final List<List<T>> src;
	private final int[] key;
	
	public NCuboid(Collection<? extends Collection<T>> src) {
		
		this.src = new ArrayList<>(src.size());
		for(Collection<T> dimension : src){
			if( !dimension.isEmpty() ){
				this.src.add(new ArrayList<>(dimension));
			} else{
				throw new IllegalArgumentException("One of the specified dimensions was empty.");
			}
		}
		
		this.key = new int[this.src.size()];
		for(int i=0; i<this.src.size(); ++i){
			key[i] = this.src.get(i).size()-1; //set to that collection's maximum index to make iterator's hasNext more efficient
		}
	}
	
	@Override
	public Iterator<List<T>> iterator(){
		return new CubeIterator();
	}
	
	private class CubeIterator implements Iterator<List<T>>{
		
		private final int[] state;
		
		private CubeIterator(){
			this.state = new int[key.length];
			Arrays.fill(state, 0);
		}
		
		@Override
		public boolean hasNext(){
			return !Arrays.equals(state, key);
		}
		
		@Override
		public List<T> next(){
			updateState();
			List<T> result = new ArrayList<>(state.length);
			for(int i=0; i<state.length; ++i){
				result.add( src.get(i).get(state[i]) );
			}
			return result;
		}
		
		private void updateState(){
			for(int i=0, carry=1; carry!=0; ++i){
				if(state[i]==key[i]){
					state[i]=0;
				} else{
					state[i]+=carry--;
				}
			}
		}
	}
	
}
