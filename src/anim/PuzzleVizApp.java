package anim;

import common.Pair;
import common.time.Time;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.DoubleProperty;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import sudoku.Claim;
import sudoku.Puzzle;
import sudoku.Puzzle.IndexInstance;
import sudoku.time.FalsifiedTime;
import sudoku.time.ThreadEvent;
import sudoku.Solver;

public class PuzzleVizApp extends Application {
	
	public PuzzleVizApp() {
	}
	
	public static void main(String[] args) {
		if(args.length > 0){
			launch(args); //calls start(Stage)
		} else{
			System.out.println("Usage: java PuzzleVizApp target-sourcefile-name");
			System.exit(1);
		}
    }
	
	/**
	 * <p>Creates and solves the sudoku puzzle specified in the file named by 
	 * the first {@link #getParameters() command-line argument} sent to 
	 * {@link main(String[]) main()}, which precipitated the call to this 
	 * method, extracts the root of the thread-termination time-tree, 
	 * {@link Stage#setScene(Scene) sets the Scene}, generates 
	 * {@code Timeline}s that animate the solution process, and plays them.</p>
	 */
	@Override
	public void start(Stage primaryStage) throws Exception{
		primaryStage.setResizable(false);
		
		Puzzle puzzle;
		ThreadEvent timeRoot;
		{
			List<String> args = getParameters().getRaw();
	        puzzle = new Puzzle(new File(args.get(0)));
	        
	        Solver solver = new Solver(puzzle);
	        solver.solve();
	        
			timeRoot = solver.getEvent();
		}
		
		Scene scene;
		Group voxelModels;
		{
			Pair<Parent,Group> createdContent = createContent(puzzle);
			scene = new Scene(createdContent.getA());
			voxelModels = createdContent.getB();
		}
		primaryStage.setScene(scene);
		
        primaryStage.show();
        
        genTimeline(voxelModels, puzzle, timeRoot).play();
	}
	
	/**
	 * <p>Creates the scene graph for this application and returns a Pair 
	 * containing the scene graph as the first element and the Group that 
	 * contains the voxel models as the second element.</p>
	 * 
	 * <p>Access to the voxel model Group is provided as an independent 
	 * result even though it is contained in the overall scene graph 
	 * because it is more efficient than requiring 
	 * {@link #start(Stage) the calling code} to extract the voxel model 
	 * Group out of the overall scene graph.</p>
	 * 
	 * @param puzzle the Puzzle whose solution process is being animated
	 * @return a Pair containing the scene graph as the first element and 
	 * the Group that contains the voxel models as the second element
	 */
	private static Pair<Parent,Group> createContent(Puzzle puzzle){
		Camera camera = genCamera();
		Group voxelModels = genVoxelModels(puzzle);
		
        Parent contentParent = genContentParent(
				genSubScene(
						camera, 
						genRoot(
								genCameraGroup(camera),
								voxelModels,
								genClaimsGroup(puzzle))));
        
        return new Pair<Parent,Group>(contentParent, voxelModels);
	}
	
	private static Camera genCamera(){
		Camera result = new PerspectiveCamera(true);
		result.getTransforms().add(new Translate(0,0,-15));
		return result;
	}
	
	/**
	 * <p>Creates all the voxel models for the claims of {@code target}, sequenced so that first quarter 
	 * of the list contains voxel models for all the claims of the target in order of increasing 
	 * {@link Claim#linearizeCoords(int, int, int, int) linearized coordinates}. The voxel models in the 
	 * first fourth of the list all pertain to the same {@link #RegionSpecies type} of Rule.  Each of 
	 * the other three fourths pertains to another {@code type} so that all voxel models for all claims are 
	 * represented.
	 * Ordering the 
	 * @param target the Puzzle whose solution process is being animated
	 * @return a {@link Group Group} encapsulating the voxel models for the Claims of {@code target}
	 * @see #associates(Claim, List)
	 */
	private static Group genVoxelModels(Puzzle puzzle){
		Group result = new Group();
		for(RuleType region : RuleType.values()){
			for(int x=0; x<puzzle.sideLength(); ++x){
				for(int y=0; y<puzzle.sideLength(); ++y){
					for(int z=0; z<puzzle.sideLength(); ++z){
						result.getChildren().add( region.newVoxelModel(puzzle, x,y,z) );
					}
				}
			}
		}
		
		genBagModels(result.getChildren(), puzzle);
		
		return result;
	}
	
