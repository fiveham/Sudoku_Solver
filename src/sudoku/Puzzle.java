package sudoku;

import common.Pair;
import common.Universe;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import sudoku.parse.Parser;
import sudoku.parse.SadmanParser;
import sudoku.parse.TxtParser;

/**
 * <p>Represents a sudoku puzzle as a bipartite graph of {@link Claim truth-claims} such as "Cell
 * 1,7 has value 3." and {@link Fact groupings of claims} of which exactly one claim per grouping is
 * true.</p>
 * @author fiveham
 */
public class Puzzle extends SudokuNetwork{
	
  /**
   * <p>The index for the x-dimension in an ordered triple.</p>
   */
	public static final int X_DIM = 0;
	
  /**
   * <p>The index for the y-dimension in an ordered triple.</p>
   */
	public static final int Y_DIM = 1;
	
  /**
   * <p>The index for the z-dimension in an ordered triple.</p>
   */
	public static final int Z_DIM = 2;
	
  /**
   * <p>A three-dimensional array of this Puzzle's Claims.</p>
   */
	private SpaceMap claims;
	
  /**
   * <p>a list of the valid coordinate values for this puzzle's dimensions.</p>
   */
	private List<IndexValue> indices;
	
  /**
   * <p>A list of lists of all the dimensional indices for this Puzzle.</p>
   */
	private List<List<IndexInstance>> dimensions;
	
	private final Universe<Fact> factUniverse;
	private final Universe<Claim> claimUniverse;
	
  /**
   * <p>Constructs a Puzzle using the text in {@code f}.</p>
   * @param f the file containing the puzzle in text form
   * @throws FileNotFoundException if {@code f} cannot be found or read
   */
	public Puzzle(File f, String charset) throws FileNotFoundException{
		this(chooseParser(f, charset));
	}
	
	/**
	 * <p>Constructs a Puzzle using the information supplied by {@code parser}.</p>
	 * @param parser tells the constructor what the magnitude and initial values are for this Puzzle
	 */
	public Puzzle(Parser parser){
		super(parser.mag());
		
		this.indices = genIndices(sideLength, this);
		this.dimensions = genDimensions(indices, this);
		
		this.claims = new SpaceMap(this);
		this.nodes.addAll(genRuleNodes(this, sideLength, claims));
		this.nodes.ensureCapacity(nodes.size()+sideLength*sideLength*sideLength);
		StreamSupport.stream(claims.spliterator(), false).forEach(nodes::add);
		
		for(Claim c : parseText(parser.values())){
			Init specificValue = new Init(this, c);
			nodes.add(specificValue);
		}
		
		this.claimUniverse = new Universe<>(claimStream());
		this.factUniverse = new Universe<>(factStream());
	}
	
	/**
	 * <p>Chooses which type of Parser should be used to interpret {@code f}.</p>
	 * @param f the file to be read
	 * @param charset the name of the charset to be used to read {@code f}
	 * @return a Parser that parses the contents of {@code f}
	 * @throws FileNotFoundException if {@code f} could not be read
	 * @throws IllegalArgumentException if {@code f} has an extension other than .txt or .sdk
	 */
	private static Parser chooseParser(File f, String charset) throws FileNotFoundException{
		String extension = f.getName();
		extension = extension.substring(extension.lastIndexOf("."));
		
		switch(extension){
		case ".txt": return new TxtParser(f, charset);
		case ".sdk": return new SadmanParser(f, charset);
		default: throw new IllegalArgumentException(
		    "Illegal file extension " + extension + " only .txt and .sdk allowed.");
		}
	}
	
