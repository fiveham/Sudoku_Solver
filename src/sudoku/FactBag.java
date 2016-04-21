package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class FactBag extends ToolSet<Claim>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2191355198146127036L;
	public static final int SIZE_WHEN_SOLVED = 1;
	public static final int SIZE_WHEN_XOR = 2;
	
	private final Puzzle puzzle;
	private final Puzzle.RegionSpecies type;
	//private final Set<Rule> is = new HashSet<>(Claim.INIT_OWNER_COUNT-1);
	
	private BoundingBox boundingBox;
	
	public FactBag(Puzzle puzzle, Puzzle.RegionSpecies type, BoundingBox bb){
		super();
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.RegionSpecies type, Collection<Claim> c, BoundingBox bb) {
		super(c);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.RegionSpecies type, int initialCapacity, BoundingBox bb) {
		super(initialCapacity);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	public FactBag(Puzzle puzzle, Puzzle.RegionSpecies type, int initialCapacity, float loadFactor, BoundingBox bb) {
		super(initialCapacity, loadFactor);
		this.puzzle = puzzle;
		this.type = type;
		this.boundingBox = bb;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("Rule: type:").append(type).append("\t").append(boundingBox.toString());
		for(Claim c : this){
			sb.append(System.getProperty("line.separator")).append("\t").append(c.toString());
		}
		
		return sb.toString();
	}
	
	public Puzzle.IndexValue xMin(){
		return boundingBox.xMin;
	}
	
	public Puzzle.IndexValue xMax(){
		return boundingBox.xMax;
	}
	
	public Puzzle.IndexValue yMin(){
		return boundingBox.yMin;
	}
	
	public Puzzle.IndexValue yMax(){
		return boundingBox.yMax;
	}
	
	public Puzzle.IndexValue zMin(){
		return boundingBox.zMin;
	}
	
	public Puzzle.IndexValue zMax(){
		return boundingBox.zMax;
	}
	
	public BoundingBox getBoundingBox(){
		return boundingBox;
	}
	
	Puzzle.RegionSpecies getType(){
		return type;
	}
	
	public boolean intersects(FactBag fb){
		FactBag small=fb;
		FactBag large=this;
		if(size() < fb.size()){
			small = this;
			large = fb;
		}
		for(Claim c : small){
			if(c.getOwners().contains(large)){
				return true;
			}
		}
		return false;
	}
	
	boolean setAsSoleElement_ONLY_Claim_MAY_CALL_THIS_METHOD(Claim soleClaim){
		boolean result = false;
		
		for(Claim c : new ArrayList<>(this)){
			if( !c.equals(soleClaim) ){
				result |= c.setFalse();
			}
		}
		
		if(result){
			validateFinalState();
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
			result |= remove_internal(c);
		}
		return result;
	}
	
	@Override
	public boolean retainAll(Collection<?> c){
		boolean result = false;
		Iterator<Claim> e = iterator();
		for(Claim claim; e.hasNext();) {
			if (!c.contains(claim=e.next())) {
				result |= remove_internal(claim);
			}
		}
		
		if(result){
			validateFinalState();
		}		
		
		return result;
	}
	
	@Override
	public boolean add(Claim c){
		c.addOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(this);
		return super.add(c);
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
		boolean result = remove_internal(o);
		
		if(result){
			validateFinalState();
		}
		
		return result;
	}
	
	private boolean remove_internal(Object o){
		boolean result = super.remove(o);
		
		if(result){
			Claim c = (Claim) o;
			c.removeOwner_ONLY_FactBag_MAY_CALL_THIS_METHOD(this);
		}
		
		return result;
	}
	
	/**
	 * Overrides the HashSet implementation, which uses iterator().remove(), 
	 * which would bypass the safe-removal protocols required for proper 
	 * operation of the a Puzzle.
	 * @param c
	 * @return
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		Claim claim;
		if(size() > c.size()) {
			for (Iterator<?> i = c.iterator(); i.hasNext(); ){
				modified |= remove_internal(i.next());
			}
		} else for(Iterator<Claim> i = iterator(); i.hasNext(); ) {
			if (c.contains(claim=i.next())) {
				modified |= remove_internal(claim);
			}
		}
		
		if(modified){
			validateFinalState();
		}
		
		return modified;
	}
	/*@Override
	public boolean removeAll(Collection<?> c){
		boolean result = false;
		for(Object o : c){
			result |= remove(o);
		}
		return result;
	}*/
	
	private void validateFinalState(){
		if( size() == SIZE_WHEN_SOLVED ){
			puzzle.registerResolvable(new TotalLocalization(this));
		} else if( shouldCheckForValueClaim() ){
			findAndAddressValueClaim();
		} else if( isEmpty() ){
			throw new IllegalStateException("A Rule is not allowed to be empty. this.toString(): "+toString());
		}
	}
	
	private void findAndAddressValueClaim(){
		Set<FactBag> possibleSupersets = new HashSet<>();
		
		for(Claim c : this){
			possibleSupersets.addAll(c.getOwners());
		}
		possibleSupersets.remove(this);
		
		for(FactBag possibleSuperset : possibleSupersets){
			if(possibleSuperset.hasProperSubset(this)){
				puzzle.registerResolvable(new ValueClaim(this, possibleSuperset));
				return;
			}
		}
	}
	
	@Override
	public int hashCode(){
		return boundingBox.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof FactBag){
			FactBag f = (FactBag) o;
			return f.puzzle.equals(puzzle) && f.type.equals(type) && f.boundingBox.equals(boundingBox) && super.equals(f);
		}
		return false;
	}
	
	/**
	 * Returns true if this Rule is possibly contained in another 
	 * Rule as a subset of the other bag in a manner compatible with 
	 * the deprecated solution technique known as ValueClaim.
	 * @return
	 */
	private boolean shouldCheckForValueClaim(){
		return type == Puzzle.RegionSpecies.CELL ? false : size() <= puzzle.magnitude();
	}
	
	public boolean equalsAsSet(FactBag fb){
		return super.equals(fb);
	}
	
	@Override
	public void clear(){
		throw new UnsupportedOperationException();
	}
	
	public static class BoundingBox{
		
		private final Puzzle.IndexValue xMin, xMax, yMin, yMax, zMin, zMax;
		
		public BoundingBox(Puzzle.IndexValue xMin, Puzzle.IndexValue xMax, Puzzle.IndexValue yMin, Puzzle.IndexValue yMax, Puzzle.IndexValue zMin, Puzzle.IndexValue zMax){
			this.xMin = xMin;
			this.xMax = xMax;
			this.yMin = yMin;
			this.yMax = yMax;
			this.zMin = zMin;
			this.zMax = zMax;
		}
		
		private static final List<Function<BoundingBox,Puzzle.IndexValue>> funcs = new ArrayList<>();
		static{
			funcs.add((bb)->bb.xMin);
			funcs.add((bb)->bb.yMin);
			funcs.add((bb)->bb.zMin);
			funcs.add((bb)->bb.xMax);
			funcs.add((bb)->bb.yMax);
			funcs.add((bb)->bb.zMax);
		}
		
		@Override
		public int hashCode(){
			int s = xMin.getPuzzle().sideLength();
			
			int a = Claim.linearizeCoords(xMin.intValue(), yMin.intValue(), zMin.intValue(), s);
			int b = Claim.linearizeCoords(xMax.intValue(), yMax.intValue(), zMax.intValue(), s);
			
			return a*s*s*s + b;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof BoundingBox){
				BoundingBox b = (BoundingBox) o;
				
				boolean result = true;
				for(Function<BoundingBox,Puzzle.IndexValue> func : funcs){
					result = result && func.apply(this)==func.apply(b);
				}
				return result;
			}
			return false;
		}
		
		@Override
		public String toString(){
			return "BoundingBox from "+xMin.intValue()+","+yMin.intValue()+","+zMin.intValue()+" to "+xMax.intValue()+","+yMax.intValue()+","+zMax.intValue();
		}
	}
	
	@Override
	public Iterator<Claim> iterator(){
		return new SafeRemovingIterator();
	}
	
	private class SafeRemovingIterator implements Iterator<Claim>{
		private Iterator<Claim> wrappee = FactBag.super.iterator();
		@Override
		public void remove(){
			throw new UnsupportedOperationException("Must use Rule.remove(Object) to remove an item from a Rule.");
		}
		@Override
		public Claim next(){
			return wrappee.next();
		}
		@Override
		public boolean hasNext(){
			return wrappee.hasNext();
		}
	}
}
