package sudoku;

import java.util.Arrays;
import java.util.List;

public class Row extends Region {
	
	public Row(Puzzle p, Index i){
		super(p,i);
	}
	
	public int compareTo(Region otherRegion){
		if(otherRegion instanceof Row){
			if(puzzle != otherRegion.puzzle){
				throw new IllegalArgumentException("These two Rows do not belong to the same Puzzle.");
			}
			List<Index> l = Arrays.asList(Index.values());
			return l.indexOf(index) - l.indexOf(otherRegion.index);
			
		} else{
			throw new IllegalArgumentException("This Region is a Row and can only be compare to another Row.");
		}
	}
}
