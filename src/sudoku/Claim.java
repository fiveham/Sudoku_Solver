package sudoku;

import sudoku.Puzzle.IndexValue;
import java.util.Iterator;
import java.util.Set;

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
	private static final long serialVersionUID = 1575574810729080115L;
	
	/**
	 * <p>The initial number of {@link #neighbors() Rules to which this Claim belongs}. 
	 * It is the count of the kinds of FactBags that can hold the same 
	 * Claim in common: a Rule for a cell, a box, a column, and a row.</p>
	 */
	public static final int INIT_OWNER_COUNT = Puzzle.RegionSpecies.values().length;
	
	private IndexValue x;
	private IndexValue y;
	private IndexValue symbol;
	
	/**
	 * <p>Constructs a Claim at the specified <tt>x</tt>, <tt>y</tt>, and 
	 * <tt>symbol</tt> coordinates, pertaining to the specified <tt>target</tt>.</p>
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
		this.hashCode = linearizeCoords();
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
	 * then all of this Claim's neighbors have only one Claim: <tt>this</tt> one.</p>
	 * @return true if this Claim is known to be true, false otherwise
	 */
	public boolean isKnownTrue(){
		/*for(Fact owner : this){
			if(owner.size() == Rule.SIZE_WHEN_SOLVED){
				return true;
			}
		}
		return false;*/
		return stream().anyMatch((owner)->owner.size()==Fact.SIZE_WHEN_SOLVED);
	}
	
	/**
	 * <p>Returns a single character that represents this Claim in a 
	 * pedantic description of the state of the target.</p>
	 * @return a space if this Claim is known false, the human readable 
	 * text for this Claim's symbol otherwise
	 */
	public String possText(){
		return isKnownFalse() ? " " : symbol.humanReadableSymbol();
	}
	
	/**
	 * <p>Sets this Claim true in its Puzzle. Sets all the Claims 
	 * visible to this Claim false. Merges all the 
	 * Rule neighbors of this Claim into one another, removing 
	 * three of those Rules from the target as objects. Adds a 
	 * TimeSetTrue onto the target's time stack.</p>
	 * @return true if calling this method changed the state of 
	 * this Claim, false otherwise
	 */
	boolean setTrue(SolutionEvent time){
		boolean result = false;
		
		Iterator<Fact> i = iterator();
		Fact f1 = i.next();
		while(i.hasNext()){
			result |= f1.merge(time, i.next());
		}
		
		return result;
	}
	
	/* *
	 * <p>A time node uniting the events that occur in the process of 
	 * setting this Claim true.</p>
	 * @author fiveham
	 *
	 */
	/*public class TimeSetTrue extends AbstractTime{
		private final Claim verified;
		private TimeSetTrue(){
			super(target.timeBuilder().top());
			this.verified = Claim.this;
		}
		public Claim seed(){
			return verified;
		}
	}*/
	
	/* *
	 * <p>Sets this Claim false. Removes all elements from this 
	 * set, and removes this Claim from all its neighbors. Adds 
	 * a TimeSetFalse to the target's time stack.</p>
	 * @return true if calling this method changed the state of 
	 * this Claim, false otherwise
	 */
	public boolean setFalse(SolutionEvent time){
		int initSize = size();
		
		clear(time);
		puzzle.removeNode(this);
		
		return size() != initSize;
	}
	
	/* *
	 * <p>A time node that specifies the event of this Claim being 
	 * set false.</p>
	 * @author fiveham
	 *
	 */
	/*public class TimeSetFalse extends AbstractTime{
		private final Claim setFalse;
		private TimeSetFalse(Claim falseClaim){
			super(target.timeBuilder().top());
			this.setFalse = falseClaim;
		}
		public Claim setFalse(){
			return setFalse;
		}
	}*/
	
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
	 * <p>Returns an int that encodes the x, y, and z coordinates of this Claim.</p>
	 * @see Claim#linearizeCoords(int, int, int, int)
	 * @return an int that encodes the x, y, and z coordinates of this Claim
	 */
	public int linearizeCoords(){
		return linearizeCoords(x.intValue(), y.intValue(), symbol.intValue(), puzzle.sideLength());
	}
	
	/**
	 * <p>Returns an int that encodes the specified x, y, and z coordinates as if they 
	 * belong to a Claim whose target has the specified <tt>sideLength</tt>.</p>
	 * 
	 * <p>The coordinates are concatenated as digits in a number system with a base 
	 * equal to the specified <tt>sideLength</tt>, with the first digit being the 
	 * x-coordinate, followed by the y-coordinate, followed by the z-coordinate.</p>
	 * @param x the coordinate given the highest significance
	 * @param y the coordinate given the second-highest significance
	 * @param z the coordinate given the least significance.
	 * @param sideLength the side-length of the target to which belongs the Claim 
	 * whose coordinates are being linearized.
	 * @return an int that encodes the specified x, y, and z coordinates as if they 
	 * belong to a Claim whose target has the specified <tt>sideLength</tt>
	 */
	public static int linearizeCoords(int x, int y, int z, int sideLength){
		return x*sideLength*sideLength + y*sideLength + z;
	}
	
	@Override
	public String toString(){
		return "Claim: cell "+x.intValue()+","+y.intValue()+" is "+symbol.intValue();
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
	 * <p>Returns the distance between this Claim and <tt>otherClaim</tt> 
	 * in claim-space.</p>
	 * @param otherClaim
	 * @return the distance between this Claim and <tt>otherClaim</tt> 
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
	 * <p>Returns a vector in claim-space from this Claim to <tt>cn</tt>.</p>
	 * @param cn another Claim
	 * @return a vector in claim-space from this Claim 
	 * to <tt>cn</tt>
	 */
	public int[] vectorTo(Claim cn) {
		int[] result = new int[Puzzle.DIMENSION_COUNT];
		result[0] = cn.x.intValue() - x.intValue();
		result[1] = cn.y.intValue() - y.intValue();
		result[2] = cn.symbol.intValue() - symbol.intValue();
		return result;
	}
	
	/**
	 * <p>Verifies that this Claim's state is acceptable after 
	 * it has been modified. Automatically sets this claim 
	 * false when a Rule is removed from this Claim without 
	 * accommodating the Rule's role via another remaining Rule.</p>
	 */
	@Override
	protected void validateFinalState(SolutionEvent time){ //TODO make sure this doesn't break Rule-class operations still in-progress
		if(!isEmpty() && !hasAllRules()){
			setFalse(time);
		}
	}
	
	/**
	 * <p>Returns a set of the Claims visible to this Claim. A Claim is 
	 * visible to this Claim if that Claim and this one share at least 
	 * one Rule in common as elements.</p>
	 * @return a set of the Claims that share at least one Claim with 
	 * this Claim
	 */
	public Set<Claim> visibleClaims(){
		Set<Claim> neighborsOfNeighbors = SledgeHammer2.sideEffectUnion(this,false);
		neighborsOfNeighbors.remove(this);
		return neighborsOfNeighbors;
	}
	
	/**
	 * <p>Returns true if this Claim's neighbors fulfill all the 
	 * Rule-type roles that a Claim has at initialization and that 
	 * any true Claim will have when the target is completely solved.</p>
	 * @return true if this Claim's neighbors collectively account 
	 * for all the Rule types, false otherwise
	 */
	private boolean hasAllRules(){
		byte mask = 0;
		for(Fact rule : this){
			mask |= rule.getType();
		}
		return mask == Puzzle.RegionSpecies.ALL_TYPES;
	}
}
