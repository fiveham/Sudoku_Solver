package sudoku;

public class OrganFailure extends Technique {

	public OrganFailure(Puzzle puzzle) {
		super(puzzle);
		
	}
	
	@Override
	public boolean digest(){
		
		boolean result = false;
		
		if(puzzle.isSolved()){
			return result;
		}
		
		for(Region region : puzzle.getRegions()){
			for(Symbol symbol : Symbol.values()){
				Role role = puzzle.getRole(symbol,region);
				
				//if the <symbol> in this <region> is localized to a single cell, 
				//(intersection of a row and a column) then 
				if(role.isRow() && role.isCol()){
					Region row = role.getRow();
					Region col = role.getCol();
					
					//result |= target.merge(role, target.getRole(symbol, target.getBox(IndexValue.boxIndex(x,y))) );
					result |= puzzle.merge(puzzle.getRole(symbol, row), puzzle.getRole(symbol, col));
				}
			}
		}
		
		return result;
	}
}
