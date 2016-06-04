package sudoku;

import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
	 * @throws NoUnaccountedClaims if all the Claims in {@code falsified} are 
	 * accounted for as false by other {@code FalsifiedTime}s that are nth 
	 * parents of this one
	 */
	public FalsifiedTime(Time parent, Set<Claim> falsified){
		super(parent);
		Set<Claim> upFalsified = upFalsified(this, true);
		this.falsified = Collections.unmodifiableSet(falsified.stream().filter((fc) -> !upFalsified.contains(fc)).collect(Collectors.toSet()));
		if(this.falsified.isEmpty()){
			throw new NoUnaccountedClaims("No unaccounted-for Claims specified.");
		}
	}
	
	/**
	 * <p>Returns a Set of all the Claims falsified in all the 
	 * FalsifiedTime nth parents of this FalsifiedTime.</p>
	 * @return a Set of all the Claims falsified in all the 
	 * FalsifiedTime nth parents of this FalsifiedTime
	 */
	private static Set<Claim> upFalsified(Time time, boolean skip){
		return skip(time.upTrail().stream(), skip)
				.filter((t) -> t instanceof FalsifiedTime)
				.map((t) -> (FalsifiedTime)t)
				.collect(Collector.of(
						HashSet::new, 
						(Set<Claim> r, FalsifiedTime t) -> r.addAll(t.falsified), 
						(l,r) -> {
							l.addAll(r); 
							return l;
						}));
	}
	
	private static Stream<Time> skip(Stream<Time> stream, boolean skip){
		return skip ? stream.skip(1) : stream;
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
	
	public static void clean(Set<Claim> falsified, Time parent){
		falsified.removeAll(upFalsified(parent, false));
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		result.append(" falsifying ").append(falsified().size())
				.append(" Claims directly, and ").append(deepFalse())
				.append(" Claims indirectly.").append(System.lineSeparator())
				.append("Direct: ").append(falsified()).append(System.lineSeparator());
		
		for(Time t : children()){
			String str = t.toString();
			for(Scanner s = new Scanner(str); s.hasNextLine();){
				result.append("  ").append(s.nextLine()).append(System.lineSeparator());
			}
		}
		
		return result.toString();
	}
	
	private int deepFalse(){
		int count = 0;
		
		Set<Time> layer = new HashSet<>(children());
		while(!layer.isEmpty()){
			Set<Time> newLayer = new HashSet<>();
			for(Time t : layer){
				newLayer.addAll(t.children());
				if(t instanceof FalsifiedTime){
					FalsifiedTime ft = (FalsifiedTime) t;
					count += ft.falsified().size();
				}
			}
			layer = newLayer;
		}
		
		return count;
	}
	
	static class NoUnaccountedClaims extends RuntimeException{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5259033659237872620L;
		
		NoUnaccountedClaims(String s){
			super(s);
		}
	}
}
