package sudoku;

import java.util.Collection;

public class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 324978329635129743L;
	
	/* *
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;

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
	
	/* *
	 * <p>Collapses this set down to be equal to <tt>src</tt>, 
	 * while ensuring that all released Claims are freed from 
	 * all other Rules.</p>
	 * 
	 * <p><tt>src</tt>'s <tt>types</tt> are moved from <tt>src</tt> 
	 * into <tt>this</tt> and <tt>src</tt> is 
	 * {@link Puzzle#removeNode(NodeSet) removed} from the target.</p>
	 * 
	 * @param src a Rule smaller than this Rule, which triggers 
	 * the merge event by being a proper subset of <tt>this</tt> and 
	 * which will be {@link Puzzle#removeNode(NodeSet) removed} from 
	 * the target
	 * 
	 * @return true if the target was changed by calling this 
	 * method, false otherwise
	 */
	boolean merge(SolutionEvent time, Fact src){
		boolean result = retainAll(time, src);
		
		mergeDetails(src);
		
		src.clear(time);
		puzzle.removeNode(src);
		return result;
	}
	
	void mergeDetails(Fact src){
		//details to be specified by subclasses
	}
	
	public byte getType(){
		return 0;
	}
}
