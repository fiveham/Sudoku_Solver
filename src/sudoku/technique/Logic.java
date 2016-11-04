package sudoku.technique;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import common.Sets;
import sudoku.Claim;
import sudoku.Fact;
import sudoku.Sudoku;

public class Logic {
	
	private final Collection<WhatIf> whatIf;
	private final Sudoku sudoku;
	
	public Logic(Set<? extends Claim> claims, Sudoku sudoku){
		whatIf = claims.stream()
				.map((c) -> new WhatIf(this,c))
				.collect(Collectors.toList());
		this.sudoku = sudoku;
	}
	
	public Set<Claim> consequenceIntersection(){
		return whatIf.stream()
				.map(WhatIf::consequences)
				.collect(Sets.massIntersectionCollector());
	}
	
	public boolean isDepthAvailable(Fact f){
		return whatIf.stream().anyMatch(WhatIf::isDepthAvailable);
	}
	
	public void exploreDepth(){
		//TODO stub
	}
	
	public Sudoku getSudoku(){
		return sudoku;
	}
}
