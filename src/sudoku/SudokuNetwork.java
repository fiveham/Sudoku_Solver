package sudoku;

import common.graph.BasicGraph;
import common.graph.Graph;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import sudoku.parse.Parser;

/**
 * <p>A bipartite graph of Facts and Claims.</p>
 * @author fiveham
 */
public class SudokuNetwork extends BasicGraph<NodeSet<?, ?>> implements Sudoku{
	
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
	public SudokuNetwork(int magnitude, Graph<NodeSet<?, ?>> connectedComponent){
		this(magnitude);
		this.nodes.addAll(connectedComponent.nodeStream().collect(Collectors.toList()));
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
	  
	  Map<CellPosition, Fact> cells = nodeStream()
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
