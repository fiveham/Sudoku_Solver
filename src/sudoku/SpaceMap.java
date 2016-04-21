package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import sudoku.Puzzle.IndexValue;

public class SpaceMap {
	
	private Claim[][][] stuff;
	private Puzzle puzzle;
	
	public SpaceMap(Puzzle puzzle) {
		this.puzzle = puzzle;
		stuff = new Claim[puzzle.sideLength()][puzzle.sideLength()][puzzle.sideLength()];
		for(Puzzle.IndexValue x : puzzle.indexValues()){
			for(Puzzle.IndexValue y : puzzle.indexValues()){
				for(Puzzle.IndexValue s : puzzle.indexValues()){
					stuff[x.intValue()][y.intValue()][s.intValue()] = new Claim(puzzle, x,y,s);
				}
			}
		}
	}
	
	public String getPrintingValue(Puzzle.IndexValue x, Puzzle.IndexValue y){
		List<Puzzle.IndexValue> symbols = new ArrayList<>();
		
		for(Puzzle.IndexValue s : puzzle.indexValues()){
			Claim c = get(x, y, s);
			if( !c.isKnownFalse() ){
				symbols.add(s);
			}
		}
		
		return symbols.size() == FactBag.SIZE_WHEN_SOLVED 
				? symbols.get(0).humanReadableSymbol() 
				: "0";
	}
	
	public Claim get(int x, int y, int z){
		return stuff[x][y][z];
	}
	
	public Claim get(Puzzle.IndexValue x, Puzzle.IndexValue y, Puzzle.IndexValue s){
		return stuff[x.intValue()][y.intValue()][s.intValue()];
	}
	
	public Claim get(Puzzle.IndexInstance dim1, Puzzle.IndexInstance dim2, Puzzle.IndexInstance heldConstant){
		IndexValue x = puzzle.decodeX(dim1, dim2, heldConstant);
		IndexValue y = puzzle.decodeY(dim1, dim2, heldConstant);
		IndexValue s = puzzle.decodeSymbol(dim1, dim2, heldConstant);
		return get(x, y, s);
	}
	
	public Collection<Claim> claimsWhere(Predicate<Claim> p){
		List<Claim> result = new ArrayList<>();
		for(int i=0; i<puzzle.sideLength(); ++i){
			for(int j=0; j<puzzle.sideLength(); ++j){
				for(int k=0; k<puzzle.sideLength(); ++k){
					Claim c = stuff[i][j][k];
					if(p.test(c)){
						result.add(c);
					}
				}
			}
		}
		return result;
	}
}
