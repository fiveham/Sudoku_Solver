package anim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.Box;
import javafx.scene.transform.Translate;
import javafx.animation.KeyValue;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import sudoku.Claim;
import sudoku.Puzzle;

public class VoxelModel extends Box{
	
	/**
	 * <p>The fraction of the edge-length of a true voxel which is the 
	 * thickness of an unoccupied VoxelModel.</p>
	 */
	public static final double FALSIFIED_FRACTION = 0.3;
	
	/**
	 * <p>The width, height, and depth of a voxel model ({@value}).</p>
	 */
	public static final double VOXEL_EDGE = 1.0;
	
	/**
	 * <p>Thickness of a voxel model in its squished dimension after it has 
	 * been squished pursuant to its Claim being determined false.</p>
	 */
	public static final double FALSIFIED_THICKNESS = VOXEL_EDGE * FALSIFIED_FRACTION;
	
	private final Puzzle puzzle;
	private final int x;
	private final int y;
	private final int z;
	private final PuzzleVizApp.RegionSpecies type;
	private BagModel ownerBag;
	
	private Status status;
	
	/**
	 * <p>Constructs a VoxelModel at the specified {@code x},{@code y},{@code z} coordinates in claim-space, with 
	 * the specified face-offsets, and with coordinates in physical space determined largely by position in claim-space 
	 * but modified slightly by the face-offsets.</p>
	 * 
	 * <p>The face-offsets are needed to account for the fact that the portion of a BagModel covering a given Claim 
	 * (that Claim's VoxelModel) will extend all the way to the face of the Claim's true voxel only in order to meet 
	 * the face of another VoxelModel belonging to the same BagModel and whose Claim is a 
	 * {@link Claim#spaceDistTo(Claim) distance} of 1 from the Claim of this VoxelModel. That holds true at the beginning 
	 * of the animation of the target's solution process, but once any Claims in extreme, outer-edge positions in their 
	 * BagModels are set false, the VoxelModel that is then left exposed on the new outer edge of the BagModel will 
	 * extend all the way to the face of its true voxel, remaining unchanged by the falsification and collapse of its 
	 * neighbor.</p>
	 * 
	 * @param x the x-coordinate of the center of the 1x1x1 cubic region in which this this model is positioned.
	 * @param y the y-coordinate of the center of the 1x1x1 cubic region in which this this model is positioned.
	 * @param z the z-coordinate of the center of the 1x1x1 cubic region in which this this model is positioned.
	 * @param xNeg the part of the width of this Box that is on the lower-valued side of the plane specified by {@code x}.
	 * @param xPos the part of the width of this Box that is on the higher-valued side of the plane specified by {@code x}.
	 * @param yNeg the part of the width of this Box that is on the lower-valued side of the plane specified by {@code y}.
	 * @param yPos the part of the width of this Box that is on the higher-valued side of the plane specified by {@code y}.
	 * @param zNeg the part of the width of this Box that is on the lower-valued side of the plane specified by {@code z}.
	 * @param zPos the part of the width of this Box that is on the higher-valued side of the plane specified by {@code z}.
	 */
	public VoxelModel(Puzzle p, int x, int y, int z, PuzzleVizApp.RegionSpecies type, double xNeg, double xPos, double yNeg, double yPos, double zNeg, double zPos) {
		super(xNeg+xPos, yNeg+yPos, zNeg+zPos);
		
		this.puzzle = p;
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.type = type;
		
		this.ownerBag = null;
		getTransforms().add(new Translate(x+(xPos-xNeg)/2, y+(yPos-yNeg)/2, z+(zPos-zNeg)/2));
	}
	
	public Status getStatus(){
		return status;
	}
	
	/**
	 * <p>Returns the {@link #Claim Claim} whose inclusion in a certain one 
	 * of the Claim's connected Rules is modeled by this VoxelModel.</p>
	 * @return the {@link #Claim Claim} whose inclusion in a certain one 
	 * of the Claim's connected Rules is modeled by this VoxelModel
	 */
	public Claim getClaim(){
		return puzzle.claims().get(x, y, z);
	}
	
	/**
	 * <p>Returns the BagModel of which this VoxelModel is a part.</p>
	 * @return the BagModel of which this VoxelModel is a part
	 */
	public BagModel getOwnerBag(){
		return ownerBag;
	}
	
	/**
	 * <p>The wrapped int of the {@link Claim#getX() x-component} 
	 * of this VoxelModel's Claim's position in claim-space.</p>
	 * @return the wrapped int of the {@link Claim#getX() x-component} 
	 * of this VoxelModel's Claim's position in claim-space
	 */
	public int getX(){
		return x;
	}
	
