package sudoku;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public enum Index{
	
	I1(0),
	I2(1),
	I3(2),
	I4(3),
	I5(4),
	I6(5),
	I7(6),
	I8(7),
	I9(8);
	
	public static final Index MINIMUM = values()[0];
	public static final Index MAXIMUM = values()[values().length-1];
	public static final int NO_SYMBOL = 0;
	
	private static final List<Index> asList = new ArrayList<>(Arrays.asList(values()));
	
	private int v;
	
	private Index(int v){
		this.v = v;
	}
	
	public static List<Index> valuesAsList(){
		return new ArrayList<>(asList);
	}
	
	public static Index fromInt(int i){
		return values()[i];
	}
	
	public int intValue(){
		return v;
	}
}
