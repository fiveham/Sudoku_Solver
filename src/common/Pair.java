package common;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p>An immutable pair of objects of arbitrary classes.</p>
 * 
 * <p>Access the first element of a Pair by calling {@link #getA()}. 
 * Access the second element of a Pair by calling {@link #getB()}.</p>
 * @author fiveham
 *
 * @param <T>
 * @param <S>
 */
public class Pair<T,S> {
	
	public static final int ITEM_COUNT = 2;
	
	private T a;
	private S b;
	
	/**
	 * <p>Constructs a Pair whose first element is <tt>a</tt> and 
	 * whose second element is <tt>b</tt>.</p>
	 * @param a the first element of this Pair
	 * @param b the second element of this Pair
	 */
	public Pair(T a, S b) {
		this.a = a;
		this.b = b;
	}
	
	/**
	 * <p>Constructs a Pair containing the first two elements output 
	 * by the <tt>c</tt>'s {@link Collection#iterator() iterator}.</p>
	 * @param c the Collection whose iterator's first two results 
	 * will be the elements of this pair
	 */
	@SuppressWarnings("unchecked")
	public Pair(Collection<?> c){
		if(c.size()!=ITEM_COUNT){
			throw new IllegalArgumentException("Need exactly two elements in a collection. Given "+c.size()+"instead");
		}
		Iterator<?> iter = c.iterator();
		this.a = (T) iter.next();
		this.b = (S) iter.next();
	}
	
	/**
	 * <p>Returns the first element of this Pair.</p>
	 * @return the first element of this Pair
	 */
	public T getA(){
		return a;
	}
	
	/**
	 * <p>Returns the second element of this Pair.</p>
	 * @return the second element of this Pair
	 */
	public S getB(){
		return b;
	}
	
	/**
	 * <p>Returns true if <tt>o</tt> is either of the elements of 
	 * this Pair, false otherwise.</p>
	 * @param o the object being tested as a member of this Pair
	 * @return true if <tt>o</tt> is either of the elements of 
	 * this Pair, false otherwise
	 */
	public boolean contains(Object o){
		return a.equals(o) || b.equals(o);
	}
	
	/**
	 * <p>Returns the counterpart of <tt>o</tt> in this Pair if 
	 * <tt>o</tt> is a member of this Pair.</p>
	 * @param o the object whose counterpart in this Pair will 
	 * be returned
	 * @throws IllegalArgumentException if <tt>o</tt> is not a 
	 * member of this Pair
	 * @return the counterpart of <tt>o</tt> in this Pair if 
	 * <tt>o</tt> is a member of this Pair
	 */
	public Object partner(Object o){
		if(a.equals(o)){
			return b;
		} else if(b.equals(o)){
			return a;
		} else{
			throw new IllegalArgumentException("The specified object ("+o.toString()+") is not present in this pair.");
		}
	}
	
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
