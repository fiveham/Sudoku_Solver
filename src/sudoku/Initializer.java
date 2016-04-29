package sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * <p>This Technique finds an Init in an unsolved Sudoku graph and 
 * sets that Init's attached Claim true.</p>
 * 
 * <p>This technique is built as a formal Technique so that direct 
 * and automatic changes to a Puzzle pertaining to the specification 
 * of a puzzle's initial values can be incorporated into the time 
 * system without changing the time system itself.</p>
 * @author fiveham
 *
 */
public class Initializer extends Technique {
	
	/**
	 * <p>Constructs an Initializer that works to solve the 
	 * specified Sudoku.</p>
	 * @param puzzle the Sudoku that this Initializer works 
	 * to solve
	 */
	public Initializer(Sudoku puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Finds an Init in <tt>target</tt> and sets that Init's 
	 * one Claim neighbor true.</p>
	 * @return an Initialization describing the verification of 
	 * an Init's sole Claim neighbor and any and all resulting 
	 * automatic resolution events, or null if no Init is found
	 */
	@Override
	protected SolutionEvent process(){
		Init i = target.factStream().collect(new InitCollector());
		if(i != null){
			SolutionEvent result = new Initialization(i);
			i.validateFinalState(result);
			return result;
		}
		return null;
	}
	
	/**
	 * <p>A SolutionEvent describing the verification of a Claim 
	 * known to be true because the initial state of the puzzle 
	 * specifies that that cell has that value.</p>
	 * @author fiveham
	 *
	 */
	public static class Initialization extends SolutionEvent{
		private Initialization(Init init){
			super(init.claim().visibleClaims());
		}
	}
	
	private static class InitCollector implements Collector<Fact, InitCollector.Box, Init>{
		
		@Override
		public Supplier<InitCollector.Box> supplier() {
			return Box::new;
		}

		@Override
		public BiConsumer<InitCollector.Box, Fact> accumulator() {
			return (acc, fact) -> {
				if(fact instanceof Init){
					acc.set((Init)fact);
				}
			};
		}

		@Override
		public BinaryOperator<InitCollector.Box> combiner() {
			return (a1,a2) -> a1.contains() ? a1 : a2;
		}

		@Override
		public Function<InitCollector.Box, Init> finisher() {
			return (box) -> box.value;
		}
		
		private static class Box{
			Init value = null;
			boolean contains(){
				return value != null;
			}
			void set(Init i){
				if(value == null){
					value = i;
				}
			}
		}
		
		private static final Set<Collector.Characteristics> characteristics = new CharacteristicSet(Collector.Characteristics.UNORDERED);
		
		@Override
		public Set<Collector.Characteristics> characteristics() {
			return characteristics;
		}
		
		private static class CharacteristicSet extends HashSet<Collector.Characteristics>{
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 3746307781250833916L;

			CharacteristicSet(Collector.Characteristics... characteristics){
				super(characteristics.length);
				for(Collector.Characteristics c : characteristics){
					super.add(c);
				}
			}
			
			@Override
			public Iterator<Characteristics> iterator() {
				class NoRemoveIterator implements Iterator<Collector.Characteristics>{
					
					final Iterator<Collector.Characteristics> wrappedIterator;
					
					NoRemoveIterator(Iterator<Collector.Characteristics> wrappedIterator){
						this.wrappedIterator = wrappedIterator;
					}
					
					@Override
					public boolean hasNext(){
						return wrappedIterator.hasNext();
					}
					
					@Override
					public Collector.Characteristics next(){
						return wrappedIterator.next();
					}
				}
				return new NoRemoveIterator(super.iterator());
			}

			@Override
			public boolean add(Characteristics e) {
				return false;
			}

			@Override
			public boolean remove(Object o) {
				return false;
			}
			
			@Override
			public boolean addAll(Collection<? extends Characteristics> c) {
				return false;
			}
			
			@Override
			public boolean retainAll(Collection<?> c) {
				return false;
			}
			
			@Override
			public boolean removeAll(Collection<?> c) {
				return false;
			}
			
			@Override
			public void clear() {
				throw new UnsupportedOperationException();
			}
		}
		
	}
}
