package sudoku;

import java.util.*;

/**
 * A symbol in a sudoku target. typically a digit 1 through 9.
 * Serves roughly as a representation of an instance of a Value, 
 * which is impossible with just Value itself since Value is an 
 * enum so there's only one copy in memory of each state of Value.
 * @author fiveham
 *
 */
public class Symbol {
	
	private final Puzzle puzzle;
	private final Value	 value;
	
	private Cell cell    = null;
	private Role colRole = null;
	private Role rowRole = null;
	private Role boxRole = null;
	
	public Symbol(Value v, Puzzle puzz){
		if(v==null || v==Value.UNKNOWN)
			throw new IllegalArgumentException("Invalid Value");
		value = v;
		this.puzzle = puzz;
	}
	
	public boolean hasRole(Role r){
		return colRole.equals(r) || 
				rowRole.equals(r) ||
				boxRole.equals(r);
	}
	
	public Role getRowRole(){
		return rowRole;
	}
	
	public Role getColRole(){
		return colRole;
	}
	
	public Role getBoxRole(){
		return boxRole;
	}
	
	public boolean hasRowRole(){
		return rowRole!=null;
	}
	
	public boolean hasColRole(){
		return colRole!=null;
	}
	
	public boolean hasBoxRole(){
		return boxRole!=null;
	}
	
	public void setRowRole(Role r){
		if(r==null)
			throw new IllegalArgumentException("That Role is null");
		rowRole = r;
	}
	
	public void setColRole(Role r){
		if(r==null)
			throw new IllegalArgumentException("That Role is null");
		colRole = r;
	}
	
	public void setBoxRole(Role r){
		if(r==null)
			throw new IllegalArgumentException("That Role is null");
		boxRole = r;
	}
	
	public void setCell(Cell c){
		if(c==null){
			throw new IllegalArgumentException("Cell c is null");
		}
		if( !hasCell()){
			cell = c;
		}
	}
	
	public Cell getCell(){
		return cell;
	}
	
	public boolean hasCell(){
		return cell!=null;
	}
	
}