	/**
	 * <p>The wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space.</p>
	 * @return the wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space
	 */
	public int getY(){
		return y;
	}
	
	/**
	 * <p>The wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space.</p>
	 * @return the wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space
	 */
	public int getZ(){
		return z;
	}
	
	/**
	 * <p>Sets the {@code ownerBag} for this VoxelModel if it is not 
	 * already set {@code ownerBag == null}. Throws an exception if 
	 * called while {@code ownerBag} is already set.</p>
	 * @param bm the BagModel to be the new owner
	 * @throws IllegalStateException if {@code ownerBag} is already set
	 */
	void setOwnerBag(BagModel bm){
		if(ownerBag == null ){
			ownerBag = bm;
		} else{
			throw new IllegalStateException("Cannot set ownerBag because ownerBag has already been set.");
		}
	}
	
	/**
	 * <p>Length in milliseconds of the "falsify" animation where a VoxelModel 
	 * transforms from a near-cube to a square pancake.</p>
	 */
	public static final double FALSIFY_TRANSITION_TIME = 1000;
	
	/**
	 * <p>Length in milliseconds of the "vanish" animation where a VoxelModel 
	 * transforms from square pancake to a flat plane figure.</p>
	 */
	public static final double VANISH_TRANSITION_TIME = 1000;
	
	/**
	 * <p>An empty array (varargs substitute) of KeyFrame, used to unambiguously 
	 * indicate the case where no KeyFrames are produced by an operation.</p>
	 * @see #falsify()
	 */
	public static final KeyFrame[] NO_KEYFRAMES = {};
	
	/**
	 * <p>Provides an array of KeyFrames that detail the process of this VoxelModel 
	 * transforming from its initial shape and position to the shape and 
	 * position it must have in order to indicate that its associated Claim is 
	 * known false.</p>
	 * 
	 * @return an array of KeyFrames that detail the process of this VoxelModel 
	 * transforming from its initial shape and position to the shape and 
	 * position it must have in order to indicate that its associated Claim is 
	 * known false
	 */
	KeyFrame[] falsify(double initTime){
		if( status != (status = Status.FALSIFIED) ){
			KeyFrame[] result = { new KeyFrame(durationFromTime(FALSIFY_TRANSITION_TIME+initTime), keyValuesFalsify()) };
			java.util.stream.Stream.of(result);
			return result;
		} else{
			return NO_KEYFRAMES;
		}
	}
	
	/**
	 * <p>Returns KeyValues describing the end-state of this VoxelModel's contraction to a 
	 * zero-volume plane as its BagModel contracts to pull any non-bridging compressed 
	 * VoxelModels into the BagModel's interior.</p>
	 * @return an array of KeyValues describing the end-state of this VoxelModel's 
	 * collapse into oblivion
	 */
	KeyFrame[] vanish(double initTime){
		Duration periodStart = durationFromTime(initTime);
		Duration periodEnd = durationFromTime(initTime+VANISH_TRANSITION_TIME);
		
		int[] vanishSigns = vanishSigns();
		KeyFrame[] result = new KeyFrame[]{
				new KeyFrame(periodStart, keyValuesCurrentState(vanishSigns)),
				new KeyFrame(periodEnd, (ae)->setVisible(false), keyValuesVanish(vanishSigns))
		};
		
		status = Status.VANISHED;
		ownerBag.unmap(this);
		ownerBag = null;
		
		return result;
	}
	
	/**
	 * <p>Provides an array (suitable for use with varargs) of KeyValues specifying 
	 * the geometry and position of this VoxelModel at the time when this method 
	 * is called.</p>
	 * @return an array of KeyValues specifying the geometry and position of this 
	 * VoxelModel at the time when this method is called
	 */
	private KeyValue[] keyValuesCurrentState(int[] vanishSigns){
		
		List<KeyValue> result = new ArrayList<>();
		for(Dimension dimension : Dimension.values()){
			if( vanishSigns[dimension.dimNo] != VANISH_NOT ){
				DoubleProperty thickness = dimension.thicknessProperty(this);
				DoubleProperty translation = dimension.translateProperty(this);
				result.add(new KeyValue(thickness, thickness.get()));
				result.add(new KeyValue(translation, translation.get()));
			}
		}
		
		return result.toArray(KEYVALUE_ARRAY_TYPE);
	}
	
	private static final KeyValue[] KEYVALUE_ARRAY_TYPE = {};
	
