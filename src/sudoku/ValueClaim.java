package sudoku;

import java.util.*;

public class ValueClaim extends Technique {

	public ValueClaim(Puzzle puzzle) {
		super(puzzle);
		
	}
	
	public boolean digest(){
		boolean result = false;
		
		if(puzzle.isSolved()){
			return result;
		}
		
		List<Region> boxes = puzzle.getBoxes();
		List<Region> rows = puzzle.getRows();
		List<Region> cols = puzzle.getCols();
		
		for(Symbol symbol : Symbol.values()){
			for(Region box : boxes){
				Role role = puzzle.getRole(symbol, box);
				if(role.isRow()){
					Region row = role.getRow();
					result |= puzzle.merge(role, puzzle.getRole(symbol, row));
				} else if(role.isCol()){
					Region col = role.getCol();
					result |= puzzle.merge(role, puzzle.getRole(symbol, col));
				}
			}
			for(Region row : rows){
				Role role = puzzle.getRole(symbol, row);
				if(role.isBox()){
					Region box = role.getBox();
					result |= puzzle.merge(role, puzzle.getRole(symbol, box));
				}
			}
			for(Region col : cols){
				Role role = puzzle.getRole(symbol, col);
				if(role.isBox()){
					Region box = role.getBox();
					result |= puzzle.merge(role, puzzle.getRole(symbol, box));
				}
			}
		}
		
		return result;
	}
}
