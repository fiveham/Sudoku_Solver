package sudoku;

import common.ComboGenIso;
import common.ComboGenIso.ComboIterator;
import common.NCuboid;
import common.graph.BasicGraph;
import common.graph.Graph;
import common.graph.Wrap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.stream.Collectors;

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
public class SledgeHammer2 extends Technique {
	
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
	public SledgeHammer2(Puzzle puzzle) {
		super(puzzle);
	}
	
	/**
	 * <p>Divides the target into a collection of connected graphs whose 
	 * vertices are Rules and whose edges are Claims connecting Rules. 
	 * For each of those connected components, {@link #processComponent(Graph<Wrap<Rule>>) individual-component analysis} 
	 * is done, and the {@link Graph#connectedComponents() connected components} 
	 * of each such connected component that was changed are added to a 
	 * list of connected components to be analysed in the next cycle of 
	 * processing. Processing continues to cycle until there are no more 
	 * connected components to analyse.</p>
	 * 
	 * <p>Once a component has been changed by sledgehammer analysis, it 
	 * may or may not have been broken into multiple smaller connected 
	 * components, each of which should be analysed as its own separate 
	 * component for the sake of efficiency.</p>
	 */
	@Override
	protected boolean process(){
		boolean puzzleUpdated = false;
		
		Graph<Wrap<Rule>> ruleGraph = new BasicGraph<Wrap<Rule>>(
				Wrap.wrap(puzzle.ruleStream().filter((n)->n.size()>Rule.SIZE_WHEN_SOLVED).collect(Collectors.toList()), 
						(r1,r2) -> r1.intersects(r2)));
		Collection<Graph<Wrap<Rule>>> unsolvedComponents = ruleGraph.connectedComponents();
		
		while(!unsolvedComponents.isEmpty()){
			Collection<Graph<Wrap<Rule>>> newUC = new ArrayList<>();
			for(Graph<Wrap<Rule>> unsolvedComponent : unsolvedComponents){
				if(processComponent(unsolvedComponent)){
					puzzleUpdated |= newUC.addAll(unsolvedComponent.connectedComponents());
				}
			}
			unsolvedComponents = newUC;
		}
		
		return puzzleUpdated;
	}
	
	private boolean processComponent(Graph<Wrap<Rule>> unsolvedComponent){
		boolean puzzleUpdated = false;
		
		//For each source combo
		List<Rule> rules = unsolvedComponent.nodeStream().map((n)->n.wrapped()).collect(Collectors.toList());
		ComboGenIso<Rule> reds = new ComboGenIso<>(rules, MIN_SRC_COMBO_SIZE, unsolvedComponent.size()/2);
		for(Iterator<List<Rule>> redIterator = reds.iterator(); redIterator.hasNext(); ){
			List<Rule> srcCombo = redIterator.next();
			
			//Make sure source combo Rules are mutually disjoint
			for(ToolSet<Claim> srcUnion = sideEffectUnion(srcCombo, true); srcUnion != null; srcUnion=null){
				
				//For each conceivable recipient combo
				Set<Rule> nearbyRules = rulesIntersecting(srcUnion, srcCombo);
				final ComboIterator<Rule> recipIter = new ComboGenIso<>(nearbyRules, srcCombo.size(), srcCombo.size()).comboIterator();
				Consumer<NodeSet<?,?>> listener = LISTENER_GENERATOR.apply(recipIter);
				puzzle.addRemovalListener(listener);
				while(recipIter.hasNext()){
					List<Rule> recipientCombo = recipIter.next();
					
					//If the source and recipient combos make a valid sledgehammer scenario
					for(Set<Claim> claimsToSetFalse = sledgehammerValidityCheck(srcCombo, recipientCombo, srcUnion); 
							claimsToSetFalse != null && resolve(claimsToSetFalse); 
							claimsToSetFalse=null){
						redIterator.remove();
						puzzleUpdated = true;
					}
				}
				puzzle.removeRemovalListener(listener);
			}
		}
		
		return puzzleUpdated;
	}
	
	/**
	 * <p>Outputs a consumer that removes a specified NodeSet<?,?> from the 
	 * underlying collection of a specified ComboIterator<Rule>.</p>
	 */
	private static final Function<ComboIterator<Rule>,Consumer<NodeSet<?,?>>> LISTENER_GENERATOR = (recipIter) -> (ns) -> {
		if(ns instanceof Rule){
			Rule r = (Rule) ns;
			recipIter.remove(r);
		}
	};
	
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
	private static Set<Claim> sledgehammerValidityCheck(List<Rule> reds, List<Rule> greens, ToolSet<Claim> srcUnion/*, ComboIterator<Rule> recipIter*/){
		
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
	private boolean resolve(Collection<Claim> claimsToSetFalse){
		boolean result = false;
		puzzle.timeBuilder().push(new TimeSledgeHammer2Found(claimsToSetFalse));
		
		for(Claim c : claimsToSetFalse){
			result |= c.setFalse();
		}
		
		puzzle.timeBuilder().pop();
		return result;
	}
	
	/**
	 * <p>Represents an event (group) when a valid sledgehamemr 
	 * has been found and is being resolved.</p>
	 * @author fiveham
	 *
	 */
	public class TimeSledgeHammer2Found extends SolutionEvent{
		private TimeSledgeHammer2Found(Collection<Claim> claimsToSetFalse){
			super(puzzle.timeBuilder().top());
			falsified().addAll(claimsToSetFalse);
		}
	}
	
	/**
	 * <p>Returns a set of all the Rules that intersect any of the 
	 * Rules in <tt>sources</tt>.</p>
	 * @param union a pre-computed {@link SledgeHammer2#sideEffectUnion(Collection, boolean) mass-union} 
	 * of the Claims in the Rules in <tt>sources</tt>
	 * @param sources a collection of Rules to be used as an originating 
	 * combination for a sledgehammer solution event
	 * @return a set of all the Rules that intersect any of the Rules 
	 * in <tt>sources</tt>, excluding the Rules in <tt>sources</tt>.
	 */
	private Set<Rule> rulesIntersecting(Set<Claim> union, List<Rule> sources){
		Set<Rule> result = new HashSet<>();
		
		for(Claim c : union){
			result.addAll(c);
		}
		result.removeAll(sources);
		
		return result;
	}
}
