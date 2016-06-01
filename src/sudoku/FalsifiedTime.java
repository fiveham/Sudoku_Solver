package sudoku;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import common.time.AbstractTime;
import common.time.Time;

/**
 * <p>A Time in which some Claims are {@link Claim#setFalse(SolutionEvent) set false}.</p>
 * 
 * <p>This class is a base class for Times the denote events in the process of 
 * solving a sudoku puzzle, including direct {@link SolutionEvents SolutionEvents} 
 * in which a Technique changes the puzzle and indirect 
 * {@link AutoResolve AutoResolve} events in which changes made to the puzzle 
 * by some processing event allow a part of the puzzle to 
 * {@link Rule#validateFinalState(SolutionEvent) automatically detect} a 
 * solving action that it can take locally.</p>
 * @author fiveham
 *
 */
public class FalsifiedTime extends AbstractTime {
	
	private final Set<Claim> falsified;
	
	/**
	 * <p>Constructs a FalsifiedTime having the specified {@code parent} and 
	 * the specified {@code falsified} Claims.</p>
	 * @param parent the event which caused this event
	 * @param falsified the Claims set false in this event itself
	 */
	public FalsifiedTime(Time parent, Set<Claim> falsified){
		super(parent);
		Set<Claim> upFalsified = upFalsified();
		this.falsified = Collections.unmodifiableSet(falsified.stream().filter((fc) -> !upFalsified.contains(fc)).collect(Collectors.toSet()));
	}
	
	/**
	 * <p>Returns a Set of all the Claims falsified in all the 
	 * FalsifiedTime nth parents of this FalsifiedTime.</p>
	 * @return
	 */
	private Set<Claim> upFalsified(){
		return upTrail().stream().skip(1) //MAGIC
				.filter((t) -> t instanceof FalsifiedTime)
				.map((t) -> ((FalsifiedTime)t))
				.collect(Collector.of(
						HashSet::new, 
						(Set<Claim> r, FalsifiedTime t) -> r.addAll(t.falsified), 
						(l,r) -> {
							l.addAll(r); 
							return l;
						}));
	}
	
	/**
	 * <p>Returns the set of claims set false by the operation that 
	 * this time node represents.</p>
	 * 
	 * <p>{@link Set#add(Object) Additions} to the returned Set write 
	 * through to the underlying collection.</p>
	 * @return the set of claims set false by the operation that 
	 * this time node represents
	 */
	public Set<Claim> falsified(){
		return falsified;
	}
}
