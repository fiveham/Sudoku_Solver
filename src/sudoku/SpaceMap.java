package sudoku;

import common.NCuboid;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import sudoku.Puzzle.IndexValue;
import sudoku.Puzzle.IndexInstance;

/**
 * <p>Wraps a three-dimensional array of Claims in order to expedite access to them based on their
 * spatial coordinates or geometric constraints.</p> <p>Using only
 * {@link Puzzle#claimStream() claimStream()} to find the one Claim with specific coordinates is a
 * O(n^6) technique, where n is the magnitude of the target. Accessing the desired Claim directly
 * via its spatial coordinates can be achieved in O(1) time when storing them in an array or
 * ordering them in the Puzzle's underlying Graph's underlying list of vertices. Providing a
 * secondary means to access Claims in a spatial manner is preferable to adding extra constraints
 * onto the backing collection of vertices for the puzzle's Graph.</p>
 * @author fiveham
 * @author fiveham
 *
 */
class SpaceMap implements Iterable<Claim>{
	
	private Claim[][][] stuff;
	private Puzzle puzzle;
	
    /**
     * <p>Constructs a SpaceMap pertaining to the specified Puzzle and creates the Claims for that
     * Puzzle.</p>
     * @param target the Puzzle whose Claims are created and managed
     */
	public SpaceMap(Puzzle puzzle) {
		this.puzzle = puzzle;
		stuff = new Claim[puzzle.sideLength()][puzzle.sideLength()][puzzle.sideLength()];
		for(IndexValue x : puzzle.indexValues()){
			for(IndexValue y : puzzle.indexValues()){
				for(IndexValue symbol : puzzle.indexValues()){
					stuff[x.intValue()][y.intValue()][symbol.intValue()] = new Claim(puzzle, x,y,symbol);
				}
			}
		}
	}
	
    /**
     * <p>Returns the Claim in this SpaceMap's target having the specified spatial coordinates.</p>
     * @param x the x-coordinate of the Claim returned
     * @param y the y-coordinate of the Claim returned
     * @param z the z-coordinate (symbol) of the Claim returned
     * @throws ArrayIndexOutOfBoundsException if {@code target.sideLength()}
     * @return the Claim in this Puzzle having the specified spatial coordinates
     */
	public Claim get(int x, int y, int z){
		return stuff[x][y][z];
	}
	
    /**
     * <p>Returns the Claim in this SpaceMap's target having the specified spatial coordinates.</p>
     * @param x the x-coordinate of the Claim returned
     * @param y the y-coordinate of the Claim returned
     * @param s the z-coordinate (symbol) of the Claim returned
     * @return the Claim in this SpaceMap's target having the specified spatial coordinates
     */
	public Claim get(IndexValue x, IndexValue y, IndexValue s){
		return stuff[x.intValue()][y.intValue()][s.intValue()];
	}
	
    /**
     * <p>Returns the Claim in this SpaceMap's target having the specified spatial coordinates.</p>
     * @param dimA a spatial coordinate that specifies its own
     * {@link Puzzle.DimensionType dimension}
     * @param dimB a spatial coordinate that specifies its own
     * {@link Puzzle.DimensionType dimension}
     * @param dimC a spatial coordinate that specifies its own
     * {@link Puzzle.DimensionType dimension}
     * @return the Claim in this SpaceMap's target having the specified spatial coordinates
     */
	public Claim get(IndexInstance dimA, IndexInstance dimB, IndexInstance dimC){
		IndexValue x = puzzle.decodeX(dimA, dimB, dimC);
		IndexValue y = puzzle.decodeY(dimA, dimB, dimC);
		IndexValue z = puzzle.decodeSymbol(dimA, dimB, dimC);
		return get(x, y, z);
	}
	
	@Override
	public Iterator<Claim> iterator(){
		return new ClaimIterator();
	}
	
	private List<Integer> ints = null;
	
	private List<Integer> ints(){
		if(ints == null){
			ints = puzzle.getIndices().stream()
					.map(IndexValue::intValue)
					.collect(Collectors.toList());
		}
		return ints;
	}
	
  /**
   * <p>An {@literal Iterator<Claim>} that traverses claim-space starting from 0,0,0, ending at the 
   * opposite vertex, and passing through all the Claim coordinates.</p>
   * @author fiveham
	 */
	private class ClaimIterator implements Iterator<Claim>{
	  
		private final Iterator<List<Integer>> cubeIterator;
		
		private ClaimIterator(){
			this.cubeIterator =  new NCuboid<Integer>(ints(), ints(), ints()).iterator();
		}
		
		@Override
		public boolean hasNext(){
			return cubeIterator.hasNext();
		}
		
		@Override
		public Claim next(){
			List<Integer> coords = cubeIterator.next();
			int x = coords.get(Puzzle.X_DIM);
			int y = coords.get(Puzzle.Y_DIM);
			int z = coords.get(Puzzle.Z_DIM);
			return get(x,y,z);
		}
	}
}
