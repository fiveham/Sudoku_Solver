package sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a matrix or table of {@link Claim truth-claims} in a Puzzle 
 * to be used in analysis for certain solution techniques.
 * @author fiveham
 *
 */
public class Table {
	
	private Puzzle puzzle;
	private ShadowReflection.Type shadowReflectionType;
	private Puzzle.Dimension heldConstant;
	private Claim[][] map;
	private List<Column> columns;
	
	public Table(Puzzle puzzle, ShadowReflection.Type type, Puzzle.Dimension heldConstant) {
		this.puzzle = puzzle;
		this.shadowReflectionType = type;
		this.heldConstant = heldConstant;
		this.columns = new ArrayList<>(Puzzle.SIDE_LENGTH);
		this.map = new Claim[Puzzle.SIDE_LENGTH][Puzzle.SIDE_LENGTH];
		populateMapAndColumnList();
	}
	
	private void populateMapAndColumnList(){
		for(int i=0; i<Puzzle.SIDE_LENGTH; ++i){
			List<Claim> list = new ArrayList<>(Puzzle.SIDE_LENGTH);
			for(int j=0; j<Puzzle.SIDE_LENGTH; ++j){
				Puzzle.Dimension top = new Puzzle.Dimension(shadowReflectionType.dimTop(), i);
				Puzzle.Dimension side = new Puzzle.Dimension(shadowReflectionType.dimSide(), j);
				
				Claim claim = puzzle.getClaimAt(top, side, heldConstant);
				
				map[i][j] = claim;
				list.add(claim);
			}
			columns.add(new Column(list, new Puzzle.Dimension(shadowReflectionType.dimTop(), i+1)));
		}
	}
	
	public List<Column> getColumns(){
		return new ArrayList<>(columns);
	}
	
	public List<SudokuEnum> topDimList(){
		return Arrays.asList(shadowReflectionType.dimTop().valuesOfPertinentSudokuEnum());
	}
	
	public List<SudokuEnum> sideDimList(){
		return Arrays.asList(shadowReflectionType.dimSide().valuesOfPertinentSudokuEnum());
	}
	
	public class Column{
		
		private List<Claim> contents;
		private Puzzle.Dimension dimension;
		
		private Column(){}
		
		private Column(List<Claim> contents, Puzzle.Dimension dimension){
			this.contents = new ArrayList<>(contents);
			this.dimension = dimension;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Column){
				Column c = (Column) o;
				return c.contents.equals(contents) && c.dimension.equals(dimension);
			}
			return false;
		}
		
		public Puzzle.Dimension getDim(){
			return dimension;
		}
		
		public Claim getClaimWithDim(Puzzle.Dimension dim){
			return puzzle.getClaimAt(Table.this.heldConstant, Table.Column.this.dimension, dim);
		}
		
		public Set<Puzzle.Dimension> sideDimValsWithPossibleClaim(){
			Set<Puzzle.Dimension> result = new HashSet<>(contents.size());
			
			for(int i=0; i<contents.size(); ++i){
				Puzzle.Dimension d = new Puzzle.Dimension(shadowReflectionType.dimSide(), i+1);
				Claim claim = getClaimWithDim(d);
				
				if( !claim.isKnownFalse() ){
					result.add(d);
				}
			}
			
			return result;
		}
		
		/*private int possibleCount(){
			return contents.size() - falseCount();
		}*/
		
		/*private int falseCount(){
			int result = 0;
			for(Claim c : contents){
				if(c.isKnownFalse()){
					++result;
				}
			}
			return result;
		}*/
		
		/*private List<Claim> possibleClaims(){
			List<Claim> result = new ArrayList<>(contents.size());
			for(Claim c : contents){
				if( !c.isKnownFalse() ){
					result.add(c);
				}
			}
			return result;
		}*/
	}
	
}
