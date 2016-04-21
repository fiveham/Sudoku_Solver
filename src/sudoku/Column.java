package sudoku;

import java.util.Arrays;
import java.util.List;

public class Column extends Region {
	
	public Column(Puzzle p, Index i){
		super(p,i);
	}
	
	public int compareTo(Region otherRegion){
		if(otherRegion instanceof Column){
			if(puzzle != otherRegion.puzzle){
				throw new IllegalArgumentException("These two Columns do not belong to the same Puzzle.");
			}
			List<Index> l = Arrays.asList(Index.values());
			return l.indexOf(index) - l.indexOf(otherRegion.index);
			
		} else{
			throw new IllegalArgumentException("This Region is a Column and can only be compare to another Column.");
		}
	}
	
}
