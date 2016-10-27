package sudoku;

import java.util.Collection;

public abstract class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3547362110024521237L;

	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule that 
	 * is a {@code xor}. A Rule is a {@code xor} if it has two Claims, 
	 * because exactly one of them is true and the other is false. Given 
	 * that the Rule is satisfied (the Rule is {@code true}), the Claims, 
	 * as inputs to the Rule, make such a Rule a {@code xor} operation on 
	 * its neighbors.</p>
	 */
	public static final int SIZE_WHEN_XOR = 2;
	
	protected Fact(Puzzle puzzle, Collection<Claim> c, int hash) {
		super(puzzle, hash);
		addAll(c);
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Fact){
			Fact r = (Fact) o;
			return r.puzzle.equals(puzzle) && super.equals(r);
		}
		return false;
	}
	
	public boolean insideVisible(){
		return visible().stream().anyMatch((v) -> v.containsAll(this));
	}
	
	public boolean isSolved(){
		return size() == SIZE_WHEN_SOLVED;
	}
	
	public boolean isXor(){
		return size() == SIZE_WHEN_XOR;
	}
}
