package sudoku.parse;

import common.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import sudoku.Sudoku;

/**
 * <p>A utility class that tries to parse a target out of a text source via a specified Scanner that
 * scans from that text.</p> <p>Used by Puzzle to read a sudoku puzzle of unknown dimensions from a
 * .txt file and to extract both the content of the puzzle and the
 * {@link Sudoku#magnitude() magnitude} of the puzzle without requiring separate passes for each of
 * those results.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class TxtParser implements Parser{
	
	private int mag;
	private final List<Integer> values;
	
    /**
     * <p>Constructs a Parser that extracts and parses text via the specified Scanner. {@code s} is
     * closed by this constructor.</p>
     * @param s the Scanner that sources the text that this Parser analyses. {@code s} is closed by
     * this constructor
     */
	public TxtParser(File f, String charset) throws FileNotFoundException{
		Pair<List<Integer>,Integer> pair = null;
		for(TextFormatStyle style : TextFormatStyle.values()){
			pair = style.parse.apply(new Scanner(f, charset));
			if(pair != null){
				break;
			}
		}
		
		if(pair == null){
			throw new IllegalArgumentException("Could not parse specified file as txt.");
		}
		this.values = pair.getA();
		this.mag = pair.getB();
	}
	
	public static final int TYPICAL_BLOCK_SIDE_LENGTH = 9;
	
	private static enum TextFormatStyle{
		BLOCK((s) -> {
			List<String> lines = new ArrayList<>(TYPICAL_BLOCK_SIDE_LENGTH);
			do{
				if(!s.hasNextLine()){
					return null;
				}
				String line = s.nextLine();
				lines.add(line);
				if(line.length() != lines.get(0).length()){
					return null;
				}
			} while(lines.size() < lines.get(0).length());
			
			int mag = (int) Math.sqrt(lines.size());
			if(mag*mag != lines.size()){ //side-length of square is a square number?
				return null;
			}
			
			List<Integer> result = new ArrayList<>(lines.size() * lines.size());
			for(String line : lines){
				for(int i = 0; i<line.length(); ++i){
					try{
						result.add(Integer.parseInt(line.substring(i, i+1), lines.size()+1));
					} catch(NumberFormatException e){
						return null;
					}
				}
			}
			
			return new Pair<>(result,mag);
		}), 
		TOKEN((s) -> {
			List<Integer> val = new ArrayList<>();
			while(s.hasNextInt()){
				val.add(s.nextInt());
			}
			
			int mag = (int) Math.sqrt(Math.sqrt(val.size()));
			if(mag*mag*mag*mag != val.size() 
					|| val.stream().anyMatch((i) -> i > mag*mag)){
				return null;
			}
			
			return new Pair<>(val,mag);
		});
		
		private final Function<Scanner, Pair<List<Integer>, Integer>> parse;
		
		private TextFormatStyle(Function<Scanner, Pair<List<Integer>, Integer>> parse){
			this.parse = parse;
		}
	}
	
    /**
     * <p>Returns the magnitude of the puzzle specified by the text behind the Scanner sent to the
     * {@link #Parser(Scanner) constructor}, which is determined as a side-effect of the
     * {@link #parse(Scanner) parsing} process.</p>
     * @return the magnitude of the puzzle specified by the text behind the Scanner sent to the
     * {@link #Parser(Scanner) constructor}
     */
	@Override
	public int mag(){
		return mag;
	}
	
    /**
     * <p>Returns a list of the integers present in the text of the puzzle as read from the text
     * source specified by the Scanner sent to the {@link #Parser(Scanner) constructor}.</p>
     * @return the initial values in the cells of this Parser's puzzle, where 0 represents an empty
     * cell
     */
	@Override
	public List<Integer> values(){
		return values;
	}
}
