package sudoku.technique;

import common.BackedSet;
import common.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
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
	public ColorChain apply(Sudoku sudoku){
		return new ColorChain(sudoku);
	}
	
	@Override
	public TechniqueEvent process(){
		return implications();
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
		Optional<TechniqueEvent> result = target.factStream()
				.sorted(Comparator.comparingInt(Fact::size))
				.map(this::implications)
				.filter(Objects::nonNull)
				.findFirst();
		return result.isPresent() 
				? result.get() 
				: null;
	}
	
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
		Set<Claim> con = new Logic(f).nonEmptyConsequenceIntersection();
		return con.isEmpty() 
				? null
				: new SolveEventImplications(f, con).falsifyClaims();
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
			try{
				this.puzzle = claims.iterator().next().getPuzzle();
			} catch(NoSuchElementException e){
				throw new IllegalArgumentException("Could not get any Claims from the specified set.");
			}
			
			whatIfs = claims.stream()
					.map((c) -> new WhatIf(c))
					.collect(Collectors.toList());
			
			popularity = new HashMap<>();
		}
		
		public Set<Claim> nonEmptyConsequenceIntersection(){
			Set<Claim> result;
			while( (result = consequenceIntersection()).isEmpty() && isDepthAvailable()){
				exploreDepth();
			}
			return result;
		}
		
		public Set<Claim> consequenceIntersection(){
			return whatIfs.stream()
					.map(WhatIf::consequences)
					.collect(Sets.massIntersectionCollector());
		}
		
		public boolean isDepthAvailable(){
			return whatIfs.stream().anyMatch(WhatIf::isDepthAvailable);
		}
		
		public void exploreDepth(){
			populatePopularity();
			whatIfs = whatIfs.stream()
					.map(WhatIf::exploreDepth)
					.collect(Sets.massUnionCollector());
		}
		
		private void populatePopularity(){
			popularity = Sets.countingUnion(whatIfs.stream()
					.map((wi) -> wi.reducedFacts()
							.collect(Collectors.toList())));
		}
		
		private int popularity(WhatIf.ReducedFact rf){
			return popularity.containsKey(rf) 
					? popularity.get(rf) 
					: 0;
		}
		
		private Map<WhatIf.ReducedFact,Integer> popularity;
		
		private Comparator<WhatIf.ReducedFact> byPopularity(){
			return Comparator.comparingInt(this::popularity).reversed();
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
			
			private Collection<Claim> consequences(){
				return consequences;
			}
			
			public boolean isDepthAvailable(){
				return 0 != partiallyReducedFacts().count();
			}
			
			private Stream<ReducedFact> reducedFacts(){
				return filteredReducedFacts(ColorChain::factReduced);
			}
			
			private Stream<ReducedFact> partiallyReducedFacts(){
				return filteredReducedFacts(ColorChain::factPartiallyReduced);
			}
			
			private Stream<ReducedFact> fullyReducedFacts(){
				return filteredReducedFacts(ColorChain::factFullyReduced);
			}
			
			private Stream<ReducedFact> filteredReducedFacts(BiPredicate<Fact,BackedSet<Claim>> test){
				return target.factStream()
						.map((f) -> {
							BackedSet<Claim> bs = new BackedSet<>(puzzle.claimUniverse(), f);
							bs.removeAll(assumptions);
							bs.removeAll(consequences);
							
							ReducedFact result = test.test(f, bs) 
									? new ReducedFact(f, bs) 
									: null;
							return result;
						})
						.filter(Objects::nonNull);
			}
			
			public Collection<WhatIf> exploreDepth(){
				return claimsToExplore()
						.map(this::explore)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
			}
			
			private Stream<Claim> claimsToExplore(){
				return partiallyReducedFacts()
						.sorted(Comparator.comparingInt(ReducedFact::reducedSize).thenComparing(byPopularity()))
						.findFirst().get().getReducedForm().stream();
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
			
			private boolean hasIllegalEmptyFact(){
				return fullyReducedFacts()
						.anyMatch(ReducedFact::isIllegalEmpty); 
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
			
			private class ReducedFact{
				
				private final Fact f;
				private final BackedSet<Claim> reducedForm;
				
				ReducedFact(Fact f, BackedSet<Claim> reducedForm){
					this.f = f;
					this.reducedForm = reducedForm;
				}
				
				@Override
				public boolean equals(Object o){
					if(o instanceof ReducedFact){
						ReducedFact that = (ReducedFact) o;
						return that.f.equals(this.f) && that.reducedForm.equals(this.reducedForm); 
					}
					return false;
				}
				
				@Override
				public int hashCode(){
					return f.hashCode() + reducedForm.hashCode();
				}
				
				public Fact getFact(){
					return f;
				}
				
				public int initialSize(){
					return f.size();
				}
				
				public BackedSet<Claim> getReducedForm(){
					return reducedForm;
				}
				
				public int reducedSize(){
					return reducedForm.size();
				}
				
				public boolean isIllegalEmpty(){
					return !isLegalEmpty();
				}
				
				public boolean isLegalEmpty(){
					return intersectionHasSize(f, assumptions, Fact.TRUE_CLAIM_COUNT) 
							&& intersectionHasSize(f, consequences, f.size() - Fact.TRUE_CLAIM_COUNT);
				}
				
				private boolean intersectionHasSize(Fact f, BackedSet<Claim> set, int size){
					Set<Claim> result = set.clone();
					result.retainAll(f);
					return result.size() == size;
				}
			}
		}
	}
	
	private static boolean factPartiallyReduced(Fact f, BackedSet<Claim> bs){
		return 0 < bs.size() && bs.size() < f.size();
	}
	
	private static boolean factFullyReduced(Fact f, BackedSet<Claim> bs){
		return bs.size() == 0;
	}
	
	private static boolean factReduced(Fact f, BackedSet<Claim> bs){
		return bs.size() < f.size();
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
