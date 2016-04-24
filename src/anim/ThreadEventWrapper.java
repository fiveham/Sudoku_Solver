package anim;

import common.time.AbstractTime;
import java.util.List;
import java.util.Map;
import javafx.animation.Timeline;
import sudoku.Claim;
import sudoku.ThreadEvent;

public class ThreadEventWrapper extends AbstractTime {
	
	private ThreadEvent counterpart;
	private Timeline timeline;
	
	public ThreadEventWrapper(ThreadEventWrapper parent, ThreadEvent counterpart, Map<Claim,List<VoxelModel>> modelHandler) {
		super(parent);
		
		this.counterpart = counterpart;
		this.timeline = PuzzleVizApp.solutionEventTimeline(counterpart.wrapped(), modelHandler);
		
		if(hasParent()){
			parent.addChild(this);
		}
		
		counterpart.children()
				.stream()
				.filter( (t) -> t instanceof ThreadEvent )
				.map( (t) -> (ThreadEvent) t )
				.forEach( (t) -> addChild(new ThreadEventWrapper(this, t, modelHandler)) );
	}
	
	public ThreadEvent wrapped(){
		return counterpart;
	}
	
	public Timeline timeline(){
		return timeline;
	}
}