	/**
	 * <p>Creates the {@code BagModel}s for the solved Puzzle, 
	 * and gives each VoxelModel belonging to each BagModel a 
	 * {@link VoxelModel#getOwnerBag() reference} to that 
	 * BagModel.</p>
	 * 
	 * <p>This method does not return a value and does not add 
	 * an element to the scene graph because a BagModel's 
	 * representation in the scene is exactly the representation 
	 * of its voxel models; as such, any extant BagModel is 
	 * already represented properly in the scene.</p>
	 * 
	 * <p>Access to the BagModels is only ever needed through 
	 * their VoxelModels. Even the one context that calls this method 
	 * doesn't need access to any of the BagModels once they've 
	 * been assigned all their VoxelModels.</p>
	 * 
	 * @param voxels a List of all the VoxelModels (a List<Node> 
	 * in the calling context, but all the child nodes listed 
	 * are necessarily VoxelModels in that context) needed to 
	 * animate the Puzzle {@code p}
	 * @param p the Puzzle whose solution process is being animated
	 */
	private static void genBagModels(List<? super VoxelModel> voxels, Puzzle p){
		int claimCount = (int)Math.pow(p.sideLength(), Puzzle.DIMENSION_COUNT);
		for(RuleType reg : RuleType.values()){
			
			Puzzle.RuleType region = reg.pertainsTo;
			int offset = reg.ordinal() * claimCount;
			
			for(IndexInstance dimA : region.dimA(p)){
				
				for(IndexInstance dimB : region.dimB(p)){
					List<VoxelModel> vmList = new ArrayList<>(p.sideLength());
					
					for(IndexInstance dimC : region.dimInsideRule(p)){
						Puzzle.IndexValue[] i = p.decodeXYZ(dimA, dimB, dimC);
						
						int x = i[Puzzle.X_DIM].intValue();
						int y = i[Puzzle.Y_DIM].intValue();
						int z = i[Puzzle.Z_DIM].intValue();
						int index = Claim.linearizeCoords(x, y, z, p.sideLength()) + offset;
						vmList.add( (VoxelModel) voxels.get(index) );
					}
					new BagModel(p, vmList, reg.bagColor);
				}
			}
		}
	}
	
	public static final double SMALL_HALFEDGE = 0.3;
	
	/**
	 * <p>The distance ({@value}) from the center of a VoxelModel's true voxel to a 
	 * face of the VoxelModel that faces another VoxelModel belonging to the same 
	 * BagModel.</p>
	 */
	public static final double LONG_HALFEDGE = 0.5;
	
	/**
	 * <p>The distance ({@value}) from the center of a VoxelModel's true voxel to a 
	 * face of the VoxelModel that does not face another VoxelModel belonging to the 
	 * same BagModel.</p>
	 */
	public static final double MED_HALFEDGE = (SMALL_HALFEDGE + LONG_HALFEDGE)/2;
	
	private static final RuleType.DimensionSelector SELECT_X = (x,y,z) -> x;
	private static final RuleType.DimensionSelector SELECT_Y = (x,y,z) -> y;
	private static final RuleType.DimensionSelector SELECT_Z = (x,y,z) -> z;
	private static final RuleType.DimensionSelector NO_SELECTION = (x,y,z) -> 0;
	
