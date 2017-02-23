package sudoku.parse;

import common.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

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
   * @throws FileNotFoundException if {@code f} could not be read
   */
	public TxtParser(File f, String charset) throws FileNotFoundException{
		Pair<List<Integer>, Integer> pair;
		try{
		  pair = Stream.of(TextFormatStyle.values())
	        .map((style) -> style.parse(f, charset))
	        .filter(Objects::nonNull)
	        .findFirst()
	        .orElse(null);
		} catch(ScannerNotConstructed e){
		    throw e.getCause();
		}
		
		if(pair == null){
			throw new IllegalArgumentException("Could not parse specified file as txt.");
		}
		this.values = pair.getA();
		this.mag = pair.getB();
	}
	
	/**
	 * <p>A non-checked exception used to smuggle a FileNotFoundException out of a lambda 
	 * expression so the FileNotFoundException can be rethrown outside the lambda expression.</p>
	 * @author fiveham
	 */
	private static class ScannerNotConstructed extends RuntimeException{
	  
    private static final long serialVersionUID = 4859671724169737013L;
    
    /**
     * <p>Constructs a ScannerNotConstructed whose cause is {@code src}.</p>
     * @param src the FileNotFoundException thrown inside a lambda expression that needs to be 
     * smuggled out into the context around that lambda expression
     */
    private ScannerNotConstructed(FileNotFoundException src){
	    super(src);
	  }
	  
    /**
     * @return the FileNotFoundException that this unchecked exception is smuggling out of a lambda 
     * expression
     */
	  @Override
	  public FileNotFoundException getCause(){
	    return (FileNotFoundException) super.getCause();
	  }
	}
	
	public static final int TYPICAL_BLOCK_SIDE_LENGTH = 9;
	
	/**
	 * <p>A TextFormatStyle represents a known style of formatting the text of a file to represent a 
	 * sudoku Puzzle.</p>
	 * <p>Currently, only block and token formats are represented in this enum.</p>
	 * @author fiveham
	 */
	private static enum TextFormatStyle{
	  
	  /**
	   * <p>Assumes that the puzzle in a file is presented at the very beginning and is represented as
	   * a block of base-{@code n} digits, where {@code n} is 1 plus the side-length of the puzzle. 
	   * Being arranged in a block means that each cell's initial value is described by exactly one 
	   * character, that each row is represented as the concatenation of those cell characters, and 
	   * the first {@code sideLength} lines are the rows of the puzzle.</p> 
	   * <p>A zero indicates an empty cell.</p>
	   */
		BLOCK(TextFormatStyle::blockParse), 
		
		/**
		 * <p>Assumes that the puzzle in a file is presented as a whitespace-delimited sequence of 
		 * base-10 integers.</p>
		 * <p>These tokens must be the first tokens in the file. The end of the region of the source 
		 * file where the cells' initial values are declared is represented with end-of-file or by a 
		 * token that cannot be parsed as an int. How many int tokens there are must be the fourth power 
		 * of an integer.</p>
		 */
		TOKEN(TextFormatStyle::tokenParse);
		
		private final Function<Scanner, Pair<List<Integer>, Integer>> parse;
		
		private TextFormatStyle(Function<Scanner, Pair<List<Integer>, Integer>> parse){
			this.parse = parse;
		}
		
		/**
		 * <p>Reads {@code f} using the specified {@code charset} name and tries to parse the file 
		 * according to the rules of this TextFormatStyle.</p>
		 * @param f the file to be read
		 * @param charset the name of the charset used in the file
		 * @return the initial values of the cells of the puzzle described in {@code f} and the square 
		 * root of the side-length of the puzzle
		 * @throws ScannerNotConstructed if a FileNotFoundException was thrown trying to open {@code f}
		 */
		private Pair<List<Integer>, Integer> parse(File f, String charset){
		  Scanner s = null;
		  try{
		    s = new Scanner(f, charset);
		  } catch(FileNotFoundException e){
		    throw new ScannerNotConstructed(e);
		  }
		  
		  Pair<List<Integer>, Integer> result = parse.apply(s);
		  s.close();
		  return result;
		}
		
		/**
		 * <p>Parses a file as a sudoku puzzle under the assumption that the puzzle's initial values are
		 * presented as a block, where the initial cell values for the first row are the first line, the 
		 * second row is the second line, etc., and where the number of characters on each line is 
		 * equal to the number of such lines.</p>
		 * <p>The number of characters in the first line is the number of lines that will be parsed into 
		 * a puzzle.</p>
		 * <p>If the puzzle described by the file is not structured correctly, it is invalid and null 
		 * is returned. Incorrect structures are: 
		 * <ul>
		 * <li>if any of the rows has a length not equal to that of the first row</li>
		 * <li>if the number of rows (the length of the first row) is not a square number</li>
		 * <li>if the number of rows is not equal to the length of the first row</li>
		 * <li>if any character in a row cannot be {@link Integer#parseInt(String,int) parsed} as an 
		 * integer in the base equal to 1 plus the number of rows</li>
		 * </ul></p>
		 * @param s a Scanner reading a file
		 * @return the initial values of the cells of the puzzle described by {@code s} or null if the 
     * puzzle is invalid
		 */
		private static Pair<List<Integer>, Integer> blockParse(Scanner s){
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
	    if(mag * mag != lines.size()){ //side-length of square is a square number?
	      return null;
	    }
	    
	    List<Integer> result = new ArrayList<>(lines.size() * lines.size());
	    for(String line : lines){
	      for(int i = 0; i < line.length(); ++i){
	        try{
	          result.add(Integer.parseInt(line.substring(i, i + 1), lines.size() + 1));
	        } catch(NumberFormatException e){
	          return null;
	        }
	      }
	    }
	    
	    return new Pair<>(result, mag);
	  }
	  
		/**
		 * <p>Parses the content output by {@code s} as a sudoku puzzle where the first {@code n} 
		 * tokens are base-10 ints defining the initial values of the cells of the puzzle, where 
		 * {@code n} is the square of the side-length of the puzzle.</p>
		 * <p>If the number of int tokens found at the start of the scanned content is not the fourth 
		 * power of an integer, of if any of the ints read in is greater than the square root of the 
		 * number of ints read in, the puzzle described is invalid and null is returned.</p>
		 * <p>This method assumes that a non-int token or end-of-file is present to indicate the end of 
		 * the field of initial cell values.</p>
		 * @param s a Scanner reading a file 
		 * @return the initial values of the cells of the puzzle described by {@code s} or null if the 
		 * puzzle is invalid
		 */
	  private static Pair<List<Integer>, Integer> tokenParse(Scanner s){
	    List<Integer> val = new ArrayList<>();
	    while(s.hasNextInt()){
	      val.add(s.nextInt());
	    }
	    
	    int mag = (int) Math.sqrt(Math.sqrt(val.size()));
	    if(mag * mag * mag * mag != val.size() || val.stream().anyMatch((i) -> i > mag * mag)){
	      return null;
	    }
	    
	    return new Pair<>(val, mag);
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
