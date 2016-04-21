package sudoku;

import common.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/*
 * http://www.sadmansoftware.com/sudoku/colouring.php
 * 
 * If you can chain together a bunch of FactBags of size 2 
 * all pertaining to the same Symbol, then any cells outside 
 * that chain that are in a bag with one of the Claims in 
 * the chain and in another bag with another Claim of 
 * opposite truth-state in the Chain, then that outside-the-
 * chain cell can have the Symbol in question marked impossible 
 * in it.
 * 
 */
public class ColorChain extends Technique {
	
	public static final Function<Index,Predicate<FactBag>> FactBag_TEST_GENERATOR = (s) -> ((fb) -> fb.size() == FactBag.SIZE_WHEN_XOR && fb.zMax() == s && fb.zMin() == s );
	
	public ColorChain(Puzzle puzzle) {
		super(puzzle);
	}
	
	private boolean claimProvenFalseByChainContradiction(Claim claim, Graph concom){
		boolean neighborsPositive = false;
		boolean neighborsNegative = false;
		
		for(Graph.Node node : concom.nodes){
			if( claim.sharesRegionWith(node.claim) ){
				if(node.color > 0){
					neighborsPositive = true;
				} else{
					neighborsNegative = true;
				}
			}
		}
		
		return neighborsPositive && neighborsNegative;
	}
	
	@Override
	protected boolean process() {
		boolean result = false;
		
		//iterate over the symbol-layers of the cube form of the target
		for(Index symbol : Index.values()){
			
			Collection<FactBag> binaryBags = puzzle.factBagsWhere( FactBag_TEST_GENERATOR.apply(symbol) );
			List<Claim> claimsInBinaryBags = new ArrayList<>(SledgeHammer2.unionAll(binaryBags));
			Collection<Claim> possibleClaims = puzzle.claims().claimsWhere( (c) -> !c.isKnownFalse() && !c.isKnownTrue() );
			
			//check all cells on this layer irrespective of their chain memberships
			Graph g = new Graph(claimsInBinaryBags, binaryBags);
			List<Graph> components = g.connectedComponents();
			for(Graph concom : components){
				for(Claim c : possibleClaims){
					if( claimProvenFalseByChainContradiction(c, concom) ){
						
						result |= c.setFalse();
						
					}
				}
			}
			
			while(resolveComponents(components)){
				result = true;
			}
			
		}
		
		return result;
	}
	
	private boolean resolveComponents(List<Graph> components){
		boolean result = false;
		for(Graph concom : components ){
			result |= concom.resolveCertainties();
		}
		return result;
	}
	
	private static class Graph{
		
		public static final int INIT_COLOR_ROOT = 0;
		
		public static int colorRoot = INIT_COLOR_ROOT;
		
		private List<Node> nodes;
		public Graph(Collection<Claim> nakedNodes, Collection<FactBag> edges){
			nodes = new ArrayList<>(nakedNodes.size());
			for(Claim c : nakedNodes){
				nodes.add(new Node(c, INIT_COLOR_ROOT, edges));
			}
		}
		
		private Graph(Collection<Node> col){
			nodes = new ArrayList<>(col);
		}
		
		public List<Node> neighbors(Node n){
			List<Node> result = new ArrayList<>();
			
			for(Node graphNode : nodes){
				if(n.neighbors.contains(graphNode.claim)){
					result.add(graphNode);
				}
			}
			
			return result;
		}
		
		/*public boolean setFalse(boolean isPositive){
			boolean result = false;
			for(Node n : nodes){
				if( isPositive == n.color > 0 ){
					result |= n.claim.setFalse();
				}
			}
			return result;
		}*/
		
