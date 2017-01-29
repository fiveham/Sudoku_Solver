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
   * @see #run()
   * @throws FileNotFoundException if the file specified by the first command-line argument could
   * not be found
   */
  public static void main(String[] args) throws FileNotFoundException, InterruptedException{
    Solver s = solver(args);
    s.solve();
    System.out.println(s.getTarget().toString());
  }

  private static Solver solver(String[] args) throws FileNotFoundException{
    if(args.length < 1){ //MAGIC
      errorExit();
    }
    File file = new File(args[SRC_FILE_ARG_INDEX]);
    return args.length < 2 //MAGIC
      ? new Solver(file)
      : new Solver(file, args[CHARSET_ARG_INDEX]);
  }
  
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
