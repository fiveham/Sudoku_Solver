package sudoku.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import sudoku.Puzzle;

/**
 * <p>Parses the initial puzzle out of a Sadman Sudoku puzzle 
 * file. This format is defined at 
 * http://www.sadmansoftware.com/sudoku/faq19.php (retrieved 25 
 * May 2016).</p>
 * 
 * <p>All elements of a Sadman format file other than the 
 * initial state of the puzzle are ignored, including the 
 * number of solutions.</p>
 * @author fiveham
 *
 */
public class SadmanParser implements Parser{
	
	public static final String INITIAL_PUZZLE_MARKER = "[Puzzle]";
	public static final char EMPTY_CELL = '.';
	
	private final int mag;
	private final List<Integer> values;
	
	public SadmanParser(File f) throws FileNotFoundException{
		Scanner s = new Scanner(f);
		while(s.hasNext() && !INITIAL_PUZZLE_MARKER.equals(s.nextLine()));
		
		if(!s.hasNext()){
			s.close();
			s = new Scanner(f);
		}
		
		StringBuilder initCells = new StringBuilder(s.nextLine());
		this.mag = (int)Math.sqrt(initCells.length());
		for(int i=1; i<mag*mag; ++i){
			initCells.append(s.nextLine());
		}
		s.close();
		this.values = new ArrayList<>(initCells.length());
		for(int i=0; i<initCells.length(); ++i){
			char c = initCells.charAt(i);
			if(c == EMPTY_CELL){
				values.add(Puzzle.BLANK_CELL);
			} else{
				values.add(Integer.parseInt(Character.toString(c), mag*mag+1));
			}
		}
	}
	
	@Override
	public int mag() {
		return mag;
	}
	
	@Override
	public List<Integer> values() {
		return values;
	}
}
