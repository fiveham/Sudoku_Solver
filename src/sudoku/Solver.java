package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class Solver {
	
	private List<Technique> techniqueList;
	private Puzzle puzzle;
	
	public Solver(String filename) throws FileNotFoundException{
		this(new Puzzle(new File(filename)));
	}
	
	public Solver(File f) throws FileNotFoundException{
		this(new Puzzle(f));
	}
	
	public Solver(Puzzle puzzle){
		this.puzzle = puzzle;
		
		techniqueList = new ArrayList<>();
		
		techniqueList.add(puzzle.resolveResolvables());
		techniqueList.add(new SledgeHammer2(puzzle));
		techniqueList.add(new ColorChain(puzzle));
	}
	
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	/**
	 * Applies each technique in <tt>techniqueList</tt> to <tt>target</tt> 
	 * 
	 * @return true if the target is solved, false otherwise
	 */
	public boolean solve(){
		for(int index = 0; 
				index < techniqueList.size(); 
				index = techniqueList.get(index).digest() ? 0 : (index+1) );
		puzzle.newEventFrame();
		return puzzle.isSolved();
	}
	
	/**
	 * Main method. Creates a Solver instance and {@link #solve() uses} it, then 
	 * {@link Puzzle#toString() prints} the target to the console.
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException{
		Solver s = new Solver(new File(args[0]));
		
		s.solve();
		
		System.out.println(s.puzzle.toString());
	}
}
