package sudoku.technique;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

/**
 * <p>This solution technique explores the implications of either of the 
 * two Claims of a two-Claim Rule being true. Any Claims that would be 
 * made false by either Claim being true must be false.</p>
 * @author fiveham
 * @see http://www.sadmansoftware.com/sudoku/xychain.php
 *
 */
public class XYChain extends AbstractTechnique {
	
	/**
	 * <p>Constructs an XYChain that works to solve the specified 
	 * {@code puzzle}.</p>
	 * @param puzzle the Puzzle that this solution technique 
	 * instance works to solve
	 */
	public XYChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	@Override
	protected TechniqueEvent process() {
		
		Set<Fact> binaryRules = target.factStream().filter((r) -> r.size()==2).collect(Collectors.toSet());
		for(Fact binaryRule : binaryRules){
			//TODO handle xor-chains of binary rules as single entities.
			Set<Claim> falseIntersection = binaryRule.stream()
					.map(XYChain::getFalsifiedClaims)
					.collect(Collector.of(
							HashSet<Claim>::new, 
							Set::addAll, 
							(a,b) -> {
								a.retainAll(b); 
								return a;
							}, 
							Collector.Characteristics.IDENTITY_FINISH, Collector.Characteristics.UNORDERED));
			
			if(!falseIntersection.isEmpty()){
				return new SolveEventXYChain(falseIntersection, binaryRule).falsifyClaims();
			}
		}
		
		return null;
	}
	
	public class SolveEventXYChain extends TechniqueEvent{
		
		private final Fact binaryRule;
		
		public SolveEventXYChain(Set<Claim> falsified, Fact binaryRule) {
			super(falsified);
			this.binaryRule = binaryRule;
		}
		
		@Override
		protected String toStringStart() {
			return "XYChain: Either solution of "+binaryRule.toString()+" falsifies "+falsified();
		}
	}
	
	/**
	 * <p>Determines which Claims must be false if the specified 
	 * Claim were true.</p>
	 * <p>Determination that a Claim must be true is 
	 * limited to the identification of Rules with a single non-false
	 * Claim.  Other solution techniques' abilities to remotely detect 
	 * the true state of Claims are not used.</p>
	 * @param assertedTrue the Claim whose asserted truth is used to 
	 * determine the states of other Claims in the puzzle
	 * @return a Set of the Claims that must be false if {@code assertedTrue} 
	 * were true.
	 */
	public static Set<Claim> getFalsifiedClaims(Claim assertedTrue){
		
		Set<Claim> trueClaims = new HashSet<>();
		Set<Claim> falseClaims = new HashSet<>();
		
		Set<Claim> newTrue = new HashSet<>();
		newTrue.add(assertedTrue);
		
		while(!newTrue.isEmpty()){
			Set<Claim> newFalse = newFalse(newTrue, falseClaims);
			
			trueClaims.addAll(newTrue);
			falseClaims.addAll(newFalse);
			
			newTrue = newTrue(falseClaims, newFalse);
		}
		
		return falseClaims;
	}
	
	/**
	 * <p.Determines which Claims, in addition to those already known to 
	 * be conditionally false, must be conditionally false.</p>
	 * <p>A Claim is conditionally false if it would have to be false 
	 * given that the Claim currently {@code assertedTrue} in 
	 * {@code getFalsifiedClaims} is asserted to be true.</p>
	 * @param newTrue
	 * @param f
	 * @return
	 */
	private static Set<Claim> newFalse(Set<Claim> newTrue, Set<Claim> f){
		Set<Claim> result = new HashSet<>();
		for(Claim newlyVerified : newTrue){
			result.addAll(newlyVerified.visible());
		}
		result.removeAll(f);
		return result;
	}
	
	/**
	 * <p>Returns a set of the Claims that must be true because all other Claims of some 
	 * Rule have already been determined to be false.</p>
	 * @param newFalse
	 * @return
	 */
	private static Set<Claim> newTrue(Set<Claim> f, Set<Claim> newFalse){
		
		Set<Fact> visibleRules = visibleRules(newFalse);
		Set<Claim> result = new HashSet<>(visibleRules.size());
		for(Fact rvisibleRule : visibleRules){
			Set<Claim> copyOfVisibleRule = new HashSet<>(rvisibleRule);
			copyOfVisibleRule.removeAll(f);
			if(copyOfVisibleRule.size() == Fact.SIZE_WHEN_SOLVED){
				result.add(rvisibleRule.iterator().next());
			}
		}
		
		return result;
	}
	
	/**
	 * <p>Returns a set of the Facts visible (adjacent) to at least one of 
	 * the Claims in {@code newFalse}.</p>
	 * @param newFalse
	 * @return
	 */
	private static Set<Fact> visibleRules(Set<Claim> newFalse){
		return Sledgehammer.massUnion(newFalse);
	}
}