	/**
	 * <p>Returns a two-element array of KeyValues specifying the end-state 
	 * of the voxel model after it has squished in accordance with its 
	 * Claim being set false.</p>
	 * 
	 * <p>The first element describes the one-dimensional dilation 
	 * transformation that moves the boundaries of the voxel model into a 
	 * position where they do not include the claim-marker at the center of 
	 * the voxel. The second element describes the translation transform that 
	 * moves the voxel model so that one face of the model remains stationary 
	 * while the opposite face does all the moving.</p>
	 * 
	 * <p>If this VoxelModel pertains to a 
	 * {@link Puzzle.RegionSpecies#CELL cell-type} Rule then the shrinking 
	 * action moves in the negative-x direction; otherwise, the motion is in 
	 * the negative-z direction. The lower and upper z-faces are available to 
	 * act as a bridge between occupied voxel models in row, column, and box 
	 * BagModels, but are not available for cell BagModels because that would 
	 * leave a visible gap between the occupied models being bridged. In the 
	 * case of cell BagModels, x and y faces are able to acceptably bridge 
	 * occupied voxels, and the choice of an x-face is arbitrary.</p>
	 * 
	 * @return a two-element array of KeyValues specifying the end-state 
	 * of the voxel model after it has squished in accordance with its 
	 * Claim being set false
	 */
	private KeyValue[] keyValuesFalsify(){
		DoubleProperty shrinkThicknessProperty = type.shrinkThicknessProperty(this);
		DoubleProperty shiftDimensionProperty = type.shiftProperty(this);
		
		KeyValue squish = new KeyValue(shrinkThicknessProperty, FALSIFIED_THICKNESS);
		KeyValue shift = new KeyValue(shiftDimensionProperty, shrinkThicknessProperty.get()/2);
		
		return new KeyValue[]{squish, shift};
	}
	
	/**
	 * <p>The factor ({@value}) by which to multiply the initial thickness of 
	 * a voxel model to obtain the final thickness of that voxel model when the 
	 * model is collapsing in the dimension in question. The thickness of a 
	 * voxel model is the height, depth, or width, depending on which dimension 
	 * is in question.</p>
	 */
	private static final int FLAT = 0;
	
	/**
	 * <p>The sign of the collapse of a voxel model collapsing such that 
	 * the model's overall motion is in the negative direction along the 
	 * (or a) dimension along which it is collapsing.</p>
	 * @see #vanishSign(int,BagModel,Function)
	 * @see #vanishSigns()
	 */
	private static final int VANISH_NEGATIVE_DIR = -1;
	
	/**
	 * <p>The sign of the collapse of a voxel model collapsing such that 
	 * the model's overall motion is in the negative direction along the 
	 * (or a) dimension along which it is collapsing.</p>
	 * @see #vanishSign(int,BagModel,Function)
	 * @see #vanishSigns()
	 */
	private static final int VANISH_POSITIVE_DIR = 1;
	
	/**
	 * <p>The sign of the collapse of a voxel model that is not collapsing 
	 * in the dimension in question.</p>
	 * @see #vanishSign(int,BagModel,Function)
	 * @see #vanishSigns()
	 */
	private static final int VANISH_NOT = 0;
	
	/**
	 * <p>Returns an array of KeyValues describing the state of this VoxelModel 
	 * after it has been contracted to zero size due to its Claim having been 
	 * set false.</p>
	 * @return an array of KeyValues describing the state of this VoxelModel 
	 * after it has collapsed due to its Claim having been set false
	 */
	private KeyValue[] keyValuesVanish(int[] vanishSigns){
		ArrayList<KeyValue> result = new ArrayList<>(6);
		
		for(Dimension dimension : Dimension.values()){
			if( vanishSigns[dimension.dimNo] != VANISH_NOT ){
				DoubleProperty thickness = dimension.thicknessProperty(this);
				DoubleProperty translation = dimension.translateProperty(this);
				result.add(new KeyValue(thickness, FLAT));
				result.add(new KeyValue(translation, translation.get() + vanishSigns[dimension.dimNo] * thickness.get()/2));
			}
		}
		
		return result.toArray(new KeyValue[0]);
	}
	
	/**
	 * <p>A spatial dimension, x, y, or z, uniting the index of the 
	 * {@link #vanishSigns() contract-sign} for that dimension, the box-thickness 
	 * property for that dimension, and the spatial position component property 
	 * for that dimension.</p>
	 * @author fiveham
	 *
	 */
	private enum Dimension{
		
		X(Puzzle.X_DIM, (vm) -> vm.widthProperty(),  (vm) -> vm.translateXProperty()), 
		Y(Puzzle.Y_DIM, (vm) -> vm.heightProperty(), (vm) -> vm.translateYProperty()), 
		Z(Puzzle.Z_DIM, (vm) -> vm.depthProperty(),  (vm) -> vm.translateZProperty());
		
