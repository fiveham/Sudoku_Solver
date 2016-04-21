package sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Index{
	
	I1,I2,I3,I4,I5,I6,I7,I8,I9;
	
	public static final Index MINIMUM = values()[0];
	public static final Index MAXIMUM = values()[values().length-1];
	
	public static final int NO_SYMBOL = 0;
	
	private static final List<Index> asList = new ArrayList<>(Arrays.asList(values()));
	
	private Index(){}
	
	public static Index boxIndex(Index ix, Index iy){
		int x = ix.ordinal();
		int y = iy.ordinal();
		
		return values()[ (y/3)*3 + (x/3) + 1 ];
	}
	
	public static List<Index> valuesAsList(){
		return new ArrayList<>(asList);
	}
	
	public static Index fromInt(int i){
		return values()[i-1];
	}
	
	public int intValue(){
		return ordinal()+1;
	}
}
