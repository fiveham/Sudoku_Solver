package sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
//import java.util.function.Function;
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
 * TODO in-chain validity checking (collapse all XORs if self-contradicting)
 * If any node in the chain graph shares a FactBag with a node of the same 
 * (non-zero) color, then collapse the entire chain, setting all the claims 
 * with that color to false.
 * 
 * TODO inter-chain bridge even-XOR-distance checks
 * If there exists a pair of XOR chains bridged by two non-XOR FactBags 
 * which are separated from one another in those two chains by an even 
 * number of XORs in one chain, then the (a) chain with the even XOR-count 
 * distance can be completely collapsed, setting all of its claims with 
 * the same color as its two claims on the bridges to false.
 * 
 * 
 * 
 * 
 * 
 */
public class ColorChain extends Technique {
	
	public static final Predicate<FactBag> BAG_IS_XOR = (fb) -> fb.size() == FactBag.SIZE_WHEN_XOR;
	
	public ColorChain(Puzzle puzzle) {
		super(puzzle);
	}
	
	private boolean claimProvenFalseByChainContradiction(Claim claim, Graph concom){
		boolean hasPosColorNeighbor = false;
		boolean hasNegColorNeighbor = false;
		
		for(Graph.Node node : concom.nodes){
			if( claim.sharesBagWith(node.claim) ){
				if(node.color > 0){
					hasPosColorNeighbor = true;
				} else if(node.color < 0){
					hasNegColorNeighbor = true;
				} else{
					throw new IllegalStateException("This Node has a 0 color: "+node.toString());
				}
			}
		}
		
		return hasPosColorNeighbor && hasNegColorNeighbor;
	}
	
	@Override
	protected boolean process() {
		boolean result = false;
		
		Collection<FactBag> xorBags = puzzle.factBagsWhere( BAG_IS_XOR );
		List<Claim> claimsInXorBags = new ArrayList<>(SledgeHammer2.unionAll(xorBags));
		Collection<Claim> possibleClaims = puzzle.claims().claimsWhere( (c) -> !c.isKnownFalse() && !c.isKnownTrue() );
		
		Graph g = new Graph(claimsInXorBags, xorBags);
		List<Graph> components = g.connectedComponents();
		
		for(Graph concom : components){
			result |= resolve(possibleClaims, concom);
		}
		
		return result;
	}
	
	private boolean resolve(Collection<Claim> possibleClaims, Graph concom){
		boolean result = false;
		List<Claim> claimsSetFalse = new ArrayList<>();
		
		for(Claim c : possibleClaims){
			if( claimProvenFalseByChainContradiction(c, concom) ){
				result |= c.setFalse();
				claimsSetFalse.add(c);
			}
		}
		
		if(result){
			puzzle.addSolveEvent(claimsSetFalse);
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
				nodes.add(new Node(c, INIT_COLOR_ROOT));
			}
		}
		
		private Graph(Collection<Node> col){
			nodes = new ArrayList<>(col);
		}
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			
			sb.append("BasicGraph: with ").append(nodes.size()).append(" nodes");
			for(Node n : nodes){
				sb.append(System.getProperty("line.separator")).append("\t").append(n.toString());
			}
			
			return sb.toString();
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
		
		private List<Graph> concom = null;
		
		public List<Graph> connectedComponents(){
			if(concom == null){
				concom = new ArrayList<>();
				
				List<Node> nodesNotAssignedToAComponent = new ArrayList<>(nodes);
				
				while(!nodesNotAssignedToAComponent.isEmpty()){
					ConnectedComponent newConCom = new ConnectedComponent(nodes.size(), ++colorRoot, nodesNotAssignedToAComponent);
					
					while(!newConCom.cuttingEdge.isEmpty()){
						newConCom.contract();
						
						for(Node edgeNode : newConCom.edge){
							newConCom.addAll(neighbors(edgeNode));
						}
						
						nodesNotAssignedToAComponent.removeAll(newConCom.cuttingEdge);
					}
					
					concom.add( new Graph(newConCom.contract()) );
				}
			}
			return concom;
		}
		
		private static class ConnectedComponent{
			
			private Set<Node> core, edge, cuttingEdge;
			private int size;
			private int color;
			private boolean phase;
			
			public ConnectedComponent(int size, int color, List<Node> src){
				this.size = size;
				this.core = new HashSet<>(size);
				this.edge = new HashSet<>(size);
				this.cuttingEdge = new HashSet<>(size);
				this.color = color;
				this.phase = true;
				add(src.remove(0));
			}
			
			public void add(Node node){
				if( !core.contains(node) && !edge.contains(node) ){
					if(cuttingEdge.add(node)){
						node.setNewColor(phase?color:-color);
					}
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
			private Set<Claim> neighbors;
			
			public Node(Claim claim, int color){
				this.claim = claim;
				this.color = color;
				this.neighbors = new HashSet<>(Claim.INIT_OWNER_COUNT);
				for(FactBag fb : claim.getOwners()){
					if(BAG_IS_XOR.test(fb)){
						neighbors.add(partner(claim,fb));
					}
				}
			}
			
			private static Claim partner(Claim c, Set<Claim> pair){
				Iterator<Claim> iter = pair.iterator();
				Claim c1 = iter.next();
				if(c==c1){
					return iter.next();
				} else if(c==iter.next()){
					return c1;
				} else{
					throw new IllegalArgumentException("Specified claim ("+c.toString()+"( not present in specified factbag.");
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
			public String toString(){
				StringBuilder sb = new StringBuilder().append("Node for ")
						.append(claim).append(", with color ")
						.append(color).append(", with these neighbors: ");
				for(Claim c : neighbors){
					sb.append(System.getProperty("line.separator")).append("\t").append(c);
				}
				
				return sb.toString();
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
}
