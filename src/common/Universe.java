package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	
	private final List<E> indexToElement;
	private final Map<E, Integer> elementToIndex;
	
	/**
	 * <p>Constructs a Universe containing exactly the elements of {@code c}.</p>
	 * @param c a set whose elements will become this Universe
	 */
	public Universe(Set<? extends E> c){
		this.indexToElement = Collections.unmodifiableList(new ArrayList<>(c));
		Map<E, Integer> elementToIndex = new HashMap<>();
		for(int i = 0; i < indexToElement.size(); ++i){
			elementToIndex.put(indexToElement.get(i), i);
		}
		this.elementToIndex = Collections.unmodifiableMap(elementToIndex);
	}
	
	/**
	 * <p>Constructs a Universe containing exactly the unique elements of {@code s}.</p>
	 * @param s a stream whose contents will become this Universe
	 */
	public Universe(Stream<? extends E> s){
		this.indexToElement = 
		    Collections.unmodifiableList(new ArrayList<>(s.collect(Collectors.toSet())));
		Map<E, Integer> elementToIndex = new HashMap<>();
		for(int i = 0; i < indexToElement.size(); ++i){
			elementToIndex.put(indexToElement.get(i), i);
		}
		this.elementToIndex = Collections.unmodifiableMap(elementToIndex);
	}
	
	/**
	 * <p>Returns true if {@code o} is in this Universe, false otherwise.</p>
	 * @param o the object to be tested for its presence in this Universe
	 * @return true if {@code o} is in this Universe, false otherwise
	 */
	public boolean contains(Object o){
		return elementToIndex.containsKey(o);
	}
	
	/**
	 * <p>Returns the number of elements that are a part of this Universe.</p>
	 * @return the number of elements in this Universe
	 */
	public int size(){
		return indexToElement.size();
	}
	
	/**
	 * <p>Returns the object having index {@code i} in this Universe.</p>
	 * @param i the index of the element of this Universe to be returned
	 * @return the object having index {@code i} in this Universe
	 * @throws IndexOutOfBoundsException if {@code i} is less than 0 or greater than or equal to the 
	 * size of the Universe
	 */
	public E get(int i){
		return indexToElement.get(i);
	}
	
	/**
   * <p>Returns the index pertaining to {@code e}, or -1 if {@code e} is not in this Universe.</p>
   * @param e the object whose index in this universe is returned
   * @return the index of {@code e} in this Universe or -1 if {@code e} is not in this Universe
   */
	public int index(E e){
	  return elementToIndex.getOrDefault(e, INDEX_IF_ABSENT);
	}
	
	private static final Integer INDEX_IF_ABSENT = -1;
	
	@Override
	public int hashCode(){
		if(hash==null){
			hash = indexToElement.hashCode();
		}
		return hash;
	}
	
	private Integer hash = null;
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Universe<?>){
			Universe<?> u = (Universe<?>) o;
			return indexToElement.equals(u.indexToElement) && elementToIndex.equals(u.elementToIndex); 
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