  /**
   * <p>Generates the Rules for {@code p}.</p>
   * @param p a sudoku puzzle
   * @param sideLength the side-length of {@code p}
   * @param claims {@code p}'s SpaceMap of Claims
   * @return a list of the Rules for {@code p}
   */
	private List<Rule> genRuleNodes(Puzzle p, int sideLength, SpaceMap claims){
		List<Rule> rules = new ArrayList<>(
		    RuleType.values().length * (int) Math.pow(p.sideLength, DIMENSION_COUNT));
		for(RuleType type : RuleType.values()){
			for(IndexInstance dimA : type.dimA(p)){
				for(IndexInstance dimB : type.dimB(p)){
					Rule newerRule = new Rule(
							p, 
							type, 
							type.dimInsideRule(p).stream()
									.map((dimC) -> claims.get(dimA, dimB, dimC))
									.collect(Collectors.toList()), 
							dimA, 
							dimB);
					rules.add(newerRule);
				}
			}
		}
		return rules;
	}
	
  /**
   * <p>Generates the lists of dimension-associated valid coordinate values for {@code p}.</p>
   * @param indices the valid dimensional positions in {@code p}
   * @param p the Puzzle to which these dimensional manifests belong
   * @see #dimensions
   * @return the lists of dimension-associated valid coordinate values for {@code p}
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
   * <p>Generates the {@link #indices legal dimensional positions} for a Puzzle with the specified
   * {@code sideLength}.</p>
   * @param sideLength the side-length of {@code p}
   * @param p a sudoku puzzle
   * @see #indices
   * @return the {@link #indices legal dimensional positions} for {@code p}
   */
	private static List<IndexValue> genIndices(int sideLength, Puzzle p){
		List<IndexValue> indices = new ArrayList<>(sideLength);
		for(int i = 0; i < sideLength; ++i){
			indices.add(new IndexValue(p, i));
		}
		return Collections.unmodifiableList(indices);
	}
	
  /**
   * <p>Returns the SpaceMap coordinating this puzzle's Claims.</p>
   * @return the SpaceMap coordinating this puzzle's Claims
   */
	public SpaceMap claims(){
		return claims;
	}
	
	/**
	 * <p>Returns the Claims belonging to this Puzzle at position ({@code x}, {@code y}, {@code z}) in
	 * claim-space if this Puzzle has a Claim at that position.</p>
	 * @param x the x-coordinate to the Claim returned
	 * @param y the y-coordinate to the Claim returned
	 * @param z the z-coordinate to the Claim returned
	 * @return the Claims belonging to this Puzzle at position ({@code x}, {@code y}, {@code z}) in
   * claim-space if this Puzzle has a Claim at that position
	 * @throws ArrayIndexOutOfBoundsException if this Puzzle doesn't have a Claim at the specified 
	 * coordinates
	 */
	public Claim claim(int x, int y, int z){
		return claims.get(x, y, z);
	}
  
  /**
   * <p>Returns the {@link Universe} of all the Facts belonging to this Puzzle.</p>
   * @return the {@link Universe} of all the Facts belonging to this Puzzle
   */
	public Universe<Fact> factUniverse(){
		return factUniverse;
	}
	
	/**
	 * <p>Returns the {@link Universe} of all the Claims belonging to this Puzzle.</p>
	 * @return the {@link Universe} of all the Claims belonging to this Puzzle
	 */
	public Universe<Claim> claimUniverse(){
		return claimUniverse;
	}
	
  /**
   * <p>Returns a list (sorted) of all the {@link #IndexValue index values} that exist for this
   * Puzzle.</p>
   * @return a list (sorted) of all the {@link #IndexValue index values} that exist for this
   * Puzzle.
   */
	List<IndexValue> indexValues(){
		return indices;
	}
	
	/**
	 * <p>Returns a list of all the dimensional indices for this Puzzle pertaining to the specified 
	 * dimension.</p>
	 * @param dim the dimension whose pertinent dimensional indices are returned
	 * @return a list of all the dimensional indices for this Puzzle pertaining to the specified 
   * dimension
	 */
	List<IndexInstance> indexInstances(DimensionType dim){
		return dimensions.get(dim.ordinal());
	}
	
  /**
   * <p>Returns the IndexValue in this Puzzle that is equivalent to the specified int.</p>
   * @param i the int whose equivalent IndexValue will be returned
   * @return the IndexValue for this Puzzle equivalent to {@code i}
   * @throws IllegalArgumentException if {@code i} is negative or greater than or equal to this 
   * Puzzle's {@link #sideLength() sideLength}.
   */
	private IndexValue indexFromInt(int i){
		try{
			return indices.get(i);
		} catch(IndexOutOfBoundsException e){
			throw new IllegalArgumentException(e);
		}
	}
	
