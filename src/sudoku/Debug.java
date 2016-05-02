package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * <p>This class provides less verbose {@link #log(String) access} 
 * to a log file to which to print debugging information.</p>
 * @author fiveham
 *
 */
public class Debug {
	
	private static final PrintStream log = /*System.out;*/initLog();
	
	/**
	 * <p>Returns the PrintStream referred to by <tt>log</tt>, 
	 * which points to a file named "dump.txt".</p>
	 * <p><tt>log</tt> is initialized by a call to this method.</p>
	 * @return the PrintStream to be stored in <tt>log</tt>
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
	 * <p>Prints <tt>s</tt> to the file specified by <tt>log</tt>.</p>
	 * @param s the string to be printed to the file specified by 
	 * <tt>log</tt>
	 */
	public static void log(Object s){
		log.println(s);
	}
	
	/**
	 * <p>Prints a newline to the file specified by <tt>log</tt>.</p>
	 */
	public static void log(){
		log.println();
	}
}
