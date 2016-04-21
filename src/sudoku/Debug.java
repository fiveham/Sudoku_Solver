package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

class Debug {
	
	private static final PrintStream log = initLog();
	
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
	
	static void log(String s){
		log.println(s);
	}
	
	static void log(){
		log.println();
	}
	
}
