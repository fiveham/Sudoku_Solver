package sudoku;

import java.util.*;
import java.io.*;

public class Puzzle {
	
	public static final int SIZE = 9;
	
	private List<Region> columns;
	private List<Region> rows;
	private List<Region> boxes;
	private List<Region> regions;
	
	private List<Role> roles;
	
	public Puzzle(File f) throws FileNotFoundException{
		this(new Scanner(f));
	}
	
	public Puzzle(String s){
		this(new Scanner(s));
	}
	
	public Puzzle(Scanner s){
		columns = initColumns(this);
		columns.sort(null);
		
		rows = initRows(this);
		rows.sort(null);
		
		boxes = initBoxes(this);
		boxes.sort(null);
		
		regions = new ArrayList<>();
		regions.addAll(columns);
		regions.addAll(rows);
		regions.addAll(boxes);
		
		roles = new ArrayList<>(SIZE*SIZE*3);
		for(Symbol symbol : Symbol.values()){
			for(Region r : regions){
				roles.add(new Role(symbol, r));
			}
		}
		
		for(Index y : Index.values()){
			for(Index x : Index.values()){
				int value = s.nextInt();
				if(value != Symbol.NONE){
					Symbol symbol = Symbol.fromInt(value);
					unifyAll(symbol, x, y);
				}
			}
		}
	}
	
	public List<Role> getRolesFor(Region r){
		List<Role> result = new ArrayList<>();
		for(Role role : roles){
			if(role.hasRegion(r)){
				result.add(role);
			}
		}
		return result;
	}
	
	public boolean isSolved(){
		return roles.size() == SIZE*SIZE;
	}
	
	//FIXME Remove regions whose symbols are localized from possibility lists of roles for other symbols in intersecting regions.
	private void unifyAll(Symbol s, Index x, Index y){
		Region col = getCol(x);
		Region row = getRow(y);
		
		merge(getRole(s,row), getRole(s,col));
	}
	
	public boolean merge(Role r1, Role r2){
		if(r1.symbol() != r2.symbol()){
			throw new IllegalArgumentException("Symbol mismatch: "+r1.symbol()+" != "+r2.symbol());
		}
		
		if( r1.isRow() && r2.isCol() ){ //r1 and r2 are row and column roles
			
			return tripleJoin(r1,r2);
			
		} else if(r1.isCol() && r2.isRow()){ 
			
			return tripleJoin(r2,r1);
			
		} else{
			int initSize = roles.size();
			roles.remove(r1);
			roles.remove(r2);
			roles.add(new Role(r1,r2));
			return initSize - roles.size() >= MIN_NET_REM_COUNT_FOR_SUCCESSFUL_MERGE;
		}
	}
	
	public static final int MIN_NET_REM_COUNT_FOR_SUCCESSFUL_MERGE = 1;
	
	private boolean tripleJoin(Role rowRole, Role colRole){
		
		Region row = rowRole.getRow();
		Region col = colRole.getCol();
		
		Index y = row.index();
		Index x = col.index();
		
		Role boxRole = getRole( rowRole.symbol(), getBox( Index.boxIndex(x,y) ) );
		
		int initSize = roles.size();
		roles.remove(boxRole);
		roles.remove(rowRole);
		roles.remove(colRole);
		roles.add(new Role(rowRole, colRole, boxRole));
		
		for(Symbol s : Symbol.values()){
			if(s!=rowRole.symbol()){
				Role r = getRole(s, row);
				Role c = getRole(s, col);
				differentiateRoles(r,c);
			}
		}
		
		return initSize - roles.size() >= MIN_NET_REM_COUNT_FOR_SUCCESSFUL_MERGE;
	}
	
	private static List<Region> initColumns(Puzzle p){
		List<Region> result = new ArrayList<>(SIZE);
		for(Index i : Index.values()){
			result.add(new Column(p,i));
		}
		return result;
	}
	
	private static List<Region> initRows(Puzzle p){
		List<Region> result = new ArrayList<>(SIZE);
		for(Index i : Index.values()){
			result.add(new Row(p,i));
		}
		return result;
	}
	
	private static List<Region> initBoxes(Puzzle p){
		List<Region> result = new ArrayList<>(SIZE);
		for(Index i : Index.values()){
			result.add(new Box(p,i));
		}
		return result;
	}
	
	public List<Region> getRegions(){
		return new ArrayList<>(regions);
	}
	
