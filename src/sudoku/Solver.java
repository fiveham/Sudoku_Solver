package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class Solver {
	
	private List<Technique> techniqueList;
	private Puzzle puzzle;
	
	public Solver(Puzzle puzzle){
		this.puzzle = puzzle;
		
		techniqueList = new ArrayList<>();
		
		//techniqueList.add(new OrganFailure(target));
		/*
		 * a non-vertical fact-bag has been scrunched down to the point where 
		 * it has only one truth-claim inside it. Now, call thatClaim.setTrue();
		 */
		
		//techniqueList.add(new ValueClaim(target));
		/*
		 * A line's blob is a proper subset of a box's blob, or a 
		 * box's blob is a proper subset of a line's blob.
		 * Now, retainAll() (safely) of the proper subset's 
		 * elements in the larger set.
		 */
		
		//techniqueList.add(new CellDeath(target));
		/*
		 * The vertical blob is already scrunched down to a single element; 
		 * now you need to make the row, column, and box blobs scrunch down 
		 * onto that single truth-claim as well (and make sure they do so 
		 * correctly, false-marking the truth-claims they excrete).
		 * call thatClaim.setTrue();
		 */
		
		/*
		 * Among the preceding techniques, a pattern emerges:
		 * Any time some blob is a proper subset of some other blob, 
		 * the larger blob is required to contract (properly, freeing 
		 * excreted truth-claims from all their nets) so as to become 
		 * equal to the aforementioned subset.
		 */
		
		//techniqueList.add(new GroupLocalizationExternal(target));
		/*
		 * This is a repulsion, reflecting an absence, instead of 
		 * one set matching another.
		 * Take the cells of a region, and find some way of diving 
		 * them into an ingroup and an outgroup. The ingroup is defined 
		 * to be the ingroup on account of its combined (Set.addAll()) 
		 * set of possible values (different from truth-claims) having 
		 * the same number of elements as the ingroup itself.
		 * Once you have an ingroup-outgroup separation, take the 
		 * complement of the aforementioned combined possible values set, 
		 * and remove all the elements of that complementary set from 
		 * the possibilities for the cells of the outgroup.  Of course, this
		 * will require translating the values to be marked impossible 
		 * into truth-claims by extracting the x and y indices of each 
		 * cell from which the values are to be marked impossible.
		 * 
		 * In this technique, instead of finding that an existing fact bag is 
		 * a proper subset of another existing fact bag and forcibly conforming 
		 * the larger bag to the smaller one, you have to construct a subset and 
		 * then forcibly anti-conform that subset's counterpart and that must be 
		 * achieved indirectly by altering the individual cell bags that 
		 * contribute to the counterpart of the aforementioned constructed 
		 * subset.
		 * 
		 */
		
		techniqueList.add(new SledgeHammer2(puzzle));
		techniqueList.add(new ColorChain(puzzle));
		
	}
	
	public boolean digest(){
		for(int index = 0; 
				index < techniqueList.size(); 
				index = techniqueList.get(index).digest() ? 0 : (index+1) );
		return puzzle.isSolved();
	}
	
	public static void main(String[] args) throws FileNotFoundException{
		Solver s = new Solver(new Puzzle(new File(args[0])));
		s.digest();
		System.out.println(s.puzzle.toString());
	}
	
}
