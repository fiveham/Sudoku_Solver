package sudoku.parse;

import common.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * <p>A utility class that parses a sudoku puzzle out of a text file.</p>
 * @author fiveham
 */
public class TxtParser implements Parser{
	
	private int mag;
	private final List<Integer> values;
	
  /**
   * <p>Constructs a TxtParser parses the text of {@code f} as a sudoku puzzle.</p>
   * @param f the file from which a puzzle is read
   * @param charset the name of the charset used to read the text of {@code f}
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
	
	/**
	 * <p>A TextFormatStyle represents a known style of formatting the text of a file to represent a 
	 * sudoku Puzzle.</p>
	 * <p>Currently, only block and token formats are represented in this enum.</p>
	 * @author fiveham
	 */
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
		
		private final Function<Scanner,Pair<List<Integer>,Integer>> parse;
		
		private TextFormatStyle(Function<Scanner,Pair<List<Integer>,Integer>> parse){
			this.parse = parse;
		}
	}
	
  /**
   * <p>Returns the magnitude of the puzzle described in the file used to construct this parser.</p>
   * @return the magnitude of the puzzle described in the file used to construct this parser
   */
	@Override
	public int mag(){
		return mag;
	}
	
  /**
   * <p>Returns a list of the integers present in the file used to construct this parser. The 
   * entries in the list are in the order in which they were encountered.</p>
   * @return the initial values in the cells of this Parser's puzzle, where 0 represents an empty
   * cell, listed in encounter order
   */
	@Override
	public List<Integer> values(){
		return values;
	}
}
