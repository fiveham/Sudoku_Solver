package sudoku;

import common.graph.Graph;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class Solver implements Runnable{
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_INITIALIZER_SOURCE = new ArrayList<>(1);
	static {
		DEFAULT_INITIALIZER_SOURCE.add( (sudoku) -> new Initializer(sudoku) );
	}
	
	public static final List<Function<Sudoku,Technique>> DEFAULT_PROCESSOR_SOURCE = new ArrayList<>(2);
	static {
		DEFAULT_PROCESSOR_SOURCE.add( (sudoku) -> new SledgeHammer2(sudoku) );
		DEFAULT_PROCESSOR_SOURCE.add( (sudoku) -> new ColorChain(sudoku) );
	}
	
	public static final BiFunction<Sudoku, List<Function<Sudoku,Technique>>, List<Technique>> SOURCE_TO_TECHNIQUES = 
			(sudoku,funcList) -> funcList.stream().map((func)->func.apply(sudoku)).collect(Collectors.toList());
	
	public static final List<Technique> NO_INITIALIZERS = new ArrayList<>(0);
	public static final List<Function<Sudoku,Technique>> NO_INITIALIZER_SOURCE = new ArrayList<>(0);
	
	private final List<Function<Sudoku,Technique>> initializerSource;
	private final List<Function<Sudoku,Technique>> processorSource;
	
	private final List<Technique> processors;
	private final List<Technique> initializers;
	private final Sudoku target;
	
	private final ThreadEvent eventParent;
	private ThreadEvent event = null;
	
	private final Panopticon watcher;
	
	/**
	 * <p>Constructs a Solver that works to solve the target defined 
	 * at the beginning of the file named <tt>filename</tt>.</p>
	 * @param filename the name of the file containing the target to 
	 * be solved
	 * @throws FileNotFoundException if the named file could not be 
	 * found
	 */
	public Solver(String filename) throws FileNotFoundException{
		this(new Puzzle(new File(filename)), new Panopticon());
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the target 
	 * specified by the text at the beginning of <tt>f</tt>.</p>
	 * @param f the file containing a target to be solved
	 * @throws FileNotFoundException if <tt>f</tt> could not 
	 * be found
	 */
	public Solver(File f) throws FileNotFoundException{
		this(new Puzzle(f), new Panopticon());
	}
	
	/**
	 * <p>Constructs a Solver that works to solve the specified 
	 * <tt>target</tt>.</p>
	 * @param target the Puzzle to be solved
	 */
	public Solver(Sudoku puzzle){
		this(puzzle, new Panopticon());
	}

	private Solver(Sudoku puzzle, Panopticon watcher){
		this(puzzle, null, watcher, DEFAULT_INITIALIZER_SOURCE, DEFAULT_PROCESSOR_SOURCE);
	}
	
	private Solver(Sudoku sudoku, ThreadEvent eventParent, List<Function<Sudoku,Technique>> initializers, List<Function<Sudoku,Technique>> processors){
		this(sudoku, eventParent, new Panopticon(), initializers, processors);
	}
	
	private Solver(Sudoku sudoku, ThreadEvent eventParent, Panopticon watcher, List<Function<Sudoku,Technique>> initializers, List<Function<Sudoku,Technique>> processors){
		this.target = sudoku;
		
		this.initializerSource = initializers;
		this.processorSource = processors;
		
		this.initializers = SOURCE_TO_TECHNIQUES.apply(sudoku, initializers);
		this.processors = SOURCE_TO_TECHNIQUES.apply(sudoku, processors);
		
		this.eventParent = eventParent;
		this.watcher = watcher;
	}
	
	private Solver(Sudoku sudoku, ThreadEvent eventParent, List<Function<Sudoku,Technique>> processors){
		this(sudoku, eventParent, new Panopticon(), NO_INITIALIZER_SOURCE, processors);
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
	
	private ThreadEvent initialize(){
		return handleTechniques(initializers, eventParent);
	}
	
	/**
	 * <p>Applies each technique in <tt>processors</tt> to 
	 * <tt>target</tt>. If a technique reports that it was made 
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
				return new ThreadEvent(eventParent, solutionEvent);
			}
		}
		return null;
	}
	
	public static final BiFunction<Solver,Graph<NodeSet<?,?>>,Solver> HAS_NO_INITIALIZERS = 
			(solver,component) -> new Solver(new SudokuNetwork(solver.target.magnitude(), component), solver.event, solver.processorSource);
	public static final BiFunction<Solver,Graph<NodeSet<?,?>>,Solver> HAS_INITIALIZERS = 
			(solver,component) -> new Solver(new SudokuNetwork(solver.target.magnitude(), component), solver.event, solver.initializerSource, solver.processorSource);
	
	/**
	 * <p>Creates a thread to {@link #run() run} this Solver and creates 
	 * a daemon thread to watch the thread group to which the Solver thread 
	 * belongs. Once the thread group being watched becomes empty (has 
	 * {@link ThreadGroup#activeCount() no active threads}) the daemon thread
	 * terminates. This method {@link Thread#join() waits} for the daemon 
	 * thread to terminate before returning.</p>
	 * <p>Use this method when creating a single initial Solver for a 
	 * <tt>Puzzle</tt>.</p>
	 * @throws InterruptedException
	 */
	public void solve() throws InterruptedException{
		Thread monitor = new Thread(watcher);
		monitor.setDaemon(true);
		
		new Thread(watcher, this).start(); //calls run()
		monitor.start();
		
		monitor.join();
	}
	
	@Override
	public void run(){
		if((event=initialize()) != null){
			target.connectedComponents().stream().forEach( (component) -> 
					new Thread(watcher, HAS_INITIALIZERS.apply(this, component))
					.start());
		} else if((event=process()) != null){
			target.connectedComponents().stream().forEach((component) -> 
					new Thread(watcher, HAS_NO_INITIALIZERS.apply(this, component))
					.start());
		}
		watcher.notify();
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
		Solver s = new Solver(new File(args[0]));
		s.solve();
		System.out.println(s.target.toString());
	}
	
	private static class Panopticon extends ThreadGroup implements Runnable{
		public static final BiConsumer<Panopticon,Boolean> DO_NOTHING = (monitor,activeCountGT0) -> {};
		public static final BiConsumer<Panopticon,Boolean> CHECK_START = (monitor,activeCountGT0) -> {
			if(activeCountGT0){
				monitor.started = true;
				monitor.action = DO_NOTHING;
			}
		};
		
		private boolean started = false;
		private BiConsumer<Panopticon,Boolean> action = CHECK_START;
		
		private Panopticon(){
			super("sudoku");
		}
		
		@Override
		public synchronized void run(){
			boolean activeCountGT0;
			while( (activeCountGT0 = activeCount() > 0) || !started ){
				action.accept(this,activeCountGT0);
				try{
					wait(500);
				} catch(InterruptedException e){
					System.out.println("INTERRUPTED EXCEPTION");
					//do nothing
				}
			}
		}
	}
}
