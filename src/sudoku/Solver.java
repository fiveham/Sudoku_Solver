package sudoku;

import common.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
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
public class Solver implements Runnable{ //TODO switch order of Sledgehammer and ColorChain techniques to debug/test ColorChain
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_INITIALIZER_SOURCE = new ArrayList<>(1);
	static {
		DEFAULT_INITIALIZER_SOURCE.add( (sudoku) -> new Initializer(sudoku) );
	}
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_PROCESSOR_SOURCE = new ArrayList<>(2);
	static {
		Collections.addAll(DEFAULT_PROCESSOR_SOURCE, ColorChain::new, Sledgehammer::new);
	}
	
	public static final BiFunction<Sudoku, List<Function<Sudoku,Technique>>, List<Technique>> SOURCE_TO_TECHNIQUES = 
			(sudoku,funcList) -> funcList.stream().map((func)->func.apply(sudoku)).collect(Collectors.toList());
	
	public static final List<Function<Sudoku,Technique>> NO_INITIALIZER_SOURCE = new ArrayList<>(0);
	
	private final List<Function<Sudoku,Technique>> initializerSource;
	private final List<Function<Sudoku,Technique>> processorSource;
	
	private final List<Technique> processors;
	private final List<Technique> initializers;
	private final Sudoku target;
	
	private final ThreadEvent eventParent;
	private ThreadEvent event = null;
	
	private final SudokuThreadGroup group;
	private final Object lock;
	
	/**
	 * <p>Constructs a Solver that works to solve the target 
	 * specified by the text at the beginning of {@code f}.</p>
	 * @param f the file containing a target to be solved
	 * @throws FileNotFoundException if {@code f} could not 
	 * be found
	 */
	public Solver(File f) throws FileNotFoundException{
		this(new Puzzle(f));
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the specified 
	 * {@code target}.</p>
	 * @param target the Puzzle to be solved
	 */
	public Solver(Sudoku puzzle){
		this(puzzle, null, new SudokuThreadGroup(), new Object(), DEFAULT_INITIALIZER_SOURCE, DEFAULT_PROCESSOR_SOURCE);
	}
	
	private Solver(Sudoku target, ThreadEvent eventParent, SudokuThreadGroup group, Object waiter, List<Function<Sudoku,Technique>> initializers, List<Function<Sudoku,Technique>> processors){
		this.target = target;
		
		this.initializerSource = initializers;
		this.processorSource = processors;
		
		this.initializers = SOURCE_TO_TECHNIQUES.apply(target, initializers);
		this.processors = SOURCE_TO_TECHNIQUES.apply(target, processors);
		
		this.eventParent = eventParent;
		this.group = group;
		if(eventParent==null){
			group.setRootSolver(this);
		}
		
		this.lock = waiter;
	}
	
	public ThreadEvent getEvent(){
		return event;
	}
	
	/**
	 * <p>Returns the Puzzle that this Solver works to solve.</p>
	 * @return the Puzzle that this Solver works to solve
	 */
	public Sudoku getPuzzle(){
		return target;
	}
	
	public static final BiFunction<Solver,SudokuNetwork,Solver> HAS_NO_INITIALIZERS = 
			(solver,network) -> new Solver(network, solver.event, solver.group, solver.lock, NO_INITIALIZER_SOURCE, solver.processorSource);
	public static final BiFunction<Solver,SudokuNetwork,Solver> HAS_INITIALIZERS = 
			(solver,network) -> new Solver(network, solver.event, solver.group, solver.lock, solver.initializerSource, solver.processorSource);
	
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
		
		Thread operation = new Thread(group, this, "solver_0");
		
		operation.start(); //calls run()
		
		while(group.activeCount() > 0){
			synchronized(lock){
				//if(group.activeCount() > 0){
					lock.wait(100);
				//}
			}
		}
	}
	
