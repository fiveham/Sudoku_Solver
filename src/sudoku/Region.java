package sudoku;

import java.util.*;

public abstract class Region implements Comparable<Region> {
	
	List<Role> roles;
	
	protected Puzzle puzzle;
	protected Index index;
	
	protected Region(Puzzle p, Index i){
		this.puzzle = p;
		this.index = i;
	}
	
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	public Index index(){
		return index;
	}
	
	public Role role(Symbol s){
		for(Role r : roles){
			if(r.symbol()==s){
				return r;
			}
		}
		return null;
	}
	
	abstract public int compareTo(Region otherRegion);
	
}
