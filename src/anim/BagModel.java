package anim;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import common.graph.BasicGraph;
import common.graph.Wrap;
import java.util.function.BiPredicate;
import sudoku.Claim;
import sudoku.Puzzle;
import javafx.animation.Timeline;
import javafx.scene.paint.PhongMaterial;

/**
 * <p>Describes a geometric model of a {@link claims.nosymbol.zerobase.arbsize.graph.Rule Rule} 
 * at a given point in the process of solving the target to which such a Rule belongs.</p>
 * 
 * <p>Coordinates the VoxelModels whose Claims all pertain to the same maximally-pedantic Rule 
 * that existed when the target to which those Claims and Rules belong was initialized.</p>
 * @author fiveham
 */
public class BagModel {
	
	private Set<Claim> markedVoxels;
	private Set<Claim> occupiedVoxels;
	
	private final Map<Claim,VoxelModel> map;
	
	/**
	 * <p>Constructs a BagModel coordinating the specified <tt>voxels</tt>. The 
	 * <tt>voxels</tt> are all given the specified <tt>bagColor</tt>.</p>
	 * @param p the target to which this BagModel pertains
	 * @param voxels the VoxelModels coordinated by this BagModel
	 * @param bagColor the color given to this fact-bag model and all its 
	 * constituent VoxelModels
	 */
	public BagModel(Puzzle p, Collection<VoxelModel> voxels, PhongMaterial bagColor){
		this.map = new HashMap<>(p.sideLength());
		
		for(VoxelModel vm : voxels){
			int x = vm.x();
			int y = vm.y();
			int z = vm.z();
			Claim c = p.claims().get(x,y,z);
			map.put(c, vm);
			vm.setOwnerBag(this);
			vm.setMaterial(bagColor);
		}
		
		this.markedVoxels   = new HashSet<>(map.keySet());
		this.occupiedVoxels = new HashSet<>(map.keySet());
	}
	
	/**
	 * <p>Handles an event when a VoxelModel is {@link VoxelModel#disoccupy() disoccupied}. 
	 * Removes <tt>disoccupiedVM</tt>'s Claim from this BagModel's list of 
	 * </tt>occupiedVoxels</tt>.</p>
	 * @param disoccupiedVM a VoxelModel belonging to this BagModel which is being 
	 * {@link VoxelModel#disoccupy() disoccupied}
	 * @see VoxelModel#disoccupy()
	 * @return true if the collection of </tt>occupiedVoxels</tt> of this BagModel was 
	 * changed by this operation, false otherwise
	 */
	public boolean notifyDisoccupied(VoxelModel disoccupiedVM){
		return occupiedVoxels.remove( disoccupiedVM.getClaim() );
	}
	
	/**
	 * <p>Handles an event when a VoxelModel is {@link VoxelModel#disown() disowned}. 
	 * Removes the specified VoxelModel from <tt>markedVoxels</tt> and removes the 
	 * mapping from the model's associated Claim to the VoxelModel.</p>
	 * @param disownedVM a VoxelModel belonging to this BagModel which is being 
	 * {@link VoxelModel#disown() disowned}
	 * @return true if <tt>markedVoxels</tt> or the Claim-to-VoxelModel <tt>map</tt> 
	 * is changed by this method
	 */
	public boolean notifyDisown(VoxelModel disownedVM){
		Claim claim = disownedVM.getClaim();
		return markedVoxels.remove(claim) | map.remove(claim, disownedVM);
	}
	
	/**
	 * <p>Returns the VoxelModel belonging to this BagModel that 
	 * pertains to the specified Claim, or <tt>null</tt> if there 
	 * is no VoxelModel pertaining to <tt>c</tt> in this BagModel.</p>
	 * @param c a Claim whose corresponding VoxelModel in this 
	 * BagModel is returned
	 * @return the VoxelModel belonging to this BagModel that 
	 * pertains to the specified Claim, or <tt>null</tt> if there 
	 * is no VoxelModel pertaining to <tt>c</tt> in this BagModel
	 */
	public VoxelModel mapGet(Claim c){
		return map.get(c);
	}
	
	/**
	 * <p>Returns a BagModel from which all unnecessarily marked voxels have 
	 * been removed so that voxels are only modeled if they are occupied 
	 * or if they bridge two occupied voxels.
	 * 
	 * For generating the model for after some falsified claims have been 
	 * removed.
	 * @param occupied
	 * @param candidateMarkedClaims
	 * @return
	 */
	public void trimUnoccupiedExtremeVoxels(Timeline timeline){
		Set<Claim> emptyVoxels = markedVoxels.stream()
				.filter((e)->!occupiedVoxels.contains(e))
				.collect(Collectors.toSet()); //TODO use stream/filter to replace that one other time I did a "set = new HashSet(otherSet); set.removeAll(otherOtherSet);"
		
		for(double time = 0; 
				0 != removeEmptyVoxels(
						emptyVoxels, 
						timeline, 
						time+=VoxelModel.TRANSITION_TIME_DISOCCUPY.toMillis()););
		
		emptyVoxels.addAll(occupiedVoxels);
		markedVoxels = occupiedVoxels;
	}
	
