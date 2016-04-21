package sudoku;

import java.util.HashSet;
import java.util.Set;

import common.time.AbstractTime;
import common.time.Time;

public class FalsifiedTime extends AbstractTime {
	
	private final Set<Claim> falsified;
	
	public FalsifiedTime(Time parent) {
		super(parent);
		this.falsified = new HashSet<>();
	}

	public FalsifiedTime(Time parent, Time focus) {
		super(parent, focus);
		this.falsified = new HashSet<>();
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
