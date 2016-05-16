package common;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * <p>Manages an arbitrary number of abstract dimensions, each of 
 * which can take an arbitrary, constant number of different values.</p>
 * 
 * <p>With two dimensions, the NCuboid is a rectangle, and with three 
 * a rectangular prism (cuboid).</p>
 * 
 * <p>Accounting for two options can be done with two nested {@code for} 
 * loops, and accounting for three can be done with three nested 
 * {@code for} loops, but to account for an arbitrary number of 
 * independent dimensions, something more adaptable than hard-coded loops 
 * is required.</p>
 * 
 * <p>To that end, this class treats each possible position in an 
 * N-dimensional cuboid as a number in a variable-radix number-system. 
 * For example, in a 3-dimensional cuboid where the highest-order dimension 
 * has two options, the middle-order dimension has 8 options, and the lowest
 * -order dimension has 5 options, the digits of the equivalent number 
 * would be [1 to 2][1 to 8][1 to 5]. This abstract number is stored 
 * internally as an int array.</p>
 * @author fiveham
 *
 */
public class NCuboid<T> implements Iterable<List<T>> {
	
	private final List<List<? extends T>> src;
	private final int[] key;
	
	/**
	 * <p>Constructs an NCuboid whose dimensions are the elements of 
	 * {@code src}.</p>
	 * @param src
	 * @throws IllegalArgumentException if any of the dimensions specified 
	 * as elements of {@code src} is empty
	 */
	public NCuboid(Collection<? extends Collection<? extends T>> src) {
		
		this.src = new ArrayList<>(src.size());
		if(src.parallelStream().anyMatch((dimension)->dimension.isEmpty())){
			throw new IllegalArgumentException("One of the specified dimensions was empty.");
		} else{
			src.stream().forEach((dimension)->this.src.add(new ArrayList<T>(dimension)));
		}
		
		this.key = new int[this.src.size()];
		for(int i=0; i<this.src.size(); ++i){
			key[i] = this.src.get(i).size()-1; //set to that collection's maximum index to make iterator's hasNext more efficient
		}
	}
	
	@SafeVarargs
	public NCuboid(Collection<? extends T>... srcs){
		this(Arrays.asList(srcs).stream().map((collection) -> new ArrayList<>(collection)).collect(Collectors.toList()));
	}
	
	@Override
	public Iterator<List<T>> iterator(){
		return new CubeIterator();
	}
	
	/**
	 * <p>An Iterator which keeps track of its position in an N-dimensional 
	 * cuboid defined by the dimensions specified as elements of {@code src}.</p>
	 * @author fiveham
	 *
	 */
	private class CubeIterator implements Iterator<List<T>>{
		
		private final int[] state;
		
		private CubeIterator(){
			this.state = new int[key.length];
			Arrays.fill(state, 0);
			state[0]--;
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
					state[i]++;
					carry = 0;
				}
			}
		}
	}
	
}
