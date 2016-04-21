package sudoku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import common.ComboGen;
import common.ComboGenIso;
import common.graph.BasicGraph;
import common.graph.Graph;
import common.graph.Wrap;
import common.Pair;
import java.util.Iterator;
import common.NCuboid;
import java.util.stream.Collectors;

/**
 * Defines a process for finding false claims in a sudoku target.  
 * @author fiveham
 *
 */
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
	 * DONE The "N sources and N recipients" model doesn't directly (efficiently) account 
	 * for CellDeath and OrganFailure, as it requires several separate one-to-one analyses 
	 * comparing a single source bag to a single recipient bag at a time. This requires three 
	 * separate analysis steps to perform what the original SledgeHammer techinque could do 
	 * in a single step.
	 * 
	 * DONE single-bag-originated sledgehammer cases are now accounted for by the bag detecting 
	 * its own need to initiate a collapse. See Rule#verifyFinalState()
	 * 
	 * Personal note: The thing I liked the least about the Table-based solution perspective 
	 * was the fact that it takes multiple passes to resolve a CellDeath or an OrganFailure. 
	 * Ironically, by trying to incorporate all of the disjoint-bag cases covered by table-
	 * based analysis into the sledgehammer, I have also brought that weakness of table-
	 * based analysis into the sledgehammer.
	 * 
	 * 
	 */
	
	private final int MAX_SRC_COMBO_SIZE = puzzle.sideLength()-1; //XXX Find out what the actual largest possible source-combo size is.
	private final int MIN_SRC_COMBO_SIZE = 2;
	
	public SledgeHammer2(Puzzle puzzle) {
		super(puzzle);
	}
	
	@Override
	protected boolean process() {
		boolean puzzleHasUpdated = false;
		
		for(List<FactBag> srcCombo : new ComboGen<>(puzzle.factBagsWhere((fb)->fb.size()>1), MIN_SRC_COMBO_SIZE, MAX_SRC_COMBO_SIZE)){
			ToolSet<Claim> srcUnion = new ToolSet<>();
			int cumulativeSize = unionAllNullIfNonDisjoint(srcUnion, srcCombo);
			
			if( cumulativeSize == srcUnion.size() ){
				Set<FactBag> nearbyBags = bagsIntersecting(srcUnion, srcCombo);
				
				for(List<FactBag> recipCombo : new ComboGen<>(nearbyBags, srcCombo.size(), srcCombo.size())){
					ToolSet<Claim> recipUnion = unionAll(recipCombo);
					
					if(recipUnion.hasProperSubset(srcUnion) 
							&& eachElementIntersectsAtLeastTwoElementsFromOtherCombo(srcCombo, recipCombo)){
						puzzleHasUpdated |= resolve(recipUnion.complement(srcUnion));
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	/**
	 * Get a 
	 * @return
	 */
	protected boolean process2(){
		boolean puzzleHasUpdated = false;
		
		//create a BasicGraph of the target
		Graph<Wrap<FactBag>> pGraph = new BasicGraph<>(Wrap.wrap(puzzle.getFactbags(), (f1,f2)->f1.intersects(f2)));
		Collection<Graph<Wrap<FactBag>>> pCCs = pGraph.connectedComponents();
		
		for(Graph<Wrap<FactBag>> bagNetwork : pCCs){
			
			List<FactBag> noDupes = noDuplicates(bagNetwork.nodeStream().map((wfb)->wfb.wrapped()).collect(Collectors.toList()));//Also, check for dupes every time you need to use some collection of FBs
			ComboGenIso<FactBag> reds = new ComboGenIso<>( noDupes, 1, noDupes.size()/2);
			for(Iterator<List<FactBag>> redIter=reds.iterator(); redIter.hasNext();){
				List<FactBag> srcCombo = redIter.next();
				
				ToolSet<Claim> srcUnion = sideEffectUnion(srcCombo,true);
				if( srcUnion != null ){
					Set<FactBag> nearbyBags = possibleGreenBags(srcUnion, srcCombo, noDupes=noDuplicates(noDupes));
					
					ComboGenIso<FactBag> greens = new ComboGenIso<>(nearbyBags, srcCombo.size(), nearbyBags.size(), ComboGenIso.Direction.DECREASE);
					for(Iterator<List<FactBag>> greenIter = greens.iterator(); greenIter.hasNext();){ //implicitly accounts for greenbags.size() >= redbags.size()
						List<FactBag> recipCombo = greenIter.next();
						
						ToolSet<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipCombo, srcUnion);
						if( claimsToSetFalse != null && resolve(claimsToSetFalse)){
							redIter.remove();
							puzzleHasUpdated = true;
						}
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}

	protected boolean process3(){
		boolean puzzleHasUpdated = false;
		
		for(Graph<Wrap<FactBag>> bagNetwork : new BasicGraph<>(Wrap.wrap(puzzle.getFactbags(), (f1,f2)->f1.intersects(f2))).connectedComponents()){
			
			List<FactBag> noDupes = noDuplicates(bagNetwork.nodeStream().map((wfb)->wfb.wrapped()).collect(Collectors.toList())); //Also, check for dupes every time you need to use some collection of FBs
			ComboGenIso<FactBag> reds = new ComboGenIso<>( noDupes, 1, noDupes.size()/2);
			for(Iterator<List<FactBag>> redIter=reds.iterator(); redIter.hasNext();){
				List<FactBag> srcCombo = redIter.next();
				
				ToolSet<Claim> srcUnion = sideEffectUnion(srcCombo,true);
				if( srcUnion != null ){
					Set<FactBag> nearbyBags = possibleGreenBags(srcUnion, srcCombo, noDupes=noDuplicates(noDupes));
					
					//ComboGenIso<Rule> greens = new ComboGenIso<>(nearbyBags, srcCombo.size(), nearbyBags.size(), ComboGenIso.Direction.DECREASE);
					ComboGenIso<FactBag> greens = new ComboGenIso<>(nearbyBags, srcCombo.size(), srcCombo.size());
					for(Iterator<List<FactBag>> greenIter = greens.iterator(); greenIter.hasNext();){ //implicitly accounts for greenbags.size() >= redbags.size()
						List<FactBag> recipCombo = greenIter.next();
						
						ToolSet<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipCombo, srcUnion);
						if( claimsToSetFalse != null && resolve(claimsToSetFalse)){
							redIter.remove();
							puzzleHasUpdated = true;
						}
					}
				}
			}
		}
		
		return puzzleHasUpdated;
	}
	
	/**
	 * <p>Returns true if the specified collection of "green" recipient FactBags and 
	 * the specified collection of "red" source FactBags together constitute a 
	 * valid Sledgehammer solution scenario, false otherwise. 
	 * They are a valid Sledgehammer solution scenario if <ul>
	 * <li>The union of all the green FactBags contains as a subset (or 
	 * as a proper subset in order for resolution to make any difference) the 
	 * union of all the red FactBags.</li>
	 * <li>Each red Rule intersects at least a certain number of the green FactBags.</li>
	 * <li>Each green Rule intersects at least a certain number of the red FactBags.</li>
	 * </ul></p>
	 */
	private static ToolSet<Claim> sledgehammerValidityCheck(List<FactBag> reds, List<FactBag> greens, ToolSet<Claim> srcUnion){
		ToolSet<Claim> recipUnion = sideEffectUnion(greens,false);
		if(!recipUnion.hasSubset(srcUnion)){
			return null;
		}
		
		recipUnion.removeAll(srcUnion);
		for(List<Claim> solutionState : new NCuboid<Claim>(reds)){
			if(isPossibleSolution(solutionState)){
				for(Claim c : recipUnion){
					if( isPossibleClaim(c,solutionState) ){
						return null;
					}
				}
			}
		}
		return recipUnion;
	}
	
	/**
	 * Returns false if the specified solution-state is impossible, true 
	 * otherwise. A solution-state is impossible if any two specified Claims 
	 * share at least one {@link Claim#getOwners() owner}.
	 */
	private static boolean isPossibleSolution(List<Claim> solutionState){
		List<List<FactBag>> ownerList = new ArrayList<>(solutionState.size());
		
		for(Claim c : solutionState){
			ownerList.add(c.getOwners());
		}
		
		return sideEffectUnion(ownerList,true) != null;
	}
	
	/**
	 * Returns true if <tt>c</tt> can still be true given that all the 
	 * Claims in <tt>givens</tt> are known true, false otherwise.
	 */
	private static boolean isPossibleClaim(Claim c, List<Claim> givens){
		for(Claim given : givens){
			if(c.sharesBagWith(given)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * returns a set of factbags from noDupes and not found in srcCombo, each of which 
	 * intersects at least a certain number of the factbags from srcCombo and such that 
	 * each member of srcCombo intersects at least of certain number of the factbags in 
	 * the returned set.  The certain number of intersections is usually 2 but is 1 when 
	 * srcCombo.size()==1.
	 * @param srcUnion
	 * @param srcCombo
	 * @param noDupes
	 * @return
	 */
	private Set<FactBag> possibleGreenBags(ToolSet<Claim> srcUnion, List<FactBag> srcCombo, List<FactBag> noDupes){
		Set<FactBag> neighb = bagsIntersecting(srcUnion, srcCombo);
		neighb.retainAll(noDupes);
		return neighb;
	}
	
	/**
	 * Unions all the FactBags in srcCombo into one set and returns 
	 * that set, unless some Claims are shared among the FactBags in 
	 * srcCombo, in which case null is returned instead.
	 * @return a set containing all the Claims in all the FactBags that 
	 * are contained as elements of srcCombo or null if any of those 
	 * Claims are shared among more than one of the FactBags in srcCombo.
	 */
	private static <T> ToolSet<T> sideEffectUnion(List<? extends Collection<T>> srcCombo, boolean nullIfNotDisjoint){
		ToolSet<T> result = new ToolSet<>();
		
		int cumulativeSize = 0;
		for(Collection<T> redBag : srcCombo){
			result.addAll(redBag);
			cumulativeSize += redBag.size();
		}
		
		return !nullIfNotDisjoint || cumulativeSize==result.size() ? result : null;
	}
	
	/**
	 * Returns a collection of Rule such that no element is equal (when interpreted as 
	 * an ordinary Set) to any other.  FactBags that are set-equal are not necessarily 
	 * equal as FactBags, since their object-sameness as FactBags is dependent on their 
	 * RegionSpecies, initial bounding box, or other factors as well as Set contents.
	 * @param c
	 * @return
	 */
	private static List<FactBag> noDuplicates(Iterable<FactBag> c){
		List<Pair<FactBag,HashSet<Claim>>> shit = new ArrayList<>();
		List<FactBag> result = new ArrayList<>();
		for(FactBag bag : c){
			HashSet<Claim> h = new HashSet<>(bag);
			if(!contain(shit, h)){
				shit.add( new Pair<>(bag, h) );
				result.add(bag);
			}
		}
		
		return result;
	}
	
	private static boolean contain(List<Pair<FactBag,HashSet<Claim>>> shit, HashSet<Claim> h){
		for(Pair<FactBag,HashSet<Claim>> thing : shit){
			if(thing.getB().equals(h) ){
				return true;
			}
		}
		return false;
	}
	
	private boolean eachElementIntersectsAtLeastTwoElementsFromOtherCombo(List<FactBag> src, List<FactBag> recip){
		
		int[] srcValues = new int[src.size()];
		Arrays.fill(srcValues, 0);
		int[] recipValues = new int[recip.size()];
		Arrays.fill(recipValues, 0);
		
		for(int s=0; s<src.size(); ++s){
			for(int r=0; r<recip.size(); ++r){
				if(src.get(s).intersects(recip.get(r))){
					++srcValues[s];
					++recipValues[r];
				}
			}
		}
		
		for(int[] array : new int[][]{srcValues, recipValues}){
			for(int v : array){
				if(v<2){ //XXX magic no
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean resolve(Collection<Claim> claimsToSetFalse){
		boolean result = false;
		for(Claim c : claimsToSetFalse){
			result |= c.setFalse();
		}
		if(result){
			puzzle.addSolveEvent(claimsToSetFalse);
		}
		return result;
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
	
	private static int unionAllNullIfNonDisjoint(ToolSet<Claim> outputSet, List<FactBag> bags){
		int cumulativeSize = 0;
		for(FactBag bag : bags){
			outputSet.addAll(bag);
			cumulativeSize += bag.size();
			if(cumulativeSize!=outputSet.size()){
				return -1;
			}
		}
		return cumulativeSize/* == result.size() ? result : UNION_OF_NON_DISJOINT_SETS*/;
	}
}
