package sudoku;

import common.Pair;

public class SledgeHammer extends Technique {
	
	/*
	 * Can generalize this for cases where multiple small sets overlap 
	 * multiple larger sets.  To do so, account for any number of 
	 * smaller sets (instead of only accounting for cases with just one 
	 * event-triggering smaller set) and account similarly for the 
	 * possibility of multiple larger sets to be reduced.  The essential 
	 * test remains mostly the same: union your candidate originating 
	 * sets, union your candidate receiving sets, and then perform the 
	 * extant test using those unioned originating and receiving sets 
	 * instead of using the naked sets directly.
	 */
	
	public SledgeHammer(Puzzle puzzle) {
		super(puzzle);
	}
	
	@Override
	public boolean process() {
		boolean puzzleHasUpdated = false;
		
		//for every factbag in the target,
		//    for every other fact bag that intersects one of those
		//    (or more philosophically, for every factbag-intersection in the target)
		//        if A is a proper subset of B
		//            collapse B down to A (B.collapseTo(A))
		//XXX this could be retrofitted to work as generalized Sledgehammer by (after incorporating the union-all step) 
		//something like iterating over all the even-sized combinations of factbags in the target, and dividing each into 
		//a source half and a recip half right down the middle.  Nah, that's wrong.
		
		for(Pair<FactBag,FactBag> intersection : puzzle.getFactBagIntersections()){
			FactBag a = intersection.getA();
			FactBag b = intersection.getB();
			
			if(a.hasProperSubset(b)){
				puzzleHasUpdated |= a.collapseTo(b);
			} else if(b.hasProperSubset(a)){
				puzzleHasUpdated |= b.collapseTo(a);
			}
		}
		
		return puzzleHasUpdated;
	}
	
}
