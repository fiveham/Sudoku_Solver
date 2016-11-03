package sudoku;

import sudoku.Puzzle.IndexValue;

/**
 * <p>Represents a claim that "Cell x,y has value z."</p>
 * 
 * <p>This is implemented as a set of the Rules that have this 
 * Claim as an element.</p>
 * 
 * @author fiveham
 *
 */
public class Claim extends NodeSet<Fact,Claim>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2402719833037606449L;
	
	/**
	 * <p>The initial number of {@link #neighbors() Rules to which this Claim belongs}. 
	 * It is the count of the kinds of FactBags that can hold the same 
	 * Claim in common: a Rule for a cell, a box, a column, and a row.</p>
	 */
	public static final int INIT_OWNER_COUNT = Puzzle.RuleType.values().length;
	
	private final IndexValue x;
	private final IndexValue y;
	private final IndexValue symbol;
	
	/**
	 * <p>Constructs a Claim at the specified {@code x}, {@code y}, and 
	 * {@code symbol} coordinates, pertaining to the specified {@code target}.</p>
	 * @param target the {@link Puzzle Puzzle} of whose underlying 
	 * {@link common.graph.Graph Graph} this Claim is a 
	 * {@link common.graph.Vertex vertex}
	 * @param x the x-coordinate of this Claim in claim-space
	 * @param y the y-coordinate of this Claim in claim-space
	 * @param symbol the z-coordinate of this Claim in claim-space
	 */
	public Claim(Puzzle puzzle, IndexValue x, IndexValue y, IndexValue symbol) {
		super(puzzle, INIT_OWNER_COUNT, linearizeCoords(x.intValue(), y.intValue(), symbol.intValue(), puzzle.sideLength()));
		this.x = x;
		this.y = y;
		this.symbol = symbol;
	}
	
	/**
	 * <p>Returns true if this Claim has been set false and all its internal 
	 * properties correspond to that fact, false otherwise. No guarantee is 
	 * made that formerly connected Facts reflect this Claim's falsehood at 
	 * the time this method is called; they may still contain a reference to 
	 * this Claim as a {@link #neighbors() neighbor}.</p>
	 * @return true if this Claim is known to be false, 
	 * false otherwise
	 */
	public boolean isSetFalse(){
		return isEmpty();
	}
	
	/**
	 * <p>Returns true if this Claim has been set true and all its internal 
	 * properties correspond to that fact, false otherwise.</p>
	 * @return true if this Claim has been set true and all its internal 
	 * properties correspond to that fact, false otherwise
	 */
	public boolean isSetTrue(){
		return stream().allMatch(Fact::isSolved);
	}
	
	/**
	 * <p>Sets this Claim false. Removes all elements from this 
	 * set, and removes this Claim from all its neighbors.</p>
	 * @param time the TechniqueEvent which precipitated the call 
	 * to this method
	 * @return true if calling this method changed the state of 
	 * this Claim, false otherwise
	 */
	public boolean setFalse(){
		int initSize = size();
		clear();
		return size() != initSize;
	}
	
	public IndexValue getX(){
		return x;
	}
	
	public IndexValue getY(){
		return y;
	}
	
	public IndexValue getSymbol(){
		return symbol;
	}
	
	@Override
	public String toString(){
		return "Claim: cell "+x.humanReadableIntValue()+","+y.humanReadableIntValue()+" is "+symbol.humanReadableIntValue();
	}
	
	@Override
	public boolean equals(Object o){
		if( o instanceof Claim){
			Claim c = (Claim) o;
			return c.x == x && c.y == y && c.symbol == symbol && c.puzzle == puzzle;
		}
		return false;
	}
	
	/**
	 * <p>Returns the distance between this Claim and {@code otherClaim} 
	 * in claim-space.</p>
	 * @param otherClaim
	 * @return the distance between this Claim and {@code otherClaim} 
	 * in claim-space
	 */
	public double spaceDist(Claim otherClaim){
		int dx = this.x.intValue() - otherClaim.x.intValue();
		int dy = this.y.intValue() - otherClaim.y.intValue();
		int dz = this.symbol.intValue() - otherClaim.symbol.intValue();
		return Math.sqrt( dx*dx + dy*dy + dz*dz );
	}
	
	/**
	 * <p>Returns a vector in claim-space from this Claim to {@code cn}.</p>
	 * @param cn another Claim
	 * @return a vector in claim-space from this Claim 
	 * to {@code cn}
	 */
	public int[] vectorTo(Claim cn) {
		int[] result = new int[Puzzle.DIMENSION_COUNT];
		result[Puzzle.X_DIM] = cn.x.intValue() - x.intValue();
		result[Puzzle.Y_DIM] = cn.y.intValue() - y.intValue();
		result[Puzzle.Z_DIM] = cn.symbol.intValue() - symbol.intValue();
		return result;
	}
}
