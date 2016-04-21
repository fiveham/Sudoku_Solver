package sudoku;

import java.util.Iterator;
import java.util.List;
import sudoku.Puzzle.IndexValue;
import sudoku.Puzzle.IndexInstance;
import java.util.stream.Collectors;

/**
 * <p>Wraps a three-dimensional array of Claims in order to 
 * expedite access to them based on their spatial coordinates 
 * or geometric constraints.</p>
 * 
 * <p>Using only {@link Puzzle#claimStream() claimStream()} to 
 * find the one Claim with specific coordinates is a O(n^6) technique, 
 * where n is the magnitude of the target. Accessing the desired 
 * Claim directly via its spatial coordinates can be acheived in 
 * O(1) time.</p>
 * 
 * @author fiveham
 *
 */
public class SpaceMap implements Iterable<Claim>{
	
	private Claim[][][] stuff;
	private Puzzle puzzle;
	
	/**
	 * <p>Constructs a SpaceMap pertaining to the specified Puzzle and 
	 * creates the Claims for that Puzzle.</p>
	 * @param target the Puzzle whose Claims are created and managed
	 */
	public SpaceMap(Puzzle puzzle) {
		this.puzzle = puzzle;
		stuff = new Claim[puzzle.sideLength()][puzzle.sideLength()][puzzle.sideLength()];
		for(IndexValue x : puzzle.indexValues()){
			for(IndexValue y : puzzle.indexValues()){
				for(IndexValue s : puzzle.indexValues()){
					stuff[x.intValue()][y.intValue()][s.intValue()] = new Claim(puzzle, x,y,s);
				}
			}
		}
	}
	
	/**
	 * <p>Returns a single-character string describing the value of the 
	 * cell at x,y in the map's target. If the cell isn't solved, the 
	 * string is a 0; otherwise, it is the {@link Puzzle.IndexValue#humanReadableSymbol() human-readable symbol} 
	 * for the value that the cell has.</p>
	 * @param x the x-coordinate of the cell whose value is output
	 * @param y the y-coordinate of the cell whose value is output
	 * @return a single-character string describing the value of the 
	 * cell at x,y in the map's target
	 */
	public String getPrintingValue(IndexValue x, IndexValue y){
		List<IndexValue> symbols = puzzle.indexValues().stream().filter((symbol)->!get(x,y,symbol).isKnownFalse()).collect(Collectors.toList());
		
		return symbols.size() == Rule.SIZE_WHEN_SOLVED 
				? symbols.get(0).humanReadableSymbol() 
				: "0";
	}
	
	/**
	 * <p>Returns the Claim in this SpaceMap's target having the 
	 * specified spatial coordinates.</p>
	 * @param x the x-coordinate of the Claim returned
	 * @param y the y-coordinate of the Claim returned
	 * @param z the z-coordinate (symbol) of the Claim returned
	 * @throws ArrayIndexOutOfBoundsException if <tt>x<tt>, <tt>y<tt>, 
	 * or <tt>z<tt> are equal to or greater than <tt>target.sideLength()</tt>
	 * @return  the Claim in this Puzzle having the specified spatial 
	 * coordinates
	 */
	public Claim get(int x, int y, int z){
		return stuff[x][y][z];
	}
	
	/**
	 * <p>Returns the Claim in this SpaceMap's target having the specified 
	 * spatial coordinates.</p>
	 * @param x the x-coordinate of the Claim returned
	 * @param y the y-coordinate of the Claim returned
	 * @param s the z-coordinate (symbol) of the Claim returned
	 * @return the Claim in this SpaceMap's target having the specified 
	 * spatial coordinates
	 */
	public Claim get(IndexValue x, IndexValue y, IndexValue s){
		return stuff[x.intValue()][y.intValue()][s.intValue()];
	}
	
	/**
	 * <p>Returns the Claim in this SpaceMap's target having the specified 
	 * spatial coordinates.</p>
	 * @param dim1 a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
	 * @param dim2 a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
	 * @param heldConstant a spatial coordinate that specifies its own {@link Puzzle.DimensionType dimension}
	 * @return the Claim in this SpaceMap's target having the specified 
	 * spatial coordinates
	 */
	public Claim get(IndexInstance dim1, IndexInstance dim2, IndexInstance heldConstant){
		IndexValue x = puzzle.decodeX(dim1, dim2, heldConstant);
		IndexValue y = puzzle.decodeY(dim1, dim2, heldConstant);
		IndexValue s = puzzle.decodeSymbol(dim1, dim2, heldConstant);
		return get(x, y, s);
	}
	
	@Override
	public Iterator<Claim> iterator(){
		return new ClaimIterator();
	}
	
	/**
	 * <p>An Iterator<Claim> that traverses the claim-space starting 
	 * from 0,0,0 by incrementing z, then maybe y if z overflowed, 
	 * then maybe x if y overflowed.</p>
	 * @author fiveham
	 *
	 */
	private class ClaimIterator implements Iterator<Claim>{
		private int x = 0;
		private int y = 0;
		private int z = 0;
		
		@Override
		public boolean hasNext(){
			return x<puzzle.sideLength() &&  y<puzzle.sideLength() && z<puzzle.sideLength();
		}
		
		@Override
		public Claim next(){
			Claim result = get(x,y,z);
			updateCoords();
			return result;
		}
		
		/**
		 * <p>Increments z, but resets it to 0 and increments y if z is 
		 * too large after incrementing (if z == target.sideLength()), 
		 * but resets y to 0 and increments x if y is too large after 
		 * incrementing.</p>
		 */
		private void updateCoords(){
			if(++z==puzzle.sideLength()){
				z=0;
				if(++y==puzzle.sideLength()){
					y=0;
					++x;
				}
			}
		}
	}
}
