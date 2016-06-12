package sudoku;

import common.graph.BasicGraph;
import common.graph.Graph;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sudoku.parse.Parser;

/**
 * <p>A bipartite graph of Rules and Claims.</p>
 * @author fiveham
 *
 */
public class SudokuNetwork extends BasicGraph<NodeSet<?,?>> implements Sudoku{
	
	/**
	 * <p>The fundamental order of the target to which the nodes of this graph pertain, 
	 * equal to the square root of the {@link #sideLength side length}.</p>
	 */
	protected final int magnitude;
	
	/**
	 * <p>The number of coordinates along a dimension of the target to which the nodes 
	 * of this graph belong.</p>
	 */
	protected final int sideLength;
	
	public SudokuNetwork(int magnitude) {
		this.magnitude = magnitude;
		this.sideLength = magnitude*magnitude;
	}
	
	public SudokuNetwork(int magnitude, Graph<NodeSet<?,?>> connectedComponent){
		this(magnitude);
		this.nodes.addAll(connectedComponent.nodeStream().collect(Collectors.toList()));
	}
	
	/**
	 * <p>Returns true if this SudokuNetwork is solved, false otherwise. A 
	 * SudokuNetwork is solved iff all of its {@code Fact}s each have only 
	 * one {@code Claim}.</p>
	 * @return true if this SudokuNetwork is solved, false otherwise
	 */
	@Override
	public boolean isSolved(){
		return factStream().allMatch(Fact::isSolved);
	}
	
	@Override
	public Stream<Fact> factStream(){
		return nodes.stream().filter(Fact.class::isInstance).map(Fact.class::cast);
	}
	
	/**
	 * <p>Returns a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.</p>
	 * @return a Stream of all the {@code Claim}-type nodes in this 
	 * Puzzle's underlying graph.
	 */
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
}
