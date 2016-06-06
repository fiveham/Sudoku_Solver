package sudoku;

import common.ToolSet;
import java.util.Set;
import sudoku.Puzzle.IndexValue;
import sudoku.technique.SledgeHeur;
import sudoku.time.FalsifiedTime;

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
	
	private IndexValue x;
	private IndexValue y;
	private IndexValue symbol;
	
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
		super(puzzle, INIT_OWNER_COUNT);
		this.x = x;
		this.y = y;
		this.symbol = symbol;
		this.hashCode = linearizeCoords(x.intValue() ,y.intValue() ,symbol.intValue() , puzzle.sideLength());
	}
	
	/**
	 * <p>Returns true if this Claim is known to be false, false otherwise. 
	 * A Claim is known to be false if it has been removed from any of its 
	 * Rules as a neighbor. Because of the enforced symmetry of connections 
	 * between NodeSets and their elements/neighbors and because Claims 
	 * automatically remove all their neighbors when they detect that they've 
	 * been disconnected by one of their neighbors, a Claim being known false 
	 * is equivalent to that Claim having no neighbors at all. As such, a 
	 * call to this method is equivalent to a call to {@link Set#isEmpty() isEmpty()}.</p>
	 * @return true if this Claim is known to be false, 
	 * false otherwise
	 */
	public boolean isKnownFalse(){
		return isEmpty();
	}
	
	/**
	 * <p>Returns true if this Claim is known to be true, false otherwise. 
	 * If this Claim is known true and there aren't any Rules that still need 
	 * to {@link Rule#verifyFinalState() collapse automatically} in the target, 
	 * then all of this Claim's neighbors have only one Claim: {@code this} one.</p>
	 * @return true if this Claim is known to be true, false otherwise
	 */
	public boolean isKnownTrue(){
		return stream().anyMatch(Fact::isSolved);
	}
	
	/**
	 * <p>Returns a single character that represents this Claim in a 
	 * pedantic description of the state of the target.</p>
	 * @return a space if this Claim is known false, the human readable 
	 * text for this Claim's symbol otherwise
	 */
	String possText(){
		return isKnownFalse() ? " " : symbol.humanReadableSymbol();
	}
	
	/**
	 * <p>Sets this Claim true in its Puzzle. Sets all the Claims 
	 * visible to this Claim false. Merges all the 
	 * Rule neighbors of this Claim into one another, removing 
	 * three of those Rules from the target as objects. Adds a 
	 * TimeSetTrue onto the target's time stack.</p>
	 * 
	 * @return true if calling this method changed the state of 
	 * this Claim, false otherwise
	 */
	boolean setTrue(FalsifiedTime time){
		Set<Claim> s = visibleClaims();
		int init = s.size();
		
		if(!setTrueInProgress){
			setTrueInProgress = true;
			
			s.stream().forEach((c) -> c.setFalse(time));
			
			setTrueInProgress = false;
		} else{
			throw new IllegalStateException("Cannot set Claim true while setting the same Claim true.");
		}
		
		return init != visibleClaims().size();
	}
	
	/**
	 * <p>Sets this Claim false. Removes all elements from this 
	 * set, and removes this Claim from all its neighbors.</p>
	 * @param time the SolutionEvent which precipitated the call 
	 * to this method
	 * @return true if calling this method changed the state of 
	 * this Claim, false otherwise
	 */
	public boolean setFalse(FalsifiedTime time){
		int initSize = size();
		if(!setFalseInProgress()){
			clear(time);
		}
		return size() != initSize;
	}
	
	private boolean setFalseInProgress(){
		return !isEmpty() && size() < INIT_OWNER_COUNT;
	}
	
	private boolean setTrueInProgress = false;
	
	public boolean setTrueInProgress(){
		return setTrueInProgress;
	}
	
	/**
	 * <p>Returns the int value of this Claim's x-coordinate.</p>
	 * @return the int value of this Claim's x-coordinate
	 */
	public int getX(){
		return x.intValue();
	}
	
	/**
	 * <p>Returns the int value of this Claim's y-coordinate.</p>
	 * @return the int value of this Claim's y-coordinate
	 */
	public int getY(){
		return y.intValue();
	}
	
	/**
	 * <p>Returns the int value of this Claim's symbol.</p>
	 * @return the int value of this Claim's symbol
	 */
	public int getZ(){
		return symbol.intValue();
	}
	
	/**
	 * <p>Returns an int that encodes the specified x, y, and z coordinates as if they 
	 * belong to a Claim whose target has the specified {@code sideLength}.</p>
	 * 
	 * <p>The coordinates are concatenated as digits in a number system with a base 
	 * equal to the specified {@code sideLength}, with the first digit being the 
	 * x-coordinate, followed by the y-coordinate, followed by the z-coordinate.</p>
	 * @param x the coordinate given the highest significance
	 * @param y the coordinate given the second-highest significance
	 * @param z the coordinate given the least significance.
	 * @param sideLength the side-length of the target to which belongs the Claim 
	 * whose coordinates are being linearized.
	 * @return an int that encodes the specified x, y, and z coordinates as if they 
	 * belong to a Claim whose target has the specified {@code sideLength}
	 */
	public static int linearizeCoords(int x, int y, int z, int sideLength){
		return x*sideLength*sideLength + y*sideLength + z;
	}
	
	@Override
	public String toString(){
		return "Claim: cell "+x.humanReadableIntValue()+","+y.humanReadableIntValue()+" is "+symbol.humanReadableIntValue();
	}
	
	private final int hashCode;
	
	@Override
	public int hashCode(){
		return hashCode;
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
	public double spaceDistTo(Claim otherClaim){
		int x = this.x.intValue();
		int y = this.y.intValue();
		int z = this.symbol.intValue();
		int cx = otherClaim.x.intValue();
		int cy = otherClaim.y.intValue();
		int cz = otherClaim.symbol.intValue();
		return Math.sqrt( (x-cx)*(x-cx) + (y-cy)*(y-cy) + (z-cz)*(z-cz) );
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
	
	/**
	 * <p>Returns a set of the Claims visible to this Claim. A Claim is 
	 * visible to this Claim if that Claim and this one share at least 
	 * one Rule in common as elements.</p>
	 * @return a set of the Claims that share at least one Claim with 
	 * this Claim
	 */
	public ToolSet<Claim> visibleClaims(){
		ToolSet<Claim> neighborsOfNeighbors = new ToolSet<>(SledgeHeur.massUnion(this));
		neighborsOfNeighbors.remove(this);
		return neighborsOfNeighbors;
	}
}
