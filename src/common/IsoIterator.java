package common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * <p>A TestIterator meant for exploring a space of combinations. As 
 * such, removing elements from the backing collection should have 
 * an effect on subsequent results from next(). This class provides 
 * convenience methods for excluding certain elements of the backing 
 * collection from combinations returned from next().</p>
 * @author fiveham
 *
 * @param <T>
 */
public class IsoIterator<T> extends TestIterator<List<T>> {
	
	protected final Set<T> exclude;
	
	/**
	 * <p>Constructs an IsoIterator wrapping the specified Iterator and 
	 * having the specified {@code tests}.</p>
	 * @param wrappedIterator the Iterator that supplies this IsoIterator 
	 * with elements to test and contingently reduce
	 * @param tests tests that results from {@code wrappedIterator} must 
	 * pass in order to be returned by next() in this class
	 */
	@SafeVarargs
	public IsoIterator(Iterator<List<T>> wrappedIterator, Predicate<? super List<? extends T>>... tests) {
		super(wrappedIterator, tests);
		this.exclude = new HashSet<>();
		addTest(genExcludeTest(exclude));
	}
	
	/**
	 * <p>Constructs an IsoIterator wrapping the specified Iterator and 
	 * having the specified {@code tests}.</p>
	 * @param wrappedIterator the Iterator that supplies this IsoIterator 
	 * with elements to test and contingently reduce
	 * @param tests tests that results from {@code wrappedIterator} must 
	 * pass in order to be returned by next() in this class
	 */
	public IsoIterator(Iterator<List<T>> wrappedIterator, Collection<? extends Predicate<? super List<? extends T>>> tests) {
		super(wrappedIterator, tests);
		this.exclude = new HashSet<>();
		addTest(genExcludeTest(exclude));
	}
	
	/**
	 * <p>Constructs an IsoIterator wrapping the specified Iterator and 
	 * having no tests.</p>
	 * @param wrappedIterator the Iterator that supplies this IsoIterator 
	 * with elements to test and contingently reduce
	 */
	public IsoIterator(Iterator<List<T>> wrappedIterator) {
		super(wrappedIterator);
		this.exclude = new HashSet<>();
		addTest(genExcludeTest(exclude));
	}
	
	public static <T> Predicate<List<T>> genExcludeTest(Set<T> exclude){
		return (list) -> Collections.disjoint(list, exclude);
	}
	
	/**
	 * <p>Prevents {@code item} from appearing in any combinations produced 
	 * by subsequent calls to next().</p>
	 * @param item an element of the backing collection from which elements 
	 * are drawn to construct combinations
	 * @return true if the population of excluded items was changed by this 
	 * method call, false otherwise
	 */
	public boolean exclude(T item){
		return exclude.add(item);
	}
}