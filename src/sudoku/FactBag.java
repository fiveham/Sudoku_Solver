package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class FactBag extends ToolSet<Claim> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2191355198146127036L;
	public static final int SIZE_WHEN_SOLVED = 1;
	public static final int SIZE_WHEN_XOR = 2;
	
	private Puzzle puzzle;
	private Puzzle.Region type;
	
	private BoundingBox boundingBox;
	
	public FactBag(Puzzle puzzle, Puzzle.Region type, BoundingBox bb){
		super();
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.Region type, Collection<Claim> c, BoundingBox bb) {
		super(c);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.Region type, int initialCapacity, BoundingBox bb) {
		super(initialCapacity);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.Region type, int initialCapacity, float loadFactor, BoundingBox bb) {
		super(initialCapacity, loadFactor);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public Index xMin(){
		return boundingBox.xMin;
	}
	
	public Index xMax(){
		return boundingBox.xMax;
	}
	
	public Index yMin(){
		return boundingBox.yMin;
	}
	
	public Index yMax(){
		return boundingBox.yMax;
	}
	
	public Symbol zMin(){
		return boundingBox.zMin;
	}
	
	public Symbol zMax(){
		return boundingBox.zMax;
	}
	
	Puzzle.Region getType(){
		return type;
	}
	
	boolean setAsSoleElement_ONLY_CLAIM_MAY_CALL_THIS_METHOD(Claim soleClaim){
		boolean result = false;
		
		for(Claim c : new ArrayList<>(this)){
			if( !c.equals(soleClaim) ){
				result |= c.setFalse();
			}
		}
		
		return result;
	}
	
	/**
	 * collapses this set down to be equal to fb, 
	 * while ensuring that all released Claims are 
	 * freed from all other factbags.
	 */
	public boolean collapseTo(Set<Claim> fb){
		boolean result = false;
		for(Claim c : complement(fb)){
			result |= remove(c);
		}
		return result;
	}
	
	@Override
	public boolean add(Claim c){
		c.addOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(this);
		return super.add(c);
	}
	
	@Override
	public boolean addAll(Collection<? extends Claim> c){
		boolean result = false;
		for(Claim claim : c){
			result |= add(claim);
		}
		return result;
	}
	
	/**
	 * Removes <tt>o</tt> from this set. If <tt>o</tt> was actually present 
	 * in this set before its removal, then <tt>this</tt> is removed from 
	 * <tt>o</tt>'s list of owners.
	 * @param o the item to be removed from this set
	 * @return true if this set was changed as a result of this operation, false 
	 * otherwise
	 */
	@Override
	public boolean remove(Object o){
		boolean result = super.remove(o);
		
		if( isEmpty() ){
			throw new IllegalStateException("Empty Rule");
		}
		
		if(result){
			Claim c = (Claim) o;
			c.removeOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(this);
		}
		
		if( size() == SIZE_WHEN_SOLVED ){
			puzzle.registerResolvable(new Resolvable(this));
		}
		
		return result;
	}
	
	@Override
	public boolean removeAll(Collection<?> c){
		boolean result = false;
		for(Object o : c){
			result |= remove(o);
		}
		return result;
	}
	
	@Override
	public void clear(){
		throw new UnsupportedOperationException();
	}
	
	public static class BoundingBox{
		private final Index xMin, xMax, yMin, yMax;
		private final Symbol zMin, zMax;
		public BoundingBox(Index xMin, Index xMax, Index yMin, Index yMax, Symbol zMin, Symbol zMax){
			this.xMin = xMin;
			this.xMax = xMax;
			this.yMin = yMin;
			this.yMax = yMax;
			this.zMin = zMin;
			this.zMax = zMax;
		}
	}
}