	/**
	 * Removes from <tt>emptyVoxels</tt> all the Claims that are at an extreme position 
	 * in the Rule to which this BagModel pertains and returns the number of Claims removed 
	 * from <tt>emptyVoxels</tt>.
	 * @param emptyVoxels
	 * @return
	 */
	private int removeEmptyVoxels(Set<Claim> emptyVoxels, Timeline timeline, double time){
		int initSize = emptyVoxels.size();
		
		for(Iterator<Claim> i = emptyVoxels.iterator(); i.hasNext();){
			Claim claim = i.next();
			if(canRemoveEmptyVoxel( claim )){
				i.remove();
				VoxelModel toAnimate = map.get(claim);
				timeline.getKeyFrames().addAll(toAnimate.evacuate(time));
			}
		}
		
		return initSize - emptyVoxels.size();
	}
	
	public static final BiPredicate<Claim,Claim> ADJACENT_CLAIMS = (c1,c2) -> c1.spaceDistTo(c2)==1;
	
	/**
	 * returns true if removing <tt>emptyVoxel</tt> from <tt>markedVoxels</tt> 
	 * would not split <tt>markedVoxels</tt> into multiple connected components.
	 * For the purpose of this assessment, voxels are considered connected if they 
	 * share a cubic face (share two values out of X, Y, and Z with a difference of 1 
	 * in the non-same dimension).
	 * @param emptyVoxel
	 * @return
	 */
	private boolean canRemoveEmptyVoxel(Claim emptyVoxel){
		Set<Claim> newMarkedVoxels = new HashSet<>(markedVoxels); //Set<Claim> --> List<Wrap<Claim,?>>
		newMarkedVoxels.remove(emptyVoxel);
		return new BasicGraph<Wrap<Claim>>(Wrap.wrap(newMarkedVoxels, ADJACENT_CLAIMS)).connectedComponents().size() == 1;
	}
	
	/*public Mesh getMesh(){
		GraphTheory.Graph<Voxel> voxelGraph = new GraphTheory.Graph<>(wrapClaimsInVoxels(), (v1,v2) -> ADJACENT_CLAIMS.test(v1.claim(),v2.claim()));
		giveVoxelsConnectionData(voxelGraph);
		giveVoxelsCuboidGeometry(voxelGraph);
		giveVoxelsSpacePositions(voxelGraph);
		return null;
		
		
		//voxelGraph.iterableOfT();
		
		 * A voxel will be either empty, small, or large.
		 * Empty if the voxel's claim is known false and the voxel is not a {@link #markedVoxels marked voxel}.
		 * Small if the voxel is marked and known false.
		 * Large if the voxel is {@link #occupiedVoxels occupied}.
		 * 
		 * If it is empty, it does not connect to anything.
		 * Otherwise, a complex set of possible connection-states must be accounted for.
		 * 
		 * It turns out that these complex connection-states can be much less complex.  Excellent.
		 * We can just use a cuboid for a voxel whether it's occupied or not.  
		 * In the case of an unoccupied marked voxel, the cuboid is a squareXrectangleXrectangle prism squashed 
		 * onto one face of the voxel's overall cube.
		 * An occupied voxel's cuboid is a full cube filling the voxel's cube.
		 * 
		 * Let's make it so that a marked unoccupied voxel is always a pancake cuboid in the [lower|upper] half 
		 * of the voxel cube unless the MUV in question has a neighbor above or below it, in which case, 
		 * it is represented as a pancake squashed against the lower-X face of the voxel cube.
		 
		
		
		
		
		
	}*/
	
	/*private void giveVoxelsSpacePositions(GraphTheory.Graph<Voxel> voxelGraph){
		for(Voxel vox : voxelGraph.iterableOfT()){
			vox.setCuboid(vox.cuboid.addPosition(vox.claim));
		}
	}*/
	
	/*public static final float VOXEL_EDGE_LENGTH = 1.0f;
	public static final float SHORT_VOXEL_EDGE_LENGTH = 0.3f;
	public static final Point ORIGIN = new Point(0f,0f,0f);
	*/
	/*public static final Cuboid FULL_CUBE = new Cuboid(ORIGIN, new Point(VOXEL_EDGE_LENGTH, VOXEL_EDGE_LENGTH, VOXEL_EDGE_LENGTH));
	public static final Cuboid VERT_EMPTY_CUBOID = new Cuboid(ORIGIN, new Point(SHORT_VOXEL_EDGE_LENGTH, VOXEL_EDGE_LENGTH, VOXEL_EDGE_LENGTH));
	public static final Cuboid NONVERT_EMPTY_CUBOID = new Cuboid(ORIGIN, new Point(VOXEL_EDGE_LENGTH, VOXEL_EDGE_LENGTH, SHORT_VOXEL_EDGE_LENGTH));
	*/
	/*private void giveVoxelsCuboidGeometry(GraphTheory.Graph<Voxel> voxelGraph){
		for(Voxel vox : voxelGraph.iterableOfT()){
			if(!vox.claim.isKnownFalse()){
				vox.setCuboid(FULL_CUBE);
			} else{
				//SideConnection scUp = vox.sideConnections[4], scDn = vox.sideConnections[5];
				vox.setCuboid( vox.isConnectedVertical()  
						? VERT_EMPTY_CUBOID 
						: NONVERT_EMPTY_CUBOID );
			}
		}
	}*/
	
