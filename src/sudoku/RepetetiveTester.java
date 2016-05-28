package sudoku;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * <p>Runs {@code Solver} for each of a collection of sudoku 
 * puzzles.</p>
 * @author fiveham
 *
 */
public class RepetetiveTester {
	
	public static final String SADMAN_DIRECTORY = "./puzzles/sadman/";
	
	public static void main(String[] args) {
		File sadmanDir = new File(SADMAN_DIRECTORY);
		
		for(String sadman : sadmanDir.list()){
			File f = new File(SADMAN_DIRECTORY + sadman);
			
			try{
				Solver solver = new Solver(f);
				System.out.println(f.getName());
				solver.solve();
				System.out.println(solver.getPuzzle().toString());
			} catch(FileNotFoundException e){
				System.out.println("Could not find file "+f.getName());
			} catch(InterruptedException e){
				System.out.println("InterruptedException for file "+f.getName());
			}
		}
	}
	
}
