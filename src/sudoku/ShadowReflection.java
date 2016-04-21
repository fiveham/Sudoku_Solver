package sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import common.ComboGen;

public class ShadowReflection extends Technique {
	
	/*
	 * try to generalize this so it incorporates the "Sledgehammer" 
	 * version of X-wing and maybe some other things.
	 * 
	 * As-is, this technique looks at sets of cells belonging to the 
	 * same region, extracts a union-set of possible Symbols for the 
	 * cells in the current cell-set, and takes action if that Symbol-
	 * set is equal in size to the cell-set.
	 * The action is the modify the outgroup of cells in the region.
	 * 
	 * If x-wing were incorporated, then this technique would look at 
	 * sets of rows/columns, extract a union-set of columns/rows that 
	 * may possibly house the instance of a given Symbol that resides 
	 * in a row/column in the current row/column-set, and take action 
	 * if the size of that column/row union-set is equal to that of 
	 * the initial row/column set.
	 * The action is to modify the recipient columns/rows.
	 * 
	 * ...however, there is still a sort of shadow-reflection like in 
	 * the current case.
	 * Imagine a Jellyfish scenario (row-initiated) such that the 16 
	 * cells at grid-intersections are all crammed into the upper left 
	 * corner of the sudoku target.  In that configuration, the target 
	 * can be divided into four regions: the 4x4 in the upper left, a 
	 * 5x5 in the lower right, and two 5x4s in the other two corners. 
	 * The 5x4 in the upper right is a shadow of impossibilities, and 
	 * resolving this jellyfish means reflecting that shadow across the 
	 * diagonal into the other 5x4 region. Reflecting an absence-shadow 
	 * like that across the main diagonal is the canonical description 
	 * of the resolution of a GroupLocalization.
	 * 
	 * GroupLocalization creates the table, given a reference (X, Y, or 
	 * block index) to a region, by laying out claims' truth-states in 
	 * terms of (1) cell's identity and (2) Symbol.
	 * When this is done using a simple x or Y, analysis is being 
	 * performed on a simple slice of the target's cube form.
	 * 
	 * LineHatch analysis creates the table, given a Symbol, by isolating 
	 * the slice of the target's cube form for which the truth claims on 
	 * that layer pertain to the given Symbol. The table's dimensions are 
	 * treated as (1) row identity and (2) column identity
	 * 
	 * Looking at these two techniques in terms of geometry regarding the 
	 * cube form of the target, i see that we generate tables based on all 
	 * three facial orientations as well as based on boxes' cells.  Since 
	 * all three face-orientations are accounted for and since boxes are a 
	 * preposterous clusterfuck, I suspect that there may be no futher tables 
	 * to account for as long as GroupLocalization and LineHatch are 
	 * accounted for.
	 * 
	 * Let's check all the possible combinations of things that could account 
	 * for a new sort of perspective that we could use to construct a table 
	 * for shadow reflection.
	 * 
	 * Hold constant	Top dimension	side dimension	irrelevant dimensions
	 * 		X			 Y (cell ID)	 symbol				block ID
	 * 		Y			 X (cell ID)	 symbol				block ID
	 * 	  symbol		 X				 Y					block ID
	 * 	 block ID		 cell ID		 symbol				X, Y
	 * 
	 * 
	 * Known dimensions: X, Y, symbol, blockID, cellID (role), 
	 * 
	 * Studied dimension-permutations exhaustively with pen and paper. 
	 * Conclusion: these four are the only permutation/combo things that 
	 * result in a 9x9 matrix of individual objects.
	 * 
	 */
	
	public static final int MIN_INGROUP_SIZE = 2;
	
	public static final ToolSet<Symbol> ALL_SYMBOLS = new ToolSet<>(Symbol.values().length); //TODO remove
	static{
		for(Symbol s : Symbol.values()){
			ALL_SYMBOLS.add( s );
		}
	}

