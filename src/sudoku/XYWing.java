package sudoku;

import java.util.*;

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
		
		final int SHARED_VALUE_COUNT = 1;
		
		boolean puzzleHasUpdated = false;
		
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		//first, get a list of the cells with two candidates
		List<Cell> candidatePairCells = cellsWithTwoCandidates(puzzle);
		
		//iterate over all the size-3 subcomponents of the available cells
		for(List<Cell> currentTriad : GraphTheory.subgraphs(candidatePairCells, 3, this) )
		
			
			//check whether it's structured like a water molecule
			if( GraphTheory.hasLinearTopology(currentTriad, this) ){
				
				//Check whether the hydrogen-positioned cells have
				//different candidate lists
				Cell[] wingTips = getWingTips(currentTriad);
				Cell cell1 = wingTips[0];
				Cell cell2 = wingTips[1];
				if( SetTheory.intersection( cell1.getPossibleValues(), 
						cell2.getPossibleValues() ).size() == SHARED_VALUE_COUNT ){
					
					//Get the list of cells that connect to the two
					//"wing tip" cells from the current XY-Wing triad
					List<Cell> affectedCells = affectableCells(puzzle, currentTriad);
					
					//normally at this point you would want to check each cell to see if
					//it has candidates pertaining to the XY-wing under analysis, but
					//the way that this list is constructed, any cell in it necessarily
					//has a pertinent candidate
					
					//thus, it is now time to resolve
					for(Cell currentCell : affectedCells)
						puzzleHasUpdated |= currentCell.setValueImpossible(getTipSharedValue( wingTips ));
				}
			}
		
		return puzzleHasUpdated;
	}
	
	/*
	 * returns a set of cells able to be affected by the triad
	 */
	private List<Cell> affectableCells(Puzzle puzzle, List<Cell> triad){
		List<Cell> returnList = new ArrayList<Cell>();
		
		//get the list of the two cells that can actually affect cells
		Cell[] wingTips = getWingTips(triad);
		
		//get all the neighbor cells of the meaningful network cells
		ArrayList<Cell> cellsInRegions = wingTips[0].neighbors();
		ArrayList<Cell> neighbors2 = wingTips[1].neighbors();
		
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
	private Cell[] getWingTips(List<Cell> triad){
		Cell cell1 = triad.get(0);
		Cell cell2 = triad.get(1);
		Cell cell3 = triad.get(2);
		if( !connect(cell1, cell2) ){
			return new Cell[]{cell1, cell2};
		}
		else if( !connect(cell2, cell3) ){
			return new Cell[]{cell2, cell3};
		}
		else if( !connect(cell1, cell3) ){
			return new Cell[]{cell1, cell3};
		}
		else{								//This code should never be reached
			throw new IllegalArgumentException("cells connect triangularly");
		}
	}
	
	/*
	 * Returns the value shared by the two cells in a triad 
	 * that do not connect with each other.
	 */
	private Value getTipSharedValue(Cell[] wingTips){
		
		final int SHARED_VALUE_COUNT = 1;
		
		List<Value> sharedValues = SetTheory.intersection(
				wingTips[0].getPossibleValues(), wingTips[1].getPossibleValues() );
		
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
	private static List<Cell> cellsWithTwoCandidates(Puzzle puzzle){
		
		final int VALUE_PAIR_SIZE = 2;
		
		List<Cell> returnList = new ArrayList<>();
		
		for(int y=Index.MINIMUM.intValue(); y<=Index.MAXIMUM.intValue(); y++)
			for(int x=Index.MINIMUM.intValue(); x<=Index.MAXIMUM.intValue(); x++){
				Cell currentCell = puzzle.getCell(x,y);
				
				if( currentCell.getPossibleValues().size() == VALUE_PAIR_SIZE )
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
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
	public boolean connect(Cell cell1, Cell cell2){
		final int SHARED_CANDIDATE_COUNT = 1;
		return SetTheory.intersection(cell1.getPossibleValues(), cell2.getPossibleValues()).size() == SHARED_CANDIDATE_COUNT
				&& cell1.sharesRegionWith(cell2);
	}
}
