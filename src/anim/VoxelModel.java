package anim;

import java.util.ArrayList;
import java.util.HashMap;
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
	public static final double UNOCCUPIED_FRACTION = 0.3;
	
	/**
	 * <p>The width, height, and depth of a voxel model ({@value}).</p>
	 */
	public static final double VOXEL_EDGE = 1.0;
	
	/**
	 * <p>Half of {@link #VOXEL_EDGE VOXEL_EDGE}, which is the distance 
	 * that a voxel model has to move in a given dimension while collapsing 
	 * along that dimension. Under that circumstance, a voxel model has to 
	 * translate by half its thickness in order to keep the ostensibly non-
	 * moving face fixed in space.</p>
	 */
	public static final double VOXEL_HALF = VOXEL_EDGE/2;
	
	/**
	 * <p>Thickness of a voxel model in its squished dimension after it has 
	 * been squished pursuant to its Claim being determined false.</p>
	 */
	public static final double COMPRESSED_THICKNESS = VOXEL_EDGE * UNOCCUPIED_FRACTION;
	
	private final Puzzle puzzle;
	private final int x;
	private final int y;
	private final int z;
	private final PuzzleVizApp.RegionSpecies type;
	private BagModel ownerBag;
	
	private Status status;
	
	/* 
	 * TODO check for and correct evacuation-translations that assume all VMs are centered on their true voxel 
	 * because end-cap VMs are not centered
	 */
	
	/* 
	 * TODO ensure consistent naming of VoxelModel shape-change methods: 
	 * use "falsified" for the initial squish that moves a Claim outside of its VoxelModel
	 * use "vanished" for the subsequent squish that flattens a VoxelModel so it has zero volume
	 */
	
	/**
	 * <p>Constructs a VoxelModel at the specified <tt>x</tt>,<tt>y</tt>,<tt>z</tt> coordinates in claim-space, with 
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
	 * @param xNeg the part of the width of this Box that is on the lower-valued side of the plane specified by <tt>x</tt>.
	 * @param xPos the part of the width of this Box that is on the higher-valued side of the plane specified by <tt>x</tt>.
	 * @param yNeg the part of the width of this Box that is on the lower-valued side of the plane specified by <tt>y</tt>.
	 * @param yPos the part of the width of this Box that is on the higher-valued side of the plane specified by <tt>y</tt>.
	 * @param zNeg the part of the width of this Box that is on the lower-valued side of the plane specified by <tt>z</tt>.
	 * @param zPos the part of the width of this Box that is on the higher-valued side of the plane specified by <tt>z</tt>.
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
	 * <p>Sets the <tt>ownerBag</tt> for this VoxelModel if it is not 
	 * already set <tt>ownerBag == null</tt>. Throws an exception if 
	 * called while <tt>ownerBag</tt> is already set.</p>
	 * @param bm the BagModel to be the new owner
	 * @throws IllegalStateException if <tt>ownerBag</tt> is already set
	 */
	void setOwnerBag(BagModel bm){
		if(ownerBag == null ){
			ownerBag = bm;
		} else{
			throw new IllegalStateException("Cannot set ownerBag because ownerBag has already been set.");
		}
	}
	
	public static final double COMPRESS_TRANSITION_TIME = 1000;
	
	/**
	 * <p>An empty array (varargs substitute) of KeyFrame, used to unambiguously 
	 * indicate the case where no KeyFrames are produced by an operation.</p>
	 * @see #disoccupy()
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
			return new KeyFrame[]{ new KeyFrame(durationFromTime(COMPRESS_TRANSITION_TIME+initTime), keyValuesFalsify()) };
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
		Duration periodEnd = durationFromTime(initTime+COMPRESS_TRANSITION_TIME);
		
		KeyFrame[] result = new KeyFrame[]{
				new KeyFrame(periodStart, keyValuesCurrentState()),
				new KeyFrame(periodEnd, (ae)->setVisible(false), keyValuesVanish())
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
	private KeyValue[] keyValuesCurrentState(){
		return new KeyValue[]{
				new KeyValue(widthProperty(), getWidth()),
				new KeyValue(heightProperty(), getHeight()),
				new KeyValue(depthProperty(), getDepth()),
				new KeyValue(translateXProperty(), getTranslateX()),
				new KeyValue(translateYProperty(), getTranslateY()),
				new KeyValue(translateZProperty(), getTranslateZ())
		};
	}
	
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
	 * @return a two-element array of KeyValues specifying the end-state 
	 * of the voxel model after it has squished in accordance with its 
	 * Claim being set false
	 */
	private KeyValue[] keyValuesFalsify(){
		DoubleProperty shrinkThicknessProperty = type.shrinkThicknessProperty(this);
		DoubleProperty shiftDimensionProperty = type.shiftProperty(this);
		
		KeyValue squish = new KeyValue(shrinkThicknessProperty, COMPRESSED_THICKNESS);
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
	private static final int CONTRACT_NEGATIVE = -1;
	
	/**
	 * <p>The sign of the collapse of a voxel model collapsing such that 
	 * the model's overall motion is in the negative direction along the 
	 * (or a) dimension along which it is collapsing.</p>
	 * @see #vanishSign(int,BagModel,Function)
	 * @see #vanishSigns()
	 */
	private static final int CONTRACT_POSITIVE = 1;
	
	/**
	 * <p>The sign of the collapse of a voxel model that is not collapsing 
	 * in the dimension in question.</p>
	 * @see #vanishSign(int,BagModel,Function)
	 * @see #vanishSigns()
	 */
	private static final int CONTRACT_NOT = 0;
	
	/**
	 * <p>Returns an array of KeyValues describing the state of this VoxelModel 
	 * after it has been contracted to zero size due to its Claim having been 
	 * set false.</p>
	 * @return an array of KeyValues describing the state of this VoxelModel 
	 * after it has collapsed due to its Claim having been set false
	 */
	private KeyValue[] keyValuesVanish(){
		int[] vanishSigns = vanishSigns();
		ArrayList<KeyValue> result = new ArrayList<>(6);
		
		for(Dimension dimension : Dimension.values()){
			if( vanishSigns[dimension.dimNo] != CONTRACT_NOT ){
				DoubleProperty thickness = dimension.thicknessProperty(this);
				DoubleProperty translation = dimension.translateProperty(this);
				result.add(new KeyValue(thickness, FLAT));
				result.add(new KeyValue(translation, translation.get() + vanishSigns[dimension.dimNo] * thickness.get()/2));
			}
		}
		
		return result.toArray(new KeyValue[0]);
	}
	
	/**
	 * <p></p>
	 * @author fiveham
	 *
	 */
	private enum Dimension{
		
		X(X_DIM, (vm) -> vm.widthProperty(),  (vm) -> vm.translateXProperty()), 
		Y(Y_DIM, (vm) -> vm.heightProperty(), (vm) -> vm.translateYProperty()), 
		Z(Z_DIM, (vm) -> vm.depthProperty(),  (vm) -> vm.translateZProperty());
		
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
	 * <p>The index of the {@link #vanishSigns() collapse-sign} for the x-dimension.</p>
	 */
	private static final int X_DIM = 0;
	
	/**
	 * <p>The index of the {@link #vanishSigns() collapse-sign} for the y-dimension.</p>
	 */
	private static final int Y_DIM = 1;
	
	/**
	 * <p>The index of the {@link #vanishSigns() collapse-sign} for the z-dimension.</p>
	 */
	private static final int Z_DIM = 2;
	
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
	 * <p>Returns an int ({@link #CONTRACT_NEGATIVE -1}, {@link #CONTRACT_NOT 0}, 
	 * or {@link #CONTRACT_POSITIVE 1} specifying the direction in which this 
	 * VoxelModel is collapsing.</p>
	 * 
	 * <p>If this model does not need to collapse when this method is called, 
	 * 0 is returned. If this model needs to collapse in the positive direction 
	 * on the dimension internally referenced by <tt>claimSrc</tt>, 1 is 
	 * returned. Otherwise, the model needs to collapse in the negative direction 
	 * on the dimension internally referenced by <tt>claimSrc.</tt>.</p>
	 * 
	 * @param dim a value 
	 * @param claimSrc
	 * @return an int ({@link #CONTRACT_NEGATIVE -1}, {@link #CONTRACT_NOT 0}, 
	 * or {@link #CONTRACT_POSITIVE 1} specifying the direction in which this 
	 * VoxelModel is collapsing
	 */
	private static int vanishSign(int dim, BagModel ownerBag, Function<Integer,Claim> thing){
		Claim claimNeg = thing.apply(dim-1);
		Claim claimPos = thing.apply(dim+1);
		
		boolean hasNegativeDirectionNeighbor = ownerBag.map().containsKey(claimNeg);
		boolean hasPositiveDirectionNeighbor = ownerBag.map().containsKey(claimPos);
		
		if( !hasNegativeDirectionNeighbor && hasPositiveDirectionNeighbor ){
			return CONTRACT_POSITIVE;
		} else if( hasNegativeDirectionNeighbor && !hasPositiveDirectionNeighbor ){
			return CONTRACT_NEGATIVE;
		} else{
			return CONTRACT_NOT;
		}
	}
	
	/**
	 * <p>Returns a Duration constructed using the specified <tt>time</tt> 
	 * value.</p>
	 * 
	 * <p>Returned values are extracted from a HashMap cache and are added 
	 * to the cache if they are not already present.</p>
	 * 
	 * @param time the time for which a {@link Duration#toMillis() corresponding} 
	 * Duration is returned
	 * @return a Duration constructed using the specified <tt>time</tt> 
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
			return puzzle == vm.puzzle && x == vm.x && y == vm.y && z == vm.z && type == vm.type && ownerBag == vm.ownerBag;
		}
		return false;
	}
	
	public enum Status{
		OCCUPIED, FALSIFIED, VANISHED;
	}
}
