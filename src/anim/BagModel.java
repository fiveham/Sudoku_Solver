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
	 * 
	 * @param disoccupiedVM a VoxelModel belonging to this BagModel which is being 
	 * {@link VoxelModel#disoccupy() disoccupied}
	 * @see VoxelModel#disoccupy()
	 * @return true if the collection of </tt>occupiedVoxels</tt> of this BagModel was 
	 * changed by this operation, false otherwise
	 */
	public boolean notifyDisoccupied(VoxelModel disoccupiedVM){  //TODO replace separate marked and occupied voxel lists with one voxel list and use stream.filter on properties of the VoxelModels
		return occupiedVoxels.remove( disoccupiedVM.getClaim() );
	}
	
	/**
	 * <p>Handles an event when a VoxelModel is {@link VoxelModel#disown() disowned}. 
	 * Removes the specified VoxelModel from <tt>markedVoxels</tt> and removes the 
	 * mapping from the model's associated Claim to the VoxelModel.</p>
	 * 
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
	 * 
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
	 * or if they bridge two occupied voxels.</p>
	 * 
	 * <p>For generating the model for after some falsified claims have been 
	 * removed.</p>
	 * @param timeline a Timeline animating the contraction of this BagModel 
	 * after some of its Claims have been falsified and those Claims' 
	 * VoxelModels have been compressed to mark their Claims as false in 
	 * the animation
	 * @param postDisoccupyLength initial length of <tt>timeline</tt> before 
	 * animation for this BagModel's contraction is added and before any other 
	 * BagModels' contractions that occur at the same time as that of this one 
	 * are added to <tt>timeline</tt>
	 * @return
	 */
	public void trimUnoccupiedExtremeVoxels(Timeline timeline, double postDisoccupyLength){
		Set<Claim> emptyVoxels = markedVoxels.parallelStream()
				.filter((e)->!occupiedVoxels.contains(e))
				.collect(Collectors.toSet());
		
		for(double time = postDisoccupyLength; 
				0 != removeEmptyVoxels(emptyVoxels, timeline, time);
				time += CONTRACT_TRANSITION_TIME);
		
		emptyVoxels.addAll(occupiedVoxels);
		markedVoxels = occupiedVoxels;
	}
	
	public static final double CONTRACT_TRANSITION_TIME = VoxelModel.COMPRESS_TRANSITION_TIME;
	
	/**
	 * <p>Removes from <tt>emptyVoxels</tt> all the Claims that are at an extreme position 
	 * in the Rule to which this BagModel pertains and returns the number of Claims removed 
	 * from <tt>emptyVoxels</tt>.</p>
	 * @param emptyVoxels Claims whose VoxelModels are to be removed (in terms of being 
	 * {@link #markedVoxels marked} from this BagModel
	 * @return the number of Claims whose VoxelModels were 
	 * {@link VoxelModel#evacuate(double) removed} from this BagModel
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
	 * <p>Returns true if removing <tt>emptyVoxel</tt> from <tt>markedVoxels</tt> 
	 * would not split <tt>markedVoxels</tt> into multiple connected components.</p>
	 * 
	 * <p>For the purpose of this assessment, voxels are considered connected if they 
	 * share a cubic face (share two values out of X, Y, and Z with a difference of 1 
	 * in the non-same dimension).</p>
	 * @param emptyVoxel the VoxelModel whose removal from this BagModel's list of 
	 * <tt>markedVoxels</tt> is tested in terms of whether that removal splits this 
	 * BagModel visible features into multiple connected components
	 * @return true if removing <tt>emptyVoxel</tt> from <tt>markedVoxels</tt> 
	 * would not split <tt>markedVoxels</tt> into multiple connected components, 
	 * false otherwise
	 */
	private boolean canRemoveEmptyVoxel(Claim emptyVoxel){
		Set<Claim> newMarkedVoxels = new HashSet<>(markedVoxels);
		newMarkedVoxels.remove(emptyVoxel);
		return new BasicGraph<Wrap<Claim>>(Wrap.wrap(newMarkedVoxels, ADJACENT_CLAIMS)).connectedComponents().size() == 1;
	}
}
