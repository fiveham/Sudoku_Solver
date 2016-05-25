package common.time;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractTime implements Time{
	
	protected final Time parent;
	protected final List<Time> children;
	
	protected Time focus;
	
	public AbstractTime(Time parent) {
		this.parent = parent;
		this.children = new ArrayList<>();
		this.focus = this;
	}
	
	@Override
	public Time parent(){
		return parent;
	}
	
	@Override
	public boolean hasParent(){
		return parent != null;
	}
	
	@Override
	public List<Time> children(){
		return children;
	}
	
	@Override
	public boolean hasChildren(){
		return !children.isEmpty();
	}
	
	@Override
	public boolean defers(){
		return focus != this;
	}
	
	@Override
	public Time focus(){
		return focus;
	}
	
	@Override
	public Time currentTime(){
		Time result = focus;
		while( result.defers() ){
			result = result.focus();
		}
		return result;
	}
	
	@Override
	public List<Time> currentTrail(){
		return trailGen((t) -> t.focus(), (t)->t.hasChildren());
	}
	
	@Override
	public List<Time> upTrail(){
		return trailGen((t) -> t.parent(), (t)->t.hasParent());
	}
	
	private List<Time> trailGen(Function<Time,Time> upDown, Predicate<Time> test){
		List<Time> result = new ArrayList<>();
		
		Time pointer = this;
		while(test.test(pointer)){
			result.add(pointer);
			pointer = upDown.apply(pointer);
		}
		result.add(pointer);
		
		return result;
	}
	
	@Override
	public boolean addChild(Time child){
		boolean result = children.add(child);
		focus = child;
		return result;
	}
	
	@Override
	public boolean hasNextTime(){
		return currentTime().hasSuccessor();
	}
	
	@Override
	public Time nextTime(){
		return currentTime().successor();
	}
	
	@Override
	public boolean hasPrevTime(){
		return currentTime().hasPredecessor();
	}
	
	@Override
	public Time prevTime(){
		return currentTime().predecessor();
	}
	
	@Override
	public boolean hasSuccessor(){
		return hasNextChild() || (parent()!=null && parent().hasSuccessor());
	}
	
	@Override
	public Time successor(){
		return hasNextChild() ? nextChild().currentTime() : parent().successor();
	}
	
	@Override
	public boolean hasPredecessor(){
		return hasPrevChild() || (parent()!=null && parent().hasPredecessor());
	}
	
	@Override
	public Time predecessor(){
		return hasPrevChild() ? prevChild().currentTime() : parent.predecessor();
	}
	
	@Override
	public boolean hasNextChild(){
		int index = children.indexOf(focus)+1;
		return 0 <= index && index < children.size()-1;
	}
	
	@Override
	public Time nextChild(){
		return focus = children.get( children.indexOf(focus)+1 );
	}
	
	@Override
	public boolean hasPrevChild(){
		int index = children.indexOf(focus)-1;
		return 0 <= index && index < children.size()-1;
	}
	
	@Override
	public Time prevChild(){
		return focus = children.get( children.indexOf(focus)-1 );
	}
	
	@Override
	public void toStart(){
		if(defers()){
			this.focus = children().get(0);
			for(Time child : children()){
				child.toStart();
			}
		}
	}
	
	@Override
	public void toEnd(){
		if(defers()){
			this.focus = children().get(children.size()-1);
			for(Time child : children()){
				child.toEnd();
			}
		}
	}
	
	@Override
	public Iterator<Time> iterator(){
		return new TimeIterator();
	}
	
	/**
	 * <p>An iterator that traverses the time tree of which this time 
	 * node is the root. Leaf nodes are output by {@code next()}.</p>
	 * @author fiveham
	 *
	 */
	private final class TimeIterator implements Iterator<Time>{
		
		private boolean start;
		
		private TimeIterator(){
			this.start = true;
		}
		
		@Override
		public Time next(){
			if(start){
				start = false;
				return currentTime();
			} else{
				return nextTime();
			}
		}
		
		@Override
		public boolean hasNext(){
			return hasNextTime() || start;
		}
	}
}