	private static final Function<int[],Double> MED  = (i) -> MED_HALFEDGE;
	private static final Function<int[],Double> L0ML = (i) -> i[0]<0?MED_HALFEDGE:LONG_HALFEDGE;
	private static final Function<int[],Double> G0ML = (i) -> i[0]>0?MED_HALFEDGE:LONG_HALFEDGE;
	private static final Function<int[],Double> L1ML = (i) -> i[1]<0?MED_HALFEDGE:LONG_HALFEDGE;
	private static final Function<int[],Double> G1ML = (i) -> i[1]>0?MED_HALFEDGE:LONG_HALFEDGE;
	
	private static final Function<Puzzle,Integer> EDGE_BOX = (p) -> p.magnitude()-1;
	private static final Function<Puzzle,Integer> EDGE_LIN = (p) -> p.sideLength()-1;
	
	private static final BiFunction<Puzzle,Integer,Integer> DIM_IN_BOX = (p,i) -> i%p.magnitude();
	private static final BiFunction<Puzzle,Integer,Integer> DIM_IN_LIN = (p,i) -> i;
	
	private static final double BAGMODEL_OPACITY = 0.3;
	private static final PhongMaterial CELL_BLUE    = new PhongMaterial(Color.web("blue",BAGMODEL_OPACITY));
	private static final PhongMaterial COLUMN_GREEN = new PhongMaterial(Color.web("green",BAGMODEL_OPACITY));
	private static final PhongMaterial ROW_RED      = new PhongMaterial(Color.web("red",BAGMODEL_OPACITY));
	private static final PhongMaterial BOX_YELLOW   = new PhongMaterial(Color.web("yellow",BAGMODEL_OPACITY));
	
	/**
	 * <p>A mapping from each type of Rule to several pieces of information 
	 * pertinent to that type of Rule in the context of animating the 
	 * process of solving a Puzzle.</p>
	 * 
	 * <p>The pieces of information mapped from each region type are 
	 * <ul>
	 * <li>pertinent {@link Puzzle.RuleType Puzzle.RegionSpecies}</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the lower-X face of that VoxelModel</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the higher-X face of that VoxelModel</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the lower-Y face of that VoxelModel</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the higher-Y face of that VoxelModel</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the lower-Z face of that VoxelModel</li>
	 * 
	 * <li>a function that takes a {@link RuleType#getSigns(Puzzle,int,int,int) pair of signs} and outputs 
	 * the distance from the center of a VoxelModel having those signs to 
	 * the higher-Z face of that VoxelModel</li>
	 * 
	 * <li>a function accepting a Puzzle and outputing the maximum value that a 
	 * certain dimension withing the pertinent region type can have, to be used 
	 * in computing the signs passed to the six functions described above: 
	 * if a VoxelModel's (adjusted) position equals the value output by this 
	 * function, it is on the far right edge of its BagModel, in whatever dimension 
	 * was tested, in which case, that VoxelModel's face pointing in that direction 
	 * is slightly closer to the center of the VoxelModel's true voxel</li>
	 * 
	 * <li>a function accepting a Puzzle and an int indicating a position along 
	 * a dimension in the Puzzle and outputting an adjusted value of the input 
	 * int that stays within the bounds implicitly incorporated in that function: 
	 * For linear Rules/BagModels, the input int is output, but for Box types, the 
	 * input int must be modded by the Puzzle's magnitude</li>
	 * 
	 * <li>the first physical dimension pertinent to this type of region</li>
	 * 
	 * <li>the second physical dimension pertinent to this type of region</li>
	 * 
	 * <li>the PhongMaterial color for the VoxelModels of BagModels of this type</li>
	 * 
	 * <li>a reference to the dimension of a VoxelModel belonging to a BagModel of 
	 * this type along which the VoxelModel compresses when its Claim is discovered 
	 * to be false</li>
	 * 
	 * <li>a reference to the component of the position of a VoxelModel belonging to 
	 * a BagModel of this type that needs to shift as this VoxelModel compresses once 
	 * its Claim is discovered to be false in order to keep the non-moving face stationary</li>
	 * </ul></p>
	 * @see Puzzle.RuleType
	 * @author fiveham
	 *
	 */
	public static enum RuleType{
		CELL  (Puzzle.RuleType.CELL,   MED,  MED,  MED,  MED,  L0ML, G0ML, EDGE_LIN, DIM_IN_LIN, SELECT_Z, NO_SELECTION, CELL_BLUE,    VoxelModel::widthProperty, VoxelModel::translateXProperty), 
		COLUMN(Puzzle.RuleType.COLUMN, MED,  MED,  L0ML, G0ML, MED,  MED,  EDGE_LIN, DIM_IN_LIN, SELECT_X, NO_SELECTION, COLUMN_GREEN, VoxelModel::depthProperty, VoxelModel::translateZProperty), 
		ROW   (Puzzle.RuleType.ROW,    L0ML, G0ML, MED,  MED,  MED,  MED,  EDGE_LIN, DIM_IN_LIN, SELECT_Y, NO_SELECTION, ROW_RED,      VoxelModel::depthProperty, VoxelModel::translateZProperty), 
		BOX   (Puzzle.RuleType.BOX,    L0ML, G0ML, L1ML, G1ML, MED,  MED,  EDGE_BOX, DIM_IN_BOX, SELECT_X, SELECT_Y,     BOX_YELLOW,   VoxelModel::depthProperty, VoxelModel::translateZProperty);
		
