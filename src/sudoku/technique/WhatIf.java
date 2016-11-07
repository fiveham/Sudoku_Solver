package sudoku.technique;

import common.BackedSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Puzzle;
import sudoku.Sudoku;

public class WhatIf implements Cloneable{
	
	private Set<Claim> assumptions;
	private Set<Claim> consequences;
	private Puzzle puzzle;
	private Sudoku sudoku;
	
	public WhatIf(Sudoku sudoku, Claim c){
		assumptions = new HashSet<>();
		assumptions.add(c);
		consequences = new HashSet<>(c.visible());
		this.puzzle = c.getPuzzle();
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
	private WhatIf(Set<Claim> assumptions, Set<Claim> consequences, Puzzle puzzle, Sudoku sudoku){
		this.assumptions = new HashSet<>(assumptions);
		this.consequences = new HashSet<>(consequences);
		this.puzzle = puzzle;
		this.sudoku = sudoku;
	}
	
	/*
	 * TODO redesign WhatIf's allocation of true and false Claims to prevent contradictions
	 * caused by assuming true a Claim visible to another assumed-true Claim
	 * This will also help develop a system to account for contradictory assumptions whose 
	 * contradictory natures aren't immediately obvious.
	 * TODO throw an exception when new assumptions and consequences cause a Fact to contain only 
	 * false Claims.
	 */
	public boolean assumeTrue(Claim c){
		return assumptions.add(c) | consequences.addAll(c.visible());
	}
	
	public Collection<Claim> consequences(){
		return consequences;
	}
	
	public boolean isDepthAvailable(){
		return partiallyAccountedFacts()
				.findFirst().isPresent();
	}
	
	public Collection<WhatIf> exploreDepth(){
		return smallestAffectedFact().stream()
				.map(this::explore)
				.collect(Collectors.toList());
	}
	
	private WhatIf explore(Claim c){
		WhatIf out = clone();
		out.assumeTrue(c);
		return out;
	}
	
	@Override
	public WhatIf clone(){
		return new WhatIf(assumptions, consequences, puzzle, sudoku);
	}
	
	private Fact smallestAffectedFact(){
		return partiallyAccountedFacts()
				.sorted(ColorChain.SMALL_TO_LARGE)
				.findFirst().get();
	}
	
	//TODO track a list of partially affected facts, and enhance it when needed.
	private Stream<Fact> partiallyAccountedFacts(){
		Map<Fact,Integer> lastSizes = new HashMap<>();
		return sudoku.factStream()
				.peek((f) -> {
					Set<Claim> bs = new BackedSet<>(puzzle.claimUniverse(), f);
					bs.removeAll(assumptions);
					bs.removeAll(consequences);
					lastSizes.put(f, bs.size());
				})
				.filter(factPartiallyAccounted(lastSizes));
	}
	
	private Predicate<Fact> factPartiallyAccounted(Map<Fact,Integer> lastSizes){
		return (f) -> 0 < lastSizes.get(f) && lastSizes.get(f) < f.size();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof WhatIf){
			WhatIf that = (WhatIf) o;
			return this.sudoku == that.sudoku
					&& this.puzzle == that.puzzle
					&& this.assumptions.equals(that.assumptions) 
					&& this.consequences.equals(that.consequences);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return assumptions.hashCode() + consequences.hashCode();
	}
}
