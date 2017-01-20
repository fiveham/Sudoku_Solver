package sudoku.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
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
		this(() -> new Scanner(f, charset));
	}
	
	public SadmanParser(File f) throws FileNotFoundException{
		this(() -> new Scanner(f));
	}
	
	/**
	 * <p>This is a knockoff of {@literal java.util.function.Supplier<Scanner>} that declares that it 
	 * throws a FileNotFoundException as part of its method header. This allows the two public 
	 * constructors to send Scanner-suppliers to the relevant constructor without needing to include 
	 * try-catch blocks inside the lambda bodies they send.</p>
	 * @author fiveham
	 */
	@FunctionalInterface
	private interface FNFSupplier{
	  public Scanner get() throws FileNotFoundException;
	}
	
	/**
	 * <p>Constructs a SadmanParser based on the content output by the Scanner supplied by 
	 * {@code supplier}.</p>
	 * <p>Scans the file until {@literal "[Puzzle]"} is found or the end-of-file is reached. 
	 * Thereafter, the content of the puzzle is read in or a new Scanner replaces the previous one, 
	 * respectively. In the latter case, the new Scanner reads the first lines of the file to read the 
	 * puzzle just as a Scanner that found a {@literal "[Puzzle]"} marker would do with the lines 
	 * after the marker. 
	 * @param supplier supplies a Scanner for a file specified in a public constructor
	 * @throws IllegalArgumentException if the file could not be parsed according to the Sadman format
	 * @throws FileNotFoundException if the file backing {@code supplier} could not be read
	 * @throws NumberFormatException if a character describing the initial value of a cell cannot be 
	 * parsed as an int
	 */
	private SadmanParser(FNFSupplier supplier) throws FileNotFoundException{
		Scanner s = supplier.get();
		while(s.hasNext() && !INITIAL_PUZZLE_MARKER.equals(s.nextLine()));
		
		if(!s.hasNext()){
			s.close();
			s = supplier.get();
		}
		
		StringBuilder initCells;
		try{
			initCells = new StringBuilder(s.nextLine());
			this.mag = (int) Math.sqrt(initCells.length());
			for(int i = 1; i < mag * mag; ++i){
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
