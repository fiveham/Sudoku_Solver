package sudoku;

import java.util.Collection;

public class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule that 
	 * is a {@code xor}.  A Rule is a {@code xor} if it has two Claims, 
	 * because exactly one of them is true and the other is false. Given 
	 * that we know that the Rule is satisfied (is {@code true}), the 
	 * Claims, as inputs to the Rule, make such a Rule a {@code xor} 
	 * operation on its neighbors.</p>
	 */
	private static final int SIZE_WHEN_XOR = 2;
	
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
	
	public boolean isXor(){
		return size() == SIZE_WHEN_XOR;
	}
}
