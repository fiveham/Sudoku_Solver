package sudoku.technique;

import common.ComboGen;
import common.Pair;
import common.graph.BasicGraph;
import common.graph.Graph;
import common.graph.Wrap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;
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
		Set<Claim> xorClaims = target.factStream().filter(Fact::isXor).collect(Sledgehammer.massUnionCollector());
		List<Wrap<Claim>> linked = Wrap.wrap(xorClaims, (c1, c2) -> !Collections.disjoint(c1, c2));
		Collection<Graph<Wrap<Claim>>> chains = new BasicGraph<>(linked).connectedComponents();
		
		for(Graph<Wrap<Claim>> chain : chains){
			List<Wrap<Claim>> unused = chain.nodeStream().filter((w) -> w.neighbors().size() == 1).collect(Collectors.toList());
			
			for(List<Wrap<Claim>> pair : new ComboGen<>(unused, Pair.ITEM_COUNT, Pair.ITEM_COUNT)){
				Wrap<Claim> w0 = pair.get(0);
				Wrap<Claim> w1 = pair.get(1);
				
				Set<Claim> vis0 = w0.wrapped().visible();
				Set<Claim> vis1 = w1.wrapped().visible();
				
				Set<Claim> visBoth = vis0;
				visBoth.retainAll(vis1);
				
				if(!visBoth.isEmpty()){
					try{
						TechniqueEvent event = new SolveEventXYChain(visBoth);
						visBoth.stream().forEach((c) -> c.setFalse(event));
						return event;
					} catch(FalsifiedTime.NoUnaccountedClaims e){
						continue;
					}
				}
			}
		}
		
		return null;
	}
	
	public class SolveEventXYChain extends TechniqueEvent{
		private SolveEventXYChain(Set<Claim> falsified){
			super(falsified);
		}
	}
}
