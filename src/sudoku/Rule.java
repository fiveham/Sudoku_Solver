package sudoku;

import common.time.Time;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import sudoku.Puzzle.RegionSpecies;
import sudoku.Puzzle.IndexInstance;

public class Rule extends Fact{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2191355198146127036L;
	
	/**
	 * <p>The number ({@value}) of elements (neighbors) of a Rule that 
	 * is a <tt>xor</tt>.  A Rule is a <tt>xor</tt> if it has two Claims, 
	 * because exactly one of them is true and the other is false. Given 
	 * that we know that the Rule is satisfied (is <tt>true</tt>), the 
	 * Claims, as inputs to the Rule, make such a Rule a <tt>xor</tt> 
	 * operation.</p>
	 */
	public static final int SIZE_WHEN_XOR = 2;
	
	/**
	 * <p>The {@link Puzzle.RegionSpecies types} of this Rule.</p>
	 * 
	 * <p>Initially, a Rule has exactly one type, but as the target 
	 * gets solved, eventually Rules must merge, moving the <tt>types</tt> 
	 * of Rules that are merged into other Rules (and are removed from the 
	 * target) into the <tt>types</tt> of the Rules into which 
	 * such mergers occur.</p>
	 * 
	 * <p>Merging <tt>types</tt> lists in this fashion represents the 
	 * merging of roles expressed in statements about a solution of a 
	 * sudoku target such as "The 5 in row 1 is the 5 in box 1".  One 
	 * of these Rule objects receives the <tt>types</tt> of the other 
	 * to mark that it is both "the 5 in row 1" and "the 5 in box 1".</p>
	 * 
	 * <p>Claims rely on the availability of <tt>types</tt> from their 
	 * Rule neighbors to determine whether they need to set themselves 
	 * false due to removal from a Rule or they've been removed from a 
	 * Rule that's actually being removed from the target as part of 
	 * redundancy-elimination in  which case the removed Claims don't 
	 * need to set themselves false.</p>
	 */
	private byte types;
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle and 
	 * having only the specified <tt>types</tt>.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 */
	public Rule(Puzzle puzzle, RegionSpecies type, IndexInstance dimA, IndexInstance dimB){
		super(puzzle);
		this.types = type.mask();
		this.hashCode = Claim.linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength());
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified <tt>types</tt>, and containing 
	 * all the elements of <tt>c</tt>.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param c a collection whose elements this Rule will also 
	 * have as elements
	 */
	public Rule(Puzzle puzzle, RegionSpecies type, Collection<Claim> c, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, c);
		this.types = type.mask();
		this.hashCode = Claim.linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength());
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified <tt>types</tt>, with the 
	 * specified initial capacity.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param initialCapacity the initial capacity of this Rule
	 */
	public Rule(Puzzle puzzle, RegionSpecies type, int initialCapacity, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, initialCapacity);
		this.types = type.mask();
		this.hashCode = Claim.linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength());
	}
	
	/**
	 * <p>Constructs a Rule belonging to the specified Puzzle, 
	 * having only the specified <tt>types</tt>, with the 
	 * specified initial capacity and load factor.</p>
	 * @param target the Puzzle to which this Rule belongs
	 * @param types the single initial {@link #getTypes() types} of 
	 * this Rule
	 * @param initialCapacity the initial capacity of this Rule
	 * @param loadFactor the load factor for this Rule
	 */
	public Rule(Puzzle puzzle, RegionSpecies type, int initialCapacity, float loadFactor, IndexInstance dimA, IndexInstance dimB) {
		super(puzzle, initialCapacity, loadFactor);
		this.types = type.mask();
		this.hashCode = Claim.linearizeCoords(type.ordinal(), dimA.intValue(), dimB.intValue(), puzzle.sideLength());
	}
	
	/**
	 * <p>Returns the byte describing the {@link Puzzle.RegionSpecies types} of 
	 * this Rule.</p>
	 * @return the byte describing the {@link Puzzle.RegionSpecies types} of 
	 * this Rule
	 */
	public byte getType(){
		return types;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("Rule: types:").append(typeString());
		for(Claim c : this){
			sb.append(System.getProperty("line.separator")).append("\t").append(c.toString());
		}
		
		return sb.toString();
	}
	
	/**
	 * <p>Returns a String representation of the {@link Puzzle.RegionSpecies types} of 
	 * this Rule.</p>
	 * @return a String representation of the {@link Puzzle.RegionSpecies types} of 
	 * this Rule
	 */
	String typeString(){
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		for(RegionSpecies reg : RegionSpecies.values()){
			if((types & reg.mask()) != 0){
				if(first){
					first = false;
				} else{
					sb.append(",");
				}
				sb.append(reg);
			}
		}
		
		return sb.toString();
	}
	
	/* *
	 * <p>Collapses this set down to be equal to <tt>src</tt>, 
	 * while ensuring that all released Claims are freed from 
	 * all other Rules.</p>
	 * 
	 * <p><tt>src</tt>'s <tt>types</tt> are moved from <tt>src</tt> 
	 * into <tt>this</tt> and <tt>src</tt> is 
	 * {@link Puzzle#removeNode(NodeSet) removed} from the target.</p>
	 * 
	 * @param src a Rule smaller than this Rule, which triggers 
	 * the merge event by being a proper subset of <tt>this</tt> and 
	 * which will be {@link Puzzle#removeNode(NodeSet) removed} from 
	 * the target
	 * 
	 * @return true if the target was changed by calling this 
	 * method, false otherwise
	 */
	/*boolean merge(SolutionEvent time, Rule src){
		boolean result = retainAll(time, src);
		
		this.types |= src.types;
		src.types = 0;
		
		src.clear(time);
		puzzle.removeNode(src);
		return result;
	}*/
	
	void mergeDetails(Fact src){
		if(src instanceof Rule){
			Rule r = (Rule) src;
			this.types |= r.types;
			r.types = 0;
		} else{
			
		}
	}
	
	/**
	 * <p>Detects the need for this Rule to collapse completely and 
	 * set its sole element true if it has only one element left or 
	 * the need for this Rule to collapse partially onto one of the 
	 * Rules it intersects if that rule is a subset of this one, or 
	 * throws an exception if this Rule is empty.</p>
	 * 
	 * @throws IllegalStateException if this Rule is empty
	 */
	@Override
	protected void validateFinalState(SolutionEvent time){
		if(size() == SIZE_WHEN_SOLVED){
			for(Claim c : this){
				time.push(new TimeTotalLocalization(time.top(), c.visibleClaims()));
				c.setTrue(time);
				time.pop();
			}
		} else if( shouldCheckForValueClaim() ){
			findAndAddressValueClaim(time);
		} else if( isEmpty() ){
			throw new IllegalStateException("A Rule is not allowed to be empty. this.toString(): "+toString());
		}
	}
	
	/**
	 * <p>A time node denoting the complete collapse of a Rule because 
	 * it has exactly one Claim and encapsulating subordinate events that 
	 * occur as {@link #verifyFinalState() an automatic result} of the 
	 * aforementioned collapse.</p>
	 * @author fiveham
	 *
	 */
	public class TimeTotalLocalization extends AutoResolve{
		private TimeTotalLocalization(Time parent, Collection<Claim> falseClaims){
			super(parent, falseClaims);
		}
	}
	
	/**
	 * <p>Finds value-claim collapse scenarios and resolves them 
	 * if found. These are scenarios where one Rule is a subset 
	 * of another.</p>
	 */
	private void findAndAddressValueClaim(SolutionEvent time){
		Set<Fact> possibleSupersets = new HashSet<>();
		for(Claim c : this){
			possibleSupersets.addAll(c);
		}
		possibleSupersets.remove(this);
		
		for(Fact possibleSuperset : possibleSupersets){
			if(possibleSuperset.hasProperSubset(this)){
				time.push(new TimeValueClaim(time.top(), 
						possibleSuperset.stream()
						.filter((e)->!contains(e))
						.collect(Collectors.toSet())));
				
				possibleSuperset.merge(time, this);
				
				time.pop();
				return;
			}
		}
	}
	
	/**
	 * <p>A time node denoting a value-claim event where one Rule 
	 * is a subset of another and the superset collapses onto the 
	 * subset.</p>
	 * @author fiveham
	 *
	 */
	public class TimeValueClaim extends AutoResolve{
		private TimeValueClaim(Time parent, Collection<Claim> falseClaims){
			super(parent, falseClaims);
		}
	}
	
	private final int hashCode;
	
	@Override
	public int hashCode(){
		return hashCode;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Rule){
			Rule r = (Rule) o;
			return r.puzzle.equals(puzzle) && r.types == types/* && super.equals(r)*/;
		}
		return false;
	}
	
	/**
	 * <p>Returns true if this Rule is possibly contained in another 
	 * Rule as a subset of the other bag in a manner compatible with 
	 * the deprecated solution technique known as ValueClaim.</p>
	 * @return true if this Rule is not a {@link Puzzle.RegionSpecies#CELL cell Rule} 
	 * and has <tt>size() <= target.magnitude()</tt>
	 */
	private boolean shouldCheckForValueClaim(){
		return (types & RegionSpecies.CELL.mask()) != 0
				? false 
				: size() <= puzzle.magnitude();
	}
	
	/**
	 * <p>A time node denoting an {@link #verifyFinalState automatic collapse} 
	 * and encapsulating subordinate automatic collapses.</p>
	 * @author fiveham
	 *
	 */
	public class AutoResolve extends FalsifiedTime{
		private AutoResolve(Time parent, Collection<Claim> falseClaims){
			super(parent);
			falsified().addAll(falseClaims);
		}
	}
}
