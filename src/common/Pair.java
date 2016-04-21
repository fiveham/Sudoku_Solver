package common;

import java.util.Collection;
import java.util.Iterator;

public class Pair<T,S> {
	
	public static final int ITEM_COUNT_IN_PAIR = 2;
	
	private T a;
	private S b;
	
	public Pair(T a, S b) {
		this.a = a;
		this.b = b;
	}
	
	@SuppressWarnings("unchecked")
	public Pair(Collection<?> c){
		if(c.size()!=ITEM_COUNT_IN_PAIR){
			throw new IllegalArgumentException("Need exactly two elements in a collection. Given "+c.size()+"instead");
		}
		Iterator<?> iter = c.iterator();
		this.a = (T) iter.next();
		this.b = (S) iter.next();
	}
	
	public T getA(){
		return a;
	}
	
	public S getB(){
		return b;
	}
	
	public boolean contains(Object o){
		return a.equals(o) || b.equals(o);
	}
	
	public Object partner(Object o){
		if(a.equals(o)){
			return b;
		} else if(b.equals(o)){
			return a;
		} else{
			throw new IllegalArgumentException("The specified object ("+o.toString()+") is not present in this pair.");
		}
	}
	
	/*public List<Object> asList(){
		List<Object> result = new ArrayList<>(ITEM_COUNT_IN_PAIR);
		result.add(a);
		result.add(b);
		return result;
	}*/
	
	@Override
	public int hashCode(){
		return a.hashCode()^b.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if( o instanceof Pair<?,?>){
			Pair<?,?> p = (Pair<?,?>) o;
			return a.equals(p.a) ? b.equals(p.b) : a.equals(p.b) && b.equals(p.a);  
		}
		return false;
	}
}
