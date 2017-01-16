package sudoku;

import common.BackedSet;
import common.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sudoku.time.TechniqueEvent;

/**
 * <p>The color-chain technique exploits the fact that a Rule with only two connected Claims is
 * analogous to a {@code xor} operation. A collection of interconnected two-Claim Rules, regardless
 * of the size and shape of such a network, has only two possible solution-states.</p>
 * @author fiveham
 */
public class ConsequenceIntersection{
	
  private final Sudoku target;
  
  /**
   * <p>Constructs a ColorChain that works to solve the specified {@code target}.</p>
   * @param target the Puzzle that this Technique works to solve.
   */
	public ConsequenceIntersection(Sudoku puzzle){
	  this.target = puzzle;
	}
	
	public ConsequenceIntersection apply(Sudoku sudoku){
		return new ConsequenceIntersection(sudoku);
	}
	
	protected TechniqueEvent process(){
		return implications();
	}
	
  /**
   * <p>Tries to find an overlap among the consequences of each of the Claims of a given Fact in
   * the puzzle hypothetically being true, starting from the smallest Facts in the puzzle and
   * increasing in Fact size from there.</p>
   * @return a TechniqueEvent describing the Fact whose Claims' consequences led to progress in
   * solving the puzzle and the Claims that were falsified in that step of progress, or
   * {@code null} if no progress was made
   */
	public TechniqueEvent implications(){
		return target.factStream()
				.sorted(Comparator.comparingInt(Fact::size))
				.map(this::implications)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}
	
