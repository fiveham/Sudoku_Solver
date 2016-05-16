package sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
	 * <p>Constructs a FalsifiedTime having the specified {@code parent}.</p>
	 * @param parent the event which caused this event
	 */
	public FalsifiedTime(Time parent) {
		super(parent);
		this.falsified = new HashSet<>();
	}
	
	/**
	 * <p>Constructs a FalsifiedTime having the specified {@code parent} and 
	 * the specified {@code falsified} Claims.</p>
	 * @param parent the event which caused this event
	 * @param falsified the Claims set false in this event itself
	 */
	public FalsifiedTime(Time parent, Collection<Claim> falsified){
		super(parent);
		this.falsified = new HashSet<>(falsified);
	}
	
	/**
	 * <p>Returns the set of claims set false by the operation that 
	 * this time node represents.</p>
	 * @return the set of claims set false by the operation that 
	 * this time node represents
	 */
	public Set<Claim> falsified(){
		return falsified;
	}
}
