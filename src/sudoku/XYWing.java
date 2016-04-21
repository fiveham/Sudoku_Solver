package sudoku;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.List;
import java.util.HashSet;

public class XYWing extends Technique {
	
	/**
	 * Least number of cells in a connected network 
	 * that can affect any cells
	 */
	public static final int MIN_COMPONENT_SIZE = 3;
	
	/**
	 * Constructor.
	 * @param target			The target to which this 
	 * instance of this technique pertains.
	 */
	public XYWing(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * If the target is already solved, skip digestion. 
	 * Lists all the cells that have two possible values. 
	 * For each of the triplets among these where two cells 
	 * connect to the other cell but not to each other
	 * and the two cells that do not connect to each other
	 * share one possible vale in common and each share one 
	 * other value in common with the cell that connects to 
	 * both, finds all the cells that have both of these cells as 
	 * a neighbor and marks the value shared by the 
	 * non-connecting pair of cells impossible in these cells.
	 */
	public boolean digest(){
		
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		//first, get a list of the cells with two candidates
		Set<Cell> candidatePairCells = cellsWithTwoCandidates(puzzle);
		
		//iterate over all the size-3 subcomponents of the available cells
		for(List<Cell> currentTriad : GraphTheory.subgraphs(candidatePairCells, 3, this) )
		
			
			//check whether it's structured like a water molecule
			if( GraphTheory.hasLinearTopology(currentTriad, this) ){
				
				//Check whether the hydrogen-positioned cells have
				//different candidate lists
				List<Cell> wingTips = getWingTips(currentTriad);
				Cell cell1 = wingTips.get(0);
				Cell cell2 = wingTips.get(1);
				if( intersection(cell1.getPossibleValues(),cell2.getPossibleValues()).size() == SHARED_VALUE_COUNT ){
					
					//Get the list of cells that connect to the two
					//"wing tip" cells from the current XY-Wing triad
					Set<Cell> affectedCells = affectableCells(puzzle, currentTriad);
					
					//normally at this point you would want to check each cell to see if
					//it has candidates pertaining to the XY-wing under analysis, but
					//the way that this list is constructed, any cell in it necessarily
					//has a pertinent candidate
					
					//thus, it is now time to resolve
					for(Cell currentCell : affectedCells)
						puzzleHasUpdated |= currentCell.setImpossibleValue(getTipSharedValue( wingTips ));
				}
			}
		
		return puzzleHasUpdated;
	}
	
	/**
	 * Returns a new set equal to the intersection of a and b.
	 * The returned set must contain only elements found in both
	 * a and b.
	 * @param a					A set
	 * @param b					A set
	 * @return					Returns a new set equal to the 
	 * intersection of a and b. The returned set must contain 
	 * only elements found in both a and b.
	 */
	public static <T> Set<T> intersection(Set<T> a, Set<T> b){
		Set<T> returnSet = new HashSet<>(a);
		returnSet.retainAll(b);
		return returnSet;
	}
	
	/*
	 * returns a set of cells able to be affected by the triad
	 */
	private Set<Cell> affectableCells(Puzzle puzzle, List<Cell> triad){
		Set<Cell> returnList = new HashSet<Cell>();
		
		//get the list of the two cells that can actually affect cells
		ArrayList<Cell> wingTips = getWingTips(triad);
		
		//get all the neighbor cells of the meaningful network cells
		ArrayList<Cell> cellsInRegions = wingTips.get(0).neighbors();
		ArrayList<Cell> neighbors2 = wingTips.get(1).neighbors();
		
		//combine those two lists
		for( Cell currentCell : neighbors2 )
			if( !cellsInRegions.contains(currentCell) )
				cellsInRegions.add(currentCell);
		
		//iterate over this list of possibly affected cells
		//and return a list of those deemed affectable
		for(Cell currentCell : cellsInRegions)
			if(cellIsAffectedByTriad(currentCell, triad))
				returnList.add(currentCell);
		
		return returnList;
	}
	
	/*
	 * returns a list of the two cells from the 
	 * triad that do not connect to each other
	 * @throws IllegalArgumentException
	 */
	private ArrayList<Cell> getWingTips(List<Cell> triad){
		ArrayList<Cell> wingTips = new ArrayList<Cell>();
		Cell cell1 = triad.get(0);
		Cell cell2 = triad.get(1);
		Cell cell3 = triad.get(2);
		if( !connection().test(cell1, cell2) ){
			wingTips.add(cell1);
			wingTips.add(cell2);
		}
		else if( !connection().test(cell2, cell3) ){
			wingTips.add(cell2);
			wingTips.add(cell3);
		}
		else if( !connection().test(cell1, cell3) ){
			wingTips.add(cell1);
			wingTips.add(cell3);
		}
		else{								//This code should never be reached
			throw new IllegalArgumentException("cells connect triangularly");
		}
		return wingTips;
	}
	
	private static final int SHARED_VALUE_COUNT = 1;
	
	/*
	 * Returns the value shared by the two cells in a triad 
	 * that do not connect with each other.
	 */
	private Value getTipSharedValue(List<Cell> wingTips){
		
		List<Value> sharedValues = new ArrayList<>(wingTips.get(0).getPossibleValues());
		sharedValues.retainAll( wingTips.get(1).getPossibleValues() );
		/*Set<Value> sharedValues = Set.intersection(
				wingTips.get(0).getPossibleValues(), wingTips.get(1).getPossibleValues() );*/
		
		if(sharedValues.size() != SHARED_VALUE_COUNT)
			throw new IllegalArgumentException("cells do not share 1 value (they share "+sharedValues.size()+" instead)");
		
		return sharedValues.get(0);
	}
	
	/*
	 * Returns whether a cell has the two non-connecting 
	 * cells in a triad as neighbors and shares with them
	 * the value that they share in common.
	 */
	private boolean cellIsAffectedByTriad(Cell cell, List<Cell> triad){
		
		//If the cell's candidate list doesn't contain the value
		//that this XY-Wing can eliminate, then this cell isn't affected
		if( !cell.isPossibleValue( getTipSharedValue( getWingTips(triad) ) ) )
			return false;
		
		//If the cell is in the network, then it is not affected by the network
		if(triad.contains(cell))
			return false;
		
		return true;
	}
	
	/*
	 * Returns a set of the cells in the parameter target that 
	 * have only two values possible.
	 */
	private static Set<Cell> cellsWithTwoCandidates(Puzzle puzzle){
		
		final int VALUE_PAIR_SIZE = 2;
		
		Set<Cell> returnList = new HashSet<Cell>();
		
		for(int y=Index.MINIMUM.toInt(); y<=Index.MAXIMUM.toInt(); y++)
			for(int x=Index.MINIMUM.toInt(); x<=Index.MAXIMUM.toInt(); x++){
				Cell currentCell = puzzle.getCells()[x-1][y-1];
				
				if( currentCell.getPossibleValues().size() == VALUE_PAIR_SIZE )
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
	public static final int SHARED_CANDIDATE_COUNT = 1;
	
	public static final BiPredicate<Cell,Cell> CELL_CONNECTION = (cell1,cell2) -> intersection(cell1.getPossibleValues(), cell2.getPossibleValues()).size() == SHARED_CANDIDATE_COUNT
			&& cell1.sharesRegionWith(cell2);
	
	/**
	 * Returns whether two cells connect according to the 
	 * standards of this technique.
	 * Two cells are connected if they share a region and 
	 * share one possible value in common from their 
	 * candidate lists.
	 * @return					Returns whether two cells 
	 * connect according to the standards of this technique.
	 * Two cells are connected if they share a region and 
	 * share one possible value in common from their 
	 * candidate lists.
	 */
	@Override
	public BiPredicate<Cell,Cell> connection(){
		return CELL_CONNECTION;
	}
}
