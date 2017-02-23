package sudoku.time;

import sudoku.Claim;
import java.util.Set;

/**
 * <p>Denotes an event in which progress is made in solving a sudoku puzzle.</p>
 * @author fiveham
 */
public abstract class TechniqueEvent extends FalsifiedTime{
  
  /**
   * <p>Constructs a TechniqueEvent for a solving event in which the claims in {@code falsified} 
   * were determined to be false.</p>
   * @param falsified claims determined to be false in the event described by this object
   */
	public TechniqueEvent(Set<Claim> falsified){
		super(null, falsified);
	}
}