	public List<Region> getBoxes(){
		return new ArrayList<>(boxes);
	}
	
	public List<Region> getRows(){
		return new ArrayList<>(rows);
	}
	
	public List<Region> getCols(){
		return new ArrayList<>(columns);
	}
	
	/**
	 * <p>alters the roles' internal lists of possible regions such that 
	 * r1 and r2 are indicated to be {@link Sameness#DIFFERENT different} 
	 * roles.</p>
	 * @param r1
	 * @param r2
	 */
	public boolean differentiateRoles(Role r1, Role r2){
		boolean result = r2.setImpossibleRegions(r1.localizedRegions());
		result |= r1.setImpossibleRegions(r2.localizedRegions());
		return result;
	}
	
	public List<Role> getRoles(){
		return new ArrayList<>(roles);
	}
	
	public Role getRole(Symbol s, Region r){
		for(Role role : roles){
			if( role.symbol()==s && role.hasRegion(r) ){
				return role;
			}
		}
		throw new IllegalStateException("No role found for that region.");
	}
	
	private Region getReg(Index i, List<Region> regs, String type){
		for(Region b : regs){
			if(b.index()==i){
				return b;
			}
		}
		throw new IllegalStateException("No "+type+" found with that index.");
	}
	
	public Region getCol(Index i){
		return getReg(i, columns, "column");
	}
	
	public Region getRow(Index i){
		return getReg(i, rows, "row");
	}
	
	public Region getBox(Index i){
		return getReg(i, boxes, "box");
	}
	
	public List<Region> boxesIntersecting(Region r){
		if(boxes.contains(r)){
			List<Region> result = new ArrayList<>(1);
			result.add(r);
			return result;
		} else if(rows.contains(r)){
			switch(r.index()){
			case I1 : case I2 : case I3 : return boxes.subList(0,3);
			case I4 : case I5 : case I6 : return boxes.subList(3,6);
			default : return boxes.subList(6,9);
			}
		} else if(columns.contains(r)){
			List<Region> result = new ArrayList<>();
			switch(r.index()){
			case I1 : case I2 : case I3 : 
				result.add(getBox(Index.I1));
				result.add(getBox(Index.I4));
				result.add(getBox(Index.I7));
				return result;
			case I4 : case I5 : case I6 : 
				result.add(getBox(Index.I2));
				result.add(getBox(Index.I5));
				result.add(getBox(Index.I8));
				return result;
			default : 
				result.add(getBox(Index.I3));
				result.add(getBox(Index.I6));
				result.add(getBox(Index.I9));
				return result;
			}
		} else{
			throw new IllegalArgumentException("Region "+r.toString()+" is not contianed in this target.");
		}
	}
	
	public List<Region> colsIntersecting(Region r){
		if(columns.contains(r)){
			List<Region> result = new ArrayList<>(1);
			result.add(r);
			return result;
		} else if(rows.contains(r)){
			return new ArrayList<>(columns);
		} else if(boxes.contains(r)){
			switch(r.index()){
			case I1 : case I4 : case I7 : return columns.subList(0,3); //FIXME ensure rows/columns are sorted when initialized
			case I2 : case I5 : case I8 : return columns.subList(3,6);
			default : return rows.subList(6,9);
			}
		} else{
			throw new IllegalArgumentException("Region "+r.toString()+" is not contianed in this target.");
		}
	}
	
	public List<Region> rowsIntersecting(Region r){
		if(rows.contains(r)){
			List<Region> result = new ArrayList<>(1);
			result.add(r);
			return result;
		} else if(columns.contains(r)){
			return new ArrayList<>(rows);
		} else if(boxes.contains(r)){
			switch(r.index()){
			case I1 : case I2 : case I3 : return rows.subList(0,3);
			case I4 : case I5 : case I6 : return rows.subList(3,6);
			default : return rows.subList(6,9);
			}
		} else{
			throw new IllegalArgumentException("Region "+r.toString()+" is not contained in this target.");
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(Region row : rows){
			for(Region col : columns){
				sb.append(printingText(row,col)).append(" ");
			}
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}
	
	private int printingText(Region row, Region col){
		for(Symbol s : Symbol.values()){
			if( getRole(s,row) == getRole(s,col) ){
			//if( getRole(s,row).hasRegion(col) ){
				return s.intValue();
			}
		}
		return Symbol.NONE;
	}
}
