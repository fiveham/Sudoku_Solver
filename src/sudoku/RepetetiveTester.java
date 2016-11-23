package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Runs {@code Solver} for each of a collection of sudoku puzzles.</p>
 * @author fiveham
 * @author fiveham
 *
 */
public class RepetetiveTester {
	
	public static final String SADMAN_DIRECTORY = "./puzzles/sadman/";
	public static final String EULER_DIRECTORY = "./puzzles/project_euler/";
	
	public static void main(String[] args) {
		List<File> puzzles = Stream.of(new File(SADMAN_DIRECTORY).list()).map((s) -> new File(SADMAN_DIRECTORY + s)).collect(Collectors.toList());
		Stream.of(new File(EULER_DIRECTORY).list()).map((s) -> new File(SADMAN_DIRECTORY + s)).forEach(puzzles::add);;
		
		for(File f : puzzles){
			try{
				Solver solver = new Solver(f);
				System.out.println(f.getName());
				solver.solve();
				System.out.println(solver.getTarget().toString());
			} catch(FileNotFoundException e){
				System.out.println("Could not find file "+f.getName());
			} catch(InterruptedException e){
				System.out.println("InterruptedException for file "+f.getName());
			}
		}
	}
}
