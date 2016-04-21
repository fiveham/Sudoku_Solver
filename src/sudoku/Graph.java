package sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

//TODO make constructors and other input methods assess connection relationships between Cells 
//and incorporate that information automatically.
//Such information to include:
//identification of connected neighbors of cells
//report connected components
public class Graph extends HashSet<Cell> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5251246283676129228L;

	public Graph() {
		// TODO Auto-generated constructor stub
	}

	public Graph(Collection<Cell> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	public Graph(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public Graph(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		// TODO Auto-generated constructor stub
	}
	
	public Cell element(){
		Iterator<Cell> iter = iterator();
		Cell element = iter.next();
		//iter.remove();
		
		return element;
	}
	
}
