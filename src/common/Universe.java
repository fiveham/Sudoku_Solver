package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>A special immutable collection where each element maps one-to-one with an integer. Used as a 
 * backing for BackedSets, which identify their elements as indices in a BigInteger; such an index 
 * is the integer corresponding to the element in that BackedSet's Universe.</p>
 * @author fiveham
 * @param <E> the type of the elements of this universe and the parameter-type of BackedSets built 
 * on this Universe
 * @see java.util.EnumSet
 */
public class Universe<E> {
	
	private final List<E> ie;
	private final Map<E,Integer> ei;
	
	/**
	 * <p>Constructs a Universe containing exactly the elements of {@code c}.</p>
	 * @param c a set whose elements will become this Universe
	 */
	public Universe(Collection<? extends E> c){
		this.ie = new ArrayList<E>(c);
		this.ei = new HashMap<>();
		for(int i=0; i<ie.size(); ++i){
			ei.put(ie.get(i), i);
		}
	}
	
	/**
	 * <p>Constructs a Universe containing exactly the unique elements of {@code s}.</p>
	 * @param s a stream whose contents will become this Universe
	 */
	public Universe(Stream<? extends E> s){
		this.ie = s.collect(Collectors.toList());
		this.ei = new HashMap<>();
		for(int i=0; i<ie.size(); ++i){
			ei.put(ie.get(i), i);
		}
	}
	
	/**
	 * <p>Returns true if {@code o} is in this Universe, false otherwise.</p>
	 * @param o the object to be tested for its presence in this Universe
	 * @return true if {@code o} is in this Universe, false otherwise
	 */
	public boolean contains(Object o){
		return ei.containsKey(o);
	}
	
	/**
	 * <p>Returns the number of elements that are a part of this Universe.</p>
	 * @return the number of elements in this Universe
	 */
	public int size(){
		return ie.size();
	}
	
	/**
	 * <p>Returns the object having index {@code i} in this Universe.</p>
	 * @param i the index of the element of this Universe to be returned
	 * @return the object having index {@code i} in this Universe
	 * @throws IndexOutOfBoundsException if {@code i} is less than 0 or greater than or equal to the 
	 * size of the Universe
	 */
	public E get(int i){
		return ie.get(i);
	}
	
	public int index(E e){
		return ei.get(e);
	}
	
	public BackedSet<E> set(Collection<? extends E> c){
		return new BackedSet<E>(this,c);
	}
	
	@Override
	public int hashCode(){
		if(hash==null){
			hash = ie.hashCode();
		}
		return hash;
	}
	
	private Integer hash = null;
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Universe<?>){
			Universe<?> u = (Universe<?>) o;
			return ie.equals(u.ie) && ei.equals(u.ei); 
		}
		return false;
	}
	
  /**
   * <p>A convenience method returning an empty BackedSet backed by this Universe.</p>
   * @return an empty BackedSet backed by this Universe
   */
	public BackedSet<E> back(){
		return new BackedSet<>(this);
	}
	
  /**
   * <p>A convenience method returning a BackedSet backed by this Universe and containing the 
   * contents of {@code c}.</p>
   * @return a BackedSet backed by this Universe and containing the contents of {@code c}
   * @throws BackedSet.OutOfUniverseException if any element of {@code c} is not in this Universe
   */
	public BackedSet<E> back(Collection<? extends E> c){
		return new BackedSet<>(this, c);
	}
}
