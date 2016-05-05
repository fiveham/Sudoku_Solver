package common;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * <p>An Iterator that internally produces results but only 
 * releases those that pass every test in an arbitrary 
 * collection of tests.</p>
 * @author fiveham
 *
 * @param <T>
 */
public class TestIterator<T> implements Iterator<T>{
	
	protected final List<Predicate<? super T>> tests;
	protected final Iterator<T> wrappedIterator;
	protected T last;
	
	/**
	 * <p>Constructs a TestIterator backed internally by the specified Iterator 
	 * and having the specified <tt>tests</tt>.</p>
	 * @param wrappedIterator the Iterator that internally produces the contingent 
	 * results for this TestIterator which are only released if they pass all the 
	 * <tt>test</tt>
	 * @param tests the tests that a result produced by <tt>wrappedIterator</tt> 
	 * must pass in order to be produced as a valid result by this TestIterator
	 */
	@SafeVarargs
	public TestIterator(Iterator<T> wrappedIterator, Predicate<? super T>... tests){
		this.wrappedIterator = wrappedIterator;
		this.tests = Arrays.asList(tests);
		setLast();
	}
	
	/**
	 * <p>Constructs a TestIterator backed internally by the specified Iterator 
	 * and having the specified <tt>tests</tt>.</p>
	 * @param wrappedIterator the Iterator that internally produces the contingent 
	 * results for this TestIterator which are only released if they pass all the 
	 * <tt>test</tt>
	 * @param tests the tests that a result produced by <tt>wrappedIterator</tt> 
	 * must pass in order to be produced as a valid result by this TestIterator
	 */
	public TestIterator(Iterator<T> wrappedIterator, Collection<? extends Predicate<? super T>> tests){
		this.wrappedIterator = wrappedIterator;
		this.tests = new ArrayList<>(tests);
		setLast();
	}
	
	/**
	 * <p>Constructs a TestIterator backed internally by the specified Iterator 
	 * and having no tests.</p>
	 * @param wrappedIterator the Iterator that internally produces the contingent 
	 * results for this TestIterator which are only released if they pass all the 
	 * <tt>test</tt>
	 */
	public TestIterator(Iterator<T> wrappedIterator){
		this.wrappedIterator = wrappedIterator;
		this.tests = new ArrayList<>();
		setLast();
	}
	
	/**
	 * <p>Adds the specified <tt>test</tt> to this TestIterator's collection 
	 * of tests.</p>
	 * @param test a test to be added to this TestIterator's internal collection 
	 * of tests
	 * @return true if the collection of tests was changed by calling this 
	 * method, false otherwise
	 */
	public boolean addTest(Predicate<? super T> test){
		return tests.add(test);
	}
	
	@Override
	public boolean hasNext(){
		return last != null;
	}
	
	@Override
	public T next(){
		T result = last;
		setLast();
		return result;
	}
	
	/**
	 * <p>Extracts and tests results from the <tt>wrappedIterator</tt>, 
	 * storing each as the <tt>last</tt> result until it finds one that 
	 * passes all the <tt>tests</tt>.</p>
	 * <p>If none of the values produced by the <tt>wrappedIterator</tt> 
	 * pass all the <tt>tests</tt>, <tt>last<tt> is set to <tt>null</tt>.</p>
	 */
	private void setLast(){
		while(wrappedIterator.hasNext()){
			T item = wrappedIterator.next();
			if(tests.stream().allMatch((p)->p.test(item))){
				last = item;
				return;
			}
		}
		last = null;
	}
	
	/**
	 * <p>Returns an Iterable whose {@link Iterable#iterator() iterator()} 
	 * method returns <tt>this<tt>.</p>
	 * @return an Iterable whose {@link Iterable#iterator() iterator()} 
	 * method returns <tt>this<tt>
	 */
	public Iterable<T> iterable(){
		return new Iterable<T>(){
			@Override
			public Iterator<T> iterator(){
				return TestIterator.this;
			}
		};
	}
}
