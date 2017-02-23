package sudoku;

import sudoku.Puzzle.IndexValue;

/**
 * <p>Represents a claim that "Cell x,y has value z."</p>
 * <p>This is implemented as a set of the Rules that have this Claim as an element.</p>
 * @author fiveham
 */
public class Claim extends NodeSet<Fact, Claim>{
	
	private static final long serialVersionUID = -2402719833037606449L;
	
  /**
   * <p>The initial number of {@link #neighbors() Rules to which this Claim belongs}. It is the
   * number of {@link Puzzle.RuleType types} of Rules that can hold the same Claim in common: a 
   * cell, a box, a column, and a row.</p>
   */
	private static final int INIT_OWNER_COUNT = Puzzle.RuleType.values().length;
	
	private final IndexValue x;
	private final IndexValue y;
	private final IndexValue z;
	
  /**
   * <p>Constructs a Claim at the specified {@code x}, {@code y}, and {@code z} coordinates, as a 
   * part of the specified {@code puzzle}.</p>
   * @param target the {@link Puzzle Puzzle} of whose underlying {@link common.graph.Graph Graph}
   * this Claim is a {@link common.graph.Vertex vertex}
   * @param x the x-coordinate of this Claim in claim-space
   * @param y the y-coordinate of this Claim in claim-space
   * @param z the z-coordinate of this Claim in claim-space
   */
	public Claim(Puzzle puzzle, IndexValue x, IndexValue y, IndexValue z) {
		super(puzzle, INIT_OWNER_COUNT, linearizeCoords(x.intValue(), y.intValue(), z.intValue(), puzzle.sideLength()));
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
  /**
   * <p>Sets this Claim false. Removes all elements from this set, and removes this Claim from all
   * its neighbors.</p>
   * @return true if calling this method changed the state of this Claim, false otherwise
   */
	public boolean setFalse(){
		int initSize = size();
		clear();
		return size() != initSize;
	}
	
	/**
   * <p>Returns the x-coordinate of this Claim in claim-space.</p>
   * @return the x-coordinate of this Claim in claim-space
   */
	public IndexValue getX(){
		return x;
	}
	
	/**
	 * <p>Returns the y-coordinate of this Claim in claim-space.</p>
	 * @return the y-coordinate of this Claim in claim-space
	 */
	public IndexValue getY(){
		return y;
	}
  
  /**
   * <p>Returns the z-coordinate of this Claim in claim-space.</p>
   * @return the z-coordinate of this Claim in claim-space
   */
	public IndexValue getZ(){
		return z;
	}
	
	@Override
	public String toString(){
		return "Claim: cell " + x.humanReadableIntValue() 
		    + "," + y.humanReadableIntValue() 
		    + " is " + z.humanReadableIntValue();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Claim){
			Claim c = (Claim) o;
			return c.x == x && c.y == y && c.z == z && c.puzzle == puzzle;
		}
		return false;
	}
	
  /**
   * <p>Returns the distance between this Claim and {@code otherClaim} in claim-space.</p>
   * @param otherClaim another Claim
   * @return the distance between this Claim and {@code otherClaim} in claim-space
   */
	public double spaceDist(Claim otherClaim){
		int dx = this.x.intValue() - otherClaim.x.intValue();
		int dy = this.y.intValue() - otherClaim.y.intValue();
		int dz = this.z.intValue() - otherClaim.z.intValue();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
