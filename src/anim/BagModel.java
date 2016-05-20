package anim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import common.graph.BasicGraph;
import common.graph.Wrap;
import java.util.function.BiPredicate;
import java.util.function.Function;

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
	
	private Set<Claim> voxels;
	
	private final Map<Claim,VoxelModel> map;
	
	/**
	 * <p>Constructs a BagModel coordinating the specified {@code voxels}. The 
	 * {@code voxels} are all given the specified {@code bagColor}.</p>
	 * @param p the target to which this BagModel pertains
	 * @param voxels the VoxelModels coordinated by this BagModel
	 * @param bagColor the color given to this fact-bag model and all its 
	 * constituent VoxelModels
	 */
	public BagModel(Puzzle p, Collection<VoxelModel> voxels, PhongMaterial bagColor){
		this.map = new HashMap<>(p.sideLength());
		
		for(VoxelModel vm : voxels){
			int x = vm.getX();
			int y = vm.getY();
			int z = vm.getZ();
			Claim c = p.claims().get(x,y,z);
			map.put(c, vm);
			vm.setOwnerBag(this);
			vm.setMaterial(bagColor);
		}
		
		this.voxels = new HashSet<>(map.keySet());
	}
	
	/**
	 * <p>Removes the specified VoxelModel from this BagModel's internal map.</p>
	 * 
	 * <p>Removing mappings is necessary in order for 
	 * {@link VoxelModel#contractSign(int,BagModel,Function) VoxelModel.contractSign()} 
	 * to work.</p>
	 * @param vm the VoxelModel whose Claim-to-VoxelModel mapping is removed 
	 * from this BagModel's internal map
	 */
	void unmap(VoxelModel vm){
		map.remove(vm.getClaim());
	}
	
	/**
	 * <p>Returns this BagModel's internal map from Claims to the 
	 * VoxelModels that pertain to those Claims and belong to this 
	 * BagModel.</p>
	 * 
	 * @return this BagModel's internal map from Claims to the 
	 * VoxelModels that pertain to those Claims and belong to this 
	 * BagModel
	 */
	Map<Claim,VoxelModel> map(){
		return map;
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
	 * @param postDisoccupyLength initial length of {@code timeline} before 
	 * animation for this BagModel's contraction is added and before any other 
	 * BagModels' contractions that occur at the same time as that of this one 
	 * are added to {@code timeline}
	 * @return
	 */
	void trimUnoccupiedExtremeVoxels(Timeline timeline, double postDisoccupyLength){
		Set<Claim> unoccupiedMarkedVoxels = voxels.parallelStream()
				.filter((e) -> map.get(e).getStatus() == VoxelModel.Status.FALSIFIED)
				.collect(Collectors.toSet());
		
		for(double time = postDisoccupyLength; 
				0 != removeEmptyVoxels(unoccupiedMarkedVoxels, timeline, time);
				time += VANISH_TRANSITION_TIME);
	}
	
	public static final double VANISH_TRANSITION_TIME = VoxelModel.FALSIFY_TRANSITION_TIME;
	
	/**
	 * <p>Removes from {@code emptyVoxels} all the Claims that are at an extreme position 
	 * in the Rule to which this BagModel pertains and returns the number of Claims removed 
	 * from {@code emptyVoxels}.</p>
	 * @param emptyVoxels Claims whose VoxelModels are to be removed (in terms of being 
	 * {@link #markedVoxels marked} from this BagModel
	 * @return the number of Claims whose VoxelModels were 
	 * {@link VoxelModel#vanish(double) removed} from this BagModel
	 */
	private int removeEmptyVoxels(Set<Claim> emptyVoxels, Timeline timeline, double time){
		List<Claim> toContract = new ArrayList<>(emptyVoxels.size());
		
		for(Iterator<Claim> i = emptyVoxels.iterator(); i.hasNext();){
			Claim claim = i.next();
			if(canRemoveEmptyVoxel( claim )){
				toContract.add(claim);
			}
		}
		
		for(Claim c : toContract){
			emptyVoxels.remove(c);
			VoxelModel toAnimate = map.get(c);
			
			/*
			 * contract() removes mappings from the BagModel, allowing multiple layers of 
			 * removals to sometimes be animated together, unless calls to contract(), which 
			 * modifies this BagModel, all occur after the state-based analysis of this 
			 * BagModel that occurs in calls to canRemoveEmptyVoxel()
			 */
			timeline.getKeyFrames().addAll(toAnimate.vanish(time));
		}
		
		return toContract.size();
	}
	
	public static final BiPredicate<Claim,Claim> ADJACENT_CLAIMS = (c1,c2) -> c1.spaceDistTo(c2)==1;
	
	/**
	 * <p>Returns true if removing {@code emptyVoxel} from {@code markedVoxels} 
	 * would not split {@code markedVoxels} into multiple connected components.</p>
	 * 
	 * <p>For the purpose of this assessment, voxels are considered connected if they 
	 * share a cubic face (share two values out of X, Y, and Z with a difference of 1 
	 * in the non-same dimension).</p>
	 * @param emptyVoxel the VoxelModel whose removal from this BagModel's list of 
	 * {@code markedVoxels} is tested in terms of whether that removal splits this 
	 * BagModel visible features into multiple connected components
	 * @return true if removing {@code emptyVoxel} from {@code markedVoxels} 
	 * would not split {@code markedVoxels} into multiple connected components, 
	 * false otherwise
	 */
	private boolean canRemoveEmptyVoxel(Claim emptyVoxel){
		Set<Claim> newMarkedVoxels = voxels.parallelStream()
				.filter((e) -> e != emptyVoxel && map.get(e).getStatus() != VoxelModel.Status.VANISHED)
				.collect(Collectors.toSet());
		return new BasicGraph<Wrap<Claim>>(Wrap.wrap(newMarkedVoxels, ADJACENT_CLAIMS)).connectedComponents().size() == SINGLE_CONNECTED_COMPONENT;
	}
	
	public static final int SINGLE_CONNECTED_COMPONENT = 1;
}
