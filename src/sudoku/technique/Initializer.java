package sudoku.technique;

import sudoku.Init;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

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
public class Initializer extends AbstractTechnique {
	
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
	protected TechniqueEvent process(){
		Optional<Init> i = target.nodeStream()
				.filter(Init.class::isInstance)
				.map(Init.class::cast)
				.findFirst();
		if(i.isPresent()){
			return new Initialization(i.get()).falsifyClaims();
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
	public static class Initialization extends TechniqueEvent{
		
		private final Init src;
		
		private Initialization(Init init){
			super(init.claim().visible());
			this.src = init;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Initialization){
				Initialization se = (Initialization) o;
				return super.equals(se) && (src == null ? se.src == null : src.equals(se.src));
			}
			return false;
		}
		
		@Override
		protected String toStringStart(){
			return "Initialization from "+src;
		}
	}
}
