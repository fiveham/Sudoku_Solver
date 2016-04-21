package sudoku;

import common.time.Time;

/**
 * <p>A time node denoting and encapsulating an event in which some 
 * progress is made in solving the target.</p>
 * 
 * <p>Known solution events include<ul>
 * <li>{@link Puzzle.Initialization Initialization}</li>
 * <li>{@link SledgeHammer2.TimeSledgeHammer2Found Sledgehammer}</li>
 * <li>{@link ColorChain.TimeColorChainInternalContradiction Xor-chain collapse by self-interaction}</li>
 * <li>{@link ColorChain.TimeColorChainCollapseBridges Xor-chain collapse by chain-interaction}</li>
 * <li>{@link ColorChain.TimeColorChainExternalContradiction Elimination of Claims outside a xor-chain}</li>
 * </ul></p>
 * @author fiveham
 *
 */
public class SolutionEvent extends FalsifiedTime {
	
	public SolutionEvent(Time parent) {
		super(parent);
	}
	
	public SolutionEvent(Time parent, Time focus) {
		super(parent, focus);
	}
	
}
