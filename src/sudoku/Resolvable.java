package sudoku;

/**
 * Represents a task of collapsing FactBags onto a smaller Rule that each 
 * Rule to be collapsed contains as a proper subset. These are set up by 
 * the FactBags themselves during initial population of the Puzzle with the 
 * initial values of the cells and registered with the Puzzle for later 
 * resolution.
 * @author fiveham
 *
 */
public class Resolvable {
	
	private FactBag source;
	
	public Resolvable(FactBag src) {
		if(src.size() != FactBag.SIZE_WHEN_SOLVED){
			throw new IllegalArgumentException("The specified Rule is not resolvable.");
		}
		this.source = src;
	}
	
	public void resolve(){
		for(Claim c : source){
			c.setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD();
		}
	}
}