	public ShadowReflection(Puzzle puzzle) {
		super(puzzle);
	}
	
	public static final int MAX_COMBO_SIZE = Puzzle.SIDE_LENGTH-1;
	public static final int MIN_COMBO_SIZE = 1;

	@Override
	public boolean process(){
		boolean puzzleHasUpdated = false;
		
		for(Type type : Type.values()){
			for(Puzzle.Dimension sliceOrBox : type.holdConst.valuesOfPertinentSudokuEnumAsDims()){
				
				Table table = new Table(puzzle, type, sliceOrBox);
				
				//for each combination of Columns in the Table
				    //if the size of the combined (unioned) set of possible side-dimension values among the current 
				    //combination's Columns is the same as the size of the current combination of Columns, then
						//get the set of side-dimension values complementing the combined side-dim-value-set
						//get the set of Columns complementing the current combination of Columns
				        //for each side-dimension-value in the complement,
							//for each Column in the column-complement
								//set the Claim from the current Column pertaining to the current side-dim-value to false
								//and smuggle out the boolean return value of that method by OR'ing it into puzzleHasUpdated
				
				for(List<Table.Column> combo : new ComboGen<Table.Column>(table.getColumns(), MIN_COMBO_SIZE, MAX_COMBO_SIZE) ){ //XXX MAGIC NUMBERS
					List<Puzzle.Dimension> combinedSideDimSet = combinePossibleSideDims(combo); 
					if( combinedSideDimSet.size() == combo.size() ){
						List<Puzzle.Dimension> sideDimComplement = combinedSideDimSet.get(0).getType().valuesOfPertinentSudokuEnumAsDims();
						sideDimComplement.removeAll(combinedSideDimSet);
						
						List<Table.Column> colComplement = table.getColumns();
						colComplement.removeAll(combo);
						
						for(Puzzle.Dimension dim : sideDimComplement){
							for(Table.Column col : colComplement){
								puzzleHasUpdated |= col.getClaimWithDim(dim).setFalse();
							}
						}
					}
				}
				
				/*
				 * By using combinations and only mutating one dimension of the Table 
				 * instead of mutating both and using permutations, the order of the 
				 * dimension-types defined in the used ShadowRelfection.Type matters; 
				 * so, the reflection type for LineHatch has to operate twice--once for 
				 * row-originated events and once for column-originated events.
				 * What about the three other types of ShadowReflection?
				 * 
				 * Box-constant, looking at cell-index-in-box and symbol...
				 * by combo-ing cells, we look for a shadow in the lower left corner of the table to be reflected to the top right.
				 * This seems to leave the other way around unaccounted for.
				 * How do you find a shadow in the upper right to reflect to the lower left?
				 * If there is a shadow in the upper right, then you could just double-reflect (or rotate) 
				 * the table so that the same collection of known-false claims is in the lower left.
				 * So, symbol-first clustering is implicitly incorporated into cell-first clustering.
				 * 
				 * The same analysis applies to row- and column-constant ShadowReflection analyses.
				 * 
				 * What about symbol-constant (X-Wing, Swordfish, JellyFish, etc.)?
				 * If there exists a set of N columns such that the unioned set of the 
				 * rows in which those columns' cells that hold the current symbol can 
				 * possibly exist is of size N, then it can be known that the complementary 
				 * columns do not have their cell for this symbol in any of the rows in 
				 * that unioned row-set.
				 * While describing that, I pictured a case involving three columns.
				 * What if you have three rows like that, instead?
				 * Then, you have six columns like that and can apply the rule columnwise instead.
				 * 
				 * 
				 */
				
				//Deprecated
				//for each permutation of the top columns
					//for each permutation of the side rows
						//check for corner-shadows, and reflect them as/if they are found
				
				/*for(Permutation topPerm : new Permutation.Permuter<SudokuEnum>(table.topDimList())){
					Table permutedTop = table.permuteTop(topPerm);
					for(Permutation sidePerm : new Permutation.Permuter<SudokuEnum>(table.sideDimList())){
						Table permutedTable = permutedTop.permuteSide(sidePerm);
						
						Shadow shadow = getShadow(permutedTable);
						
						puzzleHasUpdated |= reflectShadow(shadow, permutedTable);
					}
				}*/
			}
		}
		
		return puzzleHasUpdated;
	}
	
