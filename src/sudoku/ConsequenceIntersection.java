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
		
		/**
		 * <p>Returns the maximum allowable size that a Fact's reduced counterpart can have in a WhatIf 
		 * if that reduced fact can be explored.</p>
		 * @return the maximum allowable size for an explorable reduced counterpart of a Fact
		 */
		private int sizeForExploration(){
			return whatIfs.stream()
					.filter(WhatIf::isDepthAvailable)
					.mapToInt(WhatIf::minReducedFactSize)
					.reduce(Integer.MAX_VALUE, Integer::min);
		}
		
		/**
		 * <p>Populates this Logic's popularity Map.</p>
		 */
		private void populatePopularity(){
			popularity = Sets.countingUnion(whatIfs.stream()
					.map((wi) -> wi.reducedFacts()
							.collect(Collectors.toList())));
		}
		
		private Map<Fact,Integer> popularity;
		
		/**
		 * <p>Returns a comparator that compares ReducedFacts by the popularity of their Facts in this 
		 * Logic, putting the most popular ReducedFact first. Equivalent to 
		 * {@code Comparator.comparingInt(this::popularity).reversed()}.</p>
		 * @return a comparator that compares ReducedFacts by the popularity of their Facts in this 
     * Logic, putting the most popular ReducedFact first
		 */
		private Comparator<WhatIf.ReducedFact> byPopularity(){
			return Comparator.comparingInt(this::popularity).reversed();
		}
		
		/**
		 * <p>Returns the popularity of the specified ReducedFact's Fact among this Logic's WhatIfs. 
		 * A Fact's popularity is the number of WhatIfs in this Logic which have accounted for at least 
		 * one Claim from that Fact.</p>
		 * @param f the ReducedFact whose Fact's popularity is returned
		 * @return the popularity of the specified ReducedFact's Fact among this Logic's WhatIfs
		 */
		private int popularity(WhatIf.ReducedFact f){
			return popularity.getOrDefault(f.getFact(), POPULARITY_IF_ABSENT);
		}
		
		private static final int POPULARITY_IF_ABSENT = 0;
		
		/**
		 * <p>A hypothetical scenario where some Claims are {@link WhatIf#assumptions assumed} to be 
		 * true, and the {@link WhatIf#consequences consequentially false} Claims are tracked.</p>
		 * @author fiveham
		 */
		private class WhatIf implements Cloneable{
			
		  /**
		   * <p>Claims that are assumed to be true: "WhatIf these Claims were true?"</p>
		   */
			private final BackedSet<Claim> assumptions;
			
			/**
			 * <p>Claims that must be false if the Claims in {@code assumptions} are all true.</p>
			 */
			private final BackedSet<Claim> consequences;
			
			/**
			 * <p>Constructs a WhatIf that assumes {@code c} is true, concludes the Claims visible to 
			 * {@code c} are false, and pertains to {@code c}'s puzzle.</p>
			 * @param c the Claim that this WhatIf initially assumes is true
			 */
			public WhatIf(Claim c){
				assumptions = puzzle.claimUniverse().back();
				assumptions.add(c);
				consequences = puzzle.claimUniverse().back(c.visible());
			}
			
      /**
       * <p>Constructs a WhatIf having the specified {@code assumptions},
       * {@code consequences}, and {@code puzzle}. Used to {@link #clone() clone} a
       * WhatIf.</p>
       * @param assumptions the Claims this WhatIf assumes are true
       * @param consequences the Claims this WhatIf concludes must be false
       * @param puzzle the Puzzle to which this WhatIf pertains
       * @see #clone()
       */
			private WhatIf(Set<Claim> assumptions, Set<Claim> consequences, Puzzle puzzle){
				this.assumptions = puzzle.claimUniverse().back(assumptions);
				this.consequences = puzzle.claimUniverse().back(consequences);
			}
			
			/**
			 * <p>Returns this WhatIf's consequences, Claims falsified by this WhatIf's assumptions.</p>
			 * @return this WhatIf's consequences
			 */
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

      /**
       * <p>Returns a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences}.</p>
       * <p>This method's resulting stream contains all Facts that have been 
       * {@link #partiallyReducedFactsRaw() partially reduced} and all Facts that have been 
       * {@link #fullyReducedFacts() fully reduced}.</p>
       * @return a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences}
       */
			private Stream<Fact> reducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factReduced, JUST_THE_FACTS);
			}
			
			/**
       * <p>Returns a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences} and has at least one of its Claims not included in either of those 
       * sets.</p>
       * <p>This method returns a stream of just the Facts, without pairing the qualifying each 
       * qualifying Fact with its reduced counterpart, making this method slightly faster than  
       * {@link #partiallyReducedfacts() its counterpart}, where applicable.</p>
       * @return a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences} and has at least one of its Claims not included in either of those 
       * sets
       */
			private Stream<Fact> partiallyReducedFactsRaw(){
				return filteredReducedFacts(ConsequenceIntersection::factPartiallyReduced, JUST_THE_FACTS);
			}
			
			/**
       * <p>Returns a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences} and has at least one of its Claims not included in either of those 
       * sets.</p>
       * <p>Each qualifying Fact is paired, via a ReducedFact, with a modified copy of that Fact 
       * from which the Fact's elements that occur in {@code assumptions} or {@code consequences} 
       * have been removed.</p>
       * @return a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been partially accounted for by this WhatIf's assumptions and consequences
       * so that each qualifying Fact has at least one of its Claims included in {@code assumptions}
       * or {@code consequences} and has at least one of its Claims not included in either of those 
       * sets
       */
			private Stream<ReducedFact> partiallyReducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factPartiallyReduced);
			}
			
			/**
			 * <p>Returns a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
			 * puzzle that have been fully accounted for by this WhatIf's assumptions and consequences so 
			 * that each Claim of each qualifying Fact is a member of either {@code assumptions} or 
			 * {@code consequences}.</p>
			 * <p>Each qualifying Fact is paired, via a ReducedFact, with a modified copy of that Fact 
       * from which the Fact's elements that occur in {@code assumptions} or {@code consequences} 
       * have been removed. For fully reduced Facts, the modified copy is empty.</p>
			 * @return a stream of the Facts from this WhatIf's containing ConsequenceIntersection's 
       * puzzle that have been fully accounted for by this WhatIf's assumptions and consequences so 
       * that each Claim of each qualifying Fact is a member of either {@code assumptions} or 
       * {@code consequences}
			 */
			private Stream<ReducedFact> fullyReducedFacts(){
				return filteredReducedFacts(ConsequenceIntersection::factFullyReduced);
			}
			
			/**
			 * <p>Streams the Facts of the puzzle of the ConsequenceIntersection that contains this 
       * WhatIf, copies each Fact, removes this WhatIf's consequences and assumptions from each 
       * copy, and maps each copy either to null or to a new ReducedFact made using the original 
       * Fact and the modified copy depending on whether the original Fact and the modified copy 
       * fail or pass the test defined by {@code test}, respectively. Any null elements are filtered
       * out of the resulting stream.</p>
			 * @param test a test that tests one of Facts from the puzzle of the containing 
       * ConsequenceIntersection paired with a modified copy of that Fact from which this WhatIf's 
       * assumptions and consequences have been removed
			 * @return a stream of non-null ReducedFacts based on inputs that passed the specified 
			 * {@code test} involving copies of the containing ConsequenceIntersection's puzzle's Facts 
			 * from which this WhatIf's assumptions and consequences were removed
			 */
			private Stream<ReducedFact> filteredReducedFacts(BiPredicate<Fact,BackedSet<Claim>> test){
				return filteredReducedFacts(test, ReducedFact::new);
			}
			
			/**
			 * <p>Streams the Facts of the puzzle of the ConsequenceIntersection that contains this 
			 * WhatIf, copies each Fact, removes this WhatIf's consequences and assumptions from each 
			 * copy, and maps each copy either to null or to the output of {@code bifu} when {@code bifu} 
			 * is sent the original Fact and the modified copy depending on whether the original Fact and 
			 * the modified copy fail or pass the test defined by {@code test}, respectively. Any null 
			 * elements are filtered out of the resulting stream.</p>
			 * @param <T> the type of the elements of the resulting stream
			 * @param test a test that tests one of Facts from the puzzle of the containing 
			 * ConsequenceIntersection paired with a modified copy of that Fact from which this WhatIf's 
			 * assumptions and consequences have been removed
			 * @param bifu a function that accepts one of the Facts from the puzzle of the containing 
			 * ConsequenceIntersection paired with a modified copy of that Fact form which this WhatIf's 
			 * assumptions and consequences have been removed
			 * @return a stream of non-null outputs of {@code bifu} based on inputs that passed the 
			 * specified {@code test} involving copies of the containing ConsequenceIntersection's 
			 * puzzle's Facts from which this WhatIf's assumptions and consequences were removed
			 */
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
			
			/**
			 * <p>Returns the smallest size to which a Fact has been partially reduced by the exploration 
			 * of possible solutions states in this WhatIf.</p>
			 * @return the smallest size to which a Fact has been partially reduced by the exploration 
       * of possible solutions states in this WhatIf
			 */
			private int minReducedFactSize(){
				return partiallyReducedFacts()
						.map(ReducedFact::reducedSize)
						.reduce(Integer.MAX_VALUE, Integer::min);
			}
			
			/**
			 * <p>Explores one further layer of depth in possible solution states among the Facts of the 
			 * puzzle.</p>
			 * @return a set of WhatIfs based on this WhatIf, each of which assumes one addition Claim to 
			 * be true
			 * @throws NoSuchElementException if this WhatIf has no 
       * {@link #partiallyReducedFacts() partially reduced Facts}
			 */
			private Set<WhatIf> exploreDepth(){
				return claimsToExplore()
						.map(this::explore)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
			}
			
			/**
			 * <p>Returns a Stream of Claims that should be explored by being assumed true.</p>
			 * @return a Stream of Claims that should be explored by being assumed true
			 * @throws NoSuchElementException if this WhatIf has no 
			 * {@link #partiallyReducedFacts() partially reduced Facts}
			 */
			private Stream<Claim> claimsToExplore(){
				return partiallyReducedFacts()
						.sorted(Comparator.comparingInt(ReducedFact::reducedSize).thenComparing(byPopularity()))
						.findFirst()
						.get()
						.getReducedForm()
						.stream();
			}
			
			/**
			 * <p>Tries to create a WhatIf based on this WhatIf which explores the consequences of 
			 * {@code c} being true. If {@code c} cannot be true, based on the other assumptions in this 
			 * WhatIf, then null is returned.</p>
			 * @param c a Claim to be assumed true
			 * @return a WhatIf based on this WhatIf incorporating the idea that {@code c} is true, or 
			 * null if such a WhatIf is illegal
			 */
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
			
			/**
			 * <p>Returns true if any of the Facts of this WhatIf, as altered, are, if empty, also 
			 * {@link ReducedFact#isIllegalIfEmpty() illegal}.</p>
			 * @return
			 */
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
			
			/**
			 * <p>Returns the puzzle to which pertains the ConsequenceIntersection enclosing this WhatIf. 
			 * This is a convenience method which makes WhatIf.equals() less verbose.</p>
			 * @return the puzzle to which pertains the ConsequenceIntersection enclosing this WhatIf
			 */
			private Puzzle puzzle(){
				return puzzle;
			}
			
			@Override
			public int hashCode(){
				return assumptions.hashCode() + consequences.hashCode();
			}
			
			/**
			 * <p>Pairs a Fact as yet unaltered by this analysis technique with a hypothetical altered 
			 * state it may take on.</p>
			 * @author fiveham
			 */
			private class ReducedFact{
				
				private final Fact f;
				private final BackedSet<Claim> reducedForm;
				
				/**
				 * <p>Constructs a ReducedFact pairing the unaltered Fact {@code f} with a smaller subset, 
				 * {@code reducedForm}.</p>
				 * @param f the Fact which has been reduced to {@code reducedForm}
				 * @param reducedForm a proper subset of {@code f}
				 */
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
				
				/**
				 * <p>Returns the Fact of this ReducedFact.</p>
				 * @return the Fact of this ReducedFact
				 */
				private Fact getFact(){
					return f;
				}
				
				/**
				 * <p>Returns the reduced form of this ReducedFact.</p>
				 * @return the reduced form of this ReducedFact
				 */
				private BackedSet<Claim> getReducedForm(){
					return reducedForm;
				}
				
				/**
				 * <p>Returns the size of the reduced form for this ReducedFact.</p>
				 * @return the size of the reduced form for this ReducedFact
				 */
				private int reducedSize(){
					return reducedForm.size();
				}
        
        /**
         * <p>Return true if this ReducedFact would be illegal if it were empty, false 
         * otherwise.</p>
         * @return true if this ReducedFact would be illegal if it were empty, false otherwise
         */
				private boolean isIllegalIfEmpty(){
					return !isLegalIfEmpty();
				}
				
				/**
				 * <p>Return true if this ReducedFact would be legal if it were empty, false otherwise.</p>
				 * @return true if this ReducedFact would be legal if it were empty, false otherwise
				 */
				private boolean isLegalIfEmpty(){
					return intersectionHasSize(f, assumptions, Fact.TRUE_CLAIM_COUNT) 
							&& intersectionHasSize(f, consequences, f.size() - Fact.TRUE_CLAIM_COUNT);
				}
				
				/**
				 * <p>Returns true if the intersection of {@code f} and {@code set} has {@code size} 
				 * elements.</p>
				 * @param f a set of Claims
				 * @param set a set of Claims
				 * @param size the number of elements the intersection of {@code f} and {@code set} should 
				 * have
				 * @return true if the intersection of {@code f} and {@code set} has {@code size} elements, 
				 * false otherwise
				 */
				private boolean intersectionHasSize(Fact f, BackedSet<Claim> set, int size){
					Set<Claim> result = set.clone();
					result.retainAll(f);
					return result.size() == size;
				}
			}
		}
	}
	
	/**
	 * <p>Given a Fact and a BackedSet, this outputs the Fact.</p>
	 */
	private static final BiFunction<Fact, BackedSet<Claim>, Fact> JUST_THE_FACTS = (f, bs) -> f;
	
	/**
	 * <p>Returns true if {@code reducedFact} is partially but not completely reduced from 
	 * {@code fullFact}.</p>
	 * @param fullFact a Fact which has been reduced to {@code reducedFact}
	 * @param reducedFact a subset of {@code fullFact}
	 * @return true if {@code reducedFact} is partially but not completely reduced from 
   * {@code fullFact}, false otherwise
	 */
	private static boolean factPartiallyReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return 0 < reducedFact.size() && reducedFact.size() < fullFact.size();
	}
	
	/**
	 * <p>Returns true if {@code fullFact}'s reduced counterpart has been fully reduced, having no 
	 * elements left in it.</p>
	 * @param fullFact a Fact
	 * @param reducedFact a subset of {@code fullFact} from which zero or more Claims have been 
	 * removed
	 * @return true if {@code reducedFact} is empty, false otherwise
	 */
	private static boolean factFullyReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return reducedFact.isEmpty();
	}
	
	/**
	 * <p>Returns true if {@code reducedFact} is smaller than {@code fullFact}, false otherwise.</p>
	 * <p>This method is a convenience for the sake of {@link Logic.WhatIf#reducedFacts()}.</p>
	 * @param fullFact a Fact whose possible solution states are being explored
	 * @param reducedFact a subset of {@code fullFact} from which zero or more Claims have been 
	 * removed
	 * @return true if {@code reducedFact} is smaller than {@code fullFact}, false otherwise
	 */
	private static boolean factReduced(Fact fullFact, BackedSet<Claim> reducedFact){
		return reducedFact.size() < fullFact.size();
	}
}
