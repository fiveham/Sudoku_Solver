package sudoku;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public enum Symbol{
	
	S1,S2,S3,S4,S5,S6,S7,S8,S9;
	
	public static final int NONE = 0;
	
	private Symbol(){}
	
	public static Symbol fromInt(int src){
		return values()[src-1];
	}
	
	public int intValue(){
		return ordinal();
	}
	
	public static List<Symbol> valuesAsList(){
		return new ArrayList<>(asList);
	}
	
	private static final List<Symbol> asList = new ArrayList<>(Arrays.asList(values()));
}