		private final int dimNo;
		private final Function<VoxelModel,DoubleProperty> thicknessProperty;
		private final Function<VoxelModel,DoubleProperty> translateProperty;
		
		private Dimension(int dimNo, Function<VoxelModel,DoubleProperty> thicknessProperty, Function<VoxelModel,DoubleProperty> translateProperty){
			this.dimNo = dimNo;
			this.thicknessProperty = thicknessProperty;
			this.translateProperty = translateProperty;
		}
		
		DoubleProperty thicknessProperty(VoxelModel vm){
			return thicknessProperty.apply(vm);
		}
		
		DoubleProperty translateProperty(VoxelModel vm){
			return translateProperty.apply(vm);
		}
	}
	
	/**
	 * <p>Returns a three-element array each element of which is either 
	 * -1, 0, or 1, indicating the direction in which an evacuation 
	 * collapse can occur for this VoxelModel in the dimension indicated 
	 * by the element's index in the array: 0 is x, 1 is y, and 2 is z.</p>
	 * @return a three-element array each element of which is the sign of 
	 * the collapse-motion of this voxel model in the dimension pertaining 
	 * to the index in the returned array of the value at that index
	 */
	private int[] vanishSigns(){
		return new int[]{
				vanishSign(x, ownerBag, (d)->puzzle.claims().get(d,y,z)),
				vanishSign(y, ownerBag, (d)->puzzle.claims().get(x,d,z)),
				vanishSign(z, ownerBag, (d)->puzzle.claims().get(x,y,d))
		};
	}
	
	/**
	 * <p>Returns an int ({@link #VANISH_NEGATIVE_DIR -1}, {@link #VANISH_NOT 0}, 
	 * or {@link #VANISH_POSITIVE_DIR 1} specifying the direction in which this 
	 * VoxelModel is collapsing.</p>
	 * 
	 * <p>If this model does not need to collapse when this method is called, 
	 * 0 is returned. If this model needs to collapse in the positive direction 
	 * on the dimension internally referenced by {@code claimSrc}, 1 is 
	 * returned. Otherwise, the model needs to collapse in the negative direction 
	 * on the dimension internally referenced by {@code claimSrc.}.</p>
	 * 
	 * @param dim a value 
	 * @param claimSrc
	 * @return an int ({@link #VANISH_NEGATIVE_DIR -1}, {@link #VANISH_NOT 0}, 
	 * or {@link #VANISH_POSITIVE_DIR 1} specifying the direction in which this 
	 * VoxelModel is collapsing
	 */
	private static int vanishSign(int dim, BagModel ownerBag, Function<Integer,Claim> thing){
		Claim claimNeg = thing.apply(dim-1);
		Claim claimPos = thing.apply(dim+1);
		
		boolean hasNegativeDirectionNeighbor = ownerBag.map().containsKey(claimNeg);
		boolean hasPositiveDirectionNeighbor = ownerBag.map().containsKey(claimPos);
		
		if( !hasNegativeDirectionNeighbor && hasPositiveDirectionNeighbor ){
			return VANISH_POSITIVE_DIR;
		} else if( hasNegativeDirectionNeighbor && !hasPositiveDirectionNeighbor ){
			return VANISH_NEGATIVE_DIR;
		} else{
			return VANISH_NOT;
		}
	}
	
	/**
	 * <p>Returns a Duration constructed using the specified {@code time} 
	 * value.</p>
	 * 
	 * <p>Returned values are extracted from a HashMap cache and are added 
	 * to the cache if they are not already present.</p>
	 * 
	 * @param time the time for which a {@link Duration#toMillis() corresponding} 
	 * Duration is returned
	 * @return a Duration constructed using the specified {@code time} 
	 * value
	 */
	private static Duration durationFromTime(double time){
		if(!durationMap.containsKey(time)){
			durationMap.put(time, new Duration(time));
		}
		return durationMap.get(time);
	}
	
	private static final Map<Double,Duration> durationMap = new HashMap<>(13);
	
	@Override
	public int hashCode(){
		return Claim.linearizeCoords(x, y, z, puzzle.sideLength());
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof VoxelModel){
			VoxelModel vm = (VoxelModel)o;
			return puzzle == vm.puzzle && ownerBag == vm.ownerBag && x == vm.x && y == vm.y && z == vm.z && type == vm.type;
		}
		return false;
	}
	
	public enum Status{
		OCCUPIED, FALSIFIED, VANISHED;
	}
}
