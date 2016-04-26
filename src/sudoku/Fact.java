package sudoku;

import java.util.Collection;

public class Fact extends NodeSet<Claim,Fact>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 324978329635129743L;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule when 
	 * the Claim satisfying the Rule has been completely 
	 * identified.</p>
	 */
	public static final int SIZE_WHEN_SOLVED = 1;
	
	/**
	 * <p>The {@link Puzzle.RegionSpecies types} of this Rule.</p>
	 * 
	 * <p>Initially, a Rule has exactly one type, but as the target 
	 * gets solved, eventually Rules must merge, moving the <tt>types</tt> 
	 * of Rules that are merged into other Rules (and are removed from the 
	 * target) into the <tt>types</tt> of the Rules into which 
	 * such mergers occur.</p>
	 * 
	 * <p>Merging <tt>types</tt> lists in this fashion represents the 
	 * merging of roles expressed in statements about a solution of a 
	 * sudoku target such as "The 5 in row 1 is the 5 in box 1".  One 
	 * of these Rule objects receives the <tt>types</tt> of the other 
	 * to mark that it is both "the 5 in row 1" and "the 5 in box 1".</p>
	 * 
	 * <p>Claims rely on the availability of <tt>types</tt> from their 
	 * Rule neighbors to determine whether they need to set themselves 
	 * false due to removal from a Rule or they've been removed from a 
	 * Rule that's actually being removed from the target as part of 
	 * redundancy-elimination in  which case the removed Claims don't 
	 * need to set themselves false.</p>
	 */
	protected byte types;
	
	public Fact(Puzzle puzzle, byte types){
		super(puzzle);
		this.types = types;
	}
	
	public Fact(Puzzle puzzle, byte types, Collection<Claim> c) {
		super(puzzle, c);
		this.types = types;
	}
	
	public Fact(Puzzle puzzle, byte types, int initialCapacity) {
		super(puzzle, initialCapacity);
		this.types = types;
	}
	
	public Fact(Puzzle puzzle, byte types, int initialCapacity, float loadFactor) {
		super(puzzle, initialCapacity, loadFactor);
		this.types = types;
	}
	
	public byte getType(){
		return types;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Fact){
			Fact r = (Fact) o;
			return r.puzzle.equals(puzzle) && r.types == types && super.equals(r);
		}
		return false;
	}
}
