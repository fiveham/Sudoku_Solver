package sudoku;

import java.util.Collection;
import sudoku.Puzzle.RuleType;
import sudoku.Puzzle.IndexInstance;

public class Rule extends Fact{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2703809294481675542L;
	
	private final Puzzle.RuleType type;
	private final IndexInstance dimA, dimB;
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified {@code types}, and containing 
	 * all the elements of {@code c}.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param c a collection whose elements this Rule will also 
	 * have as elements
	 */
	public Rule(Puzzle puzzle, RuleType type, Collection<Claim> c, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, c, linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength()));
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
		return "Rule: " + type.descriptionFor(dimA,dimB) /*+ " ["+size()+"]"*/;
	}
	
	public RuleType getType(){
		return type;
	}
	
	public IndexInstance dimA(){
		return dimA;
	}
	
	public IndexInstance dimB(){
		return dimB;
	}
}
