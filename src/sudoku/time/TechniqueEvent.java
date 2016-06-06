package sudoku.time;

import sudoku.Claim;
import sudoku.technique.ColorChain;
import sudoku.technique.SledgeHeur;

import java.util.Set;

/**
 * <p>A time node denoting and encapsulating an event in which some 
 * progress is made in solving the target.</p>
 * 
 * <p>Known solution events:<ul>
 * <li>{@link Puzzle.Initialization Initialization}</li>
 * <li>{@link SledgeHeur.SolveEventSledgehammer Sledgehammer}</li>
 * <li>{@link ColorChain.SolveEventColorContradiction visible-contradiction}</li>
 * <li>{@link ColorChain.SolveEventBridgeCollapse bridge-collapse}</li>
 * <li>{@link ColorChain.SolveEventBridgeJoin bridge-join}</li>
 * </ul></p>
 * @author fiveham
 *
 */
public abstract class TechniqueEvent extends FalsifiedTime{
	public TechniqueEvent(Set<Claim> falsified){
		super(null, falsified);
	}
}
