package common;

import java.util.Iterator;
import java.util.List;

/**
 * <p>A TestIterator meant for exploring a space of combinations. As such, removing elements from
 * the backing collection should have an effect on subsequent results from next(). This class
 * provides convenience methods for excluding certain elements of the backing collection from
 * combinations returned from next().</p>
 * @author fiveham
 * @author fiveham
 *
 * @param <T>
 * @param <T>@param <T>
 */
public class IsoIterator<T> extends TestIterator<List<T>> {
	
    /**
     * <p>Constructs an IsoIterator wrapping the specified Iterator and having no tests.</p>
     * @param wrappedIterator the Iterator that supplies this IsoIterator with elements to test and
     * contingently reduce
     */
	public IsoIterator(Iterator<List<T>> wrappedIterator) {
		super(wrappedIterator);
	}
}
