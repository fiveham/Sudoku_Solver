package common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Sets {
	
  /**
   * <p>The factor by which the expected size of a {@link java.util.HashSet HashSet} should be
   * multiplied went sent to HashSet's constructor as the initial capacity, if using the default
   * load factor.</p>
   * @see http://docs.oracle.com/javase/tutorial/collections/implementations/set.html
   */
	public static final int JAVA_UTIL_HASHSET_SIZE_FACTOR = 2;
	
  /**
   * <p>Adds {@code c1} to {@code c2} and returns {@code c2}. This is intended to be used as a
   * {@link java.util.stream.Collector#combiner() combiner} for a
   * {@link java.util.stream.Collector Collector}.</p>
   * @param <E> the parameter type of {@code c1} and {@code c2}
   * @param <C> the type of {@code c1} and {@code c2}
   * @param c1 a collection whose contents are to be combined with those of {@code c2}
   * @param c2 a collection whose contents are to be combined with those of {@code c1}
   * @return {@code c2} after the contents of {@code c1} are added to it
   */
	public static <E, C extends Collection<E>> C mergeCollections(C c1, C c2){
		c2.addAll(c1);
		return c2;
	}
	
  /**
   * <p>Unions the collections in {@code collections} and counts how many times each element is
   * present.</p>
   * @param <T> the type of the elements being counted
   * @param collections a stream of collections whose elements are counted
   * @return a Map from each element present in the collections in {@code collections} to the number
   * of times each such element occurs among those collections
   */
	public static <T> Map<T, Integer> countingUnion(Stream<? extends Collection<T>> collections){
		Map<T, Integer> result = new HashMap<>();
		
		collections.forEach(
		    (collection) -> collection.forEach(
		        (t) -> result.put(t, result.getOrDefault(t, 0))));
		
		return result;
	}
}
