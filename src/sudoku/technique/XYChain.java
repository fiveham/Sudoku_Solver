package sudoku.technique;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

public class XYChain extends AbstractTechnique {
	
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
				return new XYChainEvent(falseIntersection, binaryRule).falsifyClaims();
			}
		}
		
		return null;
	}
	
	class XYChainEvent extends TechniqueEvent{
		
		private final String binaryRuleString;
		
		public XYChainEvent(Set<Claim> falsified, Fact binaryRule) {
			super(falsified);
			this.binaryRuleString = binaryRule.toString();
		}
		
		@Override
		protected String toStringStart() {
			return "XYChain: Either solution of "+binaryRuleString+" falsifies "+falsified();
		}
		
	}
	
	public static Set<Claim> getFalsifiedClaims(Claim assertedTrue){
		
		Set<Claim> ver = new HashSet<>();
		Set<Claim> fal = new HashSet<>();
		
		Set<Claim> newVer = new HashSet<>();
		newVer.add(assertedTrue);
		
		while(!newVer.isEmpty()){
			Set<Claim> newFal = newFal(newVer, fal);
			
			ver.addAll(newVer);
			fal.addAll(newFal);
			
			newVer = newVer(fal, newFal);
		}
		
		return fal;
	}
	
	public static Set<Claim> newFal(Set<Claim> newVer, Set<Claim> fal){
		Set<Claim> result = new HashSet<>();
		for(Claim newlyVerified : newVer){
			result.addAll(newlyVerified.visible());
		}
		result.removeAll(fal);
		return result;
	}
	
	/**
	 * <p>Returns a set of the Claims that must be true because all other Claims of some 
	 * Rule have already been determined to be false.</p>
	 * @param newFal
	 * @return
	 */
	public static Set<Claim> newVer(Set<Claim> fal, Set<Claim> newFal){
		
		Set<Fact> visibleRules = visibleRules(newFal);
		Set<Claim> result = new HashSet<>(visibleRules.size());
		for(Fact rvisibleRule : visibleRules){
			Set<Claim> copyOfVisibleRule = new HashSet<>(rvisibleRule);
			copyOfVisibleRule.removeAll(fal);
			if(copyOfVisibleRule.size() == Fact.SIZE_WHEN_SOLVED){
				result.add(rvisibleRule.iterator().next());
			}
		}
		
		return result;
	}
	
	/**
	 * <p>Returns a set of the Facts visible (adjacent) to at least one of 
	 * the Claims in <code>newFal</code>.</p>
	 * @param newFal
	 * @return
	 */
	public static Set<Fact> visibleRules(Set<Claim> newFal){
		return Sledgehammer.massUnion(newFal);
	}
}
