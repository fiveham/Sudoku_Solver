package anim;

import java.util.Iterator;
import java.util.function.Consumer;
import javafx.animation.Timeline;

public class ParallellizableTimeline extends CleverTimeline {
	
	private final Seriality seriality;
	
	public ParallellizableTimeline(boolean runsParallel, Iterator<Timeline> tIter) {
		super(tIter);
		this.seriality = runsParallel ? Seriality.PARALLEL : Seriality.SERIAL;
	}
	
	@Override
	public void play(){
		seriality.action.accept(this);
	}
	
	private void superPlay(){
		super.play();
	}
	
	private static enum Seriality{
		SERIAL  ((pt) -> pt.superPlay()), 
		PARALLEL((pt) -> {
			pt.timeline.setOnFinished((ae)->{});
			pt.timeline.play();
			while(pt.tIter.hasNext()){
				pt.tIter.next().play();
			}
		});
		
		private final Consumer<ParallellizableTimeline> action;
		
		private Seriality(Consumer<ParallellizableTimeline> action){
			this.action = action;
		}
	}
}