	private List<Puzzle.Dimension> combinePossibleSideDims(List<Table.Column> combo){
		Set<Puzzle.Dimension> result = new HashSet<>(Puzzle.SIDE_LENGTH);
		for(Table.Column col : combo){
			result.addAll(col.sideDimValsWithPossibleClaim());
		}
		return new ArrayList<>(result);
	}
	
	/*private boolean diiigest() {
		boolean puzzleHasUpdated = false;
		
		if(target.isSolved()){
			return puzzleHasUpdated;
		}
		
		for(ToolSet<Rule> region : target.getRegions()){
			for(List<Rule> ingroup : new ComboGen<Rule>(region, MIN_INGROUP_SIZE)){
				Set<Symbol> collectivePossibilties = collectivePossibilities(ingroup);
				if( collectivePossibilties.size() == ingroup.size() ){
					List<Symbol> toBeRemoved = ALL_SYMBOLS.complement(collectivePossibilties);
					List<Rule> outgroup = region.complement(ingroup);
					
					for(Rule fb : outgroup){
						for(Symbol s : toBeRemoved){
							Claim randomClaim = randomClaim(fb);
							IndexValue x = randomClaim.x();
							IndexValue y = randomClaim.y();
							
							target.claims().get(x,y,s).setFalse();
						}
					}
					
				}
			}
		}
		
		return puzzleHasUpdated;
	}*/
	
	/*private Claim randomClaim(Rule fb){
		for(Claim c : fb){
			return c;
		}
		throw new IllegalStateException("No Claims in the specified Rule");
	}
	
	private Set<Symbol> collectivePossibilities(Collection<Rule> group){
		Set<Symbol> result = new HashSet<>();
		
		for(Rule fb : group){
			List<Symbol> possibleSymbols = possibleSymbols(fb);
			result.addAll(possibleSymbols);
		}
		
		return result;
	}*/
	
	/*private List<Symbol> possibleSymbols(Rule fb){
		List<Symbol> result = new ArrayList<>();
		
		for(Claim c : fb){
			result.add(c.symbol());
		}
		
		return result;
	}*/
	
	static enum Type{
		BOX		( Puzzle.Dimension.Type.BOX,    Puzzle.Dimension.Type.CELL_ID_IN_BOX, Puzzle.Dimension.Type.SYMBOL ),
		COL		( Puzzle.Dimension.Type.X,      Puzzle.Dimension.Type.Y,              Puzzle.Dimension.Type.SYMBOL ),
		ROW		( Puzzle.Dimension.Type.Y,      Puzzle.Dimension.Type.X,              Puzzle.Dimension.Type.SYMBOL ),
		SYMBOL	( Puzzle.Dimension.Type.SYMBOL, Puzzle.Dimension.Type.X,              Puzzle.Dimension.Type.Y );
		
		private Puzzle.Dimension.Type holdConst;
		private Puzzle.Dimension.Type dimTop;
		private Puzzle.Dimension.Type dimSide;
		
		private Type(Puzzle.Dimension.Type holdConst, Puzzle.Dimension.Type dimTop, Puzzle.Dimension.Type dimSide){
			this.holdConst = holdConst;
			this.dimTop = dimTop;
			this.dimSide = dimSide;
		}
		
		Puzzle.Dimension.Type holdConst(){
			return holdConst;
		}
		
		Puzzle.Dimension.Type dimTop(){
			return dimTop;
		}
		
		Puzzle.Dimension.Type dimSide(){
			return dimSide;
		}
	}
	
}