  /**
   * <p>Returns the IndexValue corresponding to the specified human-readable integer.</p>
   * <p>This is used to convert the 1-based human-readable integers in a puzzle's source text into 
   * the 0-based runtime IndexValues used by Puzzle to specify values of spatial coordinates.</p>
   * @param i a 1-based human-readable integer from a puzzle's source text file
   * @return a 0-based IndexValue corresponding to the 1-based integer {@code i}.
   */
	private IndexValue indexFromHumanReadableInt(int i){
		return indexFromInt(i - 1);
	}
	
	/**
   * <p>A convenience method that returns the outputs of {@link #decodeX(IndexInstance...) decodeX},
   * {@link #decodeY(IndexInstance...) decodeY}, and {@link #decodeZ(IndexInstance...) decodeZ}, as 
   * an ordered triple in the form of an array.</p>
   * @param points dimensional indices passed on to decodeX, decodeY, and decodeZ
   * @return an ordered triple of the outputs of decodeX, decodeY, and decodeZ given the same 
   * parameter this method received
   */
	public IndexValue[] decodeXYZ(IndexInstance... points){
		return new IndexValue[]{		
			decodeX(points),
			decodeY(points),
			decodeZ(points)
		};
	}
	
	/**
   * <p>Returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the x-components of the specified {@code points}.</p>
   * @param points IndexInstances from this puzzle
   * @returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the x-components of the specified {@code points}
   */
	IndexValue decodeX(IndexInstance... dims){
		return decodeDim(IndexInstance::contributionX, dims);
	}
	
	/**
   * <p>Returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the y-components of the specified {@code points}.</p>
   * @param points IndexInstances from this puzzle
   * @returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the y-components of the specified {@code points}
   */
	IndexValue decodeY(IndexInstance... dims){
		return decodeDim(IndexInstance::contributionY, dims);
	}
	
  /**
   * <p>Returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the z-components of the specified {@code points}.</p>
   * @param points IndexInstances from this puzzle
   * @returns the IndexValue for this Puzzle {@link #indexFromInt(int) corresponding} to the sum
   * of the z-components of the specified {@code points}
   */
	IndexValue decodeZ(IndexInstance... points){
		return decodeDim(IndexInstance::contributionZ, points);
	}
	
  /**
   * <p>The number of physical spatial dimensions ({@value}) pertinent to this model of a sudoku
   * puzzle.</p>
   */
	public static final int DIMENSION_COUNT = 3;
	
  /**
   * <p>Decodes the specified dimensional component of the specified {@code dims} and returns the
   * {@link #indexFromInt(int) corresponding IndexValue}.</p>
   * <p>Performs the actual work for {@link #decodeX()}, {@link #decodeY}, and {@link #decodeZ}.</p>
   * @param dimComponent a function used to specify which dimension's component (x, y, or z) will 
   * be summed
   * @param points dimensional indices
   * @return the IndexValue {@link #indexFromInt(int) corresponding} to the combined specified
   * dimensional contributions of the specified dimensional indices
   */
	private IndexValue decodeDim(
	    Function<IndexInstance, Integer> dimComponent, 
	    IndexInstance[] points){
	  
		int score = 0;
		for(IndexInstance dim : points){
			score += dimComponent.apply(dim);
		}
		return indexFromInt(score);
	}
	
  /**
   * <p>Returns a string representing this puzzle. Each cell is represented by its value if known or
   * by a space if the cell's value is unknown.</p>
   * @return a string representing this puzzle. Each cell is represented by its value if known or
   * by a space if the cell's value is unknown
   */
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		class InterNumber implements Supplier<String>{
			private int useCount = 0;
			@Override
			public String get(){
			  StringBuilder result = new StringBuilder(" ");
				if(++useCount % sideLength() == 0){
					result.append(System.lineSeparator());
				}
				return result.toString();
			}
		}
		
