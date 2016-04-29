package sudoku;

import common.ComboGen;
import common.NCuboid;
import common.Pair;
import common.TestIterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.BiPredicate;

/**
 * <p>The sledgehammer technique for solving sudoku puzzles is defined 
 * at http://onigame.livejournal.com/18580.html and at 
 * http://onigame.livejournal.com/20626.html and uses one collection 
 * of specific statements of the rules of a sudoku target against 
 * another collection of such statements to determine that certain 
 * claims about the sudoku target are false.</p>
 * 
 * <p>The sledgehammer technique relies on the fact that the target can 
 * be interpreted as a collection of {@link Claim truth-claims} about the values of the 
 * cells ("Cell x,y, has value z.") and {@link Rule groupings of those truth-claims} of 
 * which exactly one claim from a grouping is true and all the rest are 
 * false.</p>
 * 
 * <p>These groupings of claims are pedantic and precise statements 
 * of the rules that specify what makes a solution to a sudoku target valid. 
 * For example, the rule that any row contains each value exactly once 
 * pedantically expands into 81 statements in a 9x9 target: 9 statements 
 * about an individual row, each of which is actually 9 statements 
 * specifying a particular value: "Row y has value z in exactly one cell." 
 * The rule that any cell contains exactly one value similarly becomes 81 
 * pedantic statements: "For cell x,y, only one z value is correct."</p>
 * 
 * <p>The sledgehammer technique generalizes a great number of analysis 
 * techniques including
 * <ul><li>cell death</li>
 * <li>organ failure</li>
 * <li>naked pairs/triples/etc.</li>
 * <li>x-wing, swordfish, jellyfish, etc.</li>
 * <li>xy-wing, xyz-wing, etc.</li>
 * </ul></p>
 * 
 * <p>This is effected by modeling those situation as the collapse of several 
 * recipient rules onto several source rules so that all the claims belonging 
 * to the recipient rules but not belonging to any of the source rules are 
 * known to be false. No matter which (viable) set of claims among the source 
 * rules is the source rules' set of true claims, all the recipient rules' 
 * claims that aren't also source claims must be false.</p>
 * 
 * @author fiveham
 *
 */
public class Sledgehammer extends Technique {
	
	/**
	 * <p>The minimum number ({@value}) of Rules in a source combo relevant to Sledgehammer analysis. 
	 * Because Rules {@link Rule#verifyFinalState() automatically detect} when they need to 
	 * collapse and also automatically perform the resulting collapse, single-source 
	 * Sledgehammers won't be available in the target. This leaves two-Rule sources as 
	 * the smallest source combinations.</p>
	 */
	private static final int MIN_SRC_COMBO_SIZE = 2;
	
