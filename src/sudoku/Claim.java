package sudoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Represents a claim that "such-and-such Cell 
 * contains such-and-such Symbol."</p>
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
	
	private Index x;
	private Index y;
	private Index symbol;
	
	private List<FactBag> owners;
	
	public Claim(Index x, Index y, Index symbol) {
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
	
	public boolean sharesRegionWith(Claim c){
		return x == c.x || y == c.y || symbol == c.symbol || Puzzle.boxIndex(x, y) == Puzzle.boxIndex(c.x, c.y);
	}
	
	public boolean sharesBagWith(Claim c){
		Set<FactBag> othersOwners = new HashSet<>(c.owners);
		othersOwners.retainAll(owners);
		return !othersOwners.isEmpty();
	}
	
	boolean setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD(){
		boolean result = false;
		for(FactBag owner : owners){
			result |= owner.setAsSoleElement_ONLY_CLAIM_MAY_CALL_THIS_METHOD(this);
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
	
	@Override
	public int hashCode(){
		return x.intValue()*100 + y.intValue()*10 + symbol.intValue(); //XXX magic no.s
	}
	
	@Override
	public boolean equals(Object o){
		if( o instanceof Claim){
			Claim c = (Claim) o;
			return c.x == x && c.y == y && c.symbol == symbol;
		}
		return false;
	}
}