		private final Puzzle.RuleType pertainsTo;
		private final Function<int[],Double> xNeg;
		private final Function<int[],Double> xPos;
		private final Function<int[],Double> yNeg;
		private final Function<int[],Double> yPos;
		private final Function<int[],Double> zNeg;
		private final Function<int[],Double> zPos;
		private final Function<Puzzle,Integer> farEdgeForType;
		private final BiFunction<Puzzle,Integer,Integer> dimInReg;
		private final DimensionSelector dim0;
		private final DimensionSelector dim1;
		private final PhongMaterial bagColor;
		private final Function<VoxelModel,DoubleProperty> thicknessProperty;
		private final Function<VoxelModel,DoubleProperty> shiftProperty;
		
		private RuleType(Puzzle.RuleType type, 
				Function<int[],Double> xNeg, Function<int[],Double> xPos, 
				Function<int[],Double> yNeg, Function<int[],Double> yPos, 
				Function<int[],Double> zNeg, Function<int[],Double> zPos, 
				Function<Puzzle,Integer> farEdgeForType, BiFunction<Puzzle,Integer,Integer> dimInReg, 
				DimensionSelector dim0, DimensionSelector dim1,
				PhongMaterial bagColor, 
				Function<VoxelModel,DoubleProperty> thicknessProperty, 
				Function<VoxelModel,DoubleProperty> shiftProperty){
			this.pertainsTo = type;
			this.xNeg = xNeg;
			this.xPos = xPos;
			this.yNeg = yNeg;
			this.yPos = yPos;
			this.zNeg = zNeg;
			this.zPos = zPos;
			this.farEdgeForType = farEdgeForType;
			this.dimInReg = dimInReg;
			this.dim0 = dim0;
			this.dim1 = dim1;
			this.bagColor = bagColor;
			this.thicknessProperty = thicknessProperty;
			this.shiftProperty = shiftProperty;
		}
		
		public DoubleProperty shiftProperty(VoxelModel vm){
			return shiftProperty.apply(vm);
		}
		
		public DoubleProperty shrinkThicknessProperty(VoxelModel vm){
			return thicknessProperty.apply(vm);
		}
		
