package sudoku;

import common.graph.BasicGraph;
import common.graph.Graph;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import sudoku.parse.Parser;

/**
 * <p>A bipartite graph of Facts and Claims.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class SudokuNetwork extends BasicGraph<NodeSet<?,?>> implements Sudoku{
	
    /**
     * <p>The fundamental order of the target to which the nodes of this graph pertain, equal to the
     * square root of the {@link #sideLength side length}.</p>
     */
	protected final int magnitude;
	
    /**
     * <p>The number of coordinates along a dimension of the target to which the nodes of this graph
     * belong.</p>
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
     * <p>Returns true if this SudokuNetwork is solved, false otherwise. A SudokuNetwork is solved
     * iff all of its {@code Fact}s each have only one {@code Claim}.</p>
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
     * <p>Returns a Stream of all the {@code Claim}-type nodes in this Puzzle's underlying
     * graph.</p>
     * @return a Stream of all the {@code Claim}-type nodes in this Puzzle's underlying graph.
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
	  class CellPosition{
	    
	    private final int x;
	    private final int y;
	    
	    CellPosition(int x, int y){
	      this.y = y;
	      this.x = x;
	    }
	    
	    @Override
	    public boolean equals(Object o){
	      if(o instanceof CellPosition){
	        CellPosition c = (CellPosition) o;
	        return c.y == y && c.x == x;
	      }
	      return false;
	    }
	    
	    @Override
	    public int hashCode(){
	      return sideLength() * y + x;
	    }
	  }
	  
	  Map<CellPosition, Fact> cells = factStream()
        .filter(Rule.class::isInstance)
        .map(Rule.class::cast)
        .filter(Puzzle.RuleType.CELL::isTypeOf)
        .collect(Collectors.toMap(
            (cell) -> new CellPosition(cell.dimB().intValue(), cell.dimA().intValue()), 
            Function.identity()));
		
		String empty = IntStream.range(0, sideLength())
		    .mapToObj((i) -> " ")
		    .collect(Collectors.joining());
		StringBuilder result = new StringBuilder();
		for(int y = 0; y < sideLength(); ++y){
			for(int x = 0; x < sideLength(); ++x){
				result.append("|");
				Fact cell = cells.get(new CellPosition(x, y));
				if(cell == null){
					result.append(empty);
				} else{
				  Puzzle p = cell.getPuzzle();
				  int local_x = x;
				  int local_y = y;
				  result.append(IntStream.range(0, sideLength())
				      .mapToObj((z) -> cell.contains(p.claim(local_x, local_y, z)) 
				          ? Integer.toString(z, Parser.MAX_RADIX) 
				          : " ")
				      .collect(Collectors.joining()));
				}
			}
			result.append("|" + System.lineSeparator());
		}
		
		return result.toString();
	}
}
