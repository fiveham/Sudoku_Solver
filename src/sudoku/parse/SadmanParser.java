package sudoku.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Supplier;
import java.io.File;
import java.io.FileNotFoundException;
import sudoku.Puzzle;

/**
 * <p>Parses the initial puzzle out of a Sadman Sudoku puzzle file. This format is defined at
 * http://www.sadmansoftware.com/sudoku/faq19.php (retrieved 25 May 2016).</p> <p>All elements of a
 * Sadman format file other than the initial state of the puzzle are ignored, including the number
 * of solutions.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class SadmanParser implements Parser{
	
    /**
     * <p>Content of the line prior to the statement of the initial state of the puzzle.</p>
     */
	public static final String INITIAL_PUZZLE_MARKER = "[Puzzle]";
	
    /**
     * <p>Indicates an empty cell in the initial puzzle state.</p>
     */
	public static final char EMPTY_CELL = '.';
	
	private final int mag;
	private final List<Integer> values;
	
    /**
     * <p>Constructs a Parser that interprets the contents of the file {@code f} as a Sadman-format
     * sudoku puzzle, using the specified {@code charset} to read the file.</p>
     * @param f the file to be interpreted
     * @param charset the name of the charset to be used in reading the file. If the
     * @throws FileNotFoundException
     */
	public SadmanParser(File f, String charset) throws FileNotFoundException{
		this(() -> {
			try{
				return new Scanner(f, charset);
			} catch(FileNotFoundException e){
				throw new IllegalArgumentException(e);
			}
		});
	}
	
	public SadmanParser(File f) throws FileNotFoundException{
		this(() -> {
			try{
				return new Scanner(f);
			} catch(FileNotFoundException e){
				throw new IllegalArgumentException(e);
			}
		});
	}
	
	private SadmanParser(Supplier<Scanner> supplier) throws FileNotFoundException{
		Scanner s = supplier.get();
		while(s.hasNext() && !INITIAL_PUZZLE_MARKER.equals(s.nextLine()));
		
		if(!s.hasNext()){
			s.close();
			s = supplier.get();
		}
		
		StringBuilder initCells;
		try{
			initCells = new StringBuilder(s.nextLine());
			this.mag = (int)Math.sqrt(initCells.length());
			for(int i=1; i<mag*mag; ++i){
				initCells.append(s.nextLine());
			}
		} catch(NoSuchElementException e){
			throw new IllegalArgumentException("Could not parse the file as Sadman format", e);
		} finally{
			s.close();
		}
		this.values = new ArrayList<>(initCells.length());
		for(int i=0; i<initCells.length(); ++i){
			char c = initCells.charAt(i);
			values.add(c == EMPTY_CELL 
					? Puzzle.BLANK_CELL 
					: Integer.parseInt(Character.toString(c), mag*mag+1));
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