		/**
		 * Searches the node list for nodes known to be true or false 
		 * and sets other nodes in the graph with the same color to 
		 * the same truth-state.
		 * @throws IllegalStateException if 
		 * @return true if the target has been changed by this operation, 
		 * false otherwise
		 */
		public boolean resolveCertainties(){
			boolean result = false;
			
			Set<Pair<Integer,Boolean>> set = new HashSet<>();;
			for(Node node : nodes){
				if(node.claim.isKnownFalse()){
					set.add(new Pair<Integer,Boolean>(node.color,false));
				} else if(node.claim.isKnownTrue()){
					set.add(new Pair<Integer,Boolean>(node.color,true));
				}
			}
			
			switch(set.size()){
			case 1 : List<Pair<Integer,Boolean>> l1 = new ArrayList<>(set);
				Pair<Integer,Boolean> first1 = l1.get(0);
				result |= setAllByColor(first1.getA(), first1.getB());
				break;
			case 2 : List<Pair<Integer,Boolean>> l2 = new ArrayList<>(set);
				Pair<Integer,Boolean> first2 = l2.get(0), second = l2.get(1);
				if( first2.getB() ^ second.getB() ){
					result |= setAllByColor(first2.getA(), first2.getB()) | setAllByColor(second.getA(), second.getB());
				} else{
					throw new IllegalStateException("Two colors getting set to "+first2.getB());
				}
			case 0 : break;
			default : throw new IllegalStateException(set.size()+" associations exist between a color in a chain and the truth-state of the cells thus colored (max 2 allowable).");
			}
			return result;
		}
		
		private boolean setAllByColor(int color, boolean isTrue){
			boolean result = false;
			for(Node node : nodes){
				if(node.color == color){
					result |= ( isTrue ? node.claim.setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD() : node.claim.setFalse() );
				}
			}
			return result;
		}
		
		private List<Graph> concom = null;
		
		public List<Graph> connectedComponents(){
			if(concom == null){
				concom = new ArrayList<>();
				
				List<Node> nodesNotAssignedToAComponent = new ArrayList<>(nodes);
				
				for( ConnectedComponent newConCom; 
						!nodesNotAssignedToAComponent.isEmpty(); 
						concom.add( new Graph(newConCom.contract()) ) ){
					(newConCom = new ConnectedComponent(nodes.size(), colorRoot++)).add(nodesNotAssignedToAComponent.remove(0));
					for(; !newConCom.cuttingEdge.isEmpty(); 
							nodesNotAssignedToAComponent.removeAll(newConCom.cuttingEdge) ){
						newConCom.contract();
						for(Node edgeNode : newConCom.edge){
							newConCom.addAll(neighbors(edgeNode));
						}
					}
				}
			}
			return concom;
		}
		
		private static class ConnectedComponent{
			
			private Set<Node> core, edge, cuttingEdge;
			private int size;
			private int color;
			private boolean phase;
			
			public ConnectedComponent(int size, int color){
				this.size = size;
				this.core = new HashSet<>(size);
				this.edge = new HashSet<>(size);
				this.cuttingEdge = new HashSet<>(size);
				this.color = color;
				this.phase = true;
			}
			
			public void add(Node node){
				if( !core.contains(node) && !edge.contains(node) ){
					node.setNewColor(phase?color:-color);
					cuttingEdge.add(node);
				}
			}
			
			public void addAll(Collection<Node> nodes){
				for(Node n : nodes){
					add(n);
				}
			}
			
			public Set<Node> contract(){
				core.addAll(edge);
				edge = cuttingEdge;
				cuttingEdge = new HashSet<>(size);
				phase = !phase;
				return core;
			}
		}
		
		private static class Node{
			private Claim claim;
			private int color;
			private List<Claim> neighbors;
			public Node(Claim claim, int color, Collection<FactBag> edges){
				this.claim = claim;
				this.color = color;
				this.neighbors = new ArrayList<>();
				for(FactBag fb : edges){
					if(fb.contains(claim)){
						List<Claim> l = new ArrayList<>(fb);
						l.remove(claim);
						neighbors.add(l.get(0));
					}
				}
			}
			
			private void setNewColor(int color){
				if(this.color == INIT_COLOR_ROOT){
					this.color = color;
				} else{
					throw new IllegalStateException("Trying to set "+color+" as new color in place of "+this.color);
				}
			}
			
			@Override
			public int hashCode(){
				return claim.hashCode();
			}
			
			@Override
			public boolean equals(Object o){
				if(o instanceof Node){
					Node n = (Node) o;
					return claim == n.claim && color == n.color && neighbors.equals(n.neighbors);
					
				}
				return false;
			}
		}
		
	}
	
	/*private class Chain{
		
		
		
		
		public void add(){
			
		}
		
		public boolean resolve(){
			boolean result = false;
			
			
			
			return result;
		}
	}
	
	private class ColoredClaim{
		private Claim claim;
		private int color;
		public ColoredClaim(Claim claim, int color){
			this.claim = claim;
			this.color = color;
		}
	}*/
	
}
