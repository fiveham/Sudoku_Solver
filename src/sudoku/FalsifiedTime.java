package sudoku;

import java.util.HashSet;
import java.util.Set;

import common.time.AbstractTime;
import common.time.Time;

/**
 * <p>A time node in which a more-or-less loose collection of 
 * Claims are set false.</p>
 * 
 * <p>This class is known to be used as a base class for time 
 * nodes used by SledgeHammer2 and ColorChain.</p>
 * @author fiveham
 *
 */
public abstract class FalsifiedTime extends AbstractTime implements Comparable<FalsifiedTime>{
	
	private static long ID_SRC = Long.MIN_VALUE;
	
	private final Set<Claim> falsified;
	private final long id;
	
	/**
	 * <p>Constructs a FalsifiedTime whose {@link Time#parent() parent} is 
	 * <tt>parent</tt>.</p>
	 * @param parent the {@link Time#parent() parent} of this Time
	 */
	public FalsifiedTime(Time parent) {
		super(parent);
		this.falsified = new HashSet<>();
		this.id = ID_SRC++;
	}
	
	/**
	 * <p>Constructs a FalsifiedTime whose {@link Time#parent() parent} is 
	 * <tt>parent</tt> and whose {@link Time#focus() focus} is 
	 * <tt>focus</tt>.</p>
	 * @param parent
	 * @param focus
	 */
	public FalsifiedTime(Time parent, Time focus) {
		super(parent, focus);
		this.falsified = new HashSet<>();
		this.id = ID_SRC++;
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
	
	@Override
	public int compareTo(FalsifiedTime ft){
		return Long.compare(id, ft.id);
	}
}
