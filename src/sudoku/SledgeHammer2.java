package sudoku;

import common.ComboGen;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SledgeHammer2 extends Technique {
	
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
	
	/*
	 * This is a generalization of the class currently named SledgeHammer.
	 * In this class, generalization from a single source factbag with a single 
	 * recipient factbag will occur enabling use of a set of source factbags 
	 * and a set of recipient factbags.
	 */
	
	/*
	 * TODO The "N sources and N recipients" model doesn't directly (efficiently) account 
	 * for CellDeath and OrganFailure, as it requires several separate one-to-one analyses 
	 * comparing a single source bag to a single recipient bag at a time. This requires three 
	 * separate analysis steps to perform what the original SledgeHammer techinque could do 
	 * in a single step.
	 * 
	 * Personal note: The thing I liked the least about the Table-based solution perspective 
	 * was the fact that it takes multiple passes to resolve a CellDeath or an OrganFailure. 
	 * Ironically, by trying to incorporate all of the disjoint-bag cases covered by table-
	 * based analysis into the sledgehammer, I have also brought that weakness of table-
	 * based analysis into the sledgehammer.
	 * 
	 * 
	 */
	
	public SledgeHammer2(Puzzle puzzle) {
		super(puzzle);
	}
	
	private final int MAX_SRC_COMBO_SIZE = 8; //XXX Find out what the actual largest possible source-combo size is.
	private final int MIN_SRC_COMBO_SIZE = 1;
	
	@Override
	protected boolean process() {
		boolean puzzleHasUpdated = false;
		
		for(List<FactBag> srcCombo : new ComboGen<>(puzzle.getFactbags(), MIN_SRC_COMBO_SIZE, MAX_SRC_COMBO_SIZE)){
			ToolSet<Claim> srcUnion = unionAllNullIfNonDisjoint(srcCombo);
			
			if( srcUnion != UNION_OF_NON_DISJOINT_SETS ){
				Set<FactBag> nearbyBags = bagsIntersecting(srcUnion, srcCombo);
				
				for(List<FactBag> recipCombo : new ComboGen<>(nearbyBags, srcCombo.size(), srcCombo.size())){
					ToolSet<Claim> recipUnion = unionAll(recipCombo);
					
					if(recipUnion.hasProperSubset(srcUnion)){
						List<Claim> claimsToSetFalse = recipUnion.complement(srcUnion);
						
						for(Claim c : claimsToSetFalse){
							puzzleHasUpdated |= c.setFalse();
						}
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	private Set<FactBag> bagsIntersecting(Set<Claim> union, List<FactBag> sources){
		Set<FactBag> result = new HashSet<>();
		
		for(Claim c : union){
			result.addAll(c.getOwners());
		}
		result.removeAll(sources);
		
		return result;
	}
	
	static ToolSet<Claim> unionAll(Collection<FactBag> bags){
		ToolSet<Claim> result = new ToolSet<>();
		
		for(FactBag bag : bags){
			result.addAll(bag);
		}
		
		return result;
	}
	
	private static final ToolSet<Claim> UNION_OF_NON_DISJOINT_SETS = null;
	
	/**
	 * Returns a set containing the union of all the sets (factbags) 
	 * included in <tt>bags</tt> if and only if the sets in <tt>bags</tt> 
	 * are disjoint from one another (none of the sets shares any elements 
	 * with any of the other sets). If any element is included in more 
	 * than one set, then <tt>BAD_THING</tt> is returned.
	 * @param bags
	 * @return
	 */
	private static ToolSet<Claim> unionAllNullIfNonDisjoint(List<FactBag> bags){
		ToolSet<Claim> result = new ToolSet<>();
		
		int cumulativeSize = 0;
		for(FactBag bag : bags){
			result.addAll(bag);
			cumulativeSize += bag.size();
		}
		
		return cumulativeSize == result.size() ? result : UNION_OF_NON_DISJOINT_SETS;
	}
	
}
