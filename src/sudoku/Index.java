package sudoku;

import java.util.Arrays;

public enum Index {
	
	I1,I2,I3,I4,I5,I6,I7,I8,I9;
	
	private Index(){}
	
	public static Index boxIndex(Index ix, Index iy){
		int x = Arrays.asList(values()).indexOf(ix);
		int y = Arrays.asList(values()).indexOf(iy);
		
		return values()[ (y/3)*3 + (x/3) + 1 ];
	}
	
}