		Supplier<String> betweenNumbers = new InterNumber();
		
		nodeStream()
				.filter(Rule.class::isInstance)
				.map(Rule.class::cast)
				.filter(RuleType.CELL::isTypeOf)
				.sorted((cell1, cell2) -> {
					int snake1 = cell1.dimB().intValue() + cell1.dimA().intValue() * sideLength();
					int snake2 = cell2.dimB().intValue() + cell2.dimA().intValue() * sideLength();
					return Integer.compare(snake1, snake2);
				})
				.forEach((cell) -> result.append(cell.isSolved() 
						? cell.iterator().next().getZ() 
						: BLANK_CELL)
						.append(betweenNumbers.get()));
		
		return result.toString();
	}
	
  /**
   * <p>Returns the x-coordinate of the low-X edge of the specified box. The index of a box
   * follows a snaking pattern from the upper left with box 0, moving rightward and increasing,
   * then wrapping around to the next line of boxes below as needed until the lower right is
   * reached.</p>
   * @param boxIndex the index of the box whose lower-x-coordinate edge's x-coordinate is
   * returned.
   * @return the x-coordinate of the low-X edge of the box in the {@code boxIndex}-th box of a
   * Puzzle whose {@link Sudoku#magnitude() magnitude} is {@code mag}
   */
	private static int boxLowX(IndexValue boxIndex, int mag){
		return mag * snakeInSquareX(boxIndex, mag);
	}
	
  /**
   * <p>Returns the y-coordinate of the low-Y edge of the box in this puzzle with the specified 
   * {@link DimensionType#BOX box-index}.</p>
   * @param boxIndex the index of a box in this puzzle
   * @return the y-coordinate of the low-Y edge of the {@code boxIndex}-th box in this puzzle
   */
	private static int boxLowY(IndexValue boxIndex, int mag){
		return mag * snakeInSquareY(boxIndex, mag);
	}
	
  /**
   * <p>Returns a list of the Claims that are known true as part of the initial state of the 
   * puzzle.</p>
   * @param values the initial values of the cells of this puzzle as read from its text source. 
   * A {@value #BLANK_CELL} indicates an empty cell.</p>
   * @return a list of the Claims that are known true as part of the initial state of the puzzle
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
					throw new IllegalArgumentException(
					    "Fewer than " + (sideLength * sideLength) + " tokens in source text", e);
				}
				
				if(value != BLANK_CELL){
					knownTrueClaims.add( claims.get(x,y, indexFromHumanReadableInt(value)) );
				}
			}
		}
		
		return knownTrueClaims;
	}
	
  /**
   * <p>The value ({@value #BLANK_CELL}) used internally to refer to a blank cell or to the value in
   * a blank cell.</p>
   */
	public static final int BLANK_CELL = 0;
	
  /**
   * <p>Entries in this enum describe properties of the four types of regions in a sudoku puzzle:
   * <ul>
   * <li>box</li>
   * <li>row</li>
   * <li>column</li>
   * <li>cell</li>
   * </ul></p>
   * <p>The Claims of a Rule whose type is a given RuleType all have the same value for the first 
   * and second dimensions of that RuleType. That Rule's RuleType's third dimension's values are all
   * represented in the Claims of that Rule.</p>
   * @author fiveham
	 */
	public static enum RuleType{
		
    /**
     * <p>For a box, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the second
     * dimension is {@link Puzzle.DimensionType#BOX box-index}, and the third dimension is
     * {@link Puzzle.DimensionType#CELL_ID_IN_BOX cell-index}.</p>
     */
		BOX(
		    DimensionType.SYMBOL, 
		    DimensionType.BOX, 
		    DimensionType.CELL_ID_IN_BOX, 
		    (rt, p) -> "The " + p.getA().val.humanReadableIntValue() 
		        + " in " + rt + " " + p.getB().val.humanReadableIntValue()), 
		
    /**
     * <p>For a row, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the second
     * dimension is {@link Puzzle.DimensionType#Y y}, and the third dimension is
     * {@link Puzzle.DimensionType#X x}.</p>
     */
		ROW(
		    DimensionType.SYMBOL, 
		    DimensionType.Y, 
		    DimensionType.X, 
		    (rt, p) -> "The " + p.getA().val.humanReadableIntValue() 
		        + " in " + rt + " " + p.getB().val.humanReadableIntValue()),
		
    /**
     * <p>For a column, the first dimension is {@link Puzzle.DimensionType#SYMBOL z}, the second
     * dimension is {@link Puzzle.DimensionType#X x}, and the third dimension is
     * {@link Puzzle.DimensionType#Y y}.</p>
     */
		COLUMN(
		    DimensionType.SYMBOL, 
		    DimensionType.X, 
		    DimensionType.Y, 
		    (rt, p) -> "The " + p.getA().val.humanReadableIntValue() 
		        + " in " + rt + " " + p.getB().val.humanReadableIntValue()), 
		
    /**
     * <p>For a cell, the first dimension is {@link Puzzle.DimensionType#Y y}, the second dimension 
     * is {@link Puzzle.DimensionType#X x}, and the third dimension is
     *  {@link Puzzle.DimensionType#SYMBOL z}.</p>
     */
		CELL(
		    DimensionType.Y, 
		    DimensionType.X, 
		    DimensionType.SYMBOL,	
		    (rt, p) -> "The value in " + rt + " " 
		        + p.getB().val.humanReadableIntValue() + "," 
		        + p.getA().val.humanReadableIntValue());
		
		private final DimensionType dimAType;
		private final DimensionType dimBType;
		private final DimensionType dimCType;
		private final BiFunction<RuleType, Pair<IndexInstance, IndexInstance>, String> description;
		
		private RuleType(
		    DimensionType dimAType, 
		    DimensionType dimBType, 
		    DimensionType dimCType, 
				BiFunction<RuleType, Pair<IndexInstance, IndexInstance>, String> description){
		  
			this.dimAType = dimAType;
			this.dimBType = dimBType;
			this.dimCType = dimCType;
			this.description = description;
		}
		
        /**
         * <p>Returns a description of a Rule of this type having the specified {@code dimA} and
         * {@code dimB}.</p>
         * @param dimA the first dimension of the Rule being described
         * @param dimB the second dimension of the Rule being described
         * @return a description of a Rule of this type having the specified {@code dimA} and
         * {@code dimB}
         */
		public String descriptionFor(IndexInstance dimA, IndexInstance dimB){
			return description.apply(this, new Pair<>(dimA, dimB));
		}
		
		public boolean isTypeOf(Rule r){
			return r.getType() == this;
		}
		
        /**
         * <p>Returns a list of all the dimension-value pairs for the first dimension of a region of
         * this species in the specified Puzzle.</p>
         * @param p the Puzzle to which the returned IndexInstances belong
         * @return a list of all the dimension-value pairs for the first dimension of a region of
         * this species in the specified Puzzle
         */
		public List<IndexInstance> dimA(Puzzle p){
			return p.indexInstances(dimAType);
		}
		
        /**
         * <p>Returns a list of all the dimension-value pairs for the second dimension of a region
         * of this species in the specified Puzzle.</p>
         * @param p the Puzzle to which the returned IndexInstances belong
         * @return a list of all the dimension-value pairs for the second dimension of a region of
         * this species in the specified Puzzle
         */
		public List<IndexInstance> dimB(Puzzle p){
			return p.indexInstances(dimBType);
		}
		
        /**
         * <p>Returns a list of all the dimension-value pairs for the third dimension of a region of
         * this species in the specified Puzzle.</p>
         * @param p the Puzzle to which the returned IndexInstances belong
         * @return a list of all the dimension-value pairs for the third dimension of a region of
         * this species in the specified Puzzle
         */
		public List<IndexInstance> dimInsideRule(Puzzle p){
			return p.indexInstances(dimCType);
		}
	}
	
    /**
<<<<<<< HEAD
     * <p>The number of dimensions that the Claims of a given Rule traverse.</p>
     */
	public static final int DIMENSIONS_INSIDE_RULE = 1;
	
  /**
   * <p>A wrapper for IndexValue that internally specifies to which
   * {@link Puzzle.DimensionType dimension} (of which five are available) the wrapped IndexValue
   * pertains.</p>
   * @author fiveham
=======
     * <p>A wrapper for IndexValue that internally specifies to which
     * {@link Puzzle.DimensionType dimension} (of which five are available) the wrapped IndexValue
     * pertains.</p>
     * @author fiveham
     * @author fiveham
	 *
>>>>>>> refactor
	 */
	public static class IndexInstance{
		private DimensionType type;
		private IndexValue val;
		
    /**
     * <p>Constructs an IndexInstance pertaining to the specified dimension {@code type} and having 
     * the value {@code val}.</p>
     * @param type the dimension along which this IndedInstance lies
     * @param val this IndexInstance's position along its dimension
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
		
		/**
		 * <p>The numerical value of this IndexInstance's position along its dimension.</p>
		 * @return the numerical value of this IndexInstance's position along its dimension
		 */
		public int intValue(){
			return val.intValue();
		}
		
    /**
     * <p>The {@link Puzzle.DimensionType dimension type} of this IndexInstance.</p>
     * @return {@link Puzzle.DimensionType dimension type} of this IndexInstance
     */
		public DimensionType getType(){
			return type;
		}
		
    /**
     * <p>The x-component of the wrapped IndexValue given that it specifies a position along this
     * IndexInstance's wrapped {@link #getType() dimension}.</p>
     * @return the x-component of the wrapped IndexValue given that it pertains to the wrapped
     * dimension
     */
		public int contributionX(){
			return type.contribX.apply(val, val.puzzle.magnitude);
		}
		
    /**
     * <p>The y-component of the wrapped IndexValue given that it specifies a position along this
     * IndexInstance's wrapped {@link #getType() dimension}.</p>
     * @return the y-component of the wrapped IndexValue given that it pertains to the wrapped
     * dimension
     */
		public int contributionY(){
			return type.contribY.apply(val, val.puzzle.magnitude);
		}
		
    /**
     * <p>The z-component of the wrapped IndexValue given that it specifies a position along this
     * IndexInstance's wrapped {@link #getType() dimension}.</p>
     * @return the z-component of the wrapped IndexValue given that it pertains to the wrapped
     * dimension
     */
		public int contributionZ(){
			return type.contribZ.apply(val, val.puzzle.magnitude);
		}
	}
	
  /**
   * <p>A dimensional component contribution function that provides a contribution of zero
   * regardless of its inputs.</p>
   */
	private static final BiFunction<IndexValue,Integer,Integer> ZERO = (indx, mag) -> 0;
	
  /**
   * <p>A dimensional component contribution function that produces all the the input dimension
   * value as output.</p>
   */
	private static final BiFunction<IndexValue,Integer,Integer> INT_VALUE = 
	    (indx, mag) -> indx.intValue();
	
	private static int snakeInSquareX(IndexValue index, int magnitude){
		return index.intValue() % magnitude;
	}
	
	private static int snakeInSquareY(IndexValue index, int magnitude){
		return index.intValue() / magnitude;
	}
	
  /**
   * <p>This enum specifies the properties of the five claim-coordinating dimensions available in
   * this model of a sudoku puzzle.</p>
   * <p>These dimensions are the conventional x, y, and z dimensions and two abstract dimensions 
   * that run through the interior of the puzzle: box-index and cell-index-in-box.</p>
   * <p>Box-index is a number indicating a specific box in a given puzzle. Cell-index-in-box is a 
   * number indicating a specific cell in a given box. These two dimensions take the same shape in 
   * conventional space, snaking from the upper left hand corner (low x,y) to the high-x edge, and 
   * wrapping back around to the low-x edge at a y-value higher by 1, like reading text on a page, 
   * until ending at the lower right corner (high x,y).</p>
   * @author fiveham
	 */
	public static enum DimensionType{
		
    /**
     * <p>The conventional x dimension.</p>
     * <p>An IndexInstance in this dimension contributes all of its value to the x dimension and 
     * contributes zero to y and z.</p>
     */
		X(
		    INT_VALUE, 
		    ZERO, 
		    ZERO),
		
    /**
     * <p>The conventional y dimension.</p>
     * <p>An IndexInstance in this dimension contributes all of its value to the y dimension and 
     * contributes zero to x and z.</p>
     */
		Y(
		    ZERO, 
		    INT_VALUE, 
		    ZERO),
		
    /**
     * <p>The conventional z dimension.</p>
     * <p>An IndexInstance in this dimension contributes all of its value to the z dimension and 
     * contributes zero to x and y.</p>
     */
		SYMBOL(
		    ZERO, 
		    ZERO, 
		    INT_VALUE), 
		
    /**
     * <p>The dimension for box-index within a puzzle. This dimension snakes through a Puzzle from 
     * the upper left corner (low x,y), increases to the right (higher x), then jumps back to the 
     * next row once it reaches maximum x, continuing in this fashion until it reaches the lower 
     * right corner (high x,y) of the puzzle.</p>
     * <p>An IndexInstance in this dimension contributes its value {@code % magnitude} times 
     * {@code magnitude} to the x dimension, its value {@code / magnitude} to the y dimension, and 
     * zero to the z dimension.</p>
     */
		BOX(
		    Puzzle::boxLowX, 
		    Puzzle::boxLowY, 
		    ZERO), 
		
    /**
     * <p>The dimension for cell-index within a box. This dimension snakes through a box-sized area 
     * from the upper left corner (low x,y), increases to the right (higher x), then jumps back to 
     * the next row once it reaches maximum x within the box, continuing in this fashion until it 
     * reaches the lower right corner (high x,y) of the box.</p>
     * <p>An IndexInstance in this dimension contributes its value {@code % magnitude} to the x 
     * dimension, its value {@code / magnitude} to the y dimension, and zero to the z dimension.</p>
     */
		CELL_ID_IN_BOX(
		    Puzzle::snakeInSquareX, 
		    Puzzle::snakeInSquareY, 
		    ZERO);
		
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
   * <p>IndexValue is a mock-enum of legal coordinates. For the typical 9x9 puzzle, such an enum 
   * would have 9 elements: the numbers one through nine. This implementation of a Puzzle 
   * accommodates puzzles of all sizes; so, a static enum is not viable. Instead, a list of legal 
   * coordinates must be drawn up on a per-Puzzle basis.</p>
   * @author fiveham
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
     * <p>Constructs an IndexValue belonging to {@code puzzle} and wrapping the value {@code v}.</p>
     * @param puzzle the Puzzle to which this IndexValue belongs
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
     * <p>The puzzle that owns and created this IndexValue. This IndexValue is only valid in this 
     * Puzzle.</p>
     * @return the Puzzle that owns and created this IndexValue
     */
		public Puzzle getPuzzle(){
			return puzzle;
		}
		
    /**
     * <p>Returns this IndexValue's wrapped int value plus 1, translating the internal 0-based
     * values to the conventional 1-based values used in written sudoku puzzles.</p>
     * @return the wrapped integer plus 1
     */
		public int humanReadableIntValue(){
			return v + 1;
		}
		
    /**
     * <p>Returns {@link #humanReadableIntValue()} as a String of digits in base-
     * {@code sideLength() + 1}.</p>
     * @return {@link #humanReadableIntValue()} as a String of digits in base-
     * {@code sideLength() + 1}
     */
		public String humanReadableSymbol(){
			return Integer.toString(humanReadableIntValue(), puzzle.sideLength + 1);
		}
		
		@Override
		public String toString(){
			return Integer.toString(humanReadableIntValue());
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof IndexValue){
				IndexValue iv = (IndexValue) o;
				return puzzle == iv.puzzle && v == iv.v;
			}
			return false;
		}
	}
}
