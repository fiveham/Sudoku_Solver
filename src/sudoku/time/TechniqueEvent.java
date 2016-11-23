package sudoku.time;

import sudoku.Claim;
import java.util.Set;

/**
 * <p>A time node denoting and encapsulating an event in which some progress is made in solving the
 * target.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public abstract class TechniqueEvent extends FalsifiedTime{
	public TechniqueEvent(Set<Claim> falsified){
		super(null, falsified);
	}
	
	@Override
	public final TechniqueEvent falsifyClaims(){
		super.falsifyClaims();
		return this;
	}
}
