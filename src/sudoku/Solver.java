package sudoku;

import common.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import sudoku.technique.ColorChain;
import sudoku.technique.Initializer;
import sudoku.technique.Sledgehammer;
import sudoku.technique.Technique;
import sudoku.technique.XYChain;
import sudoku.time.TechniqueEvent;
import sudoku.time.ThreadEvent;
import sudoku.parse.Parser;

/**
 * <p>Coordinates and applies several techniques for solving a sudoku target.</p>
 * 
 * <p>Techniques are arranged in a list and are called upon in sequence until the 
 * target is solved. The sequence resets to the beginning if and when a technique 
 * in the list has made changes to the target. By resetting to the start of the 
 * list, more powerful and less expensive techniques are prioritized.</p>
 * 
 * @author fiveham
 *
 */
public class Solver{
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_INITIALIZER_SOURCE = Arrays.asList(
			Initializer::new);
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_PROCESSOR_SOURCE = Arrays.asList(
			ColorChain::new, 
			XYChain::new, 
			Sledgehammer::new);
	
	public static final List<Function<Sudoku,Technique>> NO_INITIALIZER_SOURCE = new ArrayList<>(0);
	
	private final List<Function<Sudoku,Technique>> initializerSource;
	private final List<Function<Sudoku,Technique>> processorSource;
	
	private final List<Technique> initializers;
	private final List<Technique> processors;
	
	private final Sudoku target;
	
	private final ThreadEvent eventParent;
	private ThreadEvent event = null;
	
	private final SudokuThreadGroup group;
	private final Object lock;
	
	private final String source;
	