		public VoxelModel newVoxelModel(Puzzle p, int x, int y, int z){
			int[] signs = getSigns(p, x,y,z);
			return new VoxelModel(p, x,y,z, this, 
					xNeg.apply(signs), 
					xPos.apply(signs), 
					yNeg.apply(signs), 
					yPos.apply(signs), 
					zNeg.apply(signs), 
					zPos.apply(signs));
		}
		
		private int[] getSigns(Puzzle p, int x, int y, int z){
			return new int[]{
					edgeSign(p, dimInReg.apply(p, dim0.select(x, y, z))), 
					edgeSign(p, dimInReg.apply(p, dim1.select(x, y, z)))
			};
		}
		
		/**
		 * <p>Returns -1, 1, or 0 if {@code dimensionValue} is 0, 
		 * {@code dimValueWhenPos}, or between 0 and {@code dimValueWhenPos} 
		 * respectively.</p>
		 * 
		 * <p>0 is returned when the voxel whose location within its cell, 
		 * box, row, or column or represented by {@code dimensionValue} 
		 * is not an edge position. For cells, rows, and columns, edge voxels 
		 * are on the edge of the typically 9x9x9 cube; for boxes, edge voxels 
		 * are on the borders between boxes.</p>
		 * 
		 * @param p the Puzzle whose solution process is being animated
		 * @param xyz an adjusted component of a VoxelModel's Claim's position 
		 * in physical space
		 * @return -1 if the {@code xyz} is 0, indicating the VoxelModel 
		 * in question is on the lower-valued end of the pertinent physical 
		 * dimension, 1 if {@code xyz} equals the value output by 
		 * {@code farEdgeForType} when passed {@code p} as a parameter, or 
		 * 0 otherwise
		 */
		private int edgeSign(Puzzle p, int xyz){
			return (xyz==0) ? -1 : xyz==farEdgeForType.apply(p) ? 1 : 0;
		}
		
		/**
		 * <p>Selects and returns one of the three dimensions specified 
		 * as parameters, or returns a dummy value to choose not to 
		 * make a selection.</p>
		 * @author fiveham
		 *
		 */
		@FunctionalInterface
		private static interface DimensionSelector{
			int select(int x, int y, int z);
		}
	}
	
	private static Group genContentParent(SubScene subScene){
		Group contentParent = new Group();
        contentParent.getChildren().add(subScene);
        return contentParent;
	}
	
	private static SubScene genSubScene(Camera camera, Group root){
		SubScene subScene = new SubScene(root, 300,300);
        subScene.setFill(Color.WHITE);
        subScene.setCamera(camera);
        return subScene;
	}
	
	private static Group genRoot(Node... subGroups){
		Group root = new Group();
        root.getChildren().addAll(subGroups);
        return root;
	}
	
	private static Group genCameraGroup(Camera camera){
		Group cameraGroup = new Group();
		cameraGroup.getChildren().add(camera);
		return cameraGroup;
	}
	
	public static final double CLAIM_SPHERE_RADIUS = 0.1;
	
	private static Group genClaimsGroup(Puzzle puzzle){
		Group result = new Group();
		for(int x=0; x<puzzle.sideLength(); ++x){
			for(int y=0; y<puzzle.sideLength(); ++y){
				for(int z=0; z<puzzle.sideLength(); ++z){
					Sphere claim = new Sphere(CLAIM_SPHERE_RADIUS);
					claim.getTransforms().add(new Translate(x,y,z));
					result.getChildren().add( claim );
				}
			}
		}
		return result;
	}
	
	/**
	 * <p>Wraps {@code parallelTimeline()}</p>
	 * @param voxelModels
	 * @param puzzle
	 * @return
	 */
	public static Timeline genTimeline(Group voxelModels, Puzzle puzzle, ThreadEvent timeRoot){
		return parallelTimeline(timeRoot, genModelHandler(voxelModels));
		//return depthFirstLinearTimeline(timeRoot, genModelHandler(voxelModels));
		//return breadthFirstLinearTimeline(timeRoot, genModelHandler(voxelModels));
	}
	