	@Override
	public void run(){
		Debug.log("Running a thread: " + Thread.currentThread().getName()); //DEBUG
		
		Pair<ThreadEvent,BiFunction<Solver, SudokuNetwork, Solver>> runnableSource = getRunnableSource();
		if(runnableSource != null){
			this.event = runnableSource.getA();
			BiFunction<Solver, SudokuNetwork, Solver> successorSolver = runnableSource.getB();
			
			//DEBUG
			if(event.equals(eventParent)){
				//stuff goes here
				throw new IllegalStateException(Boolean.toString(event==eventParent));
			}
			
			List<SudokuNetwork> networks = target.connectedComponents().stream()
					.map((component) -> new SudokuNetwork(target.magnitude(), component))
					.filter((sn) -> !sn.isSolved())
					.collect(Collectors.toList());
			
			//System.exit(0);//DEBUG
			
			if( !networks.isEmpty()){
				
				//DEBUG
				Debug.log("Have some unsolved networks; splitting thread: " + networks.size() + " children");
				Debug.log(target.nodeStream().findFirst().get().getPuzzle());
				Debug.log("SolutionEvent: "+event.wrapped());
				
				String name = Thread.currentThread().getName();
				for(int i=0; i<networks.size(); ++i){
					SudokuNetwork network = networks.get(i);
					
					//DEBUG
					Debug.log("Start thread for component with "+network.size()+" nodes.");
					
					new Thread(group, successorSolver.apply(this, network), name+Integer.toString(i,Parser.MAX_RADIX)).start();
				}
			} else{
				synchronized(lock){
					
					//DEBUG
					Debug.log("Have no unsolved networks; notifying lock");
					Debug.log("SolutionEvent: "+event.wrapped());
					
					lock.notify();
				}
			}
		} //TODO should there be something in the case of a null runnableSource?
	}
	
	/**
	 * <p>Applies this Solver's initializer and processor Techniques, stores 
	 * the produced SolutionEvent in {@code event}, and returns the appropriate 
	 * BiFunction to generate this Solver's children.</p>
	 * @see #HAS_INITIALIZERS
	 * @see #HAS_NO_INITIALIZERS
	 * @return
	 */
	private Pair<ThreadEvent,BiFunction<Solver, SudokuNetwork, Solver>> getRunnableSource(){
		for(TechniqueInheritance ti : TechniqueInheritance.values()){
			ThreadEvent e = ti.solutionStyle.apply(this);
			if(e != null){
				return new Pair<>(e,ti.initializerInheritance);
			}
		}
		return null;
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
			SolutionEvent solutionEvent = technique.digest();
			
			if(solutionEvent != null){
				return new ThreadEvent(eventParent, solutionEvent, Thread.currentThread().getName());
			}
		}
		return null;
	}
	
	private static enum TechniqueInheritance{
		WITH_INITIALIZERS(
				(solver)->solver.initialize(), 
				HAS_INITIALIZERS), 
		WITHOUT_INITIALIZERS(
				(solver)->solver.process(), 
				HAS_NO_INITIALIZERS);
		
		private final Function<Solver,ThreadEvent> solutionStyle;
		private final BiFunction<Solver,SudokuNetwork,Solver> initializerInheritance;
		
		private TechniqueInheritance(Function<Solver,ThreadEvent> solutionStyle, BiFunction<Solver,SudokuNetwork,Solver> initializerInheritance){
			this.solutionStyle = solutionStyle;
			this.initializerInheritance = initializerInheritance;
		}
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
		Solver s = new Solver(new File(args[SRC_FILE_ARG_INDEX]));
		s.solve();
		System.out.println(s.target.toString());
	}
	
	public static final int SRC_FILE_ARG_INDEX = 0;
	
	/**
	 * <p>Extends ThreadGroup to override uncaughtException() so that 
	 * all the Solver threads spawned from a call to main() can be 
	 * easily forced to share the same </p>
	 * @author fiveham
	 *
	 */
	public static class SudokuThreadGroup extends ThreadGroup{
		public SudokuThreadGroup(){
			super("sudoku");
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
