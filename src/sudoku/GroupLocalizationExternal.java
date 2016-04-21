package sudoku;

import common.ComboGen;
import java.util.*;

public class GroupLocalizationExternal extends Technique {
	
	public static final int MIN_CELL_COUNT = 2;

	public GroupLocalizationExternal(Puzzle puzzle) {
		super(puzzle);
	}
	
	@Override
	public boolean digest(){
		boolean result = false;
		
		if(puzzle.isSolved()){
			return result;
		}
		
		for(Region region : puzzle.getRegions()){
			
			List<Cell> regionCells = regionCells(region);
			List<Cell> unsolvedCells = unsolvedCells(regionCells);
			
			if(unsolvedCells.size() >= MIN_CELL_COUNT){
				ComboGen<Cell> combinationGenerator = new ComboGen<>(unsolvedCells, MIN_CELL_COUNT);
				for(List<Cell> combo : combinationGenerator){
					Set<Symbol> possibleSymbols = possibleSymbols(unsolvedCells);
					
					if(possibleSymbols.size() == combo.size()){
						result |= resolve(regionCells, combo, possibleSymbols);
					}
				}
			}
			
		}
		
		return result;
	}
	
	private boolean resolve(List<Cell> regionCells, List<Cell> combo, Set<Symbol> symbols){
		boolean retVal = false;
		
		for(Cell currentRegionCell : regionCells){
			if(!combo.contains(currentRegionCell)){
				for(Symbol s : symbols){
					Role r1 = puzzle.getRole(s, currentRegionCell.col);
					Role r2 = puzzle.getRole(s, currentRegionCell.row);
					retVal |= puzzle.differentiateRoles(r1, r2);
				}
			}
		}
		
		return retVal;
	}
	
	private Set<Symbol> possibleSymbols(List<Cell> cells){
		Set<Symbol> union = new HashSet<>(9);
		
		for(Cell c : cells){
			union.addAll(c.possibleSymbols());
		}
		
		return union;
	}
	
	private List<Cell> regionCells(Region region){
		List<Cell> cells = new ArrayList<>(9);
		
		List<Region> rows = puzzle.rowsIntersecting(region);
		List<Region> cols = puzzle.colsIntersecting(region);
		
		for(Region row : rows){
			for(Region col : cols){
				cells.add( new Cell((Row) row, (Column) col) );
			}
		}
		
		return cells;
	}
	
	/*private List<Cell> unsolvedCells(Region region){
		return unsolvedCells(regionCells(region));
	}*/
	
	private List<Cell> unsolvedCells(List<Cell> regionCells){
		List<Cell> retVal = new ArrayList<>(regionCells.size());
		for(Cell c : regionCells){
			if(!c.isSolved()){
				retVal.add(c);
			}
		}
		return retVal;
	}
	
	/*private List<Role> unlocalizedRoles(Region region){
		List<Role> result = target.getRolesFor(region);
		
		for(int i=result.size()-1; i>=0; --i){
			Role item = result.get(i);
			if(item.isBox() && item.isRow() && item.isCol()){
				result.remove(i);
			}
		}
		
		return result;
	}*/
	
	public class Cell{
		Row row;
		Column col;
		
		public Cell(Row row, Column col){
			this.row = row;
			this.col = col;
		}
		
		public boolean isSolved(){
			return possibleSymbols().size() == 1;
		}
		
		public Set<Symbol> possibleSymbols(){
			Set<Symbol> retVal = new HashSet<>(Symbol.values().length);
			
			for(Symbol s : Symbol.values()){
				Role rowRole = puzzle.getRole(s, row);
				Role colRole = puzzle.getRole(s, col);
				
				int successCount = 0;
				if(rowRole.hasPossCol(col)){
					++successCount;
				}
				if(colRole.hasPossRow(row)){
					++successCount;
				}
				
				switch(successCount){
				case 2 : retVal.add(s);
				case 0 : break;
				default : throw new IllegalStateException("Inconsistent row/col possibilities.");
				}
			}
			
			return retVal;
		}
		
		public void setImpossibleSymbol(Symbol s){
			Puzzle p = row.puzzle;
			
			Role rowRole = p.getRole(s, row);
			Role colRole = p.getRole(s, col);
			
			p.differentiateRoles(rowRole,colRole);
		}
	}
}