	/*private void giveVoxelsConnectionData(GraphTheory.Graph<Voxel> voxelGraph){
		for(GraphTheory.Graph.Node<Voxel> node : voxelGraph){
			Voxel vox = node.getWrapee();
			Claim c = vox.claim;
			List<Voxel> neighbors = node.getNeighbors();
			for(Voxel neb : neighbors){
				Claim cn = neb.claim;
				
				int[] vec = c.vectorTo(cn);
				
				vox.setSideConnection(vec, cn);
			}
		}
	}*/
	
	/*public List<Voxel> wrapClaimsInVoxels(){
		List<Voxel> result = new ArrayList<>(markedVoxels.size());
		for(Claim c : markedVoxels){
			result.add(new Voxel(c));
		}
		return result;
	}*/
	
	/*public class Voxel{
		private final Claim claim;
		private final SideConnection[] sideConnections; //x- x+ y- y+ z- z+
		private Cuboid cuboid = null;
		public Voxel(Claim c){
			this.claim = c;
			this.sideConnections = new SideConnection[6];
			Arrays.fill(sideConnections, SideConnection.NONE);
		}
		public Cuboid getCuboid(){
			return cuboid;
		}
		public void setCuboid(Cuboid cuboid){
			this.cuboid = cuboid;
		}
		public Claim claim(){
			return claim;
		}
		public boolean isConnectedVertical(){
			return sideConnections[4] != SideConnection.NONE || sideConnections[5] != SideConnection.NONE;
		}
		public void setSideConnection(int[] dir, Claim c){
			boolean occ = occupiedVoxels.contains(c);
			if(dir[0] < 0){
				sideConnections[0] = SideConnection.connection(occ);
			} else if(dir[0] > 0){
				sideConnections[1] = SideConnection.connection(occ);
			} else if(dir[1] < 0){
				sideConnections[2] = SideConnection.connection(occ);
			} else if(dir[1] > 0){
				sideConnections[3] = SideConnection.connection(occ);
			} else if(dir[2] < 0){
				sideConnections[4] = SideConnection.connection(occ);
			} else if(dir[2] < 0){
				sideConnections[5] = SideConnection.connection(occ);
			}
		}
	}*/
	
	/*private static class Cuboid{
		public static int VERTICES_ARRAY_LENGTH = 8;
		private final Point[] vertices;
		public Cuboid(Point aPoint, Point diagonal){
			vertices = new Point[VERTICES_ARRAY_LENGTH];
			vertices[0] = aPoint;
			vertices[1] = new Point(diagonal.x, aPoint.y,   aPoint.z);
			vertices[2] = new Point(aPoint.x,   diagonal.y, aPoint.z);
			vertices[3] = new Point(diagonal.x, diagonal.y, aPoint.z);
			vertices[4] = new Point(aPoint.x,   aPoint.y,   diagonal.z);
			vertices[5] = new Point(diagonal.x, aPoint.y,   diagonal.z);
			vertices[6] = new Point(aPoint.x,   diagonal.y, diagonal.z);
			vertices[7] = diagonal;
		}
		
		public float lowerX(){
			return extremeDim(Math::min, (p) -> p.x);
		}
		
		public float lowerY(){
			return extremeDim(Math::min, (p) -> p.y);
		}
		
		public float lowerZ(){
			return extremeDim(Math::min, (p) -> p.z);
		}
		
		public float extremeDim(BiFunction<Float,Float,Float> minmax, Function<Point,Float> dimSrc){
			return minmax.apply(dimSrc.apply(vertices[0]), dimSrc.apply(vertices[7]));
		}
		
		public Cuboid addPosition(Claim c){
			float deltaX = c.getX();
			float deltaY = c.getY();
			float deltaZ = c.getZ();
			Point lo = vertices[0];
			Point hi = vertices[7];
			Point newLo = new Point(lo.x+deltaX, lo.y+deltaY, lo.z+deltaZ);
			Point newHi = new Point(hi.x+deltaX, hi.y+deltaY, hi.z+deltaZ);
			return new Cuboid(newLo, newHi);
		}
	}*/
	
	/*public static class Point{
		public final float x, y, z;
		public Point(float x, float y, float z){
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}*/
	
	//public static final float DISCONNECT_OFFSET = 0.15f;
	
	/*public static enum SideConnection{
		NONE, 			//contributes a flat plane removed inward from the true face of the voxel by <tt>DISCONNECT_OFFSET</tt>
		CONNECT_EMPTY, 	//
		CONNECT_FULL;	//contributes a flat plane at the true face of the voxel (not rendered)
		public static SideConnection connection(boolean isVoxelOccupied){
			return isVoxelOccupied ? CONNECT_FULL : CONNECT_EMPTY;
		}
	}
	
	public static enum Direction{
		PLUS, MINUS;
	}*/
}
