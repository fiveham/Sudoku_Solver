package anim;

import javafx.animation.Timeline;
import java.util.Iterator;
import javafx.event.ActionEvent;

/**
 * This class wraps a Timeline and an Iterator<Timeline> in such a way that 
 * each Timeline returned by the iterator's next() method is automatically 
 * given an onFinished action that pulls the next (if there is one) Timeline 
 * from the iterator, assigns that iterator to <tt>timeline</tt>, gives 
 * that newly extracted Timeline the same sort of onFinished action I'm 
 * currently describing, and then {@link Timeline#play() plays} the 
 * newly-extracted Timeline.
 * In this way, all the Timelines produced by the iterator are played in 
 * the order in which the iterator yields them, without needing to 
 * store them all at once.
 * @author fiveham
 *
 */
public class CleverTimeline{
	
	protected final Iterator<Timeline> tIter;
	
	protected Timeline timeline;
	
	public CleverTimeline(Iterator<Timeline> tIter){
		this.tIter = tIter;
		tailRecursion(null, false);
		if(timeline==null){
			throw new IllegalArgumentException("The specified iterator did not have any Timelines available via next().");
		}
	}
	
	private void setTimeline(Timeline t){
		this.timeline = t;
		this.timeline.setOnFinished((ae) -> tailRecursion(ae,true));
	}
	
	private void tailRecursion(ActionEvent ae, boolean play){
		if(tIter.hasNext()){
			setTimeline(tIter.next());
			if(play){
				timeline.play();
			}
		}
	}
	
	public void play(){
		timeline.play();
	}
	
	public static class WrapIterator implements Iterator<Timeline>{
		private final Iterator<CleverTimeline> wrappedIterator;
		public WrapIterator(Iterator<CleverTimeline> wrappedIterator){
			this.wrappedIterator = wrappedIterator;
		}
		@Override
		public boolean hasNext(){
			return wrappedIterator.hasNext();
		}
		@Override
		public Timeline next(){
			return wrappedIterator.next().timeline;
		}
	}
}