	/**
	 * <p>Returns a Timeline which, when it finishes playing, plays 
	 * Timelines for each of the events that occured as a result of 
	 * the event modeled by that Timeline. Each Timeline played in 
	 * such a fashion exhibits the same child-producing behavior.</p>
	 * 
	 * <p>When one of these Timelines finishes playing, the child 
	 * Timelines are produced at that time rather than in advance, by 
	 * a call to this method.</p>
	 * @param event
	 * @param voxelModels
	 * @param puzzle
	 * @param modelHandler
	 * @return
	 */
	public static Timeline parallelTimeline(ThreadEvent event, Map<Claim,List<VoxelModel>> modelHandler){
		Timeline result = solutionEventTimeline(event.techniqueEvent(), modelHandler);
		
		result.setOnFinished((ae) -> event.children().parallelStream()
				.filter(ThreadEvent.class::isInstance)
				.forEach((ct) -> parallelTimeline((ThreadEvent)ct, modelHandler).play()));
		
		return result;
	}
	
	public static Timeline depthFirstLinearTimeline(ThreadEvent event, Map<Claim,List<VoxelModel>> modelHandler){
		ArrayList<Timeline> timelineList = new ArrayList<>(treeSize(event));
		timelineList.add(solutionEventTimeline(event.techniqueEvent(), modelHandler));
		
		for(ThreadEvent child : threadEventChildren(event)){
			depthFirstRecursion(timelineList, child, modelHandler);
		}
		
		return stitch(timelineList);
	}
	
	private static int treeSize(Time time){ //TODO redo the tree-size calculation thing in a way that makes sense in context
		AtomicInteger ai = new AtomicInteger(1);
		if(time.hasChildren()){
			treeSize(time, ai);
		}
		return ai.get();
	}
	
	private static void treeSize(Time time, AtomicInteger ai){
		ai.addAndGet(time.children().size());
		time.children().parallelStream().filter(Time::hasChildren).forEach((child) -> treeSize(child,ai));
	}
	
	private static List<Timeline> depthFirstRecursion( List<Timeline> timelineList, ThreadEvent event, Map<Claim,List<VoxelModel>> modelHandler){
		timelineList.add(solutionEventTimeline(event.techniqueEvent(), modelHandler));
		
		for(ThreadEvent child : threadEventChildren(event)){
			timelineList.addAll(depthFirstRecursion(timelineList, child, modelHandler));
		}
		
		return timelineList;
	}
	
	public static Timeline breadthFirstLinearTimeline(ThreadEvent event, Map<Claim,List<VoxelModel>> modelHandler){
		Iterator<ThreadEvent> layerIterator = breadthFirstLinearizeTime(event);
		Timeline result = solutionEventTimeline(layerIterator.next().techniqueEvent(), modelHandler);
		
		Timeline earlier = result;
		while(layerIterator.hasNext()){
			Timeline later = solutionEventTimeline(layerIterator.next().techniqueEvent(), modelHandler);
			earlier.setOnFinished((ae) -> later.play());
			earlier = later;
		}
		
		return result;
	}
	
	private static Iterator<ThreadEvent> breadthFirstLinearizeTime(ThreadEvent event){
		List<ThreadEvent> snake = new ArrayList<>();
		
		for(List<ThreadEvent> layer; !(layer = Collections.singletonList(event)).isEmpty();){
			snake.addAll(layer);
			layer = nextLayer(layer);
		}
		
		return snake.iterator();
	}
	
	private static List<ThreadEvent> nextLayer(List<ThreadEvent> layer){
		List<ThreadEvent> result = new ArrayList<>();
		
		for(ThreadEvent event : layer){
			result.addAll(event.children().stream().filter(ThreadEvent.class::isInstance).map(ThreadEvent.class::cast).collect(Collectors.toList()));
		}
		
		return result;
	}
	
