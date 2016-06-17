package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import sudoku.parse.Parser;
import sudoku.Claim;
import sudoku.Puzzle;
import sudoku.Puzzle.IndexValue;
import sudoku.Rule;

/**
 * <p>This class provides less verbose {@link #log(String) access} 
 * to a log file to which to print debugging information.</p>
 * @author fiveham
 *
 */
public class Debug {
	
	private static final State state = State.LOG;
	
	private static final PrintStream log = System.out;// initLog();
	
	/**
	 * <p>Creates the PrintStream referred to by {@code log}, 
	 * which points to a file named "dump.txt".</p>
	 * <p>{@code log} is initialized by a call to this method.</p>
	 * @return the PrintStream to be stored in {@code log}
	 */
	private static PrintStream initLog(){
		try{
			File f = new File("dump.txt");
			System.out.println("Creating log file: "+f.getAbsolutePath());
			return new PrintStream(f);
		} catch(FileNotFoundException e){
			System.out.println("Error: Could not open log file for writing.");
			System.exit(1);
			return null;
		}
	}
	
	/**
	 * <p>Prints {@code s} to the file specified by {@code log}.</p>
	 * @param s the string to be printed to the file specified by 
	 * {@code log}
	 */
	public static void log(Object s){
		state.log(log,s);
	}
	
	public static void log(boolean wrap, Object s){
		state.log(log, wrap?wrap(s.toString()):s);
	}
	
	private static final int WRAP_LEN = 2+"[Rule: The 1 in BOX 1, Rule: The 1 in BOX 2, Rule: The 1 in BOX 3, Rule: The 1 in BOX 8, Rule: The 1 in BOX 9, Rule: The 2 in BOX 6, Rule: The 2 in BOX 7, Rule: The 3 in BOX 2, Ru".length();
	
	private static String wrap(String s){
		StringBuilder out = new StringBuilder();
		Scanner scanner = new Scanner(s);
		while(scanner.hasNextLine()){
			List<String> newLines = wrapLine(scanner.nextLine());
			for(String newLine : newLines){
				out.append(newLine).append(System.lineSeparator());
			}
			
		}
		scanner.close();
		return out.toString();
	}
	
	private static List<String> wrapLine(String line){
		List<String> result = new java.util.ArrayList<>();
		
		for(int breakIndex, pointer = 0; pointer < line.length(); pointer = breakIndex){
			breakIndex = breakIndex(line);
			result.add(line.substring(pointer, breakIndex));
		}
		
		return result;
	}
	
	private static int breakIndex(String line){
		int i = WRAP_LEN;
		for(; i>0; --i){
			char c = line.charAt(i);
			if(Character.isWhitespace(c)){
				return i;
			}
		}
		for(i=WRAP_LEN+1; i<line.length(); ++i){
			char c = line.charAt(i);
			if(Character.isWhitespace(c)){
				return i;
			}
		}
		return i;
	}
	
	/**
	 * <p>Prints a newline to the file specified by {@code log}.</p>
	 */
	public static void log(){
		state.log(log,"");
	}
	
	private static enum State{
		NO_LOG( (ps,o) -> {}),
		LOG(PrintStream::println);
		
		private final BiConsumer<PrintStream,Object> writer;
		
		private State(BiConsumer<PrintStream,Object> writer){
			this.writer = writer;
		}
		
		private void log(PrintStream ps, Object o){
			writer.accept(ps,o);
		}
	}
	
	public static void main(String[] args){
		Puzzle p = new Puzzle(new Parser(){
			@Override
			public List<Integer> values(){
				return IntStream.range(0, 81).map((i) -> 0).mapToObj(Integer.class::cast).collect(Collectors.toList());
			}
			@Override
			public int mag(){
				return 3;
			}
		});
		
		IndexValue indexValue = p.indexValues().get(3);
		
		Claim c = new Claim(p, indexValue, indexValue, indexValue);
		Rule r;
		{
			List<Claim> claims = Collections.singletonList(c);
			Puzzle.IndexInstance y = p.indexInstances(Puzzle.DimensionType.Y).get(3);
			Puzzle.IndexInstance x = p.indexInstances(Puzzle.DimensionType.X).get(3);
			
			r = new Rule(p, Puzzle.RuleType.CELL, claims, y, x);
		}
		
		System.out.println();
		System.out.println("Created and linked R and C");
		
		System.out.println();
		System.out.println(c);
		System.out.println(c.contentString());
		
		System.out.println();
		System.out.println(r);
		System.out.println(r.contentString());
		
		c.remove(r);
		
		System.out.println();
		System.out.println("Pulled R out of C");
		
		System.out.println();
		System.out.println(c);
		System.out.println(c.contentString());
		
		System.out.println();
		System.out.println(r);
		System.out.println(r.contentString());
		
		r.remove(c);
		
		System.out.println();
		System.out.println("Pulled C out of R");
		
		System.out.println();
		System.out.println(c);
		System.out.println(c.contentString());
		
		System.out.println();
		System.out.println(r);
		System.out.println(r.contentString());
		
	}
}
