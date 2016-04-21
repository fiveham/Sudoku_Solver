package sudoku;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.function.Function;

public class Puzzle {
	
	public static final int MAGNITUDE = 3;
	public static final int SIDE_LENGTH = MAGNITUDE*MAGNITUDE;
	
	private SpaceMap claims;
	
	private List<FactBag> factbags = new ArrayList<>();

	private List<Resolvable> tasks;
	
	public Puzzle(File f) throws FileNotFoundException{
		this(new Scanner(f));
	}
	
	public Puzzle(String s){
		this(new Scanner(s));
	}
	
	public Puzzle(Scanner s){
		this.claims = new SpaceMap(SIDE_LENGTH);
		List<Claim> cellValues = parseText(s, claims);
		s.close();
		
		this.tasks = new ArrayList<>();
		this.factbags = new ArrayList<FactBag>();
		populateFactbags();
		
		for(Claim c : cellValues){
			c.setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD();
		}
	}
	
	public SpaceMap claims(){
		return claims;
	}
	
	public List<FactBag> getFactbags(){
		return new ArrayList<>(factbags);
	}
	
	/* *
	 * Returns a set of pairs of FactBags, each of which represents an existing 
	 * intersection (overlap) between two factbags in this target.
	 * @return
	 */
	/*private Set<Pair<Rule,Rule>> getFactBagIntersections(){
		Set<Pair<Rule,Rule>> result = new HashSet<Pair<Rule,Rule>>();
		for(Rule currentFactBag : factbags){
			for(Claim currentClaim : currentFactBag){
				for(Rule claimOwner : currentClaim.getOwners()){
					if( claimOwner != currentFactBag ){
						result.add( new Pair<Rule,Rule>(currentFactBag, claimOwner) );
					}
				}
			}
		}
		return result;
	}*/
	
	public boolean isSolved(){
		for(FactBag bag : factbags){
			if(bag.size() != FactBag.SIZE_WHEN_SOLVED){
				return false;
			}
		}
		
		return true;
	}
	
	public Claim getClaimAt(Puzzle.Dimension dim1, Puzzle.Dimension dim2, Puzzle.Dimension heldConstant){
		Index x = decodeX(dim1, dim2, heldConstant);
		Index y = decodeY(dim1, dim2, heldConstant);
		Index s = decodeSymbol(dim1, dim2, heldConstant);
		
		return claims.get(x, y, s);
	}
	
	public static Index decodeX(Puzzle.Dimension... dims){
		return decodeDim((d) -> d.contributionX(), dims);
	}
	
	public static Index decodeY(Puzzle.Dimension... dims){
		return decodeDim((d) -> d.contributionY(), dims);
	}
	
	public static Index decodeSymbol(Puzzle.Dimension... dims){
		return decodeDim((d) -> d.contributionZ(), dims);
	}
	
	public static final int DIMENSION_COUNT = 3;
	
	private static Index decodeDim( Function<Puzzle.Dimension,Integer> contrib, 
			Puzzle.Dimension[] dims){
		if(dims.length != DIMENSION_COUNT){
			throw new IllegalArgumentException("Need 3 dimensions, but "+dims.length+" were provided");
		}
		
		int score = 0;
		for(Puzzle.Dimension dim : dims){
			score += contrib.apply(dim);
		}
		
		return Index.fromInt(score);
	}
	
	private void populateFactbags(){
		for(Region region : Region.values()){
			for(Dimension dimA : region.dimA()){
				for(Dimension dimB : region.dimB()){
					FactBag regionBag = new FactBag(this, region, Region.SIZE_OF_A_REGION, region.boundingBox(dimA, dimB));
					for(Dimension dimC : region.dimC()){
						regionBag.add( getClaimAt(dimA, dimB, dimC) );
					}
					factbags.add(regionBag);
				}
			}
		}
	}
	
	void registerResolvable(Resolvable res){
		tasks.add(res);
	}
	
	/*boolean resolveTasks(){
		boolean result = false;
		for(int i=tasks.size()-1; 0<=i; --i){
			tasks.remove(i).resolve();
		}
		return result;
	}*/
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		for(Index y : Index.values()){
			for(Index x : Index.values()){
				result.append( claims.getPrintingValue(x,y) ).append(" ");
			}
			
			result.append(System.getProperty("line.separator"));
		}
		
