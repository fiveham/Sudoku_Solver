package sudoku;

import common.time.Time;
import common.time.TimeBuilder;
import java.util.Set;

/**
 * <p>A time node denoting and encapsulating an event in which some 
 * progress is made in solving the target.</p>
 * 
 * <p>Known solution events:<ul>
 * <li>{@link Puzzle.Initialization Initialization}</li>
 * <li>{@link Sledgehammer.SolveEventSledgehammer Sledgehammer}</li>
 * <li>{@link ColorChain.SolveEventColorChainVisibleContradiction visible-contradiction}</li>
 * <li>{@link ColorChain.SolveEventColorChainBridge bridge-collapse}</li>
 * </ul></p>
 * @author fiveham
 *
 */
public abstract class SolutionEvent extends FalsifiedTime implements TimeBuilder {
	
	protected Time top;
	
	public SolutionEvent(Set<Claim> falsified){
		super(null, falsified);
		top = this;
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
