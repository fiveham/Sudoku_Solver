package sudoku;

import java.io.File;
import java.io.FileNotFoundException;

public class SolverApp {
  
  /**
   * <p>Main method. Creates a Solver instance and {@link Solver#solve() uses} it, then
   * {@link Puzzle#toString() prints} the target to the console.</p>
   * <p>The first command-line argument is the name of the file from which to read the target to 
   * be solved.</p>
   * @param args command line arguments
   * @throws FileNotFoundException if the file specified by the first command-line argument could
   * not be found
   * @throws InterruptedException
   */
  public static void main(String[] args) throws FileNotFoundException, InterruptedException{
    Solver s = solver(args);
    s.solve();
    System.out.println(s.getTarget().toString());
  }
  
  /**
   * <p>Returns a Solver that works to solve the puzzle specified by the file named in the 
   * {@code args}.</p>
   * <p>If {@code args} is empty, the program {@link System#exit(int) exits}.</p>
   * @param args command-line arguments. The first arg is the name of the file containing the 
   * puzzle to be solved. The second arg, if present, names the charset to be used to read the 
   * file
   * @return  a Solver that works to solve the puzzle specified by the file named in the 
   * {@code args}
   * @throws FileNotFoundException if the file named by the first arg cannot be found or read
   */
  private static Solver solver(String[] args) throws FileNotFoundException{
    if(args.length < MIN_ARG_COUNT){
      errorExit();
    }
    File file = new File(args[SRC_FILE_ARG_INDEX]);
    return args.length < MIN_ARGS_FOR_CHARSET 
      ? new Solver(file)
      : new Solver(file, args[CHARSET_ARG_INDEX]);
  }
  
  private static final int MIN_ARG_COUNT = 1;
  private static final int MIN_ARGS_FOR_CHARSET = 2;
  
  public static final int SRC_FILE_ARG_INDEX = 0;
  public static final int CHARSET_ARG_INDEX = 1;

  /**
   * <p>Prints an error message to the standard error stream, then the program 
   * {@link System#exit(int) exits}.</p>
   */
  private static void errorExit(){
    System.err.println("Usage: java Solver puzzle-file character-encoding");
    System.exit(0);
  }
  
}
