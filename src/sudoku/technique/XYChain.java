package sudoku.technique;

import common.Pair;
import common.graph.BasicGraph;
import common.graph.Graph;
import common.graph.Wrap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
import sudoku.technique.ColorChain.ColorClaim;
import sudoku.technique.ColorChain.ColorSource;
import sudoku.time.FalsifiedTime;
import sudoku.time.TechniqueEvent;

/**
 * <p>The XYChain technique is similar to ColorChain, but instead of 
 * operating on the firm two-solution nature of a xor-chain, this 
 * operates on chains that are somewhat more vague. This technique 
 * locates all Claims in its {@link #getTarget() operating network} 
 * that belong to a {@link Rule#isXor() xor Rule} and connects such 
 * Claims to each other iff the two connected Claims share a Rule. 
 * This is similar to ColorChain's 
 * {@link ColorChain#visibleColorContradiction(Collection) visible-color} 
 * subtechnique except that the shared Rule connecting two Claims of 
 * the chain does not have to be a xor.</p>
 * 
 * <p>Unlike in a xor-chain, the relationship indicated by a connection 
 * between two Claims only pushes but does not pull: A linked Claim, 
 * if false, does <em>not</em> force its neighboring Claims in the 
 * chain to be true.</p>
 * 
 * <p>Claims outside the chain are falsified if they are visible to 
 * two Claims in the chain that each see exactly one other Claim belonging 
 * to the chain.</p>
 * @see http://www.sadmansoftware.com/sudoku/xychain.php
 * @see http://www.sadmansoftware.com/sudoku/xywing.php
 * @author fiveham
 *
 */
public class XYChain extends AbstractTechnique {
	
	public XYChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	@Override
	protected TechniqueEvent process() {
		
		/*
		 * "False push" refers to a Claim, if true, forcing its visibles to be false 
		 * (pushing falseness to them). All Claims that can see each other have this 
		 * relationship.
		 * 
		 * "True push" refers to a Claim, if false, forcing its visibles to be true 
		 * (pushing trueness to them). Claims have this relationship with Claims that 
		 * they can see through a xor Fact.
		 * 
		 * "Mix push" refers to a push graph (a directed graph) such that nodes of one 
		 * color point to the nodes onto which they push state A (false or true) and 
		 * the nodes of the opposite color point to the nodes onto which they push 
		 * state B (true or false, respectively).
		 */
		
		//Isolate all xor Rules and pack their Claims in one set
		List<Fact> xorFacts = target.factStream()
				.filter(Fact::isXor)
				.collect(Collectors.toList());
		Set<Claim> xorClaims = Sledgehammer.massUnion(xorFacts);
		
		//connect those Claims together based on their sharing a Rule
		Graph<ColorClaim> allFalsePushes = new BasicGraph<>(Wrap.wrap(xorClaims, (c1,c2) -> !Collections.disjoint(c1, c2), ColorClaim::new))
				.addContractEventListenerFactory(new ColorSource());
		
		//reversed relationships from Claim to ColorClaim 
		Map<Claim,ColorClaim> falseMap = Wrap.rawToWrap(allFalsePushes);
		Map<Claim,ColorClaim> trueMap = trueMap(falseMap, xorFacts);
		
		for(Graph<ColorClaim> falsePush : allFalsePushes.connectedComponents()){
			
			List<Pair<Predicate<ColorClaim>, Pair<Graph<ColorClaim>, Map<Claim,ColorClaim>>>> mixPushes = ColorClaim.SIGNS.stream()
					.map((sign) -> new Pair<>(sign, mixPushGraphAndMap(falsePush, sign, falseMap, trueMap)))
					.collect(Collectors.toList());
			
			for(Set<Claim> xorChain : xorChainsInXYChain(falsePush, trueMap)){
				Claim representative = xorChain.iterator().next();
				
				Set<Claim> and = mixPushes.stream()
						.map((pair) -> consequences(pair.getA(), representative, pair.getB().getA(), pair.getB().getB()))
						.collect(Collector.of(
								HashSet::new, 
								Set::addAll, 
								(left, right) -> {
									left.retainAll(right);
									return left;
								}));
				
				if(!and.isEmpty()){
					try{
						TechniqueEvent time = new SolveEventXYChain(and);
						and.stream().forEach((c) -> c.setFalse(time));
						return time;
					} catch(FalsifiedTime.NoUnaccountedClaims e){
						//do nothing
					}
				}
				
				/*
				 * Choosing which one is true first in each scenario just means choosing which 
				 * color performs which kind of push.
				 * 
				 * option-specific directed graphs can be constructed:
				 * If red is true, then reds get their neighbors from the pushFalse graph 
				 * and blues get their neighbors from the pushTrue graph
				 * 
				 * If blue is true, then blues get their neighbors form the pushFalse graph 
				 * and reds their their neighbors from the pushTrue graph
				 */
			}
		}
		
		return null;
	}
	
