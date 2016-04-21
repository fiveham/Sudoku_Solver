package sudoku;

import common.time.AbstractTime;
import common.time.Time;

public class ThreadEvent extends AbstractTime {
	
	private final SolutionEvent wrapped;
	
	public ThreadEvent(ThreadEvent parent, SolutionEvent wrapped) {
		super(parent);
		this.wrapped = wrapped;
		
		if(parent != null){
			parent.addChild(this);
		}
	}
	
	@Override
	public synchronized boolean addChild(Time time){
		return super.addChild(time);
	}
	
	public SolutionEvent wrapped(){
		return wrapped;
	}
}
