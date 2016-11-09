package sudoku.technique;

import common.BackedSet;
import common.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Puzzle;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

/**
 * <p>The color-chain technique exploits the fact that a Rule with 
 * only two connected Claims is analogous to a {@code xor} operation. 
 * A collection of interconnected two-Claim Rules, regardless of 
 * the size and shape of such a network, has only two possible 
 * solution-states.</p>
 * 
 * @author fiveham
 * 
 */
public class ColorChain extends AbstractTechnique {
	
	/**
	 * <p>Constructs a ColorChain that works to solve the 
	 * specified {@code target}.</p>
	 * @param target the Puzzle that this Technique works 
	 * to solve.
	 */
	public ColorChain(Sudoku puzzle) {
		super(puzzle);
	}
	
	@Override
	public TechniqueEvent process(){
		return implications();
	}
	
	@Override
	public ColorChain apply(Sudoku sudoku){
		return new ColorChain(sudoku);
	}
	
	/**
	 * <p>Tries to find an overlap among the consequences of each 
	 * of the Claims of a given Fact in the puzzle hypothetically 
	 * being true, starting from the smallest Facts in the puzzle
	 * and increasing in Fact size from there.</p>
	 * @return a TechniqueEvent describing the Fact whose Claims' 
	 * consequences led to progress in solving the puzzle and the 
	 * Claims that were falsified in that step of progress, or 
	 * {@code null} if no progress was made
	 */
	private TechniqueEvent implications(){
		for(Fact f : target.factStream()
				.sorted(SMALL_TO_LARGE)
				.collect(Collectors.toList())){
			TechniqueEvent result = implications(f);
			if(result != null){
				return result;
			}
		}
		return null;
	}
	
	public static final Comparator<Collection<?>> SMALL_TO_LARGE = 
			(small,large) -> Integer.compare(large.size(), small.size());
	
	/**
	 * <p>Tries to find an overlap among the consequences of each 
	 * of the Claims of {@code f} in the puzzle hypothetically 
	 * being true.</p>
	 * @return a TechniqueEvent describing the Fact whose Claims' 
	 * consequences led to progress in solving the puzzle and the 
	 * Claims that were falsified in that step of progress, or 
	 * {@code null} if no progress was made
	 */
	private TechniqueEvent implications(Fact f){
		Logic logic = new Logic(f);
		
		while(logic.consequenceIntersection().isEmpty() && logic.isDepthAvailable()){
			logic.exploreDepth();
		}
		
		Set<Claim> con = logic.consequenceIntersection();
		if(!con.isEmpty()){
			return new SolveEventImplications(f, con).falsifyClaims();
		}
		return null;
	}
	
	private class Logic {

		private final Puzzle puzzle;
		private Collection<WhatIf> whatIfs;
		
		/**
		 * 
		 * @param claims
		 * @throws IllegalArgumentException if {@code claims} is empty.
		 */
		public Logic(Set<? extends Claim> claims){
			whatIfs = claims.stream()
					.map((c) -> new WhatIf(c))
					.collect(Collectors.toList());
			try{
				this.puzzle = claims.iterator().next().getPuzzle();
			} catch(NoSuchElementException e){
				throw new IllegalArgumentException("Could not get any Claims from the specified set.");
			}
		}
		
		public Set<Claim> consequenceIntersection(){
			return whatIfs.stream()
					.map(WhatIf::consequences)
					.collect(Sets.massIntersectionCollector());
		}
		
		/*
		 * TODO use allMatch instead. If any WhatIf has no depth available 
		 * and is preventing the intersection from being non-empty, then 
		 * that intersection will never be useful and analysis should short-circuit
		 */
		public boolean isDepthAvailable(){
			return whatIfs.stream().anyMatch(WhatIf::isDepthAvailable);
		}
		
		public void exploreDepth(){
			whatIfs = whatIfs.stream()
					.map(WhatIf::exploreDepth)
					.collect(Sets.massUnionCollector());
		}
		
		private class WhatIf implements Cloneable{
			
			private final BackedSet<Claim> assumptions;
			private final BackedSet<Claim> consequences;
			
			public WhatIf(Claim c){
				assumptions = puzzle.claimUniverse().back();
				assumptions.add(c);
				consequences = puzzle.claimUniverse().back(c.visible());
			}
			
			/**
			 * <p>Constructs a WhatIf having the specified {@code assumptions}, 
			 * {@code consequences}, and {@code puzzle}. Used to {@link #clone() clone} 
			 * a WhatIf.</p>
			 * @param assumptions
			 * @param consequences
			 * @param puzzle
			 * @see #clone()
			 */
			private WhatIf(Set<Claim> assumptions, Set<Claim> consequences, Puzzle puzzle){
				this.assumptions = puzzle.claimUniverse().back(assumptions);
				this.consequences = puzzle.claimUniverse().back(consequences);
			}
			