		return result.toString();
	}
	
	public static final int MIN_COORD_FIRST_HOUSE = 0;
	public static final int MIN_COORD_SECOND_HOUSE = 3;
	public static final int MIN_COORD_THIRD_HOUSE = 6;
	
	public static Index boxIndex(Index x, Index y){
		int xInt = x.intValue();
		int yInt = y.intValue();
		
		xInt /= MAGNITUDE;
		yInt /= MAGNITUDE;
		yInt *= MAGNITUDE;
		
		return Index.fromInt( xInt + yInt );
	}
	
	public static int boxLowX(Index boxIndex){
		return (boxIndex.intValue()%3)*3;
	}
	
	public static int boxLowY(Index boxIndex){
		return (boxIndex.intValue()/3)*3;
	}
	
	public static List<Claim> parseText(Scanner s, SpaceMap claims){
		List<Claim> knownTrueClaims = new ArrayList<>();
		
		for(Index y : Index.values()){
			for(Index x : Index.values()){
				String item;
				try{
					item = s.next();
				} catch(NoSuchElementException e){
					throw new IllegalArgumentException("Fewer than "+Index.values().length * Index.values().length+" tokens in source text", e);
				}
				int equiv;
				try{
					equiv = Integer.parseInt(item);
				} catch(NumberFormatException e){
					throw new IllegalArgumentException("Illegal sudoku symbol in first 81 tokens of source text", e);
				}
				
				if(equiv != Index.NO_SYMBOL){ //TODO account for numbers too large
					//knownTrueClaims.add( new Claim(x,y,Index.fromInt(equiv-1)) );
					knownTrueClaims.add( claims.get(x,y,Index.fromInt(equiv-1)) );
				}
			}
		}
		
		return knownTrueClaims;
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
	
	public static enum Region{
		CELL	(Dimension.Type.Y, 		Dimension.Type.X, 	Dimension.Type.SYMBOL), 
		BOX		(Dimension.Type.SYMBOL, Dimension.Type.BOX,	Dimension.Type.CELL_ID_IN_BOX), 
		ROW		(Dimension.Type.SYMBOL, Dimension.Type.Y, 	Dimension.Type.X),
		COLUMN	(Dimension.Type.SYMBOL, Dimension.Type.X, 	Dimension.Type.Y);
		
		public static final int SIZE_OF_A_REGION = 9;
		
		private List<Dimension> dimAList;
		private List<Dimension> dimBList;
		private List<Dimension> dimCList;
		
		private Region(Dimension.Type dimA, Dimension.Type dimB, Dimension.Type dimC/*, Function<Puzzle,List<Rule>> factBagReference*/){
			this.dimAList = dimA.valuesAsDims();
			this.dimBList = dimB.valuesAsDims();
			this.dimCList = dimC.valuesAsDims();
		}
		
		public FactBag.BoundingBox boundingBox(Dimension dimA, Dimension dimB){
			Index x = decodeX(dimA, dimB);
			Index y = decodeY(dimA, dimB);
			Index s = decodeSymbol(dimA, dimB);
			switch(this){
			case CELL : 
				x = decodeX(dimA, dimB);
				y = decodeY(dimA, dimB);
				return new FactBag.BoundingBox(x, x, y, y, Index.MINIMUM, Index.MAXIMUM);
			case BOX : 
				x = decodeX(dimA, dimB);
				y = decodeY(dimA, dimB);
				s = decodeSymbol(dimA, dimB);
				return new FactBag.BoundingBox(x, Index.fromInt(x.intValue()+MAGNITUDE-1), y, Index.fromInt(y.intValue()+MAGNITUDE-1), s, s);
			case ROW : 
				y = decodeY(dimA, dimB);
				s = decodeSymbol(dimA, dimB);
				return new FactBag.BoundingBox(Index.I1, Index.I9, y, y, s, s);
			case COLUMN : default : 
				x = decodeX(dimA, dimB);
				s = decodeSymbol(dimA, dimB);
				return new FactBag.BoundingBox(x, x, Index.I1, Index.I9, s, s);
			}
		}
		
		public List<Dimension> dimA(){
			return new ArrayList<>(dimAList);
		}
		
		public List<Dimension> dimB(){
			return new ArrayList<>(dimBList);
		}
		
		public List<Dimension> dimC(){
			return new ArrayList<>(dimCList);
		}
	}
	
	public static class Dimension{
		
		private Type type;
		private Index val;
		
		public Dimension(Type type, Index val){
			this.type = type;
			this.val = val;
		}
		
		/*public IndexInstance(DimensionType type, int val){
			this.type = type;
			this.val = IndexValue.fromInt(val);
		}*/
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Dimension){
				Dimension d = (Dimension) o;
				return d.type.equals(type) && d.val.equals(val);
			}
			return false;
		}
		
		public Type getType(){
			return type;
		}
		
		public int contributionX(){
			return type.contribX.apply(val);
		}
		
		public int contributionY(){
			return type.contribY.apply(val);
		}
		
		public int contributionZ(){
			return type.contribZ.apply(val);
		}
		
		public static final Function<Index,Integer> ZERO            = (i) -> 0;
		public static final Function<Index,Integer> INT_VALUE       = (i) -> i.intValue();
		public static final Function<Index,Integer> X_POS_COMP_CELL = (i) -> (i.intValue()%MAGNITUDE);
		public static final Function<Index,Integer> Y_POS_COMP_CELL = (i) -> (i.intValue()/MAGNITUDE);
		public static final Function<Index,Integer> X_POS_COMP_BOX  = (i) -> (i.intValue()%MAGNITUDE)*MAGNITUDE;
		public static final Function<Index,Integer> Y_POS_COMP_BOX  = (i) -> (i.intValue()/MAGNITUDE)*MAGNITUDE;
		
		public enum Type{
			X				(INT_VALUE,       ZERO,            ZERO),
			Y				(ZERO,            INT_VALUE,       ZERO),
			SYMBOL			(ZERO,            ZERO,            INT_VALUE),
			BOX				(X_POS_COMP_BOX,  Y_POS_COMP_BOX,  ZERO), 
			CELL_ID_IN_BOX	(X_POS_COMP_CELL, Y_POS_COMP_CELL, ZERO);
			
			private Function<Index,Integer> contribX;
			private Function<Index,Integer> contribY;
			private Function<Index,Integer> contribZ;
			
			private Type(Function<Index,Integer> contribX, Function<Index,Integer> contribY, Function<Index,Integer> contribZ){
				this.contribX = contribX;
				this.contribY = contribY;
				this.contribZ = contribZ;
			}
			
			public List<Dimension> valuesAsDims(){
				Index[] enumVals = Index.values();
				List<Dimension> result = new ArrayList<>(enumVals.length);
				
				for(Index i : enumVals){
					result.add( new Dimension(this,i) );
				}
				
				return result;
			}
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
			for(int i=tasks.size(); 0<i;){
				result |= tasks.remove(--i).resolve();
			}
			return result;
		}
	}
}
