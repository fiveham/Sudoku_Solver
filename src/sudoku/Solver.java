package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Coordinates and applies several techniques for solving a sudoku target.</p>
 * 
 * <p>Techniques are arranged in a list and are called upon in sequence until the 
 * target is solved. The sequence resets to the beginning if and when a technique 
 * in the list has made changes to the target. By resetting to the start of the 
 * list, more powerful and less expensive techniques are prioritized.</p>
 * 
 * @author fiveham
 *
 */
public class Solver {
	
	private List<Technique> techniqueList;
	private Puzzle puzzle;
	
	/**
	 * <p>Constructs a Solver that works to solve the target defined 
	 * at the beginning of the file named <tt>filename</tt>.</p>
	 * @param filename the name of the file containing the target to 
	 * be solved
	 * @throws FileNotFoundException if the named file could not be 
	 * found
	 */
	public Solver(String filename) throws FileNotFoundException{
		this(new Puzzle(new File(filename)));
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the target 
	 * specified by the text at the beginning of <tt>f</tt>.</p>
	 * @param f the file containing a target to be solved
	 * @throws FileNotFoundException if <tt>f</tt> could not 
	 * be found
	 */
	public Solver(File f) throws FileNotFoundException{
		this(new Puzzle(f));
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the specified 
	 * <tt>target</tt>.</p>
	 * @param target the Puzzle to be solved
	 */
	public Solver(Puzzle puzzle){
		this.puzzle = puzzle;
		
		techniqueList = new ArrayList<>();
		
		techniqueList.add(new SledgeHammer2(puzzle));
		techniqueList.add(new ColorChain(puzzle));
	}
	
	/**
	 * <p>Returns the Puzzle that this Solver works to solve.</p>
	 * @return the Puzzle that this Solver works to solve
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/**
	 * <p>Applies each technique in <tt>techniqueList</tt> to 
	 * <tt>target</tt>. If a technique reports that it was made 
	 * a change to the target, then instead of moving on to the 
	 * next technique in the list, technique selection resets to 
	 * the start of the technique list. This reset mechanism 
	 * allows the prioritization of techniques by placing higher-
	 * priority techniques earlier in the list.</p>
	 * 
	 * @return true if the target is solved, false otherwise
	 */
	public boolean solve(){
		for(int index = 0; 
				index < techniqueList.size(); 
				index = techniqueList.get(index).digest() ? 0 : (index+1) );
		return puzzle.isSolved();
	}
	
	/**
	 * <p>Main method. Creates a Solver instance and {@link #solve() uses} it, then 
	 * {@link Puzzle#toString() prints} the target to the console.</p>
	 * 
	 * <p>The first command-line argument is the name of the file from which to read 
	 * the target to be solved.</p>
	 * 
	 * @param args command line arguments
	 * @throws FileNotFoundException if the file specified by the first command-line 
	 * argument could not be found
	 */
	public static void main(String[] args) throws FileNotFoundException{
		Solver s = new Solver(new File(args[0]));
		
		s.solve();
		
		System.out.println(s.puzzle.toString());
	}
}
