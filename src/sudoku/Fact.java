package sudoku;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

public class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * <p>Returns true if and only if the specified {@code NodeSet<?,?>} is 
	 * a {@code Rule}.</p>
	 */
	public static final Predicate<NodeSet<?,?>> IS_FACT  = (ns)->ns instanceof Fact;
	
	public static final Function<NodeSet<?,?>,Fact> AS_FACT  = (ns)->(Fact) ns;
	
	public static final Predicate<Fact> IS_XOR = (f) -> f.size() == Rule.SIZE_WHEN_XOR;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 324978329635129743L;
	
	public Fact(Puzzle puzzle){
		super(puzzle);
	}
	
	public Fact(Puzzle puzzle, Collection<Claim> c) {
		super(puzzle, c);
	}
	
	public Fact(Puzzle puzzle, int initialCapacity) {
		super(puzzle, initialCapacity);
	}
	
	public Fact(Puzzle puzzle, int initialCapacity, float loadFactor) {
		super(puzzle, initialCapacity, loadFactor);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Fact){
			Fact r = (Fact) o;
			return r.puzzle.equals(puzzle) && super.equals(r);
		}
		return false;
	}
	
	public boolean isSolved(){
		return size() == SIZE_WHEN_SOLVED;
	}
}
