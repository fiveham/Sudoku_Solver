package sudoku;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

public class Role {

	private Symbol symbol;
	Puzzle puzzle;
	
	private List<Region> possibleRows;
	private List<Region> possibleBoxes;
	private List<Region> possibleCols;
	
	/**
	 * <p>Constructs an elemental role, pertaining to a Symbol and 
	 * one Region.</p>
	 * @param s	The Symbol to which this Role pertains
	 * @param r The single Region to which this elemental role pertains
	 */
	public Role(Symbol s, Region r){
		this.symbol = s;

		this.puzzle = r.getPuzzle();
		
		this.possibleRows  = puzzle.rowsIntersecting(r);
		this.possibleBoxes = puzzle.colsIntersecting(r);
		this.possibleCols  = puzzle.boxesIntersecting(r);
	}
	
	public static final int MIN_ROLE_COUNT_FOR_MERGE = 2;
	
	/**
	 * <p>Constructs a Role merging two or more Roles such that the 
	 * constructed Role includes {@link #possibleRows as} 
	 * {@link #possibleBoxes possible} {@link #possibleCols Regions} 
	 * only those Regions shared by all the specified Roles.</p>
	 * @param roles Roles to be merged
	 * @throws IllegalArgumentException if fewer than {@value #MIN_ROLE_COUNT_FOR_MERGE} 
	 * Roles are specified.
	 */
	public Role(Role... roles){
		if(roles.length<MIN_ROLE_COUNT_FOR_MERGE){
			throw new IllegalArgumentException("Cannot merge only 1 Role.");
		}
		for(int i=0; i<roles.length-1; i++){
			Role r1 = roles[i];
			Role r2 = roles[i+1];
			if(r1.puzzle != r2.puzzle || r1.symbol != r2.symbol){
				throw new IllegalArgumentException("Roles to be merged must belong to the same target and have same symbol.");
			}
		}
		
		this.symbol = roles[1].symbol;
		this.puzzle = roles[1].puzzle;
		
		possibleRows = regionIntersection(roles, ROWS.apply(this));
		possibleCols = regionIntersection(roles, COLS.apply(this));
		possibleBoxes = regionIntersection(roles, BOXES.apply(this));
		
		for(List<Region> list : groups()){
			if(list.size() < 1){
				throw new IllegalArgumentException("Merging these Roles results in a Role lacking any of some kind of Region.");
			}
		}
	}
	
	private static Function<Role,Function<Role,List<Region>>> ROWS = (role) -> ( (r) -> r.possibleRows );
	private static Function<Role,Function<Role,List<Region>>> COLS = (role) -> ( (r) -> r.possibleCols );
	private static Function<Role,Function<Role,List<Region>>> BOXES = (role) -> ( (r) -> r.possibleBoxes );
	
	private List<Region> regionIntersection(Role[] roles, Function<Role,List<Region>> regionType){
		Set<Region> result = new HashSet<>(regionType.apply(roles[0]));
		
		for(int i=1; i<roles.length; i++){
			result.retainAll(regionType.apply(roles[i]));
		}
		
		return new ArrayList<>(result);
	}
	
	public boolean hasPossBox(Region box){
		return possibleBoxes.contains(box);
	}
	
	public boolean isBox(){
		return isType(possibleBoxes);
	}
	
	public Region getBox(){
		return getReg(possibleBoxes, "box");
	}
	
	public boolean hasPossCol(Region col){
		return possibleCols.contains(col);
	}
	
	public boolean isCol(){
		return isType(possibleCols);
	}
	
	public Region getCol(){
		return getReg(possibleCols, "column");
	}
	
	public boolean hasPossRow(Region row){
		return possibleRows.contains(row);
	}
	
	public boolean isRow(){
		return isType(possibleRows);
	}
	
	public Region getRow(){
		return getReg(possibleRows, "row");
	}
	
	private Region getReg(List<Region> list, String type){
		if(isType(list)){
			return list.get(0);
		} else{
			throw new IllegalStateException("This Role is not restricted to a single "+type+".");
		}
	}
	
	public boolean setImpossibleRegions(Collection<Region> c){
		boolean result = false;
		for(Region r : c){
			result |= setImpossibleRegion(r);
		}
		return result;
	}
	
	public boolean setImpossibleRegion(Region region){
		boolean result = possibleRows.remove(region);
		result |= possibleCols.remove(region);
		result |= possibleBoxes.remove(region);
		return result;
	}
	
	/**
	 * The number of Regions of a certain type in the list of 
	 * possible regions of that certain type if this Role is 
	 * a role for that sort of region.
	 */
	public static final int REGION_COUNT_FOR_ROLE_OF_TYPE = 1;
	
	private boolean isType(List<Region> list){
		return list.size()==REGION_COUNT_FOR_ROLE_OF_TYPE;
	}
	
	public boolean hasRegion(Region r){
		return localizedRegions().contains(r);
	}
	
	//returns a list of regions for which this Role is the role for this Role's symbol
	//for example, if this Role is only the role for a certain symbol in a Row, then 
	//the returned list contains only that Row.
	//for example, if this Role is the roel for a certain symbol in a Column and a Box, 
	//then the returned list contains exactly that Column and that Box.
	public List<Region> localizedRegions(){
		List<Region> retVal = new ArrayList<>(3);
		
		List<List<Region>> groups = groups();
		
		for(List<Region> group : groups){
			if( isType(group) ){
				retVal.add(group.get(0));
			}
		}
		
		return retVal;
	}
	
	private List<List<Region>> groups = null;
	
	private List<List<Region>> groups(){
		if(groups == null){
			groups = new ArrayList<>(3);
			groups.add(possibleRows);
			groups.add(possibleBoxes);
			groups.add(possibleCols);
		}
		return groups;
	}
	
	public Symbol symbol(){
		return symbol;
	}
}
