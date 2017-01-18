package sudoku.time;

import common.time.AbstractTime;
import common.time.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * <p>Denotes the termination of a Solver thread because the Solver made a change to its puzzle and 
 * {@link #techniqueEvent() wraps} a TechniqueEvent describing that change to the puzzle.</p>
 * @author fiveham
 */
public class ThreadEvent extends AbstractTime {
	
	private final TechniqueEvent techniqueEvent;
	private final String threadName;
	
  /**
   * <p>Constructs a ThreadEvent having the specified {@code parent} and wrapping the specified
   * TechniqueEvent.</p>
   * @param parent the end of the Solver thread that spawned the Solver thread whose end this 
   * ThreadEvent represents
   * @param techniqueEvent the event that ended the Solver thread to which this ThreadEvent pertains
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
   * <p>Adds the specified Time to this ThreadEvent as a child.</p>
   * <p>This method is synchronized because each child added shall be added from a different Solver 
   * thread. A Solver thread will only call this method once it has made a change to its puzzle. 
   * After that call, that Solver thread dies.</p>
   * @param time the new child Time
   * @return true if this Time's collection of children was changed by this call to this method,
   * false otherwise
   */
	@Override
	public synchronized boolean addChild(Time time){
		return super.addChild(time);
	}
	
  /**
   * <p>Returns the TechniqueEvent wrapped by this ThreadEvent.</p>
   * @return the TechniqueEvent wrapped by this ThreadEvent
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
