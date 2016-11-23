package sudoku.time;

import common.time.AbstractTime;
import common.time.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * <p>Denotes the termination of a Solver thread because a change was made to the puzzle being
 * solved. The SolutionEvent resulting from this change to the puzzle is
 * {@link #techniqueEvent() wrapped} by this Time.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class ThreadEvent extends AbstractTime {
	
	private final TechniqueEvent techniqueEvent;
	private final String threadName;
	
    /**
     * <p>Constructs a ThreadEvent having the specified {@code parent} and wrapping the specified
     * SolutionEvent.</p>
     * @param parent the ThreadEvent marking the change-making termination of the thread that
     * spawned the thread whose change-making termination is represented by this ThreadEvent
     * @param techniqueEvent the SolutionEvent that terminated the thread to which this ThreadEvent
     * pertains
     */
	public ThreadEvent(ThreadEvent parent, TechniqueEvent techniqueEvent, String threadName) {
		super(parent);
		this.techniqueEvent = techniqueEvent;
		this.threadName = threadName;
		
		if(parent != null){
			parent.addChild(this);
		}
	}
	
    /**
     * <p>Adds the specified Time to this ThreadEvent as a child. This method is synchronized
     * because each child added shall be added from a different thread, in order to minimize the
     * ThreadEvent time-tree. Only once a thread has changed the puzzle and is thus about to
     * terminate does it add a child ThreadEvent to the ThreadEvent that ended that thread's parent
     * thread.</p>
     * @param time the new child Time
     * @return true if this Time's collection of children was changed by this call to this method,
     * false otherwise
     */
	@Override
	public synchronized boolean addChild(Time time){
		return super.addChild(time);
	}
	
    /**
     * <p>Returns the SolutionEvent wrapped by this ThreadEvent.</p>
     * @return the SolutionEvent wrapped by this ThreadEvent
     */
	public TechniqueEvent techniqueEvent(){
		return techniqueEvent;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof ThreadEvent){
			ThreadEvent te = (ThreadEvent) o;
			return techniqueEvent.equals(te.techniqueEvent);
		}
		return false;
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder("ThreadEvent terminating ").append(threadName).append(System.lineSeparator());
		
		List<Object> subordinateTimes = new ArrayList<>(children().size()+1);
		subordinateTimes.add(techniqueEvent);
		subordinateTimes.addAll(children());
		for(Object o : subordinateTimes){
			String oString = o.toString();
			for(Scanner s = new Scanner(oString); s.hasNextLine();){
				result.append("  ").append(s.nextLine()).append(System.lineSeparator());
			}
		}
		
		return result.toString();
	}
}
