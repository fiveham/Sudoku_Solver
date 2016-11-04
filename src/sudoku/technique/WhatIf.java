package sudoku.technique;

import common.BackedSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Puzzle;

public class WhatIf {
	
	private Set<Claim> assumptions;
	private Set<Claim> consequences;
	private Logic owner;
	private Puzzle puzzle;
	
	public WhatIf(Logic owner, Claim c){
		assumptions = new HashSet<>();
		assumptions.add(c);
		consequences = new HashSet<>(c.visible());
		this.owner = owner;
		this.puzzle = c.getPuzzle();
	}
	
	public boolean assumeTrue(Claim c){
		return assumptions.add(c) | consequences.addAll(c.visible()); //TODO I used this idiom incorrectly somewhere else recently. Find and fix.
	}
	
	public Collection<Claim> consequences(){
		return consequences;
	}
	
	public boolean isDepthAvailable(){
		Map<Fact,Integer> lastSizes = new HashMap<>();
		return owner.getSudoku().factStream()
				.peek((f) -> {
					Set<Claim> bs = new BackedSet<>(puzzle.claimUniverse(), f);
					bs.removeAll(assumptions);
					bs.removeAll(consequences);
					lastSizes.put(f, bs.size());
				})
				.anyMatch((f) -> 0 < lastSizes.get(f) && lastSizes.get(f) < f.size());
	}
}
