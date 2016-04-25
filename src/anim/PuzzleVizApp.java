package anim;

import sudoku.Claim;
import sudoku.FalsifiedTime;
import sudoku.Puzzle;
import sudoku.Puzzle.IndexInstance;
import sudoku.Solver;
import sudoku.ThreadEvent;
import common.time.Time;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
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

//TODO accomodate animation of Rule mergers by having the merging Rules shift colors simultaneously until they share the same color, then one goes invisible.
public class PuzzleVizApp extends Application {
	
	public static final Function<Time,ThreadEvent> AS_THREADEVENT = (time) -> (ThreadEvent) time;
	public static final Predicate<Time> IS_THREADEVENT = (time) -> time instanceof ThreadEvent;
	
	public PuzzleVizApp() {
	}
	
	public static void main(String[] args) {
		if(args.length > 0){
			launch(args); //calls start()
		} else{
			System.out.println("Usage: java PuzzleVizApp target-sourcefile-name");
			System.exit(1);
		}
    }
	
	@Override
	public void start(Stage primaryStage) throws Exception{
		primaryStage.setResizable(false);
		
		Puzzle puzzle;
		ThreadEvent timeRoot;
		{
			Solver solver = createAndSolvePuzzle();
			puzzle = (Puzzle) solver.getPuzzle();//.get(0);
			timeRoot = (ThreadEvent) solver.getEvent();
		}
		
		Scene scene;
		Group voxelModels;
		{
			List<Parent> createdContent = createContent(puzzle);
			scene = new Scene(createdContent.get(TRUE_ROOT_INDEX_IN_createContent_LIST));
			voxelModels = (Group) createdContent.get(VOXEL_MODELS_INDEX_IN_createContent_LIST);
		}
		primaryStage.setScene(scene);
		
        primaryStage.show();
        
        genTimeline(voxelModels, puzzle, timeRoot).play();
	}
	
	/**
	 * <p>The index of the voxel models in the list returned by 
	 * {@link #createContent(Puzzle) createContent}.</p>
	 */
	private static final int VOXEL_MODELS_INDEX_IN_createContent_LIST = 1;
	
	/**
	 * <p>The index of the root of the scene graph in the list returned 
	 * by {@link #createContent(Puzzle) createContent}.</p>
	 */
	private static final int TRUE_ROOT_INDEX_IN_createContent_LIST = 0;
	
	/**
	 * <p>Creates the scene graph for this application and returns a list 
	 * containing the scene graph as the first element and the Group that 
	 * contains the voxel models as the second element.</p>
	 * 
	 * <p>Access to the voxel model Group is provided as an independent 
	 * result even though it is contained in the overall scene graph (the 
	 * other element of the returned list) because it is more efficient 
	 * than requiring {@link #start(Stage) the calling code} to extract the 
	 * voxel model Group out of the overall scene graph.</p>
	 * 
	 * @param target the Puzzle whose solution process is being animated
	 * @return a list containing the scene graph as the first element and 
	 * the Group that contains the voxel models as the second element
	 */
	private List<Parent> createContent(Puzzle puzzle){
		List<Parent> result = new ArrayList<>(2);
		
		Camera camera = genCamera();
		Group voxelModels = genVoxelModels(puzzle);
		
        result.add(
        		genContentParent(
        				genSubScene(
        						camera, 
        						genRoot(
        								genCameraGroup(camera),
        								voxelModels,
        								genClaimsGroup(puzzle)))));
        result.add(voxelModels);
        return result;
	}
	
	private Camera genCamera(){
		Camera result = new PerspectiveCamera(true);
		result.getTransforms().add(new Translate(0,0,-15));
		
		return result;
	}
	
