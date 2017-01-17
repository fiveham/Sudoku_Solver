package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import sudoku.time.ThreadEvent;
import sudoku.parse.Parser;

/**
 * <p>Coordinates and applies several techniques for solving a sudoku target.</p> <p>Techniques are
 * arranged in a list and are called upon in sequence until the target is solved. The sequence
 * resets to the beginning if and when a technique in the list has made changes to the target. By
 * resetting to the start of the list, more powerful and less expensive techniques are
 * prioritized.</p>
 * @author fiveham
 */
public class Solver{
	
	private final Sudoku target;
	
	private final ThreadEvent eventParent;
	private ThreadEvent event = null;
	
	private final SudokuThreadGroup group;
	private final Object lock;
	
	private final String source;
	
  /**
   * <p>Constructs a Solver that works to solve the target specified by the text in {@code f}. The
   * file is read using the system's default encoding.</p>
   * @param f the file containing a puzzle to be solved
   * @throws FileNotFoundException if {@code f} could not be found or read
   */
	public Solver(File f) throws FileNotFoundException{
		this(f, System.getProperty("file.encoding"));
	}
	
  /**
   * <p>Constructs a Solver that works to solve the puzzle specified by the text in {@code f}. The
   * file is read using the specified charset encoding.</p>
   * @param f the file containing a target to be solved
   * @param charset the charset encoding to be used in reading {@code f}
   * @throws FileNotFoundException if {@code f} could not be found
   */
	public Solver(File f, String charset) throws FileNotFoundException{
		this(new Puzzle(f, charset), f.getName());
	}
	
  /**
   * <p>Constructs a Solver that works to solve the specified {@code puzzle}.</p>
   * @param target the Puzzle to be solved
   */
	public Solver(Sudoku puzzle, String filename){
		this(puzzle, new SudokuThreadGroup(filename), new Object(), filename);
	}
	
	private Solver(Sudoku target, ThreadEvent eventParent, SudokuThreadGroup group, Object waiter, String source){
		this.target = target;
		
		this.eventParent = eventParent;
		this.group = group;
		
		this.lock = waiter;
		this.source = source;
	}
	
	private Solver(Sudoku target, SudokuThreadGroup group, Object waiter, String source){
		this(target, null, group, waiter, source);
		group.setRootSolver(this);
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
   * <p>Creates a thread to {@link #run() run} this Solver and creates a daemon thread to watch
   * the thread group to which the Solver thread belongs. Once the thread group being watched
   * becomes empty (has {@link ThreadGroup#activeCount() no active threads}) the daemon thread
   * terminates. This method {@link Thread#join() waits} for the daemon thread to terminate before
   * returning.</p> <p>Use this method when creating a single initial Solver for a
   * {@code Puzzle}.</p>
   * @throws InterruptedException
   */
	public void solve() throws InterruptedException{
		Thread operation = new Thread(group, this::run, source + "_0");
		operation.start(); //calls run()
		
		while(group.activeCount() > 0){
			synchronized(lock){
				//if(group.activeCount() > 0){
					lock.wait(100); //MAGIC
				//}
			}
		}
	}
	
	private void run(){
		ThreadEvent processingResult = process();
		List<SudokuNetwork> networks;
		if(processingResult != null 
				&& !(networks = target.connectedComponents().stream()
						.filter((component) -> !SudokuNetwork.isSolved(component))
						.map((component) -> new SudokuNetwork(target.magnitude(), component))
						.collect(Collectors.toList())).isEmpty()){
			String name = Thread.currentThread().getName();
			this.event = processingResult;
			for(int i = 0; i < networks.size(); ++i){
				SudokuNetwork network = networks.get(i);
				new Thread(
						group, 
						new Solver(network, event, group, lock, source)::run, 
						name + Integer.toString(i, Parser.MAX_RADIX))
						.start();
			}
		} else{
			synchronized(lock){
				lock.notify();
			}
		}
	}
	
  /**
   * <p>Applies each technique in {@code processors} to {@code target}. If a technique reports
   * that it was made a change to the target, then instead of moving on to the next technique in
   * the list, technique selection resets to the start of the technique list. This reset mechanism
   * allows the prioritization of techniques by placing higher- priority techniques earlier in the
   * list.</p>
   * @return true if the target is solved, false otherwise
   */
	private ThreadEvent process(){
		return new ThreadEvent(
		    eventParent, 
		    new ConsequenceIntersection(target).implications(), 
		    Thread.currentThread().getName());
	}
	
  /**
   * <p>Extends ThreadGroup to override uncaughtException() so that all the Solver threads spawned
   * from a call to main() can be easily forced to share the same </p>
   * @author fiveham
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
		
		private void setRootSolver(Solver s){
			this.rootSolver = s;
		}
	}
}