	/**
	 * <p>Produces a Timeline animating all the Claim-falsification and BagModel 
	 * contraction events that occured as a direct or indirect part of the 
	 * specified {@code event}.</p>
	 * @param event
	 * @param modelHandler
	 * @return
	 */
	public static Timeline solutionEventTimeline(FalsifiedTime event, Map<Claim,List<VoxelModel>> modelHandler){
		Timeline result = new Timeline();
		
		//add content for direct SolutionEvent
		addFalsificationAnimation(result, event.falsified(), modelHandler);
		
		//add content for precipitated AutoResolve events
		addAutoResolveContent(result, falsifiedTimeChildren(event), modelHandler);
		
		return result;
	}
	
	private static List<FalsifiedTime> falsifiedTimeChildren(Time time){
		return time.children().stream().filter(FalsifiedTime.class::isInstance).map(FalsifiedTime.class::cast).collect(Collectors.toList());
	}
	
	private static List<ThreadEvent> threadEventChildren(Time time){
		return time.children().stream().filter(ThreadEvent.class::isInstance).map(ThreadEvent.class::cast).collect(Collectors.toList());
	}
	
	/**
	 * <p>Adds to {@code timeline} the falsification animation (VoxleModel collapse and 
	 * BagModel contraction) an {@link FalsifiedTime event} whose {@code falsified} Claims 
	 * are specified.</p>
	 * @param timeline
	 * @param falsified
	 * @param modelHandler
	 */
	public static void addFalsificationAnimation(final Timeline timeline, Set<Claim> falsified, Map<Claim,List<VoxelModel>> modelHandler){
		double initLength = timeline.totalDurationProperty().get().toMillis();
		for(Claim c : falsified){
			for(VoxelModel vm : modelHandler.get(c)){
				timeline.getKeyFrames().addAll(vm.falsify( initLength ));
			}
		}
		
		Set<BagModel> affectedBags = affectedBags(falsified, modelHandler);
		double postDisoccupyLength = timeline.totalDurationProperty().get().toMillis();
		for(BagModel bag : affectedBags){
			bag.trimUnoccupiedExtremeVoxels(timeline, postDisoccupyLength);
		}
	}
	
	private static Set<BagModel> affectedBags(Collection<Claim> falseClaims, Map<Claim,List<VoxelModel>> modelHandler){
		Set<VoxelModel> affectedVoxels = new HashSet<>();
		for(Claim c : falseClaims){
			affectedVoxels.addAll(modelHandler.get(c));
		}
		
		Set<BagModel> affectedBags = new HashSet<>(); //bagmodels affected by the mass disoccupation that just occurred.
		for(VoxelModel vm : affectedVoxels){
			affectedBags.add(vm.getOwnerBag());
		}
		return affectedBags;
	}
	
	public static void addAutoResolveContent(Timeline timeline, List<FalsifiedTime> children, Map<Claim,List<VoxelModel>> modelHandler){
		for(FalsifiedTime child : children){
			addFalsificationAnimation(timeline, child.falsified(), modelHandler);
			if(child.hasChildren()){
				addAutoResolveContent(timeline, falsifiedTimeChildren(child), modelHandler);
			}
		}
	}
	
	private static Map<Claim,List<VoxelModel>> genModelHandler(Group voxelModels){
		return voxelModels.getChildren().stream()
				.filter(VoxelModel.class::isInstance).map(VoxelModel.class::cast)
				.collect(Collectors.groupingBy(VoxelModel::getClaim));
	}
	
	public static final int MODELS_PER_CLAIM = RuleType.values().length;
	
	private static Timeline stitch(List<Timeline> timelines){
		Iterator<Timeline> iter = timelines.iterator();
		Timeline first = iter.next();
		
		Timeline current = first;
		while(iter.hasNext()){
			final Timeline next = iter.next();
			current.setOnFinished((ae)->next.play());
			current = next;
		}
		
		return first;
	}
}