	private static List<Set<Claim>> xorChainsInXYChain(Graph<ColorClaim> falsePush, Map<Claim,ColorClaim> trueMap){
		return new BasicGraph<ColorClaim>(
				falsePush.nodeStream()
						.map(ColorClaim::wrapped)
						.map(trueMap::get)
						.collect(Collectors.toList()))
				.connectedComponents().stream()
						.map((graph) -> graph.nodeStream()
								.map(ColorClaim::wrapped)
								.collect(Collectors.toSet()))
						.collect(Collectors.toList());
	}
	
	/**
	 * <p>Returns a set of the Claims falsified if {@code xorElement} takes 
	 * on the state (true or false) that {@code state} outputs when given 
	 * as input the {@code ColorClaim} in {@code mixPush} that wraps 
	 * {@code xorElement}.</p>
	 * @param state indicates whether {@code xorElement} is to be treated as 
	 * true or false
	 * @param xorElement a claim from a xor-chain subgraph of {@code mixPush}
	 * @param mixPush a directed graph where one claim points to another claim 
	 * if the first claim, by having a certain state, forces the other claim 
	 * to have the opposite state. The forcing relationship only works for one 
	 * of the two possible states of the first claim; if the first claim has 
	 * the wrong state, it does not force the other claim's state.
	 * @return
	 */
	private static Set<Claim> consequences(
			Predicate<ColorClaim> state, 
			Claim xorElement, 
			Graph<ColorClaim> mixPush,
			Map<Claim,ColorClaim> mixPushMap){
		ColorClaim seed = mixPushMap.get(xorElement);
		
		Graph<ColorClaim> component = mixPush.component(
				mixPush.nodeStream().collect(Collectors.toList()), 
				(list) -> seed, 
				Collections.emptyList());
		
		return component.nodeStream()
				.filter(state) //these ones falsify Claims visible() to them
				.map(ColorClaim::wrapped)
				.map(Claim::visible)
				.collect(Sledgehammer.massUnionCollector());
	}
	
	private static Pair<Graph<ColorClaim>, Map<Claim,ColorClaim>> mixPushGraphAndMap(
			Graph<ColorClaim> falsePush, 
			Predicate<ColorClaim> state, 
			Map<Claim,ColorClaim> falseMap, 
			Map<Claim,ColorClaim> trueMap){
		List<ColorClaim> pushGraphNodes = falsePush.nodeStream()
				.map((cc) -> {
					ColorClaim lambdaResult = new ColorClaim(cc.wrapped());
					lambdaResult.setColor(cc.getColor());
					return lambdaResult;
				})
				.collect(Collectors.toList());
		
		Map<Claim,ColorClaim> pushGraphNodeMap = Wrap.rawToWrap(pushGraphNodes);
		
		//connect elements of pushGraphNodes to one another
		for(ColorClaim cc : pushGraphNodes){
			Map<Claim,ColorClaim> structureMap = state.test(cc) ? falseMap : trueMap;
			List<ColorClaim> neighbors = structureMap.get(cc.wrapped()).neighbors().stream()
					.map(ColorClaim::wrapped)
					.map(pushGraphNodeMap::get)
					.collect(Collectors.toList());
			cc.neighbors().addAll(neighbors);
		}
		
		return new Pair<>(
				new BasicGraph<>(pushGraphNodes), 
				Wrap.rawToWrap(pushGraphNodes));
	}
	
	private static Map<Claim,ColorClaim> trueMap(Map<Claim,ColorClaim> falseMap, Collection<Fact> xorFacts){
		List<ColorClaim> truePushW = Wrap.wrap(xorFacts, ColorClaim::new);
		truePushW.stream().forEach((cc) -> cc.setColor(falseMap.get(cc.wrapped()).getColor()));
		return Wrap.rawToWrap(truePushW);
	}
	
	public class SolveEventXYChain extends TechniqueEvent{
		private SolveEventXYChain(Set<Claim> falsified){
			super(falsified);
		}
		
		@Override
		protected String toStringStart(){
			return "XYChain scenario";
		}
	}
}
