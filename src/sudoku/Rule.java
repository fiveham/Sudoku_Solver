package sudoku;

import java.util.Collection;
import sudoku.Puzzle.RuleType;
import sudoku.Puzzle.IndexInstance;

/**
 * <p>Represents a very specific true statement about a sudoku puzzle: "Exactly one of the Claims in
 * this set is true."</p>
 * <p>The statement represented by a given Rule is "very specific" in that each typically-stated 
 * rule for the solution of a sudoku puzzle pertains to several Rule instances. For example, "Each 
 * cell has one value." becomes 81 statements for a 9x9 puzzle, saying "Cell 1,1 has one value.", 
 * "Cell 1,2 has one value.", and so forth.  For another example, "Each value occurs exactly once 
 * in each box." becomes 81 statements in a 9x9 puzzle of the form "Box {@code a} has exactly one 
 * {@code b}." where {@code a} names a specific box and {@code b} is a value.</p>
 * <p>A Rule identifies and contains the truth-claims pertaining to a single cell, a box-value 
 * pair, a column-value pair or a row-value pair: "Cell 1,1 is 1.", "Cell 1,1 is 2.", 
 * "Cell 1,1 is 3.", and so forth.; "The 5 in row 2 is in column 1.", 
 * "The 5 in row 2 is in column 2.", "The 5 in row 2 is in column 3.", and so forth.</p>
 * @author fiveham
 */
public class Rule extends Fact{
	
	private static final long serialVersionUID = 2703809294481675542L;
	
	private final Puzzle.RuleType type;
	private final IndexInstance dimA;
	private final IndexInstance dimB;
	
  /**
   * <p>Constructs a Rule belonging to the specified Puzzle, having the specified {@code type}, and 
   * containing all the elements of {@code c}.</p>
   * @param puzzle the Puzzle to which this Rule belongs
   * @param type the {@link Puzzle.RuleType type} of this Rule, whether it is a cell, box, row, or 
   * column
   * @param c a collection whose elements will be the elements of this Rule
   */
	public Rule(
	    Puzzle puzzle, 
	    RuleType type, 
	    Collection<Claim> c, 
	    IndexInstance dimA, 
	    IndexInstance dimB) {
	  
		super(
		    puzzle, 
		    c, 
		    linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength()));
		this.type = type;
		this.dimA = dimA;
		this.dimB = dimB;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Rule){
			Rule r = (Rule) o;
			return type == r.type && dimA.equals(r.dimA) && dimB.equals(r.dimB); 
		}
		return false;
	}
	
	@Override
	public String toString(){
		return "Rule: " + type.descriptionFor(dimA, dimB);
	}
	
	/**
	 * <p>Returns the {@link Puzzle.RuleType type} of this Rule, whether it is a cell, box, row, or 
	 * column.</p>
	 * @return the {@link Puzzle.RuleType type} of this Rule, whether it is a cell, box, row, or 
   * column
	 */
	public RuleType getType(){
		return type;
	}
	
	/**
	 * <p>Returns the position of this Rule in its first dimension.</p>
	 * @return the position of this Rule in its first dimension
	 */
	public IndexInstance dimA(){
		return dimA;
	}
	
	/**
	 * <p>Returns the position of this Rule in its second dimension.</p>
	 * @return the position of this Rule in its second dimension
	 */
	public IndexInstance dimB(){
		return dimB;
	}
}
