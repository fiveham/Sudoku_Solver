package sudoku;

import java.util.Collections;

/**
 * <p>An Init marks the value of a cell whose value is given as part of the initial state of the 
 * puzzle. It does this by being a Fact whose sole element is the one Claim pertaining to that cell 
 * and that value, forcing that Claim to be true and all Claims visible to it to be false.</p>
 * @author fiveham
 */
public class Init extends Fact {
	
	private static final long serialVersionUID = 7730369667106322962L;
	
	private final Claim claim;
	
  /**
   * <p>Constructs an Init belonging to the specified {@code puzzle}, marking the specified claim 
   * as true.</p>
   * @param puzzle the puzzle one of whose initial values this Init indicates
   * @param c the Claim made true by the puzzle having the initial value indicated by this Init
   */
	public Init(Puzzle puzzle, Claim c) {
		super(puzzle, Collections.singletonList(c), c.hashCode());
		this.claim = c;
	}
	
  /**
   * <p>Returns the Claim that this Init marks true.</p>
   * @return the Claim that this Init marks true
   */
	public Claim claim(){
		return claim;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("An Init verifying ");
		for(Claim c : this){
			sb.append(c);
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Init){
			Init i = (Init) o;
			return claim.equals(i.claim);
		}
		return false;
	}
}
