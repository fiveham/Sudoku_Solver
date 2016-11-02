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
		
		class Intersection<Z> extends HashSet<Z>{
			
			/**
			 * 
			 */
			private static final long serialVersionUID = -1768519565525064860L;
			
			private boolean used = false;
			
			public Intersection(){
				super();
			}
			
			public Intersection(Collection<? extends Z> coll){
				super(coll);
			}
			
			public Intersection<Z> intersect(Collection<? extends Z> coll){
				if(used){
					retainAll(coll);
				}else{
					addAll(coll);
					used = true;
				}
				return this;
			}
		}
		
		return Collector.of(
				Intersection<E>::new, 
				Intersection::intersect, 
				Intersection::intersect, 
				Intersection<E>::new, 
				Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
	}
	
	public static <T extends Collection<E>, E, Z extends Set<E>> Collector<T,?,Z> massIntersectionCollector(Supplier<Z> supplier){
		
		class Intersection{
			
			private boolean used = false;
			private Z set = supplier.get();
			
			public void intersect(Collection<E> coll){
				if(used){
					set.retainAll(coll);
				}else{
					set.addAll(coll);
					used = true;
				}
			}
			
			public Intersection combineWith(Intersection other){
				intersect(other.set);
				return this;
			}
			
			public Z unpack(){
				return set;
			}
		}
		
		return Collector.of(
				Intersection::new, 
				Intersection::intersect, 
				Intersection::combineWith, 
				Intersection::unpack, 
				Characteristics.UNORDERED);
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
