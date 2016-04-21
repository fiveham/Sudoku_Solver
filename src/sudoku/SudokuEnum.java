package sudoku;

import java.util.function.Supplier;
import java.util.function.Function;

public interface SudokuEnum {
	
	public int intValue();
	
	public enum RegisteredType{
		
		INDEX ( ()->Index.values(),  (i)->Index.fromInt(i) ),
		SYMBOL( ()->Symbol.values(), (i)->Symbol.fromInt(i) );
		
		private Supplier<? extends SudokuEnum[]> valuesSrc;
		private Function<Integer,? extends SudokuEnum> fromIntSrc;
		
		private RegisteredType(Supplier<? extends SudokuEnum[]> valuesSrc, Function<Integer,? extends SudokuEnum> fromIntSrc){
			this.valuesSrc = valuesSrc;
			this.fromIntSrc = fromIntSrc;
		}
		
		public SudokuEnum fromInt(int value){
			return fromIntSrc.apply(value);
		}
		
		public SudokuEnum[] referencedValues(){
			return valuesSrc.get();
		}
		
	}
}
