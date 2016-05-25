package sudoku.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

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
		StringBuilder initCells = new StringBuilder(s.next());
		this.mag = (int)Math.sqrt(initCells.length());
		for(int i=1; i<initCells.length(); ++i){
			initCells.append(s.nextLine());
		}
		s.close();
		this.values = new ArrayList<>(initCells.length());
		for(int i=0; i<initCells.length(); ++i){
			if(initCells.charAt(i) == EMPTY_CELL){
				values.add(0);
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
