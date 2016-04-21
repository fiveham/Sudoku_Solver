package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class Puzzle {
	
	private int magnitude;
	private int sideLength;
	
	private SpaceMap claims;
	private List<FactBag> factbags = new ArrayList<>();
	
	private List<Resolvable> tasks;
	
	private List<List<List<Claim>>> solveEventFrames;
	private List<List<Claim>> currentEventFrame;
	
	private List<IndexValue> indices;
	
	private List<List<IndexInstance>> dimensions;
	
	public Puzzle(File f) throws FileNotFoundException{
		this(new Scanner(f));
	}
	
	public Puzzle(String s){
		this(new Scanner(s));
	}
	
	public Puzzle(Scanner s){
		this.solveEventFrames = new ArrayList<>();
		this.currentEventFrame = new ArrayList<>();
		this.tasks = new ArrayList<>();
		
		Parser p = new Parser();
		List<Integer> values = p.parse(s);
		s.close();
		
		this.magnitude = p.mag;
		this.sideLength = magnitude*magnitude;
		
		this.indices = genIndices(sideLength, this);
		this.dimensions = genDimensions(indices, this);
		
		this.claims = new SpaceMap(this);
		this.factbags = populateFactbags(this, sideLength, claims);
		
		List<Claim> cellValues = parseText(this, values);
		for(Claim c : cellValues){
			c.setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD();
		}
	}
	
	private static List<FactBag> populateFactbags(Puzzle p, int sideLength, SpaceMap claims){
		List<FactBag> factbags = new ArrayList<>(RegionSpecies.values().length * p.sideLength * p.sideLength * p.sideLength);
		for(RegionSpecies region : RegionSpecies.values()){
			for(IndexInstance dimA : region.dimA(p)){
				for(IndexInstance dimB : region.dimB(p)){
					FactBag regionBag = new FactBag(p, region, sideLength, region.boundingBox(p, dimA, dimB));
					for(IndexInstance dimC : region.dimInsideFactBag(p)){
						regionBag.add( claims.get(dimA, dimB, dimC) );
					}
					factbags.add(regionBag);
				}
			}
		}
		return factbags;
	}
	
	private static List<List<IndexInstance>> genDimensions(List<IndexValue> indices, Puzzle p){
		List<List<IndexInstance>> dimensions = new ArrayList<>(DimensionType.values().length);
		for(DimensionType type : DimensionType.values()){
			List<IndexInstance> indexInstancesForType = new ArrayList<>(indices.size());
			for(IndexValue i : indices){
				indexInstancesForType.add( new IndexInstance(p, type, i) );
			}
			dimensions.add(indexInstancesForType);
		}
		return dimensions;
	}
	
	private static List<IndexValue> genIndices(int sideLength, Puzzle p){
		List<IndexValue> indices = new ArrayList<>(sideLength);
		for(int i=0; i<sideLength; ++i){
			indices.add(new IndexValue(p, i));
		}
		
		return indices;
	}
	
	public boolean addSolveEvent(Collection<Claim> solveEvent){
		return currentEventFrame.add(new ArrayList<>(solveEvent));
	}
	
	public void newEventFrame(){
		solveEventFrames.add(currentEventFrame);
		currentEventFrame = new ArrayList<>();
	}
	
	public List<List<List<Claim>>> getSolveEvents(){
		return new ArrayList<>(solveEventFrames);
	}
	
	public int magnitude(){
		return magnitude;
	}
	
	public SpaceMap claims(){
		return claims;
	}
	
	public List<FactBag> getFactbags(){
		return new ArrayList<>(factbags);
	}
	
	public Collection<FactBag> factBagsWhere(Predicate<FactBag> p){
		List<FactBag> result = new ArrayList<>();
		for(FactBag bag : factbags){
			if(p.test(bag)){
				result.add(bag);
			}
		}
		return result;
	}
	
	public List<IndexValue> indexValues(){
		return new ArrayList<>(indices);
	}
	
	public IndexValue indexFromInt(int i){
		try{
			return indices.get(i);
		} catch(IndexOutOfBoundsException e){
			throw new IllegalArgumentException("Specified i="+i+" out of bounds: 0 to "+(indices.size()-1), e);
		}
	}
	
	public IndexValue indexFromHumanReadableInt(int i){
		return indexFromInt(i-1);
	}
	
	public IndexValue minIndex(){
		return indices.get(0);
	}
	
	public IndexValue maxIndex(){
		return indices.get(indices.size()-1);
	}
	
	public boolean isSolved(){
		for(FactBag bag : factbags){
			if(bag.size() != FactBag.SIZE_WHEN_SOLVED){
				return false;
			}
		}
		
		return true;
	}
	
	public Claim getClaimAt(IndexInstance dim1, IndexInstance dim2, IndexInstance heldConstant){
		IndexValue x = decodeX(dim1, dim2, heldConstant);
		IndexValue y = decodeY(dim1, dim2, heldConstant);
		IndexValue s = decodeSymbol(dim1, dim2, heldConstant);
		
		return claims.get(x, y, s);
	}
	
	public IndexValue[] decodeXYZ(IndexInstance... dims){
		return new IndexValue[]{		
			decodeX(dims),
			decodeY(dims),
			decodeSymbol(dims)
		};
	}
	
	public IndexValue decodeX(IndexInstance... dims){
		return decodeDim((d) -> d.contributionX(), dims);
	}
	
	public IndexValue decodeY(IndexInstance... dims){
		return decodeDim((d) -> d.contributionY(), dims);
	}
	
	public IndexValue decodeSymbol(IndexInstance... dims){
		return decodeDim((d) -> d.contributionZ(), dims);
	}
	
	/**
	 * The number of physical spatial dimensions ({@value #DIMENSION_COUNT}) pertinent 
	 * to this model of a sudoku target.
	 */
	public static final int DIMENSION_COUNT = 3;
	
	private IndexValue decodeDim( Function<IndexInstance,Integer> contrib, IndexInstance[] dims){
		/*if(dims.length != DIMENSION_COUNT){
			throw new IllegalArgumentException("Need 3 dimensions, but "+dims.length+" were provided");
		}*/
		
		int score = 0;
		for(IndexInstance dim : dims){
			score += contrib.apply(dim);
		}
		
		return indexFromInt(score);
	}
	
	void registerResolvable(Resolvable res){
		tasks.add(res);
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		for(IndexValue y : indices){
			for(IndexValue x : indices){
				result.append( claims.getPrintingValue(x,y) ).append(" ");
			}
			result.append(System.getProperty("line.separator"));
		}
		
		return result.toString();
	}
	
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
	
	public int sideLength(){
		return sideLength;
	}
	
	public IndexValue boxIndex(IndexValue x, IndexValue y){
		return indices.get( x.intValue()/magnitude + y.intValue()/magnitude*magnitude );
	}
	
	public int boxLowX(IndexValue boxIndex){
		return boxLowX(boxIndex, magnitude);
	}
	
	private static int boxLowX(IndexValue boxIndex, int mag){
		return mag*X_POS_COMP_CELL.apply(boxIndex, mag);
	}
	
	public int boxLowY(IndexValue boxIndex){
		return boxLowY(boxIndex, magnitude);
	}
	
	private static int boxLowY(IndexValue boxIndex, int mag){
		return mag*Y_POS_COMP_CELL.apply(boxIndex, mag);
	}
	
	private List<Claim> parseText(Puzzle puzzle, List<Integer> values){
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
					//knownTrueClaims.add( new Claim(target, x,y, indexFromHumanReadableInt(value)) );
					knownTrueClaims.add( claims.get(x,y, indexFromHumanReadableInt(value)) );
				}
			}
		}
		return knownTrueClaims;
	}
	
	public static final int BLANK_CELL = 0;
	
	private static class Parser{
		
		private volatile int mag = 2;
		
		private int radix(){
			return mag*mag+1;
		}
		
		private int parseInt(String token){
			Integer result = null;
			while( result == null ){
				try{
					result = Integer.parseInt(token, radix());
				} catch(NumberFormatException e){
					++mag;
				}
			}
			return result;
		}
		
		private List<Integer> parse(Scanner s){
			List<Integer> result = new ArrayList<>();
			while(s.hasNext() && mag*mag*mag*mag > result.size()){
				String token = s.next();
				if(token.length() > 1){
					throw new IllegalArgumentException(token+" is more than a single char");
				}
				result.add(parseInt(token));
			}
			
			return result;
		}
	}
	
	public static enum RegionSpecies{
		CELL	(DimensionType.Y, 	   DimensionType.X, 	DimensionType.SYMBOL,         RegionSpecies::bbCell), 
		BOX		(DimensionType.SYMBOL, DimensionType.BOX,	DimensionType.CELL_ID_IN_BOX, RegionSpecies::bbBox), 
		ROW		(DimensionType.SYMBOL, DimensionType.Y, 	DimensionType.X,              RegionSpecies::bbRow),
		COLUMN	(DimensionType.SYMBOL, DimensionType.X, 	DimensionType.Y,              RegionSpecies::bbCol);
		
		private final DimensionType dimAType;
		private final DimensionType dimBType;
		private final DimensionType dimCType;
		private final BoundingBoxGenerator bbbGen;
		
		private RegionSpecies(DimensionType dimAType, DimensionType dimBType, DimensionType dimCType, BoundingBoxGenerator bbGen){
			this.dimAType = dimAType;
			this.dimBType = dimBType;
			this.dimCType = dimCType;
			this.bbbGen = bbGen;
		}
		
		public FactBag.BoundingBox boundingBox(Puzzle p, IndexInstance dimA, IndexInstance dimB){
			return bbbGen.apply(p,dimA,dimB);
		}
		
		public List<IndexInstance> dimA(Puzzle p){
			return p.dimensions.get(dimAType.intValue);
		}
		
		public List<IndexInstance> dimB(Puzzle p){
			return p.dimensions.get(dimBType.intValue);
		}
		
		public List<IndexInstance> dimInsideFactBag(Puzzle p){
			return p.dimensions.get(dimCType.intValue);
		}
		
		/**
		 * Specifies a single method that takes a Puzzle and two IndexInstances 
		 * and produces a Rule.BoundingBox.
		 * @author fiveham
		 *
		 */
		@FunctionalInterface
		private static interface BoundingBoxGenerator{
			/**
			 * Produces a BoundingBox defining the initial geometric scope of a Rule 
			 * that is otherwise externally specified by <tt>dimA</tt> and </tt>dimB</tt>.
			 * @param p the Puzzle to which belongs the Rule to which pertains the returned 
			 * BoundingBox
			 * @param dimA the first of two abstract dimensions that together indirectly 
			 * define the geometry of a specific Rule
			 * @param dimB the second of two abstract dimensions that together indirectly 
			 * define the geometry of a specific Rule
			 * @return
			 */
			public FactBag.BoundingBox apply(Puzzle p, IndexInstance dimA, IndexInstance dimB);
		}
		
		private static FactBag.BoundingBox bbCol(Puzzle p, IndexInstance dimA, IndexInstance dimB){
			IndexValue x = p.decodeX(dimA, dimB);
			IndexValue s = p.decodeSymbol(dimA, dimB);
			return new FactBag.BoundingBox(x, x, p.minIndex(), p.maxIndex(), s, s);
		}
		
		private static FactBag.BoundingBox bbRow(Puzzle p, IndexInstance dimA, IndexInstance dimB){
			IndexValue y = p.decodeY(dimA, dimB);
			IndexValue s = p.decodeSymbol(dimA, dimB);
			return new FactBag.BoundingBox( p.minIndex(), p.maxIndex(), y, y, s, s);
		}
		
		private static FactBag.BoundingBox bbBox(Puzzle p, IndexInstance dimA, IndexInstance dimB){
			IndexValue x = p.decodeX(dimA, dimB);
			IndexValue y = p.decodeY(dimA, dimB);
			IndexValue s = p.decodeSymbol(dimA, dimB);
			return new FactBag.BoundingBox(x, x.plus(p.magnitude-1), y, y.plus(p.magnitude-1), s, s);
		}
		
		private static FactBag.BoundingBox bbCell(Puzzle p, IndexInstance dimA, IndexInstance dimB){
			IndexValue x = p.decodeX(dimA, dimB);
			IndexValue y = p.decodeY(dimA, dimB);
			return new FactBag.BoundingBox(x, x, y, y, p.minIndex(), p.maxIndex());
		}
	}
	
	public static class IndexInstance{
		private DimensionType type;
		private IndexValue val;
		private Puzzle puzzle;
		
		public IndexInstance(Puzzle puzzle, DimensionType type, IndexValue val){
			this.type = type;
			this.val = val;
			this.puzzle = puzzle;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof IndexInstance){
				IndexInstance d = (IndexInstance) o;
				return d.type.equals(type) && d.val.equals(val);
			}
			return false;
		}
		
		public DimensionType getType(){
			return type;
		}
		
		public int contributionX(){
			return type.contribX.apply(val, puzzle.magnitude);
		}
		
		public int contributionY(){
			return type.contribY.apply(val, puzzle.magnitude);
		}
		
		public int contributionZ(){
			return type.contribZ.apply(val, puzzle.magnitude);
		}
	}
	
	public static final BiFunction<IndexValue,Integer,Integer> ZERO       = (indx,intgr) -> 0;
	public static final BiFunction<IndexValue,Integer,Integer> INT_VALUE  = (indx,intgr) -> indx.intValue();
	public static final BiFunction<IndexValue,Integer,Integer> X_POS_COMP_BOX = Puzzle::boxLowX;
	public static final BiFunction<IndexValue,Integer,Integer> Y_POS_COMP_BOX = Puzzle::boxLowY;
	public static final BiFunction<IndexValue,Integer,Integer> X_POS_COMP_CELL = (indx,intgr) -> (indx.intValue()%intgr);
	public static final BiFunction<IndexValue,Integer,Integer> Y_POS_COMP_CELL = (indx,intgr) -> (indx.intValue()/intgr);
	
	public static enum DimensionType{
		X				(INT_VALUE,       ZERO,            ZERO,      0),
		Y				(ZERO,            INT_VALUE,       ZERO,      1),
		SYMBOL			(ZERO,            ZERO,            INT_VALUE, 2),
		BOX				(X_POS_COMP_BOX,  Y_POS_COMP_BOX,  ZERO,      3), 
		CELL_ID_IN_BOX	(X_POS_COMP_CELL, Y_POS_COMP_CELL, ZERO,      4);
		
		private final BiFunction<IndexValue,Integer,Integer> contribX;
		private final BiFunction<IndexValue,Integer,Integer> contribY;
		private final BiFunction<IndexValue,Integer,Integer> contribZ;
		private final int intValue;
		
		private DimensionType(BiFunction<IndexValue,Integer,Integer> contribX, BiFunction<IndexValue,Integer,Integer> contribY, BiFunction<IndexValue,Integer,Integer> contribZ, int intValue){
			this.contribX = contribX;
			this.contribY = contribY;
			this.contribZ = contribZ;
			this.intValue = intValue;
		}
	}
	
	public static class IndexValue{
		
		private final Puzzle puzzle;
		private final int v;
		
		private IndexValue(Puzzle puzzle, int v){
			this.puzzle = puzzle;
			this.v = v;
		}
		
		public int intValue(){
			return v;
		}
		
		public Puzzle getPuzzle(){
			return puzzle;
		}
		
		/**
		 * Returns this IndexValue's internal int value plus 1, translating the 
		 * internal 0-8 values (in the common case of a 9x9 target) to the 
		 * conventional 1-9 values used in written sudoku puzzles
		 * @return
		 */
		public int humanReadableIntValue(){
			return v+1;
		}
		
		public String humanReadableSymbol(){
			return Integer.toString(humanReadableIntValue(),puzzle.sideLength+1);
		}
		
		/**
		 * 
		 * @param i
		 * @throws IndexOutOfBoundsException when <tt>v + i</tt> is less than 0 or greater 
		 * than the size of the list of indices for the target to which this IndexValue belongs.
		 * @return
		 */
		public IndexValue plus(int i){
			return puzzle.indices.get(v+i);
		}
	}
	
	private ResolveResolvables rr = null;
	
	public Technique resolveResolvables(){
		if(rr==null){
			rr = new ResolveResolvables();
		}
		return rr;
	}
	
	public class ResolveResolvables extends Technique{
		private ResolveResolvables(){
			super(Puzzle.this);
		}
		
		@Override
		protected boolean process(){
			boolean result = false;
			/*for(int i=tasks.size(); 0<i;){
				result |= tasks.remove(--i).resolve();
			}*/
			/*for(Iterator<Resolvable> iter = new.iterator(); iter.hasNext(); iter.remove()){
			result |= iter.next().resolve();
			}*/
			List<Resolvable> list = new ArrayList<>(tasks);
			tasks.clear();
			for(Resolvable res : list){
				result |= res.resolve();
			}
			
			return result;
		}
	}
}
