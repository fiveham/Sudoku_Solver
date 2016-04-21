package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class SpaceMap {
	
	private Claim[][][] stuff;
	
	public SpaceMap(int linearMeasure) {
		stuff = new Claim[linearMeasure][linearMeasure][linearMeasure];
		for(Index x : Index.values()){
			for(Index y : Index.values()){
				for(Symbol s : Symbol.values()){
					stuff[x.intValue()-1][y.intValue()-1][s.intValue()-1] = new Claim(x,y,s);
				}
			}
		}
	}
	
	public int getValue(Index x, Index y){
		List<Symbol> symbols = new ArrayList<>();
		
		for(Symbol s : Symbol.values()){
			Claim c = get(x, y, s);
			if( !c.isKnownFalse() ){
				symbols.add(s);
			}
		}
		
		//XXX magic no
		return symbols.size() == FactBag.SIZE_WHEN_SOLVED 
				? symbols.get(0).intValue() 
				: Symbol.NONE;
	}
	
	public Claim get(Index x, Index y, Symbol s){
		return stuff[x.intValue()-1][y.intValue()-1][s.intValue()-1];
	}
	
	public Collection<Claim> claimsWhere(Predicate<Claim> p){
		List<Claim> result = new ArrayList<>();
		for(int i=0; i<Puzzle.SIDE_LENGTH; ++i){
			for(int j=0; j<Puzzle.SIDE_LENGTH; ++j){
				for(int k=0; k<Puzzle.SIDE_LENGTH; ++k){
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
