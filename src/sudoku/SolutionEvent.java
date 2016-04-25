package sudoku;

import common.time.Time;
import common.time.TimeBuilder;
import java.util.Collection;

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
public abstract class SolutionEvent extends FalsifiedTime implements TimeBuilder {
	
	protected Time top;
	
	public SolutionEvent(){
		super(null);
	}
	
	public SolutionEvent(Collection<Claim> falsified){
		super(null, falsified);
	}
	
	@Override
	public void pop() {
		top = top.parent();
	}
	
	@Override
	public void push(Time time) {
		top.addChild(time);
		top = time;
	}
	
	@Override
	public Time top() {
		return top;
	}
}
