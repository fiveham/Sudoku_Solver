package sudoku;

import sudoku.NodeSet;
import java.util.Optional;

/**
 * <p>This Technique finds an Init in an unsolved Sudoku graph and 
 * sets that Init's attached Claim true.</p>
 * 
 * <p>This technique is built as a formal Technique so that direct 
 * and automatic changes to a Puzzle pertaining to the specification 
 * of a puzzle's initial values can be incorporated into the time 
 * system without changing the time system itself.</p>
 * @author fiveham
 *
 */
public class Initializer extends Technique {
	
	/**
	 * <p>Constructs an Initializer that works to solve the 
	 * specified Sudoku.</p>
	 * @param puzzle the Sudoku that this Initializer works 
	 * to solve
	 */
	public Initializer(Sudoku puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Finds an Init in {@code target} and sets that Init's 
	 * one Claim neighbor true.</p>
	 * @return an Initialization describing the verification of 
	 * an Init's sole Claim neighbor and any and all resulting 
	 * automatic resolution events, or null if no Init is found
	 */
	@Override
	protected SolutionEvent process(){
		Optional<NodeSet<?,?>> i = target.nodeStream().filter((e)-> e instanceof Init).findFirst();
		if(i.isPresent()){
			SolutionEvent result = new Initialization();
			i.get().validateFinalState(result);
			return result;
		}
		
		return null;
	}
	
	/**
	 * <p>A SolutionEvent describing the verification of a Claim 
	 * known to be true because the initial state of the puzzle 
	 * specifies that that cell has that value.</p>
	 * @author fiveham
	 *
	 */
	public static class Initialization extends SolutionEvent{
		private Initialization(){
			super();
		}
	}
}