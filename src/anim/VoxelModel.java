package anim;

import javafx.beans.property.DoubleProperty;
import javafx.scene.shape.Box;
import javafx.scene.transform.Translate;
import javafx.animation.KeyValue;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import sudoku.Claim;
import sudoku.Puzzle;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

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
	public static final double UNOCCUPIED_THICKNESS = VOXEL_EDGE * UNOCCUPIED_FRACTION;
	
	private final Puzzle puzzle;
	private final int x;
	private final int y;
	private final int z;
	private final PuzzleVizApp.RegionSpecies type;
	private BagModel ownerBag;
	
	/* 
	 * TODO check for and correct evacuation-translations that assume all VMs are centered on their true voxel 
	 * because end-cap VMs are not centered
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
		
		/*this.xNeg = xNeg;
		this.xPos = xPos;
		this.yNeg = yNeg;
		this.yPos = yPos;
		this.zNeg = zNeg;
		this.zPos = zPos;*/
		
		this.ownerBag = null;
		getTransforms().add(new Translate(x+(xPos-xNeg)/2, y+(yPos-yNeg)/2, z+(zPos-zNeg)/2));
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
	 * <p>Provides an array (suitable for use with varargs) of KeyValues specifying 
	 * the geometry and position of this VoxelModel at the time when this method 
	 * is called.</p>
	 * @return an array of KeyValues specifying the geometry and position of this 
	 * VoxelModel at the time when this method is called
	 */
	public KeyValue[] keyValuesCurrentState(){
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
	 * <p>Returns KeyValues describing the end-state of this VoxelModel's collapse into 
	 * oblivion.</p>
	 * @return an array of KeyValues describing the end-state of this VoxelModel's 
	 * collapse into oblivion
	 */
	public KeyFrame[] evacuate(double initTime){
		Duration periodStart = new Duration(initTime);
		Duration periodEnd = new Duration(initTime+TRANSITION_TIME_DISOCCUPY.toMillis());
		
		KeyFrame[] result = new KeyFrame[]{
				new KeyFrame(periodStart, keyValuesCurrentState()),
				new KeyFrame(periodEnd, (ae)->setVisible(false), keyValuesForEvac())
		};
		disown();
		return result;
	}
	
	/**
	 * <p>The factor ({@value}) by which to multiply the initial thickness of 
	 * a voxel model to obtain the final thickness of that voxel model when the 
	 * model is collapsing in the dimension in question. The thickness of a 
	 * voxel model is the height, depth, or width, depending on which dimension 
	 * is in question.</p>
	 * @see #SIGN_TO_THICKNESS
	 */
	public static final int FLAT = 0;
	
	/**
	 * </p>The factor ({@value}) by which to multiply the initial thickness of 
	 * a voxel model to obtain the final thickness of that voxel model when the 
	 * model is not collapsing in the dimension in question. The thickness of a 
	 * voxel model is the height, depth, or width, depending on which dimension 
	 * is in question.</p>
	 * @see #SIGN_TO_THICKNESS
	 */
	public static final int SAME_THICKNESS = 1;
	
	/**
	 * <p>The sign of the collapse of a voxel model collapsing such that 
	 * the model's overall motion is in the negative direction along the 
	 * (or a) dimension along which it is collapsing.</p>
	 * @see #collapseSign(int, ClaimSupplierByDimension)
	 * @see #collapseSigns()
	 */
	public static final int COLLAPSE_NEGATIVE = -1;
	
	/**
	 * <p>The sign of the collapse of a voxel model collapsing such that 
	 * the model's overall motion is in the negative direction along the 
	 * (or a) dimension along which it is collapsing.</p>
	 * @see #collapseSign(int, ClaimSupplierByDimension)
	 * @see #collapseSigns()
	 */
	public static final int COLLAPSE_POSITIVE = 1;
	
	/**
	 * <p>The sign of the collapse of a voxel model that is not collapsing 
	 * in the dimension in question.</p>
	 * @see #collapseSign(int, ClaimSupplierByDimension)
	 * @see #collapseSigns()
	 */
	public static final int NO_COLLAPSE = 0;
	
	/**
	 * <p>If this VoxelModel's {@link #collapseSigns() collapse-sign} in a given dimension 
	 * is non-zero, then the thickness of this VoxelModel in that dimension after this 
	 * VoxelModel has been evacuated will be zero. If this VoxelModel's collapse-sign 
	 * in a given dimension is zero, then the thickness of this VoxelModel in that 
	 * dimension after this VoxelModel has evacuated will be the same as it was before 
	 * the evacuation since it is not collapsing in that dimension.</p>
	 * 
	 * <p><tt>SIGN_TO_THICKNESS</tt> represents these facts by converting an input sign int, 
	 * -1, 0, or 1, into 0, 1, and 0 respectively.</p>
	 */
	public static final IntUnaryOperator SIGN_TO_THICKNESS = (i) -> i==NO_COLLAPSE ? SAME_THICKNESS : FLAT;
	
	/**
	 * <p>Returns an array of KeyValues describing the state of this VoxelModel 
	 * after it has collapsed due to its Claim having been set false.</p>
	 * @return an array of KeyValues describing the state of this VoxelModel 
	 * after it has collapsed due to its Claim having been set false
	 */
	private KeyValue[] keyValuesForEvac(){
		int[] collapseSigns = collapseSigns();
		return new KeyValue[]{
				new KeyValue(widthProperty(),      SIGN_TO_THICKNESS.applyAsInt(collapseSigns[X_DIM])*getWidth()),
				new KeyValue(heightProperty(),     SIGN_TO_THICKNESS.applyAsInt(collapseSigns[Y_DIM])*getHeight()),
				new KeyValue(depthProperty(),      SIGN_TO_THICKNESS.applyAsInt(collapseSigns[Z_DIM])*getDepth()),
				new KeyValue(translateXProperty(), getTranslateX() + collapseSigns[X_DIM]*getWidth()/2  /*VOXEL_HALF*/), 
				new KeyValue(translateYProperty(), getTranslateY() + collapseSigns[Y_DIM]*getHeight()/2 /*VOXEL_HALF*/), 
				new KeyValue(translateZProperty(), getTranslateZ() + collapseSigns[Z_DIM]*getDepth()/2  /*VOXEL_HALF*/)
		};
	}
	
	/**
	 * <p>The index of the {@link #collapseSigns() collapse-sign} for the x-dimension.</p>
	 */
	private static final int X_DIM = 0;
	
	/**
	 * <p>The index of the {@link #collapseSigns() collapse-sign} for the y-dimension.</p>
	 */
	private static final int Y_DIM = 1;
	
	/**
	 * <p>The index of the {@link #collapseSigns() collapse-sign} for the z-dimension.</p>
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
	private int[] collapseSigns(){
		return new int[]{
				collapseSign(x, ownerBag, (d)->puzzle.claims().get(d,y,z)),
				collapseSign(y, ownerBag, (d)->puzzle.claims().get(x,d,z)),
				collapseSign(z, ownerBag, (d)->puzzle.claims().get(x,y,d))
		};
	}
	
	/**
	 * <p>Returns an int ({@link #COLLAPSE_NEGATIVE -1}, {@link #NO_COLLAPSE 0}, 
	 * or {@link #COLLAPSE_POSITIVE 1} specifying the direction in which this 
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
	 * @return an int ({@link #COLLAPSE_NEGATIVE -1}, {@link #NO_COLLAPSE 0}, 
	 * or {@link #COLLAPSE_POSITIVE 1} specifying the direction in which this 
	 * VoxelModel is collapsing
	 */
	private static int collapseSign(int dim, BagModel ownerBag, Function<Integer,Claim> thing){
		Claim claimNeg = thing.apply(dim-1);
		Claim claimPos = thing.apply(dim+1);
		
		VoxelModel neighbVMInNegDir = ownerBag.mapGet(claimNeg);
		VoxelModel neighbVMInPosDir = ownerBag.mapGet(claimPos);
		
		if( neighbVMInNegDir==null && neighbVMInPosDir!=null ){
			return COLLAPSE_POSITIVE;
		} else if( neighbVMInNegDir!=null && neighbVMInPosDir==null ){
			return COLLAPSE_NEGATIVE;
		} else{
			return NO_COLLAPSE;
		}
	}
	
	/**
	 * <p>Removes this voxel model from its owner, marks this 
	 * voxel model as having no owner.</p>
	 * @see BagModel#notifyDisown(VoxelModel)
	 */
	private void disown(){
		ownerBag.notifyDisown(this);
		ownerBag = null;
	}
	
	/**
	 * <p>Provides an array of KeyFrames that detail the process of this VoxelModel 
	 * transforming from its initial shape and position to the shape and 
	 * position it must have in order to indicate that its associated Claim is 
	 * known false.</p>
	 * @return an array of KeyFrames that detail the process of this VoxelModel 
	 * transforming from its initial shape and position to the shape and 
	 * position it must have in order to indicate that its associated Claim is 
	 * known false
	 */
	public KeyFrame[] disoccupy(){
		if( ownerBag.notifyDisoccupied(this) ){
			return new KeyFrame[]{ new KeyFrame(TRANSITION_TIME_DISOCCUPY, keyValuesForDisoccupy()) };
		} else{
			return NO_KEYFRAMES;
		}
	}
	
	/**
	 * <p>An empty array (varargs substitute) of KeyFrame, used to unambiguously 
	 * indicate the case where no KeyFrames are produced by an operation.</p>
	 * @see #disoccupy()
	 */
	public static final KeyFrame[] NO_KEYFRAMES = {};
	
	/**
	 * <p>A radius of approximation used in determining whether a VoxelModel 
	 * is in contact with a given cube-face of the true voxel in which it lies.</p>
	 * 
	 * <p>Floating-point values have finite resolution; so, in order to accommodate 
	 * the possibility of arbitrary non-integer 
	 * {@link #VOXEL_EDGE edge-lengths for true voxels}, the geometric test used to 
	 * establish that one face of a voxel model lies on the face of its host true 
	 * voxel needs to be approximative instead of relying specifically on true 
	 * equality.</p>
	 * @see #touchesFace()
	 */
	public static final double ROUNDING_ERROR = 0.1;
	
	/**
	 * <p>Returns true if this VoxelModel is in contact with the cube-face of 
	 * the true voxel in which it lies.</p>
	 * 
	 * <p>Floating-point values have finite resolution; so, in order to accommodate 
	 * the possibility of arbitrary non-integer 
	 * {@link #VOXEL_EDGE edge-lengths for true voxels}, the geometric test used to 
	 * establish that A face of a voxel model lies on the face of its host true 
	 * voxel needs to be {@link #ROUNDING_ERROR approximative} instead of relying 
	 * specifically on true equality.</p>
	 * @param face
	 * @see Face
	 * @return true if this VoxelModel is in contact with the cube-face of 
	 * the true voxel in which it lies
	 */
	public boolean touchesFace(Face face){
		int sign = face.direction.sign;
		BiPredicate<Double,Double> compare = face.direction.compare;
		
		double translate = face.dimension.translate(this);
		double thickness = face.dimension.thickness(this);
		int dim = face.dimension.dimension(this);
		
		return compare.test(translate + sign*thickness/2, dim + sign*(VOXEL_HALF - ROUNDING_ERROR));
	}
	
	/**
	 * <p>A face of a cubic voxel.</p>
	 * <p>Pairs a {@link Dimension Dimension} with a 
	 * {@link Direction Direction}</p>
	 * @author fiveham
	 *
	 */
	public static enum Face{
		
		/**
		 * <p>The top face of a cubic voxel.</p>
		 */
		Z_POS(Dimension.Z, Direction.UP), 
		
		/**
		 * <p>The bottom face of a cubic voxel.</p>
		 */
		Z_NEG(Dimension.Z, Direction.DOWN), 
		
		/**
		 * <p>The face of a cubic voxel perpendicular to the y-axis located 
		 * at a y-value higher than that of the other face perpendicular to 
		 * the y-axis.</p>
		 */
		Y_POS(Dimension.Y, Direction.UP), 
		
		/**
		 * <p>The face of a cubic voxel perpendicular to the y-axis located 
		 * at a y-value lower than that of the other face perpendicular to 
		 * the y-axis.</p>
		 */
		Y_NEG(Dimension.Y, Direction.DOWN), 
		
		/**
		 * <p>The face of a cubic voxel perpendicular to the x-axis located 
		 * at a x-value higher than that of the other face perpendicular to 
		 * the x-axis.</p>
		 */
		X_POS(Dimension.X, Direction.UP), 
		
		/**
		 * <p>The face of a cubic voxel perpendicular to the x-axis located 
		 * at a x-value lower than that of the other face perpendicular to 
		 * the x-axis.</p>
		 */
		X_NEG(Dimension.X, Direction.DOWN);
		
		private final Dimension dimension;
		private final Direction direction;
		
		private Face(Dimension dimension, Direction direction){
			this.dimension = dimension;
			this.direction = direction;
		}
		
		/**
		 * <p>The Dimension of this Face.</p>
		 * @return the Dimension of this Face
		 */
		public Dimension dimension(){
			return dimension;
		}
		
		/**
		 * <p>The Direction of this Face.</p>
		 * @return the Direction of this Face
		 */
		public Direction direction(){
			return direction;
		}
	}
	
	/**
	 * <p>Represents a dimension in claim-space. Groups a translation 
	 * function, a thickness function, and a dimension-access function 
	 * together.</p>
	 * 
	 * <p>Each Dimension specifies a function to extract the dimensionally-
	 * pertinent translation, thickness, and position component.</p>
	 * @see Face
	 * @author fiveham
	 *
	 */
	public static enum Dimension{
		
		/**
		 * <p>The x-dimension.</p>
		 */
		X((vm) -> vm.getTranslateX(), (vm) -> vm.getWidth(),  (vm) -> vm.x), 
		
		/**
		 * <p>The y-dimension.</p>
		 */
		Y((vm) -> vm.getTranslateY(), (vm) -> vm.getHeight(), (vm) -> vm.y), 
		
		/**
		 * <p>The z-dimension.</p>
		 */
		Z((vm) -> vm.getTranslateZ(), (vm) -> vm.getDepth(),  (vm) -> vm.z);
		
		private final Function<VoxelModel,Double> translate;
		private final Function<VoxelModel,Double> thickness;
		private final Function<VoxelModel,Integer> dim;
		
		private Dimension(Function<VoxelModel,Double> translate, Function<VoxelModel,Double> dimThickness, Function<VoxelModel,Integer>  dim){
			this.translate = translate;
			this.thickness = dimThickness;
			this.dim = dim;
		}
		
		/**
		 * <p>Returns the specified VoxelModel's translation term pertaining 
		 * to this dimension.</p>
		 * @param vm the voxel model whose pertinent data is returned
		 * @return the specified VoxelModel's translation term pertaining 
		 * to this dimension
		 */
		public double translate(VoxelModel vm){
			return translate.apply(vm);
		}
		
		/**
		 * <p>Returns the specified VoxelModel's thickness term pertaining 
		 * to this dimension.</p>
		 * @param vm the voxel model whose pertinent data is returned
		 * @return the specified VoxelModel's thickness term pertaining 
		 * to this dimension
		 */
		public double thickness(VoxelModel vm){
			return thickness.apply(vm);
		}
		
		/**
		 * <p>Returns the specified VoxelModel's position component 
		 * pertaining to this dimension.</p>
		 * @param vm the voxel model whose pertinent data is returned
		 * @return the specified VoxelModel's position component 
		 * pertaining to this dimension
		 */
		public int dimension(VoxelModel vm){
			return dim.apply(vm);
		}
	}
	
	/**
	 * <p>Outputs true if the first argument is greater than the second, 
	 * false otherwise.</p>
	 */
	public static final BiPredicate<Double,Double> GREATER = (a,b)->a>b;
	
	/**
	 * <p>Outputs true if the first argument is less than the second, 
	 * false otherwise.</p>
	 */
	public static final BiPredicate<Double,Double> LESS = (a,b)->a<b;
	
	/**
	 * <p>A direction on a dimension or number-line.</p>
	 * @see Face
	 * @author fiveham
	 *
	 */
	public static enum Direction{
		
		/**
		 * <p>The direction tending toward larger positive numbers.</p>
		 * <p>Has a positive {@link #sign() sign}. Its {@link #compare(double,double) comparison} 
		 * operation is <tt>a > b</tt>.</p>
		 */
		UP(1,  GREATER), 
		
		/**
		 * <p>The direction tending toward larger negative numbers.</p>
		 * <p>Has a negative {@link #sign() sign}. Its {@link #compare(double,double) comparison} 
		 * operation is <tt>a < b</tt>.</p>
		 */
		DOWN(-1, LESS);
		
		private final int sign;
		private final BiPredicate<Double,Double> compare;
		
		private Direction(int sign, BiPredicate<Double,Double> compare){
			this.sign = sign;
			this.compare = compare;
		}
		
		/**
		 * <p>Returns the sign for this direction.</p>
		 */
		public int sign(){
			return sign;
		}
		
		/**
		 * <p>Applies this direction's number-comparison operation to 
		 * <tt>a</tt> and <tt>b</tt>.</p>
		 * @return this direction's number-comparison operation applied 
		 * to <tt>a</tt> and <tt>b</tt>
		 */
		public boolean compare(double a, double b){
			return compare.test(a, b);
		}
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
	private KeyValue[] keyValuesForDisoccupy(){
		DoubleProperty shrinkThicknessProperty = type.shrinkThicknessProperty(this);
		DoubleProperty shiftDimensionProperty = type.shiftProperty(this);
		
		KeyValue squish = new KeyValue(shrinkThicknessProperty, UNOCCUPIED_THICKNESS);
		KeyValue shift = new KeyValue(shiftDimensionProperty, shrinkThicknessProperty.get()/2);
		
		return new KeyValue[]{squish, shift};
	}
	
	/**
	 * <p>The duration of the animation for the transition of a voxel 
	 * model from occupied to unoccupied (when a Claim is 
	 * {@link Claim#setFalse() set false}). Equal to 1000 milliseconds.</p>
	 */
	public static final Duration TRANSITION_TIME_DISOCCUPY = new Duration(1000);
	
	/**
	 * <p>Sets the <tt>ownerBag</tt> for this VoxelModel if it is not 
	 * already set <tt>ownerBag == null</tt>. Throws an exception if 
	 * called while <tt>ownerBag</tt> is already set.</p>
	 * @param bm the BagModel to be the new owner
	 * @throws IllegalStateException if <tt>ownerBag</tt> is already set
	 */
	public void setOwnerBag(BagModel bm){
		if(ownerBag == null ){
			ownerBag = bm;
		} else{
			throw new IllegalStateException("Cannot set ownerBag because ownerBag has already been set.");
		}
	}
	
	/**
	 * <p>Returns the BagModel of which this VoxelModel is 
	 * a part.</p>
	 * @return the BagModel of which this VoxelModel is 
	 * a part
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
	public int x(){
		return x;
	}
	
	/**
	 * <p>The wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space.</p>
	 * @return the wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space
	 */
	public int y(){
		return y;
	}
	
	/**
	 * <p>The wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space.</p>
	 * @return the wrapped int of the {@link Claim#getY() y-component} 
	 * of this VoxelModel's Claim's position in claim-space
	 */
	public int z(){
		return z;
	}
}