	/**
	 * <p>Constructs a SledgeHammer2 that works to solve the specified Puzzle.</p>
	 * @param target the Puzzle that this SledgeHammer2 tries to solve.
	 */
	public Sledgehammer(Sudoku puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Iterates over all the possible pairs of source-combination and 
	 * recipient-combination, checks each pair for validity as a sledgehammer 
	 * solution scenario, and, if the scenario is valide, resolves it and then 
	 * returns a SolutionEvent detailing the event and any subsequent automatic 
	 * resolution events.</p>
	 * @return a SolutionEvent detailing the sledgehammer solution event that 
	 * this method found and resolved and detailing any subsequent automatic 
	 * resolution events as {@link common.time.Time#children() children} of 
	 * the sledgehammer event, or null if this method made no changes to its 
	 * <tt>target</tt>
	 */
	@Override
	protected SolutionEvent process(){
		
		//For each disjoint source combo
		Set<Rule> rules = target.factStream().filter((ns)->ns instanceof Rule).map((ns)->(Rule)ns).collect(Collectors.toSet());
		ComboGen<Rule> reds = new ComboGen<>(rules, MIN_SRC_COMBO_SIZE, rules.size()/2);
		for(Pair<List<Rule>,ToolSet<Claim>> pair : new UnionIterable(reds)){
			List<Rule> srcCombo = pair.getA();
			ToolSet<Claim> srcUnion = pair.getB();
			
			//For each conceivable recipient combo
			Set<Fact> nearbyRules = rulesIntersecting(srcUnion, srcCombo);
			for(List<Fact> recipientCombo : new ComboGen<>(nearbyRules, srcCombo.size(), srcCombo.size())){
				
				//If the source and recipient combos make a valid sledgehammer scenario
				Set<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipientCombo, srcUnion); 
				if(claimsToSetFalse != null && !claimsToSetFalse.isEmpty()){
					return resolve(claimsToSetFalse);
				}
			}
		}
		
		return null;
	}
	
	private class UnionIterable implements Iterable<Pair<List<Rule>,ToolSet<Claim>>>{
		
		Iterable<List<Rule>> wrappedIterable;
		
		UnionIterable(Iterable<List<Rule>> iterable){
			wrappedIterable = iterable;
		}
		
		@Override
		public Iterator<Pair<List<Rule>,ToolSet<Claim>>> iterator(){
			class UnionIterator implements Iterator<Pair<List<Rule>,ToolSet<Claim>>>{
				
				ToolSet<Claim> union;
				Iterator<List<Rule>> wrappedIterator;
				
				/**
				 * <p>Constructs a UnionIterator wrapping <tt>wrappedIterator</tt>.</p>
				 * @param wrappedIterator an Iterator whose outputs are filtered by 
				 * this UnionIterator
				 */
				UnionIterator(Iterable<List<Rule>> iterable){
					TestIterator<List<Rule>> wrappedIterator = new TestIterator<>(iterable.iterator());
					wrappedIterator.addTest((ruleList)->setUnion(sideEffectUnion(ruleList,true))!=null);
					this.wrappedIterator = wrappedIterator;;
					this.union = new ToolSet<>(0);
				}
				
				@Override
				public boolean hasNext(){
					return wrappedIterator.hasNext();
				}
				
				@Override
				public Pair<List<Rule>,ToolSet<Claim>> next(){
					return new Pair<>(wrappedIterator.next(),this.union);
				}
				
				/**
				 * <p>Sets this UnionIterator's <tt>union</tt></p>
				 * @param union the new <tt>union</tt> of this UnionIterator
				 * @return the parameter <tt>union</tt>
				 */
				ToolSet<Claim> setUnion(ToolSet<Claim> union){
					return this.union = union;
				}
			}
			return new UnionIterator(wrappedIterable);
		}
	}
	
	/**
	 * <p>Outputs <tt>true<tt> when Facts share at least one Claim 
	 * neighbor in common, <tt>false</tt> otherwise.</p>
	 */
	public static final BiPredicate<Fact,Fact> RULES_CONNECT = (f1,f2) -> f1.intersects(f2);
	
	/**
	 * <p>If the specified lists of source and recipient Rules together constitute a valid 
	 * Sledgehammer solution scenario, then a set of the Claims (from the recipients) 
	 * to be set false is returned, otherwise <tt>null</tt> is returned.</p>
	 * 
	 * <p><tt>reds</tt> and <tt>greens</tt> are a valid Sledgehammer solution scenario if <ul>
	 * <li>The union of all the recipient Rules contains as a proper subset the union of all 
	 * the source Rules.</li>
	 * <li>None of the source Rules shares any Claims with any other source Rule (accounted 
	 * for by a call to {@link #sideEffectUnion(Collection,boolean) sideEffectUnion(reds,true)} 
	 * in <tt>process()</tt> before this method is called)</li>
	 * <li>The number of recipient Rules equals the number of source Rules</li>
	 * <li>The source and recipient Rules constitute a connected subgraph of the target, 
	 * such that a Rule is a vertex and such that two such vertices are connected if their 
	 * Rules share at least one Claim.</li>
	 * </ul></p>
	 * 
	 * @param reds source Rules, for which every {@link #isPossibleSolution(List) possible} 
	 * solution must falsify all the non-<tt>reds</tt> Claims in <tt>greens</tt> in 
	 * order for the specified Sledgehammer solution scenario constituted by those two parameter 
	 * to be valid
	 * 
	 * @param greens recipient Rules. Every Claim among these that is not also accounted for 
	 * among <tt>reds</tt> must be guaranteed false in every {@link #isPossibleSolution(List) possible}
	 * solution-state of the <tt>reds</tt>
	 * 
	 * @param srcUnion the union of all the elements of <tt>reds</tt>, specified as a 
	 * parameter for convenience since it is needed here, was previously generated in 
	 * the {@link #process() calling context}, and remains unchanged since then
	 * 
	 * @return null if the Sledgehammer solution scenario defined by <tt>reds</tt> and 
	 * <tt>greens</tt> is not valid, a set of the Claims to be set false if the specified 
	 * solution scenario is valid
	 */
	private static Set<Claim> sledgehammerValidityCheck(List<? extends Fact> reds, List<? extends Fact> greens, ToolSet<Claim> srcUnion){
		
		Set<Claim> greenClaims = sideEffectUnion(greens,false).stream()
				.filter((e)->!srcUnion.contains(e))
				.collect(Collectors.toSet());
		if(greenClaims.isEmpty()){
			return null;
		}
		
		for(List<Claim> solutionState : new NCuboid<Claim>(reds)){
			if(isPossibleSolution(solutionState)){
				for(Claim c : greenClaims){
					if( isPossibleClaim(c,solutionState) ){
						return null;
					}
				}
			}
		}
		return greenClaims;
	}
	
	/**
	 * <p>Returns false if the specified solution-state is impossible, true 
	 * otherwise. A solution-state is impossible if any two specified Claims 
	 * share at least one Rule in common.</p>
	 * @return false if the specified solution-state is impossible, true 
	 * otherwise
	 */
	private static boolean isPossibleSolution(List<Claim> solutionState){
		return sideEffectUnion(solutionState,true) != null;
	}
	
	/**
	 * <p>Returns true if <tt>c</tt> can still be true given that all the 
	 * Claims in <tt>givens</tt> are known true, false otherwise.</p>
	 * 
	 * @param c the Claim whose truth is being assessed
	 * @param givens the Claims whose truth is assumed while assessing 
	 * the truth of <tt>c</tt>.
	 * @return true if <tt>c</tt> is not guaranteed to be false given 
	 * that all the claims in <tt>givens</tt> are true, false otherwise.
	 */
	private static boolean isPossibleClaim(Claim c, List<Claim> givens){
		for(Claim given : givens){
			if(c.intersects(given)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * <p>Unions all the collections in <tt>srcCombo</tt> into one set and returns 
	 * that set, unless some elements are shared among the collections in 
	 * srcCombo, in which case, if <tt>nullIfNotDisjoint</tt> is true, null is 
	 * returned instead.</p>
	 * @param colcol a collection of collections whose elements are combined 
	 * into one set and returned.
	 * @param nullIfNotDisjoint controls whether an intersection among the elements 
	 * of <tt>srcCombo</tt> results in <tt>null</tt> being returned.
	 * @return <tt>null</tt> if <tt>nullIfNotDisjoint</tt> is <tt>true</tt> and 
	 * some of the elements of <tt>srcCombo</tt> intersect each other, or otherwise 
	 * the mass-union of all the elements of <tt>srcCombo</tt>.
	 */
	static <T> ToolSet<T> sideEffectUnion(Collection<? extends Collection<T>> colcol, boolean nullIfNotDisjoint){
		ToolSet<T> result = new ToolSet<>();
		
		int cumulativeSize = 0;
		for(Collection<T> redBag : colcol){
			result.addAll(redBag);
			cumulativeSize += redBag.size();
		}
		
		return !nullIfNotDisjoint || cumulativeSize==result.size() ? result : null;
	}
	
	/**
	 * <p>Sets all the Claims in <tt>claimsToSetFalse</tt> false.</p>
	 * @param claimsToSetFalse the Claims to be set false
	 * @return true if any of the <tt>claimsToSetFalse</tt> were set 
	 * from possible to false, false otherwise.
	 */
	private SolutionEvent resolve(Collection<Claim> claimsToSetFalse){
		SolutionEvent time = new SolveEventSledgehammer(claimsToSetFalse);
		claimsToSetFalse.stream().forEach((c)->c.setFalse(time));
		return time;
	}
	
	/**
	 * <p>Represents an event (group) when a valid sledgehamemr 
	 * has been found and is being resolved.</p>
	 * @author fiveham
	 *
	 */
	public class SolveEventSledgehammer extends SolutionEvent{
		private SolveEventSledgehammer(Collection<Claim> claimsToSetFalse){
			falsified().addAll(claimsToSetFalse);
		}
	}
	
	/**
	 * <p>Returns a set of all the Rules that intersect any of the 
	 * Rules in <tt>sources</tt>.</p>
	 * @param union a pre-computed {@link Sledgehammer#sideEffectUnion(Collection, boolean) mass-union} 
	 * of the Claims in the Rules in <tt>sources</tt>
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in <tt>sources</tt>, excluding the Rules in <tt>sources</tt>.
	 */
	private Set<Fact> rulesIntersecting(Set<Claim> union, List<Rule> sources){
		Set<Fact> result = new HashSet<>();
		union.stream().forEach( (c)->result.addAll(c) );
		result.removeAll(sources);
		return result;
	}
}