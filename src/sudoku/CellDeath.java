package sudoku;

import java.util.*;

public class CellDeath extends Technique {

	public CellDeath(Puzzle puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Finds in the target every instance of a cell for which all but one 
	 * symbol have been eliminated as possibilities. For each row-column pair 
	 * in the target, find the number of symbols such that the instance of such 
	 * a symbol in the row could be the instance of that symbol in the column, 
	 * and if there is only one such symbol, then set the value of the cell 
	 * at the intersection of the row and column by merging the role for the 
	 * symbol and row with the role for the symbol and column (and implicitly 
	 * merge the role for the symbol and box during the process).</p>
	 * <p>This process finds and acts upon solved cells as well as cells that 
	 * are able to be solved due to having only one non-impossible value. As 
	 * such, the return value of Puzzle#merge() is combined (OR) with a boolean 
	 * value initialized to false in order to produce the return value.</p>
	 * @return true if this process made any changes to the underlying Puzzle, 
	 * false otherwise.
	 */
	@Override
	public boolean digest(){
		boolean result = false;
		
		if(puzzle.isSolved()){
			return result;
		}
		
		List<Region> rows = puzzle.getRows();
		List<Region> cols = puzzle.getCols();
		
		for(Region row : rows){
			for(Region col : cols){
				List<Symbol> shared = sharedPossibleSymbols(row,col);
				if(shared.size()==1){
					Symbol s = shared.get(0);
					result |= puzzle.merge( puzzle.getRole(s, row), puzzle.getRole(s, col) );
				}
				
			}
		}
		
		return result;
	}
	
	private List<Symbol> sharedPossibleSymbols(Region row, Region col){
		List<Symbol> result = new ArrayList<>();
		
		for(Symbol symbol : Symbol.values()){
			Role rowRole = puzzle.getRole(symbol, row);
			Role colRole = puzzle.getRole(symbol, col);
			if( rowRole.hasPossCol(col) || colRole.hasPossRow(row)){
				result.add(symbol);
			}
		}
		
		return result;
	}
}
