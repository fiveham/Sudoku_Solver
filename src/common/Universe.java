package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Universe<E> {
	
	private final List<E> ie;
	private final Map<E,Integer> ei;
	
	public Universe(Collection<? extends E> c){
		this.ie = new ArrayList<E>(c);
		this.ei = new HashMap<>();
		for(int i=0; i<ie.size(); ++i){
			ei.put(ie.get(i), i);
		}
	}
	
	public Universe(Stream<? extends E> s){
		this.ie = s.collect(Collectors.toList());
		this.ei = new HashMap<>();
		for(int i=0; i<ie.size(); ++i){
			ei.put(ie.get(i), i);
		}
	}
	
	public boolean contains(Object o){
		return ei.containsKey(o);
	}
	
	public int size(){
		return ie.size();
	}
	
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
		if(hash == null){
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
     * <p>A convenience method returning a BackedSet backed by this Universe, containing the
     * contents of {@code c}.</p>
     * @return a BackedSet backed by this Universe, containing the contents of {@code c}
     */
	public BackedSet<E> back(Collection<? extends E> c){
		return new BackedSet<>(this, c);
	}
}
