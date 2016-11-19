package sudoku.technique;

import java.util.function.Function;
import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

public interface Technique<T extends Technique<T>> extends Function<Sudoku,T>{
	
	/**
	 * <p>Performs this technique's analysis on the underlying sudoku graph.</p>
	 * 
	 * <p>If opportunities to set values in any cells or to mark values
	 * impossible in any cells arise, such an opportunity must be 
	 * exploited before the method returns.<p>
	 * @return a {@link sudoku.time.TechniqueEvent Time node} describing the 
	 * solution event that was performed, and whose subordinate Time nodes 
	 * describe subordinate steps of the solution event, such as 
	 * {@link Rule#validateFinalState() automatic resolution} of Rule-subset 
	 * scenarios, or returns null of no changes were made
	 */
	public TechniqueEvent digest();
	
	/**
	 * <p>Outputs a new Technique of the same type as the type that 
	 * implements this method which will work to solve the specified 
	 * Sudoku.</p>
	 * <p>This method exists because the code in Solver can be made 
	 * much more compact and readable by using a lambda expression to 
	 * describe the inheritance of techniques from one Solver thread 
	 * to its children and because it cannot be guaranteed that 
	 * every concrete Technique will have a constructor capable of 
	 * making a new instance of that type using only the target Sudoku 
	 * as a parameter. Additional parameters may need to be specified 
	 * in order to properly construct a new instance of a type.  So, 
	 * to get a new instance of a concrete Technique from an existing 
	 * instance, this method must be used, allowing the concrete type 
	 * to include any other necessary parameters and operations in the 
	 * construction of the new instance to be returned.</p>
	 */
	@Override
	public T apply(Sudoku sudoku);
}
