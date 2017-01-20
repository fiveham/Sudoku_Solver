package sudoku;

import java.util.Collection;

public abstract class Fact extends NodeSet<Claim,Fact>{
	
    /**
     */
	private static final long serialVersionUID = -3547362110024521237L;

    /**
     * <p>The number ({@value}) of elements (neighbors) of a Rule when the Claim satisfying the Rule
     * has been completely identified.</p>
     */
	public static final int TRUE_CLAIM_COUNT = 1;
	
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
	
	public boolean isSolved(){
		return size() == TRUE_CLAIM_COUNT;
	}
}