  /**
   * <p>Tries to find an overlap among the consequences of each of the Claims of {@code f} in the
   * puzzle hypothetically being true.</p>
   * @return a TechniqueEvent describing the Fact whose Claims' consequences led to progress in
   * solving the puzzle and the Claims that were falsified in that step of progress, or
   * {@code null} if no progress was made
   */
	private TechniqueEvent implications(Fact f){
		Set<Claim> con = new Logic(f).findConsequenceIntersection();
		return con.isEmpty() 
				? null
				: new SolveEventImplications(f, con).falsifyClaims();
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
	
	private class Logic {
		
		private final Puzzle puzzle;
		private Collection<WhatIf> whatIfs;
		
    /**
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
					.map(WhatIf::new)
					.collect(Collectors.toList());
			
			popularity = new HashMap<>();
		}
		
		public Set<Claim> findConsequenceIntersection(){
			Set<Claim> result;
			while((result = consequenceIntersection()).isEmpty() && isDepthAvailable()){
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
			int sizeForExploration = sizeForExploration();
			whatIfs = whatIfs.stream()
					.map((wi) -> wi.hasExplorableReducedFact(sizeForExploration)
							? wi.exploreDepth() 
							: new HashSet<>(Arrays.asList(wi))) 
					.reduce((c1, c2) -> {
					  c1.addAll(c2);
					  return c1;
					})
					.get();
		}
		
		private int sizeForExploration(){
			return whatIfs.stream()
					.filter(WhatIf::isDepthAvailable)
					.mapToInt(WhatIf::minReducedFactSize)
					.reduce(Integer.MAX_VALUE, Integer::min);
		}
		
		private void populatePopularity(){
			popularity = Sets.countingUnion(whatIfs.stream()
					.map((wi) -> wi.reducedFacts()
							.collect(Collectors.toList())));
		}
		
		private Map<Fact,Integer> popularity;
		
		private Comparator<WhatIf.ReducedFact> byPopularity(){
			return Comparator.comparingInt(this::popularity).reversed();
		}
		
		private int popularity(WhatIf.ReducedFact f){
			return popularity.getOrDefault(f.getFact(), POPULARITY_IF_ABSENT);
		}
		
		private static final int POPULARITY_IF_ABSENT = 0;
		
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
       * {@code consequences}, and {@code puzzle}. Used to {@link #clone() clone} a
       * WhatIf.</p>
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
			
			private boolean isDepthAvailable(){
				return 0 != partiallyReducedFactsRaw().count();
			}
			
			private boolean hasExplorableReducedFact(int maxReducedFactSizeForExploration){
				return 0 < partiallyReducedFacts()
						.filter((rf) -> rf.reducedSize() <= maxReducedFactSizeForExploration)
						.count();
			}
			
			private Stream<Fact> reducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factReduced, JUST_THE_FACTS);
			}
			
			private Stream<Fact> partiallyReducedFactsRaw(){
				return filteredReducedFacts(ConsequenceIntersection::factPartiallyReduced, JUST_THE_FACTS);
			}
			
			private Stream<ReducedFact> partiallyReducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factPartiallyReduced);
			}
			
			private Stream<ReducedFact> fullyReducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factFullyReduced);
			}
			
			private Stream<ReducedFact> filteredReducedFacts(BiPredicate<Fact,BackedSet<Claim>> test){
				return filteredReducedFacts(test, ReducedFact::new);
			}
			
			private <T> Stream<T> filteredReducedFacts(BiPredicate<Fact,BackedSet<Claim>> test, BiFunction<Fact,BackedSet<Claim>,T> bifu){
				return target.factStream()
						.map((f) -> {
							BackedSet<Claim> bs = new BackedSet<>(puzzle.claimUniverse(), f);
							bs.removeAll(assumptions);
							bs.removeAll(consequences);
							
							T result = test.test(f, bs) 
									? bifu.apply(f, bs) 
									: null;
							return result;
						})
						.filter(Objects::nonNull);
			}
			
			private int minReducedFactSize(){
				return partiallyReducedFacts()
						.map(ReducedFact::reducedSize)
						.reduce(Integer.MAX_VALUE, Integer::min);
			}
			
			private Set<WhatIf> exploreDepth(){
				return claimsToExplore()
						.map(this::explore)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
			}
			
			private Stream<Claim> claimsToExplore(){
				return partiallyReducedFacts()
						.sorted(Comparator.comparingInt(ReducedFact::reducedSize).thenComparing(byPopularity()))
						.findFirst().get().getReducedForm().stream();
			}
			
			private WhatIf explore(Claim c){
				WhatIf out = clone();
				try{
					out.assumeTrue(c);
				} catch(IllegalStateException e){
					return null;
				}
				return out;
			}
			
			@Override
			public WhatIf clone(){
				return new WhatIf(assumptions, consequences, puzzle);
			}
			
      /**
       * <p>Adds {@code c} to this WhatIf as a Claim assumed to be true, and adds the Claims
       * {@link sudoku.NodeSet#visible() visible} to {@code c} as Claims concluded to be
       * false.</p>
       * @param c a Claim to be assumed true
       * @return true if this WhatIf's collection of assumed true Claims or this WhatIf's
       * collection of concluded false Claims was changed by this operation, false otherwise
       * @throws IllegalStateException if {@code c} is already known to be false based on the
       * other Claims assumed true in this WhatIf or if the set of Claims
       * {@link sudoku.NodeSet#visible() visible} to {@code c} intersects this WhatIf's set of
       * Claims assumed true
       */
			private boolean assumeTrue(Claim c){
				boolean result = assumptions.add(c) | consequences.addAll(c.visible());
				if(!BackedSet.disjoint(assumptions, consequences)){
					throw new IllegalStateException("Overlap between Claims assumed true and Claims concluded false");
				}
				if(hasIllegalEmptyFact()){
					throw new IllegalStateException("A Fact would have all false Claims or multiple true Claims.");
				}
				return result;
			}
			
			private boolean hasIllegalEmptyFact(){
				return fullyReducedFacts()
						.anyMatch(ReducedFact::isIllegalIfEmpty); 
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
				
				private Fact getFact(){
					return f;
				}
				
				private BackedSet<Claim> getReducedForm(){
					return reducedForm;
				}
				
				private int reducedSize(){
					return reducedForm.size();
				}
				
				private boolean isIllegalIfEmpty(){
					return !isLegalIfEmpty();
				}
				
				private boolean isLegalIfEmpty(){
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
	
	private static final BiFunction<Fact,BackedSet<Claim>,Fact> JUST_THE_FACTS = (f,bs) -> f;
	
	private static boolean factPartiallyReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return 0 < reducedFact.size() && reducedFact.size() < fullFact.size();
	}
	
	private static boolean factFullyReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return reducedFact.isEmpty();
	}
	
	private static boolean factReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return reducedFact.size() < fullFact.size();
	}
}
