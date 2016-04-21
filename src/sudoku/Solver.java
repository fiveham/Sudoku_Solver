package sudoku;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;

public class Solver {
	
	private List<Technique> techniqueList;
	private Puzzle puzzle;
	
	public Solver(Puzzle puzzle){
		this.puzzle = puzzle;
		
		techniqueList = new ArrayList<>();
		
		techniqueList.add(new OrganFailure(puzzle));
		techniqueList.add(new ValueClaim(puzzle));
		techniqueList.add(new CellDeath(puzzle));
		techniqueList.add(new GroupLocalizationExternal(puzzle));
	}
	
	public void digest(){
		for(int index = 0; 
				index < techniqueList.size(); 
				index = techniqueList.get(index).digest() ? 0 : (index+1) );
	}
	
	public static void main(String[] args) throws FileNotFoundException{
		Solver s = new Solver(new Puzzle(new File(args[0])));
		s.digest();
		System.out.println(s.puzzle.toString());
	}
	
}
