package sudoku;

import sudoku.Puzzle.IndexValue;
import java.util.Set;
import java.util.function.Predicate;

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
	 * <p>The number of {@link Collection#size() size}-one (1) Facts a 
	 * Claim has when it's necessarily true (being the only Claim belonging 
	 * to that Fact) and evidently not presently being set true. After 
	 * {@link #setTrue() setTrue()} is called, it is possible for separate 
	 * neighboring Facts to independently become unary and to therefore 
	 * begin a process of falsifying the remaining Claims that this Claim 
	 * can see, which causes unecessary duplication of AutoResolve events in 
	 * the time-tree, as well as being aesthetically unpleasant.</p>
	 */
	public static final int UNARY_RULE_COUNT_FOR_SET_TRUE = 1;
	
	/**
	 * <p>Indicates whether the specified Claim is in the process of being 
	 * {@link Claim#setFalse(SolutionEvent) set false}. Outputs true if 
	 * and only if the specified Claim has a number of neighbors between 
	 * the typical initial {@value Claim#INIT_OWNER_COUNT} and 0, which 
	 * only occurs after the Claim has had some but not all of its Rule 
	 * neighbors removed while being set false.</p>
	 * 
	 * <p>It is possible for a Claim being set false to trigger one of its 
	 * (formerly) neighboring Rules to initiate a 
	 * {@link #TimeValueClaim value-claim} that targets the very same Claim 
	 * that's still in the process of being set false as one of the Claims 
	 * to be set false. Calling <tt>setFalse</tt> on such a Claim will create 
	 * a second Iterator, which will remove the remaining Rules from that 
	 * Claim, modifying the underlying collection. When that method call 
	 * and all others subordinate to the initial <tt>setFalse</tt> call on 
	 * the twice-falsified Claim return and control passes back to that 
	 * initial <tt>setFalse</tt>, the Iterator therein for that Claim will 
	 * still {@link Iterator#hasNext() have a next element} available, 
	 * even though the actual collection may be empty, resulting in a 
	 * ConcurrentModificationException the next time {@link Iterator#next() next()} 
	 * is called.</p>
	 * 
	 * <p>However, by filtering the Claims to be falsified in a value-claim 
	 * situation against this Predicate, only falsifying those that this 
	 * test says are not in the middle of being set false, multiple-
	 * falsification and the ConcurrentModificationException that comes with 
	 * it can be avoided.</p>
	 */
	public static final Predicate<Claim> CLAIM_IS_BEING_SET_FALSE = (claim) -> !claim.isEmpty() && claim.size() < Claim.INIT_OWNER_COUNT;
	
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
	 * then all of this Claim's neighbors have only one Claim: <tt>this</tt> one.</p>
	 * @return true if this Claim is known to be true, false otherwise
	 */
	public boolean isKnownTrue(){
		return stream().anyMatch((owner) -> owner.size()==Fact.SIZE_WHEN_SOLVED);
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
	boolean setTrue(SolutionEvent time){
		
		//Debug.log("Setting Claim true: "+this); //DEBUG
		
		Set<Claim> s = visibleClaims();
		int init = s.size();
		
		s.stream().filter(CLAIM_IS_BEING_SET_FALSE.negate()).forEach((c) -> c.setFalse(time));
		
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
	public boolean setFalse(SolutionEvent time){
		
		//Debug.log("Setting Claim false: "+this); //DEBUG
		
		int initSize = size();
		clear(time);
		return size() != initSize;
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
	 * it has been modified.</p>
	 */
	@Override
	protected void validateFinalState(SolutionEvent time){ //TO DO make sure this doesn't break Rule-class operations still in-progress
		//do nothing
	}
	
	/**
	 * <p>Returns a set of the Claims visible to this Claim. A Claim is 
	 * visible to this Claim if that Claim and this one share at least 
	 * one Rule in common as elements.</p>
	 * @return a set of the Claims that share at least one Claim with 
	 * this Claim
	 */
	public Set<Claim> visibleClaims(){
		Set<Claim> neighborsOfNeighbors = Sledgehammer.sideEffectUnion(this,false);
		neighborsOfNeighbors.remove(this);
		return neighborsOfNeighbors;
	}
}