package common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;

public class Sets {
	
	/**
	 * <p>The factor by which the expected size of a {@link java.util.HashSet HashSet} 
	 * should be multiplied went sent to HashSet's constructor as the initial 
	 * capacity, if using the default load factor.</p>
	 * @see http://docs.oracle.com/javase/tutorial/collections/implementations/set.html
	 */
	public static final int JAVA_UTIL_HASHSET_SIZE_FACTOR = 2;
	
	/**
	 * <p>A Collector that collects a Stream of collections into the intersection of 
	 * all the collections in the stream, so that the result is a Set containing exactly 
	 * the elements held in common by all the collections in the Stream.</p>
	 * <p>This is a convenience method equivalent to 
	 * {@link #massIntersectionCollector(Supplier) massIntersectionCollector(HashSet::new)}.</p>
	 * @param <T> the parameter type of the Stream to be collected, a type that extends 
	 * Collection with paramter type {@code E}
	 * @param <E> the parameter type of the Collections that are the elements of the 
	 * Stream to be collected
	 * @return a Collector that collects a Stream of collections into the intersection 
	 * of all the collections in the stream
	 */
	public static <T extends Collection<E>, E> Collector<T,?,Set<E>> massIntersectionCollector(){
		return massIntersectionCollector(HashSet<E>::new);
	}
	
	/**
	 * <p>A Collector that collects a Stream of collections into the intersection of 
	 * all the collections in the stream, so that the result is a Set containing exactly 
	 * the elements held in common by all the collections in the Stream. The type of the 
	 * resulting Set is specified by {@code supplier}.</p>
	 * <p>This is a convenience method equivalent to 
	 * {@link #massIntersectionCollector(Supplier) massIntersectionCollector(HashSet::new)}.</p>
	 * @param <T> the parameter type of the Stream to be collected, a type that extends 
	 * Collection with paramter type {@code E}
	 * @param <E> the parameter type of the Collections that are the elements of the 
	 * Stream to be collected
	 * @param <Z> the type of the Sets to be used to reduce the Stream to its collections' 
	 * shared intersection and of the resulting Set ultimately produced
	 * @param supplier a source of Sets of a specific type
	 * @return a Collector that collects a Stream of collections into the intersection 
	 * of all the collections in the stream
	 */
	public static <T extends Collection<E>, E, Z extends Set<E>> Collector<T,?,Z> massIntersectionCollector(Supplier<Z> supplier){
		return Collector.of(
				() -> new Intersection<Z,E>(supplier), 
				Intersection::intersect, 
				Intersection::combineWith, 
				Intersection::unpack, 
				Characteristics.UNORDERED);
	}
	
	/**
	 * <p>Wraps a Set of a specified type to be used in collecting a 
	 * Stream of collections as the intersection of all the collections 
	 * in the Stream.</p>
	 * @author fiveham
	 *
	 * @param <X> the type of the Set to be used to collect a Stream's 
	 * collections' intersection and the type of the Set in which the 
	 * ultimate result is reported
	 * @param <E> the parameter type of the collections that are elements 
	 * of the Stream being collected
	 */
	private static class Intersection<X extends Set<E>, E>{
		
		/**
		 * <p>If {@code set} is empty, either it has just been created 
		 * and hasn't been used, in which case its emptiness means nothing 
		 * to the mass-intersection to be collected, or it has been used 
		 * at least once so that the intersection of the collections in a 
		 * stream being collected is the empty set, in which case its 
		 * emptiness is meaningful.</p>
		 * <p>This bit enables the meaningless and meaningful cases of
		 * {@code set} being {@link Collection#isEmpty() empty} to be 
		 * differentiated easily.</p>
		 */
		private boolean used = false;
		private final X set;
		
		public Intersection(Supplier<X> supplier){
			set = supplier.get();
		}
		
		public void intersect(Collection<E> coll){
			if(used){
				set.retainAll(coll);
			}else{
				set.addAll(coll);
				used = true;
			}
		}
		