	/**
	 * <p>Constructs a Solver that works to solve the target 
	 * specified by the text in {@code f}. The file is read 
	 * using the system's default encoding.</p>
	 * @param f the file containing a puzzle to be solved
	 * @throws FileNotFoundException if {@code f} could not 
	 * be found or read
	 */
	public Solver(File f) throws FileNotFoundException{
		this(f, System.getProperty("file.encoding"));
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the puzzle 
	 * specified by the text in {@code f}. The file is read 
	 * using the specified charset encoding.</p>
	 * @param f the file containing a target to be solved
	 * @param charset the charset encoding to be used in reading 
	 * {@code f}
	 * @throws FileNotFoundException if {@code f} could not 
	 * be found
	 */
	public Solver(File f, String charset) throws FileNotFoundException{
		this(new Puzzle(f, charset), f.getName());
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the specified 
	 * {@code puzzle}.</p>
	 * @param target the Puzzle to be solved
	 */
	public Solver(Sudoku puzzle, String filename){
		this(puzzle, null, new SudokuThreadGroup(filename), new Object(), DEFAULT_INITIALIZER_SOURCE, DEFAULT_PROCESSOR_SOURCE, filename);
	}
	
	private Solver(Sudoku target, ThreadEvent eventParent, SudokuThreadGroup group, Object waiter, List<Function<Sudoku,Technique>> initializers, List<Function<Sudoku,Technique>> processors, String source){
		this.target = target;
		
		this.initializerSource = initializers;
		this.processorSource = processors;
		
		this.initializers = generateTechniques(target, initializers);
		this.processors   = generateTechniques(target, processors);
		
		this.eventParent = eventParent;
		this.group = group;
		if(eventParent==null){
			group.setRootSolver(this);
		}
		
		this.lock = waiter;
		this.source = source;
	}
	
	private static List<Technique> generateTechniques(Sudoku sudoku, List<Function<Sudoku,Technique>> funcList){
		return funcList.stream().map((func)->func.apply(sudoku)).collect(Collectors.toList());
	}
	
	public ThreadEvent getEvent(){
		return event;
	}
	
	/**
	 * <p>Returns the Puzzle that this Solver works to solve.</p>
	 * @return the Puzzle that this Solver works to solve
	 */
	public Sudoku getTarget(){
		return target;
	}
	
	public ThreadGroup getThreadGroup(){
		return group;
	}
	
	/**
	 * <p>Creates a thread to {@link #run() run} this Solver and creates 
	 * a daemon thread to watch the thread group to which the Solver thread 
	 * belongs. Once the thread group being watched becomes empty (has 
	 * {@link ThreadGroup#activeCount() no active threads}) the daemon thread
	 * terminates. This method {@link Thread#join() waits} for the daemon 
	 * thread to terminate before returning.</p>
	 * <p>Use this method when creating a single initial Solver for a 
	 * {@code Puzzle}.</p>
	 * @throws InterruptedException
	 */
	public void solve() throws InterruptedException{
		Thread operation = new Thread(group, this::run, source+"_0");
		operation.start(); //calls run()
		
		while(group.activeCount() > 0){
			synchronized(lock){
				//if(group.activeCount() > 0){
					lock.wait(100); 
				//}
			}
		}
	}
	
	private void run(){
		
		//DEBUG
		/*Debug.log("Running a thread: " + Thread.currentThread().getName());
		Debug.log("Current graph size: "+target.size());*/
		
		Pair<ThreadEvent, BiFunction<Solver,Sudoku, Solver>> eventAndChildSrc = applyTechniques();
		List<SudokuNetwork> networks;
		if(eventAndChildSrc != null 
				&& !(networks = target.connectedComponents().stream()
						.map((component) -> new SudokuNetwork(target.magnitude(), component))
						.filter((sn) -> !sn.isSolved())
						.collect(Collectors.toList())).isEmpty()){
			String name = Thread.currentThread().getName();
			this.event = eventAndChildSrc.getA();
			for(int i=0; i<networks.size(); ++i){
				SudokuNetwork network = networks.get(i);
				new Thread(group, eventAndChildSrc.getB().apply(this, network)::run, name+Integer.toString(i,Parser.MAX_RADIX)).start();
			}
		} else{
			synchronized(lock){
				lock.notify();
			}
		}
	}
	
	/**
	 * <p>Applies this Solver's initializer and processor Techniques and returns 
	 * the produced TechniqueEvent paired with the appropriate BiFunction to 
	 * generate this Solver's children.</p>
	 * @see #HAS_INITIALIZERS
	 * @see #HAS_NO_INITIALIZERS
	 * @return
	 */
	private Pair<ThreadEvent,BiFunction<Solver, Sudoku, Solver>> applyTechniques(){
		for(TechniqueInheritance ti : TechniqueInheritance.values()){
			ThreadEvent e = ti.solutionStyle.apply(this);
			if(e != null){
				return new Pair<>(e,ti.initializerInheritance);
			}
		}
		return null;
	}
	
	private static enum TechniqueInheritance{
		WITH_INITIALIZERS(
				Solver::initialize, 
				Solver::childWithInitializers), 
		WITHOUT_INITIALIZERS(
				Solver::process, 
				Solver::childWithoutInitializers);
		
		private final Function<Solver,ThreadEvent> solutionStyle;
		private final BiFunction<Solver,Sudoku,Solver> initializerInheritance;
		
		private TechniqueInheritance(Function<Solver,ThreadEvent> solutionStyle, BiFunction<Solver,Sudoku,Solver> initializerInheritance){
			this.solutionStyle = solutionStyle;
			this.initializerInheritance = initializerInheritance;
		}
	}
	
	private Solver childWithInitializers(Sudoku network){
		return new Solver(network, event, group, lock, initializerSource, processorSource, source);
	}
	
	private Solver childWithoutInitializers(Sudoku network){
		return new Solver(network, event, group, lock, NO_INITIALIZER_SOURCE, processorSource, source);
	}
	
	private ThreadEvent initialize(){
		return handleTechniques(initializers, eventParent);
	}
	
	/**
	 * <p>Applies each technique in {@code processors} to 
	 * {@code target}. If a technique reports that it was made 
	 * a change to the target, then instead of moving on to the 
	 * next technique in the list, technique selection resets to 
	 * the start of the technique list. This reset mechanism 
	 * allows the prioritization of techniques by placing higher-
	 * priority techniques earlier in the list.</p>
	 * 
	 * @return true if the target is solved, false otherwise
	 */
	private ThreadEvent process(){
		return handleTechniques(processors, eventParent);
	}
	
	private static ThreadEvent handleTechniques(List<Technique> techniques, ThreadEvent eventParent){
		for(Technique technique : techniques){
			TechniqueEvent techniqueEvent = technique.digest();
			if(techniqueEvent != null){
				return new ThreadEvent(eventParent, techniqueEvent, Thread.currentThread().getName());
			}
		}
		return null;
	}
	
	/**
	 * <p>Main method. Creates a Solver instance and {@link #solve() uses} it, then 
	 * {@link Puzzle#toString() prints} the target to the console.</p>
	 * 
	 * <p>The first command-line argument is the name of the file from which to read 
	 * the target to be solved.</p>
	 * 
	 * @param args command line arguments
	 * @see #run()
	 * @throws FileNotFoundException if the file specified by the first command-line 
	 * argument could not be found
	 */
	public static void main(String[] args) throws FileNotFoundException, InterruptedException{
		Debug.log("STARTING"); //DEBUG
		Solver s = solver(args);//new Solver(new File(args[SRC_FILE_ARG_INDEX]));
		s.solve();
		System.out.println(s.target.toString());
		
		//DEBUG
		Debug.log("Solution process: ");
		Debug.log(s.event);
	}
	
	private static Solver solver(String[] args) throws FileNotFoundException{
		String charset = null;
		if(args.length >= 2){
			charset = args[1];
		}
		if(args.length < 1){
			errorExit();
		}
		
		File file = new File(args[0]);
		if(charset == null){
			return new Solver(file);
		} else{
			return new Solver(file, charset);
		}
	}
	
	private static void errorExit(){
		System.err.println("Usage: java Solver puzzle-file character-encoding");
		System.exit(0);
	}
	
	public static final int SRC_FILE_ARG_INDEX = 0;
	
	/**
	 * <p>Extends ThreadGroup to override uncaughtException() so that 
	 * all the Solver threads spawned from a call to main() can be 
	 * easily forced to share the same </p>
	 * @author fiveham
	 *
	 */
	private static class SudokuThreadGroup extends ThreadGroup{
		public SudokuThreadGroup(String sourceFile){
			super(sourceFile);
		}
		
		@Override
		public void uncaughtException(Thread t, Throwable e){
			Debug.log(rootSolver.getEvent());
			e.printStackTrace();
			System.exit(1);
		}
		
		Solver rootSolver;
		
		public void setRootSolver(Solver s){
			this.rootSolver = s;
		}
	}
}
