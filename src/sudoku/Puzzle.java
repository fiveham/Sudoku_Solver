package sudoku;

import common.time.TimeBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * <p>Represents a sudoku target as a bipartite graph of 
 * {@link Claim truth-claims} such as "Cell 1,7 has value 3." and 
 * {@link Rule groupings of claims} of which one claim per grouping 
 * is true and the rest in the same group are false.</p>
 * 
 * <p>The Claims are internally stored as {@link SpaceMap a three-dimensional array}
 *  where the dimensions are the x and y coordinates that a cell may 
 *  have in the target and a z coordinate which is the values available to 
 *  the cells.</p>
 *  
 *  <p>A Puzzle features a sort of memory of history in the 
 *  form of a time tree, built upon by calling {@link #timeBuilder() timeBuilder()}</tt> 
 *  and using {@link TimeBuilder its methods}. Automatic Rule-collapse 
 *  uses this time-tree, and all Techniques used in solving the 
 *  target should do so, too.</p>
 * 
 * @author fiveham
 *
 */
public class Puzzle extends SudokuNetwork{ //FIXME one Claim is absent from the Puzzle
	
	/**
	 * <p>A three-dimensional array of this Puzzle's Claims.</p>
	 */
	private SpaceMap claims;
	
	/**
	 * <p>A list of all the valid dimension-independent values 
	 * for coordinates in this Puzzle.</p>
	 */
	private List<IndexValue> indices;
	
	/**
	 * <p>A list of lists which contain dimension-associated valid 
	 * coordinate values for this Puzzle.</p>
	 */
	private List<List<IndexInstance>> dimensions;
	
	/**
	 * <p>Constructs a Puzzle using the text in the specified file <tt>f</tt>.</p>
	 * 
	 * @param f the file containing the target in text form at the start of the file
	 * 
	 * @throws FileNotFoundException if <tt>f</tt> cannot be found or read
	 */
	public Puzzle(File f) throws FileNotFoundException{
		this(new Scanner(f), null);
	}
	
	/**
	 * <p>Constructs a Puzzle using the specified text source <tt>s</tt>.</p>
	 * 
	 * @param s the target in text
	 */
	public Puzzle(String s){
		this(new Scanner(s), null);
	}
	
	/**
	 * <p>Constructs a Puzzle based on the text scanned by <tt>s</tt>.</p>
	 * 
	 * @param s a Scanner whose {@link Scanner#next() output} is used to 
	 * create the Puzzle object
	 */
	public Puzzle(Scanner s){
		this(s, null);
	}
	
	private Puzzle(Scanner s, Parser parser){
		super((parser = new Parser(s)).mag());
		
		this.indices = genIndices(sideLength, this);
		this.dimensions = genDimensions(indices, this);
		
		this.claims = new SpaceMap(this);
		this.nodes.addAll(genRuleNodes(this, sideLength, claims));
		this.nodes.ensureCapacity(nodes.size()+sideLength*sideLength*sideLength);
		StreamSupport.stream(claims.spliterator(), false).forEach((claim)->nodes.add(claim));
		
		for(Claim c : parseText(parser.values())){
			Init specificValue = new Init(this);
			nodes.add(specificValue);
			specificValue.add(c);
		}
	}
	
	/**
	 * <p>Generates the Rules for <tt>p</tt>.</p>
	 * 
	 * @param p the Puzzle whose Rules are being generated
	 * @param sideLength the pre-computed side-length of <tt>p</tt>
	 * @param claims the pre-built array of Claims in <tt>p</tt>
	 * @return a list of the Rules for <tt>p</tt>
	 */
	private List<Rule> genRuleNodes(Puzzle p, int sideLength, SpaceMap claims){
		List<Rule> factbags = new ArrayList<>(RegionSpecies.values().length * p.sideLength * p.sideLength * p.sideLength);
		for(RegionSpecies region : RegionSpecies.values()){
			for(IndexInstance dimA : region.dimA(p)){
				for(IndexInstance dimB : region.dimB(p)){
					Rule regionBag = new Rule(p, region, sideLength, dimA, dimB);
					for(IndexInstance dimC : region.dimInsideRule(p)){
						regionBag.add( claims.get(dimA, dimB, dimC) );
					}
					factbags.add(regionBag);
				}
			}
		}
		return factbags;
	}
	
	/**
	 * <p>Generates the lists of dimension-associated valid coordinate values for 
	 * <tt>p</tt>.</p>
	 * @param indices a pre-computed list of the valid coordinate values for <tt>p</tt>
	 * @param p the Puzzle for which these dimensions are being generated
	 * @see #dimensions
	 * @return a list of the lists of dimension-associated valid coordinate values 
	 * for <tt>p</tt>
	 */
	private static List<List<IndexInstance>> genDimensions(List<IndexValue> indices, Puzzle p){
		List<List<IndexInstance>> dimensions = new ArrayList<>(DimensionType.values().length);
		for(DimensionType type : DimensionType.values()){
			List<IndexInstance> indexInstancesForType = new ArrayList<>(indices.size());
			for(IndexValue i : indices){
				indexInstancesForType.add( new IndexInstance(type, i) );
			}
			dimensions.add(indexInstancesForType);
		}
		return dimensions;
	}
	
	/**
	 * <p>Generates the {@link #indices indices} for a Puzzle with 
	 * the specified <tt>sideLength</tt>.</p>
	 * @param sideLength the pre-computed side-length of <tt>p</tt>
	 * @param p the target for which the {@link #indices indices} are 
	 * being generated
	 * @see #indices
	 * @return the list of {@link #indices indices} generated for <tt>p</tt>
	 */
	private static List<IndexValue> genIndices(int sideLength, Puzzle p){
		List<IndexValue> indices = new ArrayList<>(sideLength);
		for(int i=0; i<sideLength; ++i){
			indices.add(new IndexValue(p, i));
		}
		return indices;
	}
	
	/**
	 * <p>Returns this target's SpaceMap of claims.</p>
	 * @return this target's SpaceMap of claims
	 */
	public SpaceMap claims(){
		return claims;
	}
	
	/**
	 * <p>Returns a list (sorted) of all the {@link #IndexValue index values} 
	 * that exist for this Puzzle.</p>
	 * 
	 * @return a list (sorted) of all the {@link #IndexValue index values} 
	 * that exist for this Puzzle.
	 */
	public List<IndexValue> indexValues(){
		return new ArrayList<>(indices);
	}
	
	/**
	 * <p>Returns the IndexValue in this Puzzle that is equivalent to the 
	 * specified int.</p>
	 * 
	 * @param i the int whose equivalent IndexValue will be returned
	 * @return the IndexValue for this Puzzle equivalent to <tt>i</tt>
	 */
	public IndexValue indexFromInt(int i){
		try{
			return indices.get(i);
		} catch(IndexOutOfBoundsException e){
			throw new IllegalArgumentException("Specified i="+i+" out of bounds: 0 to "+(indices.size()-1), e);
		}
	}
	
	/**
	 * <p>Returns the in-memory IndexValue corresponding to the specified 
	 * human-readable integer value.</p>
	 * 
	 * <p>This is used to convert the 1-based human-readable integers 
	 * in a target's source file into the 0-based in-memory IndexValues 
	 * used by Puzzle to specify values of spatial coordinates.</p>
	 * 
	 * @param i a 1-based human-readable integer from a target's source 
	 * text file
	 * @return a 0-based IndexValue corresponding to the 1-based integer 
	 * <tt>i</tt>.
	 */
	public IndexValue indexFromHumanReadableInt(int i){
		return indexFromInt(i-1);
	}
	
	/**
	 * <p>Returns this Puzzle's list of indices.</p>
	 * @return this Puzzle's list of indices
	 */
	List<IndexValue> getIndices(){
		return indices;
	}
	
	/**
	 * <p>Returns the minimum IndexValue in this Puzzle.</p>
	 * @return the minimum IndexValue in this Puzzle.
	 */
	public IndexValue minIndex(){
		return indices.get(0);
	}
	
	/**
	 * <p>Returns the maximum IndexValue in this Puzzle.</p>
	 * @return the maximum IndexValue in this Puzzle.
	 */
	public IndexValue maxIndex(){
		return indices.get(indices.size()-1);
	}
	
	/**
	 * <p>Returns true if this Puzzle is solved, false otherwise.</p>
	 * @return true if this Puzzle is solved, false otherwise.
	 */
	public boolean isSolved(){
		return factStream().allMatch((rule)->rule.size()==Fact.SIZE_WHEN_SOLVED);
	}
	
	/**
	 * <p>Returns the Claim whose location in the {@link #claims SpaceMap} 
	 * corresponds to the specified values of the specified dimensions.</p>
	 * 
	 * <p>An IndexInstance pairs an IndexValue with a {@link Puzzle.DimensionType dimension}, 
	 * enabling rows, cells, columns, and boxes to unambiguously specify 
	 * coordinates in ways that make sense for those {@link #RegionSpecies region types}.</p>
	 * 
	 * @param dim1 the value in the first dimension
	 * @param dim2 the value in the second dimension
	 * @param heldConstant the value in the third dimension, which is constant 
	 * for a given Rule, regardless of type; though the second dimension value 
	 * is also held constant for non-box Rules.
	 * @return the Claim whose location in the {@link #claims SpaceMap} 
	 * corresponds to the specified values of the specified dimensions.
	 */
	public Claim getClaimAt(IndexInstance dim1, IndexInstance dim2, IndexInstance heldConstant){
		IndexValue x = decodeX(dim1, dim2, heldConstant);
		IndexValue y = decodeY(dim1, dim2, heldConstant);
		IndexValue s = decodeSymbol(dim1, dim2, heldConstant);
		
		return claims.get(x, y, s);
	}
	
	/**
	 * <p>A convenience method that returns the outputs of 
	 * {@link #decodeX(IndexInstance...) decodeX}, 
	 * {@link #decodeY(IndexInstance...) decodeY}, and 
	 * {@link #decodeSymbol(IndexInstance...) decodeSymbol}, 
	 * as an ordered triple in the form of an array.</p>
	 * @param dims dimension args passed on to decodeX, 
	 * decodeY, and decodeSymbol.
	 * @return an ordered triple of the outputs of decodeX, 
	 * decodeY, and decodeSymbol given the same args this 
	 * method received
	 */
	public IndexValue[] decodeXYZ(IndexInstance... dims){
		return new IndexValue[]{		
			decodeX(dims),
			decodeY(dims),
			decodeSymbol(dims)
		};
	}
	
	/**
	 * <p>Returns the x-component of the point in space specified 
	 * by <tt>dims</tt>. x-components of each element of <tt>dims</tt> 
	 * are determined and summed, and {@link #indexFromInt(int) the corresponding IndexValue} 
	 * is returned.</p>
	 * @param dims the x-component of the geometric point, line, or 
	 * plane specified by these IndexInstances will be returned in 
	 * the form of an IndexValue pertaining to this Puzzle.
	 * @return the x-component of the point in space specified 
	 * by <tt>dims</tt>
	 */
	public IndexValue decodeX(IndexInstance... dims){
		return decodeDim((d) -> d.contributionX(), dims);
	}
	
	/**
	 * <p>Returns the y-component of the point in space specified 
	 * by <tt>dims</tt>. y-components of each element of <tt>dims</tt> 
	 * are determined and summed, and {@link #indexFromInt(int) the corresponding IndexValue} 
	 * is returned.</p>
	 * @param dims the y-component of the geometric point, line, or 
	 * plane specified by these IndexInstances will be returned in 
	 * the form of an IndexValue pertaining to this Puzzle.
	 * @return the y-component of the point in space specified 
	 * by <tt>dims</tt>
	 */
	public IndexValue decodeY(IndexInstance... dims){
		return decodeDim((d) -> d.contributionY(), dims);
	}
	
	/**
	 * <p>Returns the z-component of the point in space specified 
	 * by <tt>dims</tt>. z-components of each element of <tt>dims</tt> 
	 * are determined and summed, and {@link #indexFromInt(int) the corresponding IndexValue} 
	 * is returned.</p>
	 * @param dims the z-component of the geometric point, line, or 
	 * plane specified by these IndexInstances will be returned in 
	 * the form of an IndexValue pertaining to this Puzzle.
	 * @return the z-component of the point in space specified 
	 * by <tt>dims</tt>
	 */
	public IndexValue decodeSymbol(IndexInstance... dims){
		return decodeDim((d) -> d.contributionZ(), dims);
	}
	
	/**
	 * <p>The number of physical spatial dimensions ({@value}) pertinent 
	 * to this model of a sudoku target.</p>
	 */
	public static final int DIMENSION_COUNT = 3;
	
	/**
	 * <p>Decodes the specified component of the specified dimension-values 
	 * and returns the {@link #indexFromInt(int) corresponding IndexValue}. 
	 * Performs the actual work for decodeX, decodeY, and decodeSymbol.</p>
	 * @param contrib a function used to specify which dimension's component 
	 * will be used for the elements of <tt>dims</tt> to produce a result.
	 * @param dims dimension-values indicating a point (or other, non-point 
	 * primitive geometric object) in space.
	 * @return the IndexValue {@link #indexFromInt(int) corresponding} to 
	 * the combined specified dimensional contributions of the specified 
	 * dimension-values.
	 */
	private IndexValue decodeDim(Function<IndexInstance,Integer> contrib, IndexInstance[] dims){
		int score = 0;
		for(IndexInstance dim : dims){
			score += contrib.apply(dim);
		}
		return indexFromInt(score);
	}
	
	/**
	 * <p>Returns a string representing the target, with each 
	 * cell represented by its solved value if known or by a 
	 * space if the cell's value is unknown.</p>
	 * @return a string representing the target, with each 
	 * cell represented by its solved value if known or by a 
	 * space if the cell's value is unknown
	 */
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		for(IndexValue y : indices){
			for(IndexValue x : indices){
				result.append( claims.getPrintingValue(x,y) ).append(" ");
			}
			result.append(System.lineSeparator());
		}
		
		return result.toString();
	}
	
	/**
	 * <p>Returns a string representation of the Puzzle, with 
	 * each cell represented by a string of <tt>sideLength</tt> 
	 * characters, each {@link Claim#possText() character} 
	 * pertaining to a possible value of the cell.</p>
	 * @return a string representation of the Puzzle, with 
	 * each cell represented by a string of <tt>sideLength</tt> 
	 * characters, each {@link Claim#possText() character} 
	 * pertaining to a possible value of the cell
	 */
	public String toStringWithPossibilities(){
		StringBuilder result = new StringBuilder();
		
		for(IndexValue y : indices){
			for(IndexValue x : indices){
				for(IndexValue z : indices){
					result.append(claims.get(x,y,z).possText());
				}
				result.append("|");
			}
			result.append(System.getProperty("line.separator"));
		}
		
		return result.toString();
	}
	
	/**
	 * <p>Returns the index of the box that includes the specified 
	 * x and y coordinates.</p>
	 * @param x the x-coordinate of a point whose surrounding box's 
	 * index is returned
	 * @param y the y-coordinate of a point whose surrounding box's
	 * index is returned
	 * @return the index of the box that includes the specified 
	 * x and y coordinates
	 */
	public IndexValue boxIndex(IndexValue x, IndexValue y){
		return indices.get( x.intValue()/magnitude + y.intValue()/magnitude*magnitude );
	}
	
	/**
	 * <p>Returns the x-coordinate of the edge of the specified box 
	 * with the lowest x-coordinate.</p>
	 * @param boxIndex the index of the box whose lower-x-coordinate 
	 * edge's x-coordinate is returned. The index of a box follows a 
	 * snaking pattern from the upper left with box 0, moving rightward 
	 * and increasing, then wrapping around to the next line of boxes 
	 * below as needed until the lower right is reached.
	 * @return the x-coordinate of the lower-x-coordinate edge of 
	 * the box in this target with the specified index
	 */
	public int boxLowX(IndexValue boxIndex){
		return boxLowX(boxIndex, magnitude);
	}
	
	private static int boxLowX(IndexValue boxIndex, int mag){
		return mag*X_POS_COMP_CELL.apply(boxIndex, mag);
	}
	
	/**
	 * <p>Returns the y-coordinate of the edge of the specified box 
	 * with the lowest y-coordinate.</p>
	 * @param boxIndex the index of the box whose lower-y-coordinate 
	 * edge's y-coordinate is returned. The index of a box follows a 
	 * snaking pattern from the upper left with box 0, moving rightward 
	 * and increasing, then wrapping around to the next line of boxes 
	 * below as needed until the lower right is reached.
	 * @return the y-coordinate of the lower-y-coordinate edge of 
	 * the box in this target with the specified index
	 */
	public int boxLowY(IndexValue boxIndex){
		return boxLowY(boxIndex, magnitude);
	}
	
	private static int boxLowY(IndexValue boxIndex, int mag){
		return mag*Y_POS_COMP_CELL.apply(boxIndex, mag);
	}
	
	/**
	 * <p>Returns a list of those Claims from {@link #claims the SpaceMap} 
	 * that are true, given the content of the source file for this target.
	 * @param values a list of the integer values of the cells of this target 
	 * as read from this target's source text. {@value #BLANK_CELL} indicates 
	 * an empty cell.</p>
	 * @return a list of those Claims from {@link #claims the SpaceMap} 
	 * that are true, given the content of the source file for this target
	 */
	private List<Claim> parseText(List<Integer> values){
		List<Claim> knownTrueClaims = new ArrayList<>();
		
		int pointer = 0;
		for(IndexValue y : indices){
			for(IndexValue x : indices){
				int value;
				try{
					value = values.get(pointer++);
				} catch(IndexOutOfBoundsException e){
					throw new IllegalArgumentException("Fewer than "+ sideLength*sideLength +" tokens in source text", e);
				}
				
				if(value != BLANK_CELL){
					knownTrueClaims.add( claims.get(x,y, indexFromHumanReadableInt(value)) );
				}
			}
		}
		return knownTrueClaims;
	}
	
	/**
	 * <p>The value ({@value #BLANK_CELL}) used internally to refer to a blank 
	 * cell or to the value in a blank cell.</p>
	 */
	public static final int BLANK_CELL = 0;
	
	/**
	 * <p>Entries in this enum describe properties of the four types of regions in a sudoku target: 
	 * <ul><li>box</li><li>row</li><li>column</li><li>cell</li></ul></p>
	 * 
	 * <p>The first two dimensions of a RegionSpecies are held constant for any 
	 * one Rule (in regards to the Rule's initial set of Claims) with that RegionSpecies 
	 * as its type, while the RegionSpecies's third dimension is fully explored within 
	 * the aforementioned Rule.</p>
	 * @author fiveham
	 *
	 */
	public static enum RegionSpecies{
		
		/**
		 * <p>For a cell, the first dimension is {@link Puzzle.DimensionType#Y y}, the 
		 * second dimension is {@link Puzzle.DimensionType#X x}, and the third dimension 
		 * is {@link Puzzle.DimensionType#SYMBOL z}.</p>
		 */
		CELL	(DimensionType.Y, 	   DimensionType.X,   DimensionType.SYMBOL,         (byte)1), 
		
		/**
		 * <p>For a box, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the 
		 * second dimension is {@link Puzzle.DimensionType#BOX box-index}, and the third dimension 
		 * is {@link Puzzle.DimensionType#CELL_ID_IN_BOX cell-index}.</p>
		 */
		BOX		(DimensionType.SYMBOL, DimensionType.BOX, DimensionType.CELL_ID_IN_BOX, (byte)2), 
		
		/**
		 * <p>For a row, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the 
		 * second dimension is {@link Puzzle.DimensionType#Y y}, and the third dimension 
		 * is {@link Puzzle.DimensionType#X x}.</p>
		 */
		ROW		(DimensionType.SYMBOL, DimensionType.Y,   DimensionType.X,              (byte)4),
		
		/**
		 * <p>For a box, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the 
		 * second dimension is {@link Puzzle.DimensionType#X x}, and the third dimension 
		 * is {@link Puzzle.DimensionType#Y y}.</p>
		 */
		COLUMN	(DimensionType.SYMBOL, DimensionType.X,   DimensionType.Y,              (byte)8);
		
		private final DimensionType dimAType;
		private final DimensionType dimBType;
		private final DimensionType dimCType;
		private final byte mask;
		
		private RegionSpecies(DimensionType dimAType, DimensionType dimBType, DimensionType dimCType, byte mask){
			this.dimAType = dimAType;
			this.dimBType = dimBType;
			this.dimCType = dimCType;
			this.mask = mask;
		}
		
		/**
		 * <p>The bitmask for this region type.</p>
		 * 
		 * <p>Cell is 1, Box is 2, Row is 4, and Column is 8.</p>
		 */
		public byte mask(){
			return mask;
		}
		
		/**
		 * <p>Returns a list of all the dimension-value pairs for 
		 * the first dimension of a region of this species in 
		 * the specified Puzzle.</p>
		 * @param p the Puzzle to which the returned IndexInstances 
		 * belong
		 * @return a list of all the dimension-value pairs for the 
		 * first dimension of a region of this species in the specified 
		 * Puzzle
		 */
		public List<IndexInstance> dimA(Puzzle p){
			return p.dimensions.get(dimAType.ordinal());
		}
		
		/**
		 * <p>Returns a list of all the dimension-value pairs for 
		 * the second dimension of a region of this species in 
		 * the specified Puzzle.</p>
		 * @param p the Puzzle to which the returned IndexInstances 
		 * belong
		 * @return a list of all the dimension-value pairs for the 
		 * second dimension of a region of this species in the specified 
		 * Puzzle
		 */
		public List<IndexInstance> dimB(Puzzle p){
			return p.dimensions.get(dimBType.ordinal());
		}
		
		/**
		 * <p>Returns a list of all the dimension-value pairs for 
		 * the third dimension of a region of this species in 
		 * the specified Puzzle.</p>
		 * @param p the Puzzle to which the returned IndexInstances 
		 * belong
		 * @return a list of all the dimension-value pairs for the 
		 * third dimension of a region of this species in the specified 
		 * Puzzle
		 */
		public List<IndexInstance> dimInsideRule(Puzzle p){
			return p.dimensions.get(dimCType.ordinal());
		}
	}
	
	/**
	 * <p>A wrapper for IndexValue that internally specifies to which 
	 * {@link Puzzle.DimensionType dimension} (of which five are available) the
	 * wrapped IndexValue pertains.</p>
	 * @author fiveham
	 *
	 */
	public static class IndexInstance{
		private DimensionType type;
		private IndexValue val;
		
		/**
		 * <p>Constructs an IndexInstance belonging to the <tt>target</tt>, pertaining 
		 * to the specified dimension <tt>type</tt> and having the value <tt>val</tt>.</p>
		 * @param target the Puzzle to which this IndexInstance belongs
		 * @param type the dimension along which this IndedInstance lies
		 * @param val the value of this IndexInstance's position along its dimension
		 */
		public IndexInstance(DimensionType type, IndexValue val){
			this.type = type;
			this.val = val;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof IndexInstance){
				IndexInstance d = (IndexInstance) o;
				return d.type.equals(type) && d.val.equals(val);
			}
			return false;
		}
		
		public int intValue(){
			return val.intValue();
		}
		
		/**
		 * <p>The {@link Puzzle.DimensionType dimension type} of this 
		 * IndexInstance.</p>
		 * @return {@link Puzzle.DimensionType dimension type} of this 
		 * IndexInstance
		 */
		public DimensionType getType(){
			return type;
		}
		
		/**
		 * <p>The x-component of the wrapped IndexValue given that it 
		 * specifies a position along this IndexInstance's wrapped 
		 * {@link #getType() dimension}.</p>
		 * @return the x-component of the wrapped IndexValue given 
		 * that it pertains to the wrapped dimension
		 */
		public int contributionX(){
			return type.contribX.apply(val, val.puzzle.magnitude);
		}

		/**
		 * <p>The y-component of the wrapped IndexValue given that it 
		 * specifies a position along this IndexInstance's wrapped 
		 * {@link #getType() dimension}.</p>
		 * @return the y-component of the wrapped IndexValue given 
		 * that it pertains to the wrapped dimension
		 */
		public int contributionY(){
			return type.contribY.apply(val, val.puzzle.magnitude);
		}

		/**
		 * <p>The z-component of the wrapped IndexValue given that it 
		 * specifies a position along this IndexInstance's wrapped 
		 * {@link #getType() dimension}.</p>
		 * @return the z-component of the wrapped IndexValue given 
		 * that it pertains to the wrapped dimension
		 */
		public int contributionZ(){
			return type.contribZ.apply(val, val.puzzle.magnitude);
		}
	}
	
	/**
	 * <p>A dimensional component contribution function that provides 
	 * a contribution of zero regardless of its inputs.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> ZERO       = (indx,mag) -> 0;
	
	/**
	 * <p>A dimensional component contribution function that produces 
	 * all the the input dimension value as output.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> INT_VALUE  = (indx,mag) -> indx.intValue();
	
	/**
	 * <p>A dimensional component contribution function providing the x-dimensional contribution 
	 * of a value along the {@link Puzzle.DimensionType#CELL_ID_IN_BOX cell-index-in-box dimension}.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> X_POS_COMP_CELL = (indx,mag) -> (indx.intValue()%mag);
	
	/**
	 * <p>A dimensional component contribution function providing the y-dimensional contribution 
	 * of a value along the {@link Puzzle.DimensionType#CELL_ID_IN_BOX cell-index-in-box dimension}.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> Y_POS_COMP_CELL = (indx,mag) -> (indx.intValue()/mag);
	
	/**
	 * <p>A dimensional component contribution function providing the x-dimensional contribution 
	 * of a value along the {@link Puzzle.DimensionType#BOX box-index dimension}.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> X_POS_COMP_BOX = (indx,mag) -> mag*X_POS_COMP_CELL.apply(indx, mag);
	
	/**
	 * <p>A dimensional component contribution function providing the y-dimensional contribution 
	 * of a value along the {@link Puzzle.DimensionType#BOX box-index dimension}.</p>
	 */
	public static final BiFunction<IndexValue,Integer,Integer> Y_POS_COMP_BOX = (indx,mag) -> mag*Y_POS_COMP_CELL.apply(indx, mag);
	
	/**
	 * <p>This enum specifies the properties of the five claim-coordinating 
	 * dimensions available in this model of a sudoku target.</p>
	 * @author fiveham
	 *
	 */
	public static enum DimensionType{
		
		/**
		 * <p>The conventional x dimension. An IndexInstance in this 
		 * dimension contributes all of its value to the x dimension 
		 * and contributes zero to y and z.</p>
		 */
		X				(INT_VALUE,       ZERO,            ZERO),
		
		/**
		 * <p>The conventional y dimension. An IndexInstance in this 
		 * dimension contributes all of its value to the y dimension 
		 * and contributes zero to x and z.</p>
		 */
		Y				(ZERO,            INT_VALUE,       ZERO),
		
		/**
		 * <p>The conventional z dimension. An IndexInstance in this 
		 * dimension contributes all of its value to the z dimension 
		 * and contributes zero to x and y.</p>
		 */
		SYMBOL			(ZERO,            ZERO,            INT_VALUE), 
		
		/**
		 * <p>The dimension for box-index within the target. This dimension 
		 * snakes through the target from the upper left corner (low x,y), 
		 * increases to the right (higher x), then jumps back to the next 
		 * row once it reaches maximum x, continuing in this fashion until 
		 * it reaches the lower right corner (high x,y).</p>
		 */
		BOX				(X_POS_COMP_BOX,  Y_POS_COMP_BOX,  ZERO), 
		
		/**
		 * <p>The dimension for cell-index within a box. This dimension 
		 * snakes through a box-sized area from the upper left corner (low x,y), 
		 * increases to the right (higher x), then jumps back to the next 
		 * row once it reaches maximum x within the box, continuing in this fashion until 
		 * it reaches the lower right corner (high x,y) of the box.</p>
		 */
		CELL_ID_IN_BOX	(X_POS_COMP_CELL, Y_POS_COMP_CELL, ZERO);
		
		private final BiFunction<IndexValue,Integer,Integer> contribX;
		private final BiFunction<IndexValue,Integer,Integer> contribY;
		private final BiFunction<IndexValue,Integer,Integer> contribZ;
		
		private DimensionType(BiFunction<IndexValue,Integer,Integer> contribX, BiFunction<IndexValue,Integer,Integer> contribY, BiFunction<IndexValue,Integer,Integer> contribZ){
			this.contribX = contribX;
			this.contribY = contribY;
			this.contribZ = contribZ;
		}
	}
	
	/**
	 * <p>A wrapper for an integer value valid for use as a coordinate in this 
	 * Puzzle along any of its Claim-coordinating dimensions.</p>
	 * 
	 * <p>The number of index values a given Puzzle has is equal to the 
	 * side-length of the target and thus may vary. In order to allow a 
	 * Puzzle to take on an arbitrary size depending solely on the Puzzle's 
	 * source text, its allowable index values must be determined at runtime 
	 * instead of being hard-coded in an enum as has previously been the 
	 * case. The use of an enum is preferable over the use of raw primitive 
	 * data types such as <tt>int</tt> because <tt>int</tt> and other 
	 * applicable types can vary over a wider range than a Puzzle ever needs 
	 * and thus need to be range-checked. Another reason to avoid the use of 
	 * primitive data types for the purposes that IndexValue fulfills is that 
	 * both an enum and a runtime-generated list of non-enum values allow 
	 * less verbose iteration, not having to explicitly range-check an int 
	 * against the target's sidelength for instance.</p>
	 * 
	 * @author fiveham
	 *
	 */
	public static class IndexValue{
		
		/**
		 * <p>The Puzzle to which this IndexValue belongs.</p>
		 */
		private final Puzzle puzzle;
		
		/**
		 * <p>The wrapped int value.</p>
		 */
		private final int v;
		
		/**
		 * <p>Constructs an IndexValue belonging to <tt>target</tt> and 
		 * wrapping <tt>v</tt>.</p>
		 * @param target the Puzzle to which this IndexValue belongs
		 * @param v the int value wrapped by this IndexValue
		 */
		private IndexValue(Puzzle puzzle, int v){
			this.puzzle = puzzle;
			this.v = v;
		}
		
		/**
		 * <p>The wrapped int value.</p>
		 * @return the wrapped int value
		 */
		public int intValue(){
			return v;
		}
		
		/**
		 * <p>The target that owns and created this IndexValue. This 
		 * IndexValue is only valid in this Puzzle.</p>
		 * @return the Puzzle that owns and created this IndedValue
		 */
		public Puzzle getPuzzle(){
			return puzzle;
		}
		
		/**
		 * <p>Returns this IndexValue's wrapped int value plus 1, translating the 
		 * internal 0-based values to the conventional 1-based values used in written 
		 * sudoku puzzles.</p>
		 * @return the wrapped integer plus 1
		 */
		public int humanReadableIntValue(){
			return v+1;
		}
		
		/**
		 * <p>Returns the human-readable representation of the wrapped integer value.</p>
		 * @return a string representation of the wrapped int value plus 1
		 */
		public String humanReadableSymbol(){
			return Integer.toString(humanReadableIntValue(), puzzle.sideLength+1);
		}
	}
}
