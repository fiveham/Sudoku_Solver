package sudoku;

import common.graph.BasicGraph;
import common.graph.Graph;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sudoku.parse.Parser;

/**
 * <p>A bipartite graph of Facts and Claims.</p>
 * @author fiveham
 */
public class SudokuNetwork extends BasicGraph<NodeSet<?,?>> implements Sudoku{
	
  /**
   * <p>The fundamental order of the target to which the nodes of this graph pertain. For a puzzle, 
   * this is the square root of the {@link #sideLength side length}.</p>
   */
	protected final int magnitude;
	
  /**
   * <p>The number of coordinates along any given dimension of the target to which the nodes of this
   * graph belong. For a Puzzle, this is the number of cells along any side of the puzzle.</p>
   */
	protected final int sideLength;
	
	/**
	 * <p>Constructs a SudokuNetwork having the specified {@code magnitude}.</p>
	 * @param magnitude the {@link #magnitude() magnitude} of this SudokuNetwork
	 */
	public SudokuNetwork(int magnitude) {
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	/**
	 * <p>Constructs a SudokuNetwork having the specified {@code magnitude}, and having as its nodes 
	 * exactly the nodes of the specified {@code connectedComponent}.</p>
	 * @param magnitude  the {@link #magnitude() magnitude} of this SudokuNetwork
	 * @param connectedComponent a graph of {@link NodeSet}s which has a single connected component
	 */
	public SudokuNetwork(int magnitude, Graph<NodeSet<?,?>> connectedComponent){
		this(magnitude);
		this.nodes.addAll(connectedComponent.nodeStream().collect(Collectors.toList()));
	}
	
	@Override
	public boolean isSolved(){
		return factStream().allMatch(Fact::isSolved);
	}
	
	@Override
	public Stream<Fact> factStream(){
		return nodes.stream().filter(Fact.class::isInstance).map(Fact.class::cast);
	}
	
	@Override
	public Stream<Claim> claimStream(){
		return nodes.stream().filter(Claim.class::isInstance).map(Claim.class::cast);
	}
	
	@Override
	public int magnitude(){
		return magnitude;
	}
	
	@Override
	public int sideLength(){
		return sideLength;
	}
	
	@Override
	public String toString(){
		Map<Integer,Fact> cells = factStream()
				.filter(Rule.class::isInstance)
				.map(Rule.class::cast)
				.filter(Puzzle.RuleType.CELL::isTypeOf)
				.collect(Collectors.toMap(
						(cell) -> NodeSet.linearizeCoords(0, cell.dimA().intValue(), cell.dimB().intValue(), sideLength()), 
						Function.identity()));
		
		StringBuilder result = new StringBuilder();
		String empty;
		{
			StringBuilder empt = new StringBuilder();
			for(int i=0; i<sideLength(); ++i){
				empt.append(" ");
			}
			empty = empt.toString();
		}
		
		for(int y = 0; y < sideLength(); ++y){
			for(int x = 0; x < sideLength(); ++x){
				result.append("|");
				Fact cell = cells.get(NodeSet.linearizeCoords(0, y, x, sideLength()));
				if(cell == null){
					result.append(empty);
				} else{
					for(int z=0; z<sideLength(); ++z){
						int val = cell.contains(cell.getPuzzle().claim(x, y, z)) ? z : 0;
						String text = val == 0 ? " " : Integer.toString(val,Parser.MAX_RADIX);
						result.append(text);
					}
				}
			}
			result.append("|" + System.lineSeparator());
		}
		
		return result.toString();
	}
	
  /**
   * <p>Returns true if {@code graph}, were it a SudokuNetwork, meets the criteria for a
   * SudokuNetwork to be solved, false otherwise.</p>
   * @param graph a graph to be interpreted as a SudokuNetwork and tested for being in a solved
   * state
   * @return true if {@code graph}, were it a SudokuNetwork, meets the criteria for a
   * SudokuNetwork to be solved, false otherwise
   */
	public static boolean isSolved(Graph<NodeSet<?,?>> graph){
		return graph.nodeStream()
				.filter(Fact.class::isInstance)
				.map(Fact.class::cast)
				.allMatch(Fact::isSolved);
	}
}
