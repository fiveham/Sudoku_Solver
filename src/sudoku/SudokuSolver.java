package sudoku;

import java.io.*;
import java.util.*;

/**
 * Tries to solve a Sudoku target using 
 * several different analysis techniques.
 * @author fiveham
 */

public class SudokuSolver {
	
	/*
	 * This class is instantiatable and executable.
	 * The main() method provides a pre-existing means
	 * of using the analysis techniques, while the
	 * instantiability allows these techniques to
	 * remain useful to external code through 
	 * instantiation of the SudokuSolver class.
	 * 
	 * Alternatively, external code may create a new 
	 * solver class that uses pre-existing and/or 
	 * new solution techniques and the pre-existing 
	 * target-related classes.
	 */
	
	/** the proper number of command-line arguments */
	public static final int 		ARGUMENT_COUNT = 1;
	
	//TO DO study
	//public static String yes_marker = "Y";
	//public static String no_marker = "n";
	
	/*
	 * The techniques to be used in solving the target,
	 * listed in their order of precedence.
	 */
	private List<Technique> 	techniqueList;
	
	private Puzzle 					puzzle;
	
	//TO DO: Remove this field and all references to it once study is complete
	//private String techniqueSuccesses;
	
	/**
	 * Constructs a SudokuSolver, assigning this solver's 
	 * target and adding a default list of techniques to 
	 * this solver's technique list.
	 * @param target			The target to which this 
	 * solver pertains.
	 */
	public SudokuSolver(Puzzle puzzle){
		this.puzzle = puzzle;
		
		techniqueList = new ArrayList<Technique>();
		
		techniqueList.add(new OrganFailure(puzzle));
		techniqueList.add(new ValueClaim(puzzle));
		techniqueList.add(new CellDeath(puzzle));
		techniqueList.add(new GroupLocalizationExternal(puzzle));
		//techniqueList.add(new GroupLocalizationInternal(target));
		techniqueList.add(new XYWing(puzzle));
		techniqueList.add(new LineHatch(puzzle));
		techniqueList.add(new RemotePairs(puzzle));
	}
	
	/**
	 * Constructs a SudokuSolver, assigning this solver's 
	 * target and setting this solver's technique list 
	 * to an externally defined list.
	 * @param target			The target to which this 
	 * solver pertains.
	 * @param techniqueList		The list of techniques 
	 * to be used by this solver in analysing its target.
	 */
	public SudokuSolver(Puzzle puzzle, ArrayList<Technique> techniqueList){
		this.puzzle = puzzle;
		this.techniqueList = new ArrayList<>(techniqueList);
	}
	
	/**
	 * A default solver process. Creates the target from 
	 * a file whose name is passed as a command-line argument, 
	 * then creates a solver instance for that target with 
	 * the default list of techniques and uses the solver. 
	 * Then it prints the results to the console. If the 
	 * target is not solved as the end of processing, 
	 * prints a report of errors in the target 
	 * @param args command-line arguments
	 */
	public static void main(String[] args){
		// Setup 
		
		// verify command-line arguments
		if(args.length!=ARGUMENT_COUNT){
				System.out.println("Usage: java SudokuSolver source_file_name");
				System.exit(1);
		}
		
		// create target
		Puzzle puzzle = null;
		try{
			puzzle = new Puzzle( new File(args[0]));
		}
		catch(FileNotFoundException e){
			System.out.println("Source file for target \""+args[0]+"\" not found.");
			System.exit(1);
		}
		catch(IOException e){															//Primarily in case file cannot be read
			System.out.println("I/O Error in creating target: "+e.getMessage());
			System.exit(1);
		}
		
		// Digest
		SudokuSolver solver = new SudokuSolver(puzzle);
		solver.digest();
		
		// Finish
		solver.printPuzzle();
		
		if( !puzzle.isSolved() )
			solver.printErrorReport();
	}
	
	/**
	 * Cycles through the list of digestion techniques. 
	 * After each technique, if that technique made any 
	 * changes to the target, resets to the first 
	 * technique in the list, otherwise, moves on to 
	 * the next technique.
	 */
	public void digest(){
		
		for(int index = 0; 
				index < techniqueList.size(); 
				index = techniqueList.get(index).digest() ? 0 : (index+1) ){}
		
		//boolean madeChanges = false;
		//for(int index = 0; index < techniqueList.size(); index = madeChanges ? 0 : (index+1) ){
		//	madeChanges = techniqueList.get(index).digest();
		//	if(madeChanges){
		//		//System.out.println("index = "+index);
		//		techniqueSuccesses += index;
		//	}
		//}
		
		//TO DO: remove this at the end of study
		//techniqueSuccesses += " " + (target.isSolved() ? 'Y' : 'n');
	}
	
	/**
	 * Prints this solver's target and a description of 
	 * this target's cells' possible values.
	 */
	private void printPuzzle(){
		System.out.println("Puzzle after digestion:");
		System.out.println();
		System.out.println(puzzle.toString());
		System.out.println();
		System.out.println(puzzle.possibilitiesAsString());
	}
	
	/**
	 * Prints a report on erroneous configurations of possibilities 
	 * in the target's regions and cells: multiple cells in a region 
	 * capable of holding a value; no cells in a region capable of 
	 * holding a value; a cell marked as holding a value with other 
	 * cells in the region marked as still capable of housing that 
	 * value; multiple values marked as present in a cell, a value 
	 * marked present in a cell with other values still marked 
	 * capable of being present in that cell, or; all values marked 
	 * impossible in a cell.
	 */
	private void printErrorReport(){
		System.out.println("Region errors:");
		
		System.out.println("Blocks:");
		for(Index index : Index.KNOWN_VALUES){
			Block block = puzzle.getBlock(index);
			if(block.hasError()){
				System.out.println("Box "+index.toInt()+":");
				System.out.println(block.error());
			}
		}
		
		System.out.println("Rows:");
		for(Index index : Index.KNOWN_VALUES){
			Line row = puzzle.getRow(index);
			if(row.hasError()){
				System.out.println("Row "+index.toInt()+":");
				System.out.println(row.error());
			}
		}
		
		System.out.println("Columns:");
		for(Index index : Index.KNOWN_VALUES){
			Line column = puzzle.getColumn(index);
			if(column.hasError()){
				System.out.println("Column "+index.toInt()+":");
				System.out.println(column.error());
			}
		}
		
		System.out.println("Cell errors:");
		for(Cell[] currentArray : puzzle.getCells())
			for(Cell currentCell : currentArray)
				if(currentCell.hasError())
					System.out.println(currentCell.error());
		
		System.out.println("");
	}
	
	/**
	 * Returns a reference to this solver's target.
	 * @return a reference to this solver's target.
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
}