	/**
	 * <p>Creates all the voxel models for the claims of <tt>target</tt>, sequenced so that first quarter 
	 * of the list contains voxel models for all the claims of the target in order of increasing 
	 * {@link Claim#linearizeCoords(int, int, int, int) linearized coordinates}. The voxel models in the 
	 * first fourth of the list all pertain to the same {@link #RegionSpecies type} of Rule.  Each of 
	 * the other three fourths pertains to another <tt>type</tt> so that all voxel models for all claims are 
	 * represented.
	 * Ordering the 
	 * @param target the Puzzle whose solution process is being animated
	 * @return a {@link Group Group} encapsulating the voxel models for the Claims of <tt>target</tt>
	 * @see #associates(Claim, List)
	 */
	private Group genVoxelModels(Puzzle puzzle){
		Group result = new Group();
		for(RegionSpecies region : RegionSpecies.values()){
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
	
	private Group genContentParent(SubScene subScene){
		Group contentParent = new Group();
        contentParent.getChildren().add(subScene);
        return contentParent;
	}
	
	private SubScene genSubScene(Camera camera, Group root){
		SubScene subScene = new SubScene(root, 300,300);
        subScene.setFill(Color.WHITE);
        subScene.setCamera(camera);
        return subScene;
	}
	
	private Group genRoot(Node... subGroups){
		Group root = new Group();
        root.getChildren().addAll(subGroups);
        return root;
	}
	
	private Group genCameraGroup(Camera camera){
		Group cameraGroup = new Group();
		cameraGroup.getChildren().add(camera);
		return cameraGroup;
	}
	
	public static final double CLAIM_SPHERE_RADIUS = 0.1;
	private Group genClaimsGroup(Puzzle puzzle){
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
	
	public static final double SMALL_HALFEDGE = 0.3;
	public static final double LONG_HALFEDGE = 0.5;
	public static final double MED_HALFEDGE = (SMALL_HALFEDGE + LONG_HALFEDGE)/2;
	
	private static final RegionSpecies.DimensionSelector SELECT_X = (x,y,z) -> x;
	private static final RegionSpecies.DimensionSelector SELECT_Y = (x,y,z) -> y;
	private static final RegionSpecies.DimensionSelector SELECT_Z = (x,y,z) -> z;
	private static final RegionSpecies.DimensionSelector NO_SELECTION = (x,y,z) -> 0;
	
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
	
	private static final Function<VoxelModel,DoubleProperty> WIDTH = (vm)->vm.widthProperty();
	private static final Function<VoxelModel,DoubleProperty> DEPTH = (vm)->vm.depthProperty();
	
	private static final Function<VoxelModel,DoubleProperty> X_POS = (vm)->vm.translateXProperty();
	private static final Function<VoxelModel,DoubleProperty> Z_POS = (vm)->vm.translateZProperty();
	
	public static enum RegionSpecies{
		CELL  (Puzzle.RegionSpecies.CELL,   MED,  MED,  MED,  MED,  L0ML, G0ML, EDGE_LIN, DIM_IN_LIN, SELECT_Z, NO_SELECTION, CELL_BLUE,    WIDTH, X_POS), 
		COLUMN(Puzzle.RegionSpecies.COLUMN, MED,  MED,  L0ML, G0ML, MED,  MED,  EDGE_LIN, DIM_IN_LIN, SELECT_X, NO_SELECTION, COLUMN_GREEN, DEPTH, Z_POS), 
		ROW   (Puzzle.RegionSpecies.ROW,    L0ML, G0ML, MED,  MED,  MED,  MED,  EDGE_LIN, DIM_IN_LIN, SELECT_Y, NO_SELECTION, ROW_RED,      DEPTH, Z_POS), 
		BOX   (Puzzle.RegionSpecies.BOX,    L0ML, G0ML, L1ML, G1ML, MED,  MED,  EDGE_BOX, DIM_IN_BOX, SELECT_X, SELECT_Y,     BOX_YELLOW,   DEPTH, Z_POS);
		
		private final Puzzle.RegionSpecies pertainsTo;
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
		
		private RegionSpecies(Puzzle.RegionSpecies type, 
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
		 * Returns -1, 1, or 0 if <tt>dimensionValue</tt> is 0, 
		 * <tt>dimValueWhenPos</tt>, or between 0 and 
		 * <tt>dimValueWhenPos</tt> respectively.
		 * 
		 * 0 is returned when the voxel whose location within its cell, 
		 * box, row, or column or represented by <tt>dimensionValue</tt> 
		 * is not an edge position. For cells, rows, and columns, edge voxels 
		 * are on the edge of the typically 9x9x9 cube; for boxes, edge voxels 
		 * are on the borders between boxes.
		 * 
		 * @param dimensionValue one dimensional component of the location of 
		 * a voxel whose model is being developed. 0-8 for a voxel in a cell, 
		 * row, or column of a 9x9 sudoku target; 0-2 for a voxel in a box of 
		 * a 9x9 sudoku target.
		 * @param dimValueWhenPos when <tt>dimensionValue</tt> is equal to 
		 * this parameter, 1 is returned.
		 * @return 
		 */
		private int edgeSign(Puzzle p, int xyz){
			return (xyz==0) ? -1 : xyz==farEdgeForType.apply(p) ? 1 : 0;
		}
		
		/**
		 * Selects and returns one of the three dimensions specified as parameters, 
		 * or returns 0.
		 * @author fiveham
		 *
		 */
		@FunctionalInterface
		private static interface DimensionSelector{
			int select(int x, int y, int z);
		}
	}
	
	public static final int MODELS_PER_CLAIM = 4;
	
	/**
	 * Returns a list of the VoxelModels in <tt>voxels</tt> that pertain to 
	 * the specified <tt>claim</tt> assuming that <tt>voxels</tt> was 
	 * created with its elements in the order implicitly assumed by this 
	 * method.
	 * This method assumes that the VoxelModels in <tt>voxels</tt> are 
	 * ordered such that <tt>claim.linearizeCoords()</tt> is the index in 
	 * <tt>voxels</tt> of the first model pertaining to <tt>claim</tt> 
	 * and that the subsequent three models are offset by the third power of 
	 * target.sideLength. As such, all the VoxelModels in <tt>voxels</tt> 
	 * pertaining to a certain {@link Puzzle.RegionSpecies pertainsTo} of Rule are 
	 * grouped together within the list.
	 * @param claim
	 * @param voxels
	 * @return
	 */
	private static List<VoxelModel> associates(Claim claim, List<Node> voxels){
		List<VoxelModel> result = new ArrayList<>(MODELS_PER_CLAIM);
		
		int sl = claim.getPuzzle().sideLength();
		int interval = sl*sl*sl;
		for(int i=claim.linearizeCoords(); i < voxels.size(); i += interval){
			result.add((VoxelModel) voxels.get(i));
		}
		
		return result;
	}
	
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
	
	private void genBagModels(List<? super VoxelModel> voxels, Puzzle p){
		for(RegionSpecies reg : RegionSpecies.values()){
			Puzzle.RegionSpecies region = reg.pertainsTo;
			int offset = reg.ordinal() * p.sideLength()*p.sideLength()*p.sideLength();
			for(IndexInstance dimA : region.dimA(p)){
				for(IndexInstance dimB : region.dimB(p)){
					List<VoxelModel> vmList = new ArrayList<>(p.sideLength());
					for(IndexInstance dimC : region.dimInsideRule(p)){
						Puzzle.IndexValue[] i = p.decodeXYZ(dimA, dimB, dimC);
						int x = i[0].intValue();
						int y = i[1].intValue();
						int z = i[2].intValue();
						int index = Claim.linearizeCoords(x, y, z, p.sideLength()) + offset;
						vmList.add( (VoxelModel) voxels.get(index) );
					}
					new BagModel(p, vmList, reg.bagColor);
				}
			}
		}
	}
	
	private Solver createAndSolvePuzzle() throws FileNotFoundException, InterruptedException{
		List<String> args = getParameters().getRaw();
        Puzzle puzzle = new Puzzle(new File(args.get(0)));
        Solver solver = new Solver(puzzle);
        solver.solve();
        return solver;
	}
	
	/*public static List<Timeline> linearizeTimeTree(Puzzle puzzle, Group voxelModels, ThreadEvent root){
		List<Timeline> result = new ArrayList<>();
		
		Map<Claim,List<VoxelModel>> modelHandler = genModelHandler(puzzle, voxelModels);
		
		Iterator<ThreadEvent> treeTraverser = treeTraverser(root);
		while(treeTraverser.hasNext()){
			result.add(solutionEventTimeline(treeTraverser.next().wrapped(), modelHandler));
		}
		
		return result;
	}
	
	private static Iterator<ThreadEvent> treeTraverser(ThreadEvent root){
		
	}*/
	
	/*
	 * XXX different styles of solution-animation can exist:
	 * 
	 * Simultaneous: the splitting of the solution process is represented 
	 * in the animation, as independent components of the puzzle continue solving 
	 * in parallel at the same time.
	 * Details: timelineForThreadEvent.setOnFinished( (ae) -> {} );
	 * 
	 * Linear: several separate options
	 * Deep: Root, child 1, gchild 1, gchild 2, ... final gchild, child 2, gchild 1, ... final gchild, ... final child, gchild 1, ... final gchild
	 * Wide: Root, child 1, child 2, ... final child, gchild1, ... final gchild, ggchild 1, ... 
	 * various mixed protocol, such as wide for the first two generations, then deep thereafter.
	 */
	
	/**
	 * <p>Wraps <tt>parallelTimeline()</tt></p>
	 * @param voxelModels
	 * @param puzzle
	 * @return
	 */
	public static Timeline genTimeline(Group voxelModels, Puzzle puzzle, ThreadEvent timeRoot){
		return parallelTimeline(timeRoot, voxelModels, puzzle, genModelHandler(puzzle, voxelModels));
		//return depthFirstLinearTimeline(puzzle.getTimeBuilder(), voxelModels, puzzle, genModelHandler(puzzle, voxelModels));
		//return breadthFirstLinearTimeline(puzzle.getTimeBuilder(), voxelModels, puzzle, genModelHandler(puzzle, voxelModels));
	}
	
	/**
	 * <p>Returns a Timeline which, when it finishes playing, starts </p>
	 * @param event
	 * @param voxelModels
	 * @param puzzle
	 * @param modelHandler
	 * @return
	 */
	public static Timeline parallelTimeline(ThreadEvent event, Group voxelModels, Puzzle puzzle, Map<Claim,List<VoxelModel>> modelHandler){
		Timeline result = solutionEventTimeline(event.wrapped(), modelHandler);
		
		result.setOnFinished((ae) -> event.children().parallelStream()
				.filter((ct) -> ct instanceof ThreadEvent)
				.forEach((ct) -> parallelTimeline((ThreadEvent)ct, voxelModels, puzzle, modelHandler).play()));
		
		return result;
	}
	
	public static Timeline depthFirstLinearTimeline(ThreadEvent event, Group voxelModels, Puzzle puzzle, Map<Claim,List<VoxelModel>> modelHandler){
		ArrayList<Timeline> timelineList = new ArrayList<>(treeSize(event));
		timelineList.add(solutionEventTimeline(event.wrapped(), modelHandler));
		
		for(ThreadEvent child : threadEventChildren(event)){
			depthFirstRecursion(timelineList, child, modelHandler);
		}
		
		return stitch(timelineList);
	}
	
	private static int treeSize(Time time){
		AtomicInteger ai = new AtomicInteger(0);
		treeSize(time, ai);
		return ai.get();
	}
	
	private static void treeSize(Time time, AtomicInteger ai){
		ai.incrementAndGet();
		time.children().parallelStream().forEach((child) -> treeSize(child,ai));
	}
	
	private static List<Timeline> depthFirstRecursion( List<Timeline> timelineList, ThreadEvent event, Map<Claim,List<VoxelModel>> modelHandler){
		timelineList.add(solutionEventTimeline(event.wrapped(), modelHandler));
		
		for(ThreadEvent child : threadEventChildren(event)){
			timelineList.addAll(depthFirstRecursion(timelineList, child, modelHandler));
		}
		
		return timelineList;
	}
	
	public static Timeline breadthFirstLinearTimeline(ThreadEvent event, Group voxelModels, Puzzle puzzle, Map<Claim,List<VoxelModel>> modelHandler){
		Iterator<ThreadEvent> layerIterator = breadthFirstLinearizeTime(event);
		Timeline result = solutionEventTimeline(layerIterator.next().wrapped(), modelHandler);
		
		Timeline earlier = result;
		while(layerIterator.hasNext()){
			Timeline later = solutionEventTimeline(layerIterator.next().wrapped(), modelHandler);
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
			result.addAll(event.children().stream().filter(IS_THREADEVENT).map(AS_THREADEVENT).collect(Collectors.toList()));
		}
		
		return result;
	}
	
	public static Timeline solutionEventTimeline(FalsifiedTime event, Map<Claim,List<VoxelModel>> modelHandler){
		Timeline result = new Timeline();
		
		//add content for direct SolutionEvent
		addFalsificationAnimation(result, event.falsified(), modelHandler);
		
		//add content for precipitated AutoResolve events
		addAutoResolveContent(result, falsifiedTimeChildren(event), modelHandler);
		
		return result;
	}
	
	private static List<FalsifiedTime> falsifiedTimeChildren(Time time){
		return time.children().stream().filter((t)->t instanceof FalsifiedTime).map((t)->(FalsifiedTime)t).collect(Collectors.toList());
	}
	
	private static List<ThreadEvent> threadEventChildren(Time time){
		return time.children().stream().filter(IS_THREADEVENT).map(AS_THREADEVENT).collect(Collectors.toList());
	}
	
	public static void addFalsificationAnimation(final Timeline timeline, Set<Claim> falsified, Map<Claim,List<VoxelModel>> modelHandler){
		for(Claim c : falsified){
			for(VoxelModel vm : modelHandler.get(c)){
				timeline.getKeyFrames().addAll(vm.disoccupy());
			}
		}
		//falsified.stream().forEach( (c) -> modelHandler.get(c).stream().forEach( (vm) -> timeline.getKeyFrames().addAll(vm.disoccupy()) ) );
		
		Set<BagModel> affectedBags = affectedBags(falsified, modelHandler);
		for(BagModel bag : affectedBags){
			bag.trimUnoccupiedExtremeVoxels(timeline);
		}
	}
	
	public static void addAutoResolveContent(Timeline timeline, List<FalsifiedTime> children, Map<Claim,List<VoxelModel>> modelHandler){
		for(FalsifiedTime child : children){
			addFalsificationAnimation(timeline, child.falsified(), modelHandler);
			if(child.defers()){ //if it has children FIXME account for this concept properly
				addAutoResolveContent(timeline, falsifiedTimeChildren(child), modelHandler);
			}
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
	
	private static Map<Claim,List<VoxelModel>> genModelHandler(Puzzle puzzle, Group voxelModels){
		Map<Claim,List<VoxelModel>> result = new HashMap<>( (int)Math.pow(puzzle.sideLength(), Puzzle.DIMENSION_COUNT) );
		
		List<Node> voxels = voxelModels.getChildren();
		
		for(Claim claim : puzzle.claims()){
			result.put(claim, associates(claim,voxels));
		}
		
		return result;
	}
}