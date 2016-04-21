package sudoku;

import java.util.function.BiPredicate;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class SinglesChain extends Technique {
	
	private Value tempValue;
	
	public SinglesChain(Puzzle puzzle){
		super(puzzle);
		tempValue = Value.UNKNOWN;
	}
	
	public boolean digest() {
		boolean puzzleHasUpdated = false;
		
		tempValue = Value.UNKNOWN;
		
		for(Value currentValue : Value.KNOWN_VALUES){
			tempValue = currentValue;
			
			//get list of cells that have the current value in their candidate list
			List<Cell> cellsForValue = cellsForValue(puzzle, currentValue);
			
			//separate that list into connected components
			Set<? extends Set<Cell>> components = GraphTheory.connectedComponents(cellsForValue, 1, this);
			
			//iterate over the connected components
			for( Set<Cell> currentComponent : components ){
				
				
				
			}
			
		}
		
		tempValue = Value.UNKNOWN;
		
		return puzzleHasUpdated;
	}
	
	public static final int POSSIBLE_CELLS_COUNT = 2;
	
	@Override
	public BiPredicate<Cell,Cell> connection(){
		return (c1, c2) -> connect(tempValue, c1, c2);
	}
	
	private static boolean connect(Value v, Cell cell1, Cell cell2) {
		
		Region sharedRegion = null;
		if( cell1.getBlock().contains(cell2) ){
			sharedRegion = cell1.getBlock();
		}
		else if(cell1.getRow().contains(cell2)){
			sharedRegion = cell1.getRow();
		}
		else if(cell1.getColumn().contains(cell2)){
			sharedRegion = cell1.getColumn();
		}
		else{
			return false;	//cell1 and cell2 do not share a region so do not connect
		}
		
		//if their shared region doesn't have just the two of
		//them as its cellsPossibleForValue list, return false
		//(just compare the size of the list to 2)
		if( sharedRegion.cellsPossibleForValue(v).size() != POSSIBLE_CELLS_COUNT )
			return false;
		
		return true;
	}

}
