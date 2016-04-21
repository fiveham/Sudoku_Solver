package common;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class TestIterator<T> implements Iterator<T>{
	
	private final List<Predicate<T>> tests;
	private final Iterator<T> wrappedIterator;
	private T last;
	
	@SafeVarargs
	public TestIterator(Iterator<T> wrappedIterator, Predicate<T>... tests){
		this.wrappedIterator = wrappedIterator;
		this.tests = Arrays.asList(tests);
		setLast();
	}
	
	public TestIterator(Iterator<T> wrappedIterator, Collection<Predicate<T>> tests){
		this.wrappedIterator = wrappedIterator;
		this.tests = new ArrayList<>(tests);
		setLast();
	}
	
	public TestIterator(Iterator<T> wrappedIterator){
		this.wrappedIterator = wrappedIterator;
		this.tests = new ArrayList<>();
		setLast();
	}
	
	public boolean addTest(Predicate<T> test){
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
}
