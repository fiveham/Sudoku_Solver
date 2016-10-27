package sudoku.technique;

import java.util.function.Function;
import sudoku.Rule;
import sudoku.Sudoku;
import sudoku.time.TechniqueEvent;

public interface Technique extends Function<Sudoku,Technique>{
	
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
}
