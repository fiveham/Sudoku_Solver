package sudoku;

import java.util.ArrayList;
import java.util.function.BiPredicate;

import common.ComboGen;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

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
		ArrayList<Set<Value>> pairsList = candidatePairs(puzzle);
		// for each one,
		for(Set<Value> currentPair : pairsList ){
			// get the list of cells with that value pair for its candidate list
			Set<Cell> cellsForNetworks = cellsWithCandidateList(puzzle, currentPair);
			
			// get the list of valid connected components of the current list of cells
			Set<? extends Set<Cell>> networks = GraphTheory.connectedComponents(cellsForNetworks, MIN_COMPONENT_SIZE, this);
			
			// for each connected component
			for(Set<Cell> currentNetwork : networks){
				
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
	private static ArrayList<Set<Value>> candidatePairs(Puzzle puzzle){
		ArrayList<Set<Value>> returnList = new ArrayList<Set<Value>>();
		
		for(int y=Index.MINIMUM.toInt(); y<=Index.MAXIMUM.toInt(); y++)
			for(int x=Index.MINIMUM.toInt(); x<=Index.MAXIMUM.toInt(); x++){
				Set<Value> possibleValues = puzzle.getCells()[x-1][y-1].getPossibleValues();
				
				if( possibleValues.size() == PAIR_SIZE && 
						!returnList.contains(possibleValues) ){
					
					//Determine how many cells have this 
					int currentPairCount = 1;
					for(int yy = y; yy <= Index.MAXIMUM.toInt(); yy++)
						for(int xx = x+1; xx <= Index.MAXIMUM.toInt(); xx++)
							if( puzzle.getCells()[xx-1][yy-1].getPossibleValues().equals(possibleValues) )
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
	private static Set<Cell> cellsWithCandidateList(Puzzle puzzle, Set<Value> pair){
		Set<Cell> returnList = new HashSet<Cell>();
		
		for(int y=Index.MINIMUM.toInt(); y<=Index.MAXIMUM.toInt(); y++)
			for(int x=Index.MINIMUM.toInt(); x<=Index.MAXIMUM.toInt(); x++){
				Cell currentCell = puzzle.getCells()[x-1][y-1];
				if(currentCell.getPossibleValues().equals(pair))
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
	/*
	 * Returns a list of cells from the parameter target that are 
	 * able to be affected by the network.
	 */
	private ArrayList<Cell> affectableCells(Puzzle puzzle, Set<Cell> pairNetwork){
		ArrayList<Cell> returnList = new ArrayList<Cell>();
		
		for(int y=Index.MINIMUM.toInt(); y<=Index.MAXIMUM.toInt(); y++)
			for(int x=Index.MINIMUM.toInt(); x<=Index.MAXIMUM.toInt(); x++){
				Cell currentCell = puzzle.getCells()[x-1][y-1];
				
				if(cellIsAffectedByNetwork(currentCell, new Graph(pairNetwork)))
					returnList.add(currentCell);
			}
		
		return returnList;
	}
	
	/*
	 * Returns whether the parameter cell is affected by 
	 * the parameter network (list of cells).
	 */
	private boolean cellIsAffectedByNetwork(Cell cell, Graph network){
		
		final int MIN_CONNECTED_CELLS = 2;
		
		/* 
		 * If there's no intersection in the candidate lists of the cell and the network
		 * then the cell isn't affected by the network
		 */
		//if( Set.intersection(cell.getPossibleValues(), network.extract().getPossibleValues()).isEmpty() )
		Set<Value> cellPossVals = cell.getPossibleValues();
		Set<Value> netElemPossVals = network.element().getPossibleValues();
		cellPossVals.retainAll(netElemPossVals);
		if( cellPossVals.isEmpty() ){
			return false;
		}
		
		//If the cell is in the network, then it is not affected by the network
		//in the way that this analysis technique is meant to deal with.
		if(network.contains(cell)){
			return false;
		}
		 
		//get the list of cells from the network that are connected to
		//the cell in question
		Set<Cell> connectedCells = new HashSet<Cell>();
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
	private boolean cellsContainBothPhasesInNetwork(Set<Cell> neighbors, Set<Cell> network){
		//Set<Set<Cell>> pairsList = Set.combinationsForMagnitude(neighbors, PAIR_SIZE);
		//for(Set<Cell> cellPair : pairsList){
		for(List<Cell> cellPair : new ComboGen<Cell>(neighbors, PAIR_SIZE, PAIR_SIZE)){
			if( GraphTheory.walkLengthInNetwork(cellPair.get(0), cellPair.get(1), new ArrayList<>(network), this) % 2 == 1 ){	//Step length is odd
				return true;
			}
		}
		
		return false;
	}
	
	/*
	 * Marks all the values in the parameter values set as 
	 * impossible in all the cells in the parameter cells 
	 * set.
	 * 
	 * Returns whether any changes were made.
	 */
	private boolean resolve(ArrayList<Cell> cells, Set<Value> values){
		boolean returnValue = false;
		
		for( Cell currentCell : cells )
			for( Value currentValue : values )
				returnValue |= currentCell.setImpossibleValue(currentValue);
		
		return returnValue;
	}
	
	/* *
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
	/*public boolean connect(Cell cell1, Cell cell2){
		return cell1.sharesRegionWith(cell2);
	}*/
	
	public static final BiPredicate<Cell,Cell> CELL_CONNECTION = (c1, c2) -> c1.sharesRegionWith(c2);
	
	@Override
	public BiPredicate<Cell,Cell> connection(){
		return CELL_CONNECTION;
	}
}