		public Intersection<X,E> combineWith(Intersection<X,E> other){
			intersect(other.set);
			return this;
		}
		
		public X unpack(){
			return set;
		}
	}
	
	/**
	 * <p>Unions all the collections in {@code collections} into one set and returns 
	 * that set.</p>
	 * @param <E> The type of the elements of the Set produced and of the collections 
	 * that are unioned to produce that Set
	 * @param <C> The type of the elements of {@code collections} which contain 
	 * elements of type {@code E}.
	 * @param collections a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @return the union of all the collections that are elements of {@code collections}
	 */
	public static <E, C extends Collection<E>> Set<E> massUnion(Collection<C> collections){
		return collections.stream().collect(massUnionCollector());
	}
	
	/**
	 * <p>Collects a Stream of collections as the union of all the collections in the Stream.</p>
	 * <p>This is a convenience method equivalent to 
	 * {@link #massUnionCollector(Supplier) massUnionCollector(HashSet::new)}.</p>
	 * @param <S> the type of the elements of the resulting mass-union set and of the 
	 * collections in the Stream being collected
	 * @param <T> the type of the collections in the Stream being collected
	 * @return a Collector that collects a Stream of collections as the union of all the 
	 * collections in the Stream
	 */
	public static <S, T extends Collection<S>> Collector<T,?,Set<S>> massUnionCollector(){
		return massUnionCollector(HashSet<S>::new);
	}
	
	/**
	 * <p>Collects a Stream of collections as the union of all the collections in the Stream.</p>
	 * @param <S> the type of the elements of the resulting mass-union set and of the 
	 * collections in the Stream being collected
	 * @param <T> the type of the collections in the Stream being collected
	 * @param <U> the type of the mass-union Set produced by the returned Collector and 
	 * the type of the Sets used to accumulate the collections from the Stream being 
	 * collected
	 * @param supplier a source of a specific type of Set to be used to gather the mass-union 
	 * and which will be the type of the resulting set produced by the returned Collector.</p>
	 * @return a Collector which collects a Stream of collections as the union of all the 
	 * collections in the Stream
	 */
	public static <S, T extends Collection<S>, U extends Set<S>> Collector<T,?,U> massUnionCollector(Supplier<U> supplier){
		return Collector.of(
				supplier, 
				Set::addAll, 
				Sets::mergeCollections);
	}
	
	/**
	 * <p>Adds {@code c1} to {@code c2} and returns {@code c2}. This is intended to 
	 * be used as a {@link java.util.stream.Collector#combiner() combiner} for a 
	 * {@link java.util.stream.Collector Collector}.</p>
	 * @param <E> the parameter type of {@code c1} and {@code c2}
	 * @param <C> the type of {@code c1} and {@code c2}
	 * @param c1 a collection whose contents are to be combined with those of 
	 * {@code c2}
	 * @param c2 a collection whose contents are to be combined with those of 
	 * {@code c1}
	 * @return {@code c2} after the contents of {@code c1} are added to it
	 */
	public static <E, C extends Collection<E>> C mergeCollections(C c1, C c2){
		c2.addAll(c1);
		return c2;
	}
	
	/**
	 * <p>Unions the collections in {@code collections} and counts how many times 
	 * each element is present.</p>
	 * @param <T> the type of the elements being counted
	 * @param collections
	 * @return a Map from each element present in the collections in 
	 * {@code collections} to the number of times each such element occurs among 
	 * the collections in {@code collections}.
	 */
	public static <T> Map<T,Integer> countingUnion(Collection<? extends Collection<T>> collections){
		return countingUnion(collections.stream());
	}
	
	public static <T> Map<T,Integer> countingUnion(Stream<? extends Collection<T>> collections){
		Map<T,Integer> result = new HashMap<>();
		
		collections.forEach((collection) -> {
			for(T t : collection){
				result.put(t, result.containsKey(t) 
						? 1+result.get(t) 
						: 1 );
			}
		});
		
		return result;
	}
}
