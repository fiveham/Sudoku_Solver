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

public class Universe<E> {
	
	private final List<E> indexToElement;
	private final Map<E, Integer> elementToIndex;
	
	public Universe(Set<? extends E> c){
		this.indexToElement = Collections.unmodifiableList(new ArrayList<>(c));
		Map<E, Integer> elementToIndex = new HashMap<>();
		for(int i = 0; i < indexToElement.size(); ++i){
			elementToIndex.put(indexToElement.get(i), i);
		}
		this.elementToIndex = Collections.unmodifiableMap(elementToIndex);
	}
	
	public Universe(Stream<? extends E> s){
		this.indexToElement = 
		    Collections.unmodifiableList(new ArrayList<>(s.collect(Collectors.toSet())));
		Map<E, Integer> elementToIndex = new HashMap<>();
		for(int i = 0; i < indexToElement.size(); ++i){
			elementToIndex.put(indexToElement.get(i), i);
		}
		this.elementToIndex = Collections.unmodifiableMap(elementToIndex);
	}
	
	public boolean contains(Object o){
		return elementToIndex.containsKey(o);
	}
	
	public int size(){
		return indexToElement.size();
	}
	
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
     * <p>A convenience method returning a BackedSet backed by this Universe, containing the
     * contents of {@code c}.</p>
     * @return a BackedSet backed by this Universe, containing the contents of {@code c}
     */
	public BackedSet<E> back(Collection<? extends E> c){
		return new BackedSet<>(this, c);
	}
}
