package sudoku;

import common.NCuboid;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import sudoku.Puzzle.IndexValue;
import sudoku.Puzzle.IndexInstance;

/**
 * <p>Wraps a three-dimensional array of the Claims belonging to a Puzzle in order to expedite 
 * access to those Claims using spatial coordinates or geometric constraints.</p>
 * <p>By contrast, using only {@link Puzzle#claimStream() claimStream()} to find the one Claim with 
 * specific coordinates takes O(n^6) time, where n is the magnitude of the puzzle. Accessing the 
 * desired Claim via its spatial coordinates with a SpaceMap takes O(1) time.<p>
 * @author fiveham
 */
class SpaceMap implements Iterable<Claim>{
	
	private Claim[][][] claimSpace;
	private Puzzle puzzle;
	
  /**
   * <p>Constructs a SpaceMap pertaining to the specified Puzzle and creates the Claims for that
   * Puzzle.</p>
   * @param target the Puzzle whose Claims are created and managed
   */
	SpaceMap(Puzzle puzzle) {
		this.puzzle = puzzle;
		
	  int len = puzzle.sideLength();
	  claimSpace = new Claim[len][len][len];
	  
		for(IndexValue x : puzzle.indexValues()){
			for(IndexValue y : puzzle.indexValues()){
				for(IndexValue z : puzzle.indexValues()){
					claimSpace[x.intValue()][y.intValue()][z.intValue()] = new Claim(puzzle, x, y, z);
				}
			}
		}
	}
	
  /**
   * <p>Returns the Claim in this SpaceMap's puzzle having the specified spatial coordinates.</p>
   * @param x the x-coordinate of the Claim returned
   * @param y the y-coordinate of the Claim returned
   * @param z the z-coordinate (symbol - 1) of the Claim returned
   * @return the Claim in this Puzzle having the specified spatial coordinates
   * @throws ArrayIndexOutOfBoundsException if {@code x}, {@code y}, or {@code z} is less than 0 or 
   * greater than or equal to this SpaceMap's puzzle's {@link Puzzle#sideLength() side-length}
   */
	public Claim get(int x, int y, int z){
		return claimSpace[x][y][z];
	}
	
  /**
   * <p>Returns the Claim in this SpaceMap's puzzle having the specified spatial coordinates.</p>
   * @param x the x-coordinate of the Claim returned
   * @param y the y-coordinate of the Claim returned
   * @param z the z-coordinate (symbol - 1) of the Claim returned
   * @return the Claim in this SpaceMap's target having the specified spatial coordinates
   */
	public Claim get(IndexValue x, IndexValue y, IndexValue z){
		return claimSpace[x.intValue()][y.intValue()][z.intValue()];
	}
	
  /**
   * <p>Returns the Claim in this SpaceMap's target having the specified spatial coordinates.</p>
   * @param dimA a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
   * @param dimB a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
   * @param dimC a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
   * @return the Claim in this SpaceMap's target having the specified spatial coordinates
   */
	public Claim get(IndexInstance dimA, IndexInstance dimB, IndexInstance dimC){
		IndexValue x = puzzle.decodeX(dimA, dimB, dimC);
		IndexValue y = puzzle.decodeY(dimA, dimB, dimC);
		IndexValue z = puzzle.decodeZ(dimA, dimB, dimC);
		return get(x, y, z);
	}
	
	@Override
	public Iterator<Claim> iterator(){
		return new ClaimIterator(ints());
	}
	
	private List<Integer> ints = null;
	
	/**
	 * <p>Returns a list of the legal dimensional indices for this SpaceMap's puzzle.</p>
	 * @return a list of the legal dimensional indices for this SpaceMap's puzzle
	 */
	private List<Integer> ints(){
		if(ints == null){
			ints = puzzle.indexValues().stream()
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
		
		private ClaimIterator(List<Integer> ints){
			this.cubeIterator =  new NCuboid<Integer>(ints, ints, ints).iterator();
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
			return get(x, y, z);
		}
	}
}
