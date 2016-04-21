package sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Represents a claim that "such-and-such cell 
 * contains such-and-such symbol."</p>
 * @author fiveham
 *
 */
public class Claim {
	
	/**
	 * The initial size for the {@link #owners owner list}. It is 
	 * the count of the kinds of FactBags that can hold the same 
	 * Claim in common; that is FactBags for: a Cell; a Row; a Column, 
	 * and; a Box.
	 */
	public static final int INIT_OWNER_COUNT = 4;
	
	private Puzzle.IndexValue x;
	private Puzzle.IndexValue y;
	private Puzzle.IndexValue symbol;
	
	private List<FactBag> owners;
	
	private Puzzle puzzle;
	
	public Claim(Puzzle puzzle, Puzzle.IndexValue x, Puzzle.IndexValue y, Puzzle.IndexValue symbol) {
		this.puzzle = puzzle;
		this.x = x;
		this.y = y;
		this.symbol = symbol;
		this.owners = new ArrayList<>(INIT_OWNER_COUNT);
	}
	
	public boolean isKnownFalse(){
		return owners.isEmpty();
	}
	
	public boolean isKnownTrue(){
		for(FactBag owner : owners){
			if(owner.size() == FactBag.SIZE_WHEN_SOLVED){
				return true;
			}
		}
		return false;
	}
	
	public String possText(){
		return isKnownFalse() ? " " : symbol.humanReadableSymbol();
	}
	
	public boolean sharesBagWith(Claim c){
		return !Collections.disjoint(owners, c.owners);
	}
	
	boolean setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD(){
		boolean result = false;
		Set<Claim> claimsSetFalse = new HashSet<>();
		for(FactBag owner : owners){
			claimsSetFalse.addAll(owner);
			result |= owner.setAsSoleElement_ONLY_Claim_MAY_CALL_THIS_METHOD(this);
		}
		claimsSetFalse.remove(this);
		if(result){
			puzzle.addSolveEvent(new ArrayList<>(claimsSetFalse));
		}
		return result;
	}
	
	public boolean setFalse(){
		boolean result = false;
		for(FactBag owner : new ArrayList<>(owners)){
			result |= owner.remove(this);
		}
		return result;
	}
	
	boolean addOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(FactBag bag){
		return owners.add(bag);
	}
	
	boolean removeOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(FactBag bag){
		return owners.remove(bag);
	}
	
	public List<FactBag> getOwners(){
		return new ArrayList<>(owners);
	}
	
	public int getX(){
		return x.intValue();
	}
	
	public int getY(){
		return y.intValue();
	}
	
	public int getZ(){
		return symbol.intValue();
	}
	
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	public int linearizeCoords(){
		return linearizeCoords(x.intValue(), y.intValue(), symbol.intValue(), puzzle.sideLength());
	}
	
	public static int linearizeCoords(int x, int y, int z, int sideLength){
		return x*sideLength*sideLength + y*sideLength + z;
	}
	
	@Override
	public String toString(){
		return "Claim: cell "+x.intValue()+","+y.intValue()+" is "+symbol.intValue();
	}
	
	@Override
	public int hashCode(){
		return linearizeCoords();
	}
	
	@Override
	public boolean equals(Object o){
		if( o instanceof Claim){
			Claim c = (Claim) o;
			return c.x == x && c.y == y && c.symbol == symbol;
		}
		return false;
	}
	
	public double spaceDistTo(Claim otherClaim){
		int x = this.x.intValue();
		int y = this.y.intValue();
		int z = this.symbol.intValue();
		int cx = otherClaim.x.intValue();
		int cy = otherClaim.y.intValue();
		int cz = otherClaim.symbol.intValue();
		return Math.sqrt( (x-cx)*(x-cx) + (y-cy)*(y-cy) + (z-cz)*(z-cz) );
	}

	public int[] vectorTo(Claim cn) {
		int[] result = new int[3]; //XXX magic no
		result[0] = cn.x.intValue() - x.intValue();
		result[1] = cn.y.intValue() - y.intValue();
		result[2] = cn.symbol.intValue() - symbol.intValue();
		return result;
	}
}
