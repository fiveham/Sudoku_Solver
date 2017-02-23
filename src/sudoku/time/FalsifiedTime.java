package sudoku.time;

import common.Sets;
import common.time.AbstractTime;
import common.time.Time;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import sudoku.Claim;

/**
 * <p>An event in which some Claims are {@link Claim#setFalse(FalsifiedTime) set false}.</p>
 * <p>This class is a base class for events in the process of solving a sudoku puzzle.</p>
 * @author fiveham
 */
public abstract class FalsifiedTime extends AbstractTime {
	
	private final Set<Claim> falsified;
	
  /**
   * <p>Constructs a FalsifiedTime having the specified {@code parent} and representing the
   * falsification of Claims from {@code falsified} that are not included as
   * {@link #falsified() claims} of any nth-parents of this Time that are also FalsifiedTimes.</p>
   * @param parent the event which caused this event and of which this event is a part
   * @param falsified the Claims set false in this event
   * @throws NoUnaccountedClaims if all the Claims in {@code falsified} are accounted for as false
   * by this Time's {@code FalsifiedTime} nth-parents
   */
	public FalsifiedTime(Time parent, Set<Claim> falsified){
		super(parent);
		
		Set<Claim> f = new HashSet<>(falsified);
		upTrail().stream()
    		.skip(1)
    		.filter(FalsifiedTime.class::isInstance)
    		.map(FalsifiedTime.class::cast)
    		.map(FalsifiedTime::falsified)
    		.forEach(f::removeAll);
		if(f.isEmpty()){
		  throw new NoUnaccountedClaims("No unaccounted-for Claims specified.");
		}
		this.falsified = Collections.unmodifiableSet(f);
		this.falsified.stream().forEach(Claim::setFalse);
	}
	
  /**
   * <p>Returns the unmodifiable set of claims set false by this event.</p>
   * @return the unmodifiable set of claims set false by this event
   */
	public Set<Claim> falsified(){
		return falsified;
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder(toStringStart())
				.append(" falsifying ")
				.append(falsified().size())
				.append(" Claims directly, and ")
				.append(deepFalse())
				.append(" Claims indirectly.")
				.append(System.lineSeparator())
				.append("Direct: ")
				.append(falsified())
				.append(System.lineSeparator());
		
		for(Time t : children()){
			String str = t.toString();
			for(Scanner s = new Scanner(str); s.hasNextLine();){
				result.append("  ").append(s.nextLine()).append(System.lineSeparator());
			}
		}
		
		return result.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof FalsifiedTime){
			FalsifiedTime ft = (FalsifiedTime) o;
			return falsified().equals(ft.falsified());
		}
		return false;
	}
	
  /**
   * <p>A short description of this type of Time. This is used as the start of the output of 
   * {@link #toString()}. A trailing space should not be included. The description should be a noun 
   * phrase.</p>
   * @return a short description of this type of Time
   */
	protected abstract String toStringStart();
	
  /**
   * <p>Returns the number of Claims {@link #falsified() falsified} by all the FalsifiedTime
   * nth-children of this Time.</p>
   * @return the number of Claims {@link #falsified() falsified} by all the FalsifiedTime
   * nth-children of this Time
   */
	private int deepFalse(){
		int count = 0;
		
		Set<Time> currentLayer = children().stream()
		    .filter(FalsifiedTime.class::isInstance)
		    .collect(Collectors.toSet());
		while(!currentLayer.isEmpty()){
			count += currentLayer.stream()
			    .filter(FalsifiedTime.class::isInstance)
			    .map(FalsifiedTime.class::cast)
			    .map(FalsifiedTime::falsified)
			    .mapToInt(Set::size)
			    .reduce(0, Integer::sum);
			currentLayer = currentLayer.stream()
          .map(Time::children)
          .map(HashSet<Time>::new)
          .reduce(
              new HashSet<Time>(), 
              Sets::mergeCollections);
		}
		
		return count;
	}
	
	/**
   * <p>An Exception thrown when a FalsifiedTime is constructed without any specified falsified
   * Claims not already {@link #falsified falsified} by that FalsifiedTime's nth-parents.</p>
   * @author fiveham
   */
	public static class NoUnaccountedClaims extends RuntimeException{
		
		private static final long serialVersionUID = 7063069284727178843L;
		
		private NoUnaccountedClaims(String s){
			super(s);
		}
	}
}
