package sudoku;

import java.util.*;

/**
 * A technique for solving sudoku puzzles.
 * A chain or network of cells that have the 
 * same pair of possible values is described. 
 * Cells outside the network that have at 
 * least two cells in the network as neighbors,
 * where there exists a pair among those 
 * neighbors that have an odd walk length 
 * between them in their network have both 
 * of the values that all cells in the 
 * network share as possible values marked 
 * impossible.
 * @author fiveham
 */
public class RemotePairs extends Technique{
	
	/**
	 * The number of values in a cell that can be part
	 * of a chain of remote pairs.
	 */
	public static final int PAIR_SIZE = 2;
	
	/**
	 * The minimum number of cells that a graph of remote
	 * pairs must have in order for the graph itself to have 
	 * any impact on any cells.
	 */
	public static final int MIN_COMPONENT_SIZE = 4;
	
	/**
	 * Constructor.
	 * @param target			The target to which this instance of this
	 * analysis technique pertains.
	 */
	public RemotePairs(Puzzle puzzle){
		super(puzzle);
	}
	
	/**
	 * Skips analysis if the target is already solved.
	 * Iterates over all the networks of cells that have 
	 * exactly two values possible in them. Iterates over 
	 * all connected components of at least a certain 
	 * minimum size within the current graph, in case 
	 * any one graph is internally disconnected.
	 * Checks all cells outside the current connected 
	 * component to see if it has two cells in the 
	 * component that are an odd walk length apart in 
	 * the network as neighbors. Marks as impossible the values 
	 * in the value pair that defines the network in 
	 * all these cells that have two odd-walk-length-apart
	 * cells from the network as neighbors.
	 * @return					Returns whether any changes were 
	 * made to this technique's target as a result of this analysis.
	 */
	public boolean digest(){
		boolean puzzleHasUpdated = false;
		
		// skip digestion if the target is already solved
		if(puzzle.isSolved())
			return puzzleHasUpdated;
		
		// get a list of all existing value pairs in the target
		List<List<Value>> pairsList = candidatePairs(puzzle);
		// for each one,
		for(List<Value> currentPair : pairsList ){
			// get the list of cells with that value pair for its candidate list
			List<Cell> cellsForNetworks = cellsWithCandidateList(puzzle, currentPair);
			
			// get the list of valid connected components of the current list of cells
			List<List<Cell>> networks = GraphTheory.connectedComponents(cellsForNetworks, MIN_COMPONENT_SIZE, this);
			
			// for each connected component
			for(List<Cell> currentNetwork : networks){
				
				// get the list of all cells that share at least 
				// two regions with the cells of the network
				ArrayList<Cell> affectableCells = affectableCells(puzzle, currentNetwork);
				
				// if that list is not empty,
				if( !affectableCells.isEmpty() ){
					
					// resolve a LineHatch task based on
					// that list and the current pair of values, setting puzzleHasUpdated to true 
					// in the process.
					puzzleHasUpdated |= resolve(affectableCells, currentPair);;
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	/*
	 * Returns a list of all the pairs of values that some cell
	 * or cells in the target has or have as its or their list 
	 * of possible values.
	 */
	private static List<List<Value>> candidatePairs(Puzzle puzzle){
		List<List<Value>> returnList = new ArrayList<>();
		
		for(int y=Index.MINIMUM.intValue(); y<=Index.MAXIMUM.intValue(); y++)
			for(int x=Index.MINIMUM.intValue(); x<=Index.MAXIMUM.intValue(); x++){
				List<Value> possibleValues = puzzle.getCell(x,y).getPossibleValues();
				
				if( possibleValues.size() == PAIR_SIZE && 
						!returnList.contains(possibleValues) ){
					
					//Determine how many cells have this 
					int currentPairCount = 1;
					for(int yy = y; yy <= Index.MAXIMUM.intValue(); yy++)
						for(int xx = x+1; xx <= Index.MAXIMUM.intValue(); xx++)
							if( puzzle.getCell(xx, yy).getPossibleValues().equals(possibleValues) )
								currentPairCount++;
							
					if(currentPairCount >= MIN_COMPONENT_SIZE)
						returnList.add(possibleValues);
				}
			}
		
		return returnList;
	}
	
	/*
	 * Returns a list of all the cells in the target that 
	 * have the parameter pair as their list of possible 
	 * values.
	 */
	private static List<Cell> cellsWithCandidateList(Puzzle puzzle, List<Value> pair){
		List<Cell> returnList = new ArrayList<>();
		
		for(int y=Index.MINIMUM.intValue(); y<=Index.MAXIMUM.intValue(); y++)
			for(int x=Index.MINIMUM.intValue(); x<=Index.MAXIMUM.intValue(); x++){
				Cell currentCell = puzzle.getCell(x, y);
				if(currentCell.getPossibleValues().equals(pair))
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
	/*
	 * Returns a list of cells from the parameter target that are 
	 * able to be affected by the network.
	 */
	private ArrayList<Cell> affectableCells(Puzzle puzzle, List<Cell> pairNetwork){
		ArrayList<Cell> returnList = new ArrayList<Cell>();
		
		for(int y=Index.MINIMUM.intValue(); y<=Index.MAXIMUM.intValue(); y++)
			for(int x=Index.MINIMUM.intValue(); x<=Index.MAXIMUM.intValue(); x++){
				Cell currentCell = puzzle.getCell(x,y);
				
				if(cellIsAffectedByNetwork(currentCell, pairNetwork))
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
	/*
	 * Returns whether the parameter cell is affected by 
	 * the parameter network (list of cells).
	 */
	private boolean cellIsAffectedByNetwork(Cell cell, List<Cell> network){
		
		final int MIN_CONNECTED_CELLS = 2;
		
		/* 
		 * If there's no intersection in the candidate lists of the cell and the network
		 * then the cell isn't affected by the network
		 */
		if( SetTheory.intersection(cell.getPossibleValues(), network.get(0).getPossibleValues()).isEmpty() )
			return false;
		
		//If the cell is in the network, then it is not affected by the network
		//in the way that this analysis technique is meant to deal with.
		if(network.contains(cell))
			return false;
		 
		//get the list of cells from the network that are connected to
		//the cell in question
		List<Cell> connectedCells = new ArrayList<>();
		for(Cell networkCell : network)
			if( cell.sharesRegionWith(networkCell) )
				connectedCells.add(networkCell);
		
		//
		if( connectedCells.size() < MIN_CONNECTED_CELLS)
			return false;
		
		//check whether the cells to which the cell in question is
		//connected are out of phase (if a certain one of them is
		//Value A, then at least one other must be Value B.)
		if( !cellsContainBothPhasesInNetwork(connectedCells, network) )
			return false;
		
		return true;
	}
	
	/*
	 * Returns whether there exists a walk of odd length between
	 * any two cells from the parameter neighbors through the graph 
	 * defined by the parameter network.
	 */
	private boolean cellsContainBothPhasesInNetwork(List<Cell> neighbors, List<Cell> network){
		
		final int PAIR_SIZE = 2;
		
		List<List<Cell>> pairsList =
				GraphTheory.combinationsForMagnitude(neighbors, PAIR_SIZE);
		for(List<Cell> cellPair : pairsList)
			if( GraphTheory.walkLengthInNetwork(cellPair.get(0), cellPair.get(1), network, this) % 2 == 1 )	//Step length is odd
				return true;
		
		return false;
	}
	
	/*
	 * Marks all the values in the parameter values set as 
	 * impossible in all the cells in the parameter cells 
	 * set.
	 * 
	 * Returns whether any changes were made.
	 */
	private boolean resolve(ArrayList<Cell> cells, List<Value> values){
		boolean returnValue = false;
		
		for( Cell currentCell : cells )
			for( Value currentValue : values )
				returnValue |= currentCell.setValueImpossible(currentValue);
		
		return returnValue;
	}
	
	/**
	 * Returns whether two cells are connected by the standards 
	 * of this analysis technique.
	 * 
	 * This technique's standard specifies that two cells are 
	 * connected if they share a region in common--if any one 
	 * block, row, or column contains them both.
	 * @return					 Returns whether two cells 
	 * are connected by the standards 
	 * of this analysis technique.
	 * 
	 * This technique's standard specifies that two cells are 
	 * connected if they share a region in common--if any one 
	 * block, row, or column contains them both.
	 */
	public boolean connect(Cell cell1, Cell cell2){
		return cell1.sharesRegionWith(cell2);
	}
}
