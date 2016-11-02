package common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

public class Sets {
	
	public static <T extends Collection<E>, E> Collector<T,?,Set<E>> massIntersectionCollector(){
		return massIntersectionCollector(HashSet<E>::new);
	}
	
	public static <T extends Collection<E>, E, Z extends Set<E>> Collector<T,?,Z> massIntersectionCollector(Supplier<Z> supplier){
		return Collector.of(
				() -> new Intersection<Z,E>(supplier), 
				Intersection::intersect, 
				Intersection::combineWith, 
				Intersection::unpack, 
				Characteristics.UNORDERED);
	}
	
	private static class Intersection<X extends Set<E>, E>{
		
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
	 * <p>Unions all the collections in {@code srcCombo} into one set and returns 
	 * that set, unless some elements are shared among the collections in 
	 * srcCombo, in which case, if {@code nullIfNotDisjoint} is true, null is 
	 * returned instead.</p>
	 * @param <E> The type of the elements of the Set produced
	 * @param <C> The type of the elements of {@code collections} which contain 
	 * elements of type {@code E}.
	 * @param collections a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @param nullIfNotDisjoint controls whether an intersection among the elements 
	 * of {@code srcCombo} results in {@code null} being returned.
	 * @return {@code null} if {@code nullIfNotDisjoint} is {@code true} and 
	 * some of the elements of {@code srcCombo} intersect each other, or otherwise 
	 * the mass-union of all the elements of {@code srcCombo}.
	 */
	public static <E, C extends Collection<E>> Set<E> massUnion(Collection<C> collections){
		return collections.stream().collect(massUnionCollector());
	}
	
	public static <S, T extends Collection<S>> Collector<T,?,Set<S>> massUnionCollector(){
		return Collector.of(
				HashSet<S>::new, 
				Set::addAll, 
				Sets::mergeCollections);
	}
	
	public static <S, T extends Collection<S>> Collector<T,?,Set<S>> massUnionCollector(Supplier<Set<S>> supplier){
		return Collector.of(
				supplier, 
				Set::addAll, 
				Sets::mergeCollections);
	}
	
	/**
	 * <p>Adds {@code c2} to {@code c1} and returns {@code c1}. This is intended to 
	 * be used as a {@link java.util.stream.Collector#combiner() combiner} for a 
	 * {@link java.util.stream.Collector Collector}.</p>
	 * @return {@code c1} after the contents of {@code c2} are added to it
	 */
	public static <E, C extends Collection<E>> C mergeCollections(C c1, C c2){
		c1.addAll(c2);
		return c1;
	}
	
	public static <T> Map<T,Integer> countingUnion(Collection<? extends Collection<T>> collections){
		Map<T,Integer> result = new HashMap<>();
		
		for(Collection<T> collection : collections){
			for(T t : collection){
				result.put(t, result.containsKey(t) 
						? 1+result.get(t) 
						: 1 );
			}
		}
		
		return result;
	}
}
