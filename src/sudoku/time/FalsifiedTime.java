package sudoku.time;

import common.time.AbstractTime;
import common.time.Time;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;
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
		this.falsified = new HashSet<>(falsified);
		this.falsified.removeAll(upFalsified(this, true));
		if(this.falsified.isEmpty()){
			throw new NoUnaccountedClaims("No unaccounted-for Claims specified.");
		}
	}
	
  /**
   * <p>Returns a set of all the Claims falsified in all the FalsifiedTime nth-parents of this
   * Time.</p>
   * @return a set of all the Claims falsified in all the FalsifiedTime nth parents of this Time
   */
	private static Set<Claim> upFalsified(Time time, boolean skip){
		return skip(time.upTrail().stream(), skip)
				.filter(FalsifiedTime.class::isInstance)
				.map(FalsifiedTime.class::cast)
				.map(FalsifiedTime::falsified)
				.map(HashSet<Claim>::new)
				.reduce((c1, c2) -> {
				  c1.addAll(c2);
				  return c1;
				})
				.get();
	}
	
  /**
   * <p>{@link Stream#skip(long) Skips} the first element of {@code stream} if and only if
   * {@code skip == true}.</p<
   * @param stream a Stream whose first element may be skipped
   * @param skip specifies whether {@code stream}'s first element will be skipped
   * @return a Stream consisting of the remaining elements of {@code stream} after optionally 
   * skipping the first element.
   */
	private static Stream<Time> skip(Stream<Time> stream, boolean skip){
		return skip 
		    ? stream.skip(1) 
		    : stream;
	}
	
  /**
   * <p>Returns the unmodifiable set of claims set false by this event.</p>
   * @return the unmodifiable set of claims set false by this event
   */
	public Set<Claim> falsified(){
		return falsified;
	}
	
  /**
   * <p>The first time this method is called, {@link Claim#setFalse() falsifies} the Claims
   * specified to be {@link #falsified() falsified} by the event represented by this object.
   * Subsequent calls do nothing.</p> <p>This method allows duplicated Claim-falsification code to
   * be centralized and unified. Performing this falsification inside the FalsifiedTime
   * constructor is not safe, since the members of this object's subclasses, if any, have not been
   * assigned, meaning those classes' methods, if called upon before a call to setFalse returns,
   * may not work as intended. By separating the mass-falsification process into a method like
   * this, subclass instances can finish constructing before their methods may be needed.</p>
   * <p>This method is best used at the time when this object is constructed.</p>
   * @return this FalsifiedTime
   */
	public FalsifiedTime falsifyClaims(){
		falsifyClaims.run();
		falsifyClaims = DO_NOTHING;
		return this;
	}
	
	private static final Runnable DO_NOTHING = () -> {};
	
	private Runnable falsifyClaims = () -> falsified().stream().forEach(Claim::setFalse);
	
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
   * <p>A short description of this type of Time. The output of {@code toString()} begins with
   * this. A trailing space should not be included. The description should take the form of a noun
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
		
		Set<Time> layer = new HashSet<>(children());
		while(!layer.isEmpty()){
			Set<Time> newLayer = new HashSet<>();
			for(Time t : layer){
				newLayer.addAll(t.children());
				if(t instanceof FalsifiedTime){
					count += ((FalsifiedTime) t).falsified().size();
				}
			}
			layer = newLayer;
		}
		
		return count;
	}
	
  /**
   * <p>An Exception thrown when a FalsifiedTime is constructed without any specified falsified
   * Claims not already {@link #falsified falsified} by the constructed FalsifiedTime's
   * FalsifiedTime nth-parents.</p>
   * @author fiveham
   * @author fiveham
	 *
	 */
	public static class NoUnaccountedClaims extends RuntimeException{
		
		private static final long serialVersionUID = 7063069284727178843L;
		
		private NoUnaccountedClaims(String s){
			super(s);
		}
	}
}
