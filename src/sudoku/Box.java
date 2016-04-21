package sudoku;

import java.util.*;

public class Box extends Region {
	
	public Box(Puzzle p, Index i){
		super(p,i);
	}
	
	public int compareTo(Region otherRegion){
		if(otherRegion instanceof Box){
			if(puzzle != otherRegion.puzzle){
				throw new IllegalArgumentException("These two Boxes do not belong to the same Puzzle.");
			}
			List<Index> l = Arrays.asList(Index.values());
			return l.indexOf(index) - l.indexOf(otherRegion.index);
			
		} else{
			throw new IllegalArgumentException("This Region is a Box and can only be compare to another Box.");
		}
	}
}