			/**
			 * <p>Adds {@code c} to this WhatIf as a Claim assumed to be true, and 
			 * adds the Claims {@link sudoku.NodeSet#visible() visible} to {@code c} 
			 * as Claims concluded to be false.</p>
			 * @param c a Claim to be assumed true
			 * @return true if this WhatIf's collection of assumed true Claims or this 
			 * WhatIf's collection of concluded false Claims was changed by this operation, 
			 * false otherwise
			 * @throws IllegalStateException if {@code c} is already known to be false based 
			 * on the other Claims assumed true in this WhatIf or if the set of Claims 
			 * {@link sudoku.NodeSet#visible() visible} to {@code c} intersects this WhatIf's 
			 * set of Claims assumed true
			 */
			public boolean assumeTrue(Claim c){
				boolean result = assumptions.add(c) | consequences.addAll(c.visible());
				if(!Collections.disjoint(assumptions, consequences)){
					throw new IllegalStateException("Overlap between Claims assumed true and Claims concluded false");
				}
				if(hasIllegalEmptyFact()){
					throw new IllegalStateException("A Fact would have all false Claims or multiple true Claims.");
				}
				return result;
			}
			
			private Collection<Claim> consequences(){
				return consequences;
			}
			
			public boolean isDepthAvailable(){
				return partiallyAccountedFacts()
						.findFirst().isPresent();
			}
			
			public Collection<WhatIf> exploreDepth(){
				return smallestPartiallyAccountedFact().stream()
						.map(this::explore)
						.filter((a) -> a != null)
						.collect(Collectors.toList());
			}
			
			private WhatIf explore(Claim c){
				try{
					WhatIf out = clone();
					out.assumeTrue(c);
					return out;
				} catch(IllegalStateException e){
					return null;
				}
			}
			
			@Override
			public WhatIf clone(){
				return new WhatIf(assumptions, consequences, puzzle);
			}
			
			private boolean hasIllegalEmptyFact(){
				return !filteredAccountedFacts(ColorChain::factFullyAccounted)
						.allMatch((f) -> {
							Set<Claim> trueIntersection = assumptions.clone();
							trueIntersection.retainAll(f);
							Set<Claim> falseIntersection = consequences.clone();
							falseIntersection.retainAll(f);
							return trueIntersection.size() == 1 && falseIntersection.size() == f.size() - 1;
						}); 
			}
			
			private Fact smallestPartiallyAccountedFact(){
				return partiallyAccountedFacts()
						.sorted(ColorChain.SMALL_TO_LARGE)
						.findFirst().get();
			}
			
			private Stream<Fact> partiallyAccountedFacts(){
				return filteredAccountedFacts(ColorChain::factPartiallyAccounted);
			}
			
			private Stream<Fact> filteredAccountedFacts(Function<Map<Fact,Integer>,Predicate<Fact>> func){
				Map<Fact,Integer> lastSizes = new HashMap<>();
				return target.factStream()
						.peek((f) -> {
							Set<Claim> bs = new BackedSet<>(puzzle.claimUniverse(), f);
							bs.removeAll(assumptions);
							bs.removeAll(consequences);
							lastSizes.put(f, bs.size());
						})
						.filter(func.apply(lastSizes));
			}
			
			@Override
			public boolean equals(Object o){
				if(o instanceof WhatIf){
					WhatIf that = (WhatIf) o;
					return this.puzzle() == that.puzzle()
							&& this.assumptions.equals(that.assumptions) 
							&& this.consequences.equals(that.consequences);
				}
				return false;
			}
			
			private Puzzle puzzle(){
				return puzzle;
			}
			
			@Override
			public int hashCode(){
				return assumptions.hashCode() + consequences.hashCode();
			}
		}
	}
	
	private static Predicate<Fact> factPartiallyAccounted(Map<Fact,Integer> lastSizes){
		return (f) -> 0 < lastSizes.get(f) && lastSizes.get(f) < f.size();
	}
	
	private static Predicate<Fact> factFullyAccounted(Map<Fact,Integer> lastSizes){
		return (f) -> lastSizes.get(f) == 0;
	}
	
	public static class SolveEventImplications extends TechniqueEvent{
		
		private final Fact initFact;
		
		SolveEventImplications(Fact f, Set<Claim> falsifiedClaims){
			super(falsifiedClaims);
			this.initFact = f;
		}
		
		@Override
		protected String toStringStart() {
			return "Exploration of the consequences of the possible solutions of " + initFact;
		}
	}
}
