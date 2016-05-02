package sudoku;

import java.util.Collection;
import java.util.function.Function;

public class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;
	
	public static final Function<Claim,Boolean> CLAIM_IS_TRUE_NOT_YET_SET_TRUE = 
			(c) -> c.stream().filter((f) -> f.size() == SIZE_WHEN_SOLVED)
			.count() == Claim.UNARY_RULE_COUNT_FOR_SET_TRUE;
	
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
}
