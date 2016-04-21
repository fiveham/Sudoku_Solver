package sudoku;

public class TotalLocalization extends Resolvable{
	
	protected FactBag source;
	
	public TotalLocalization(FactBag src) {
		if(src.size() != FactBag.SIZE_WHEN_SOLVED){
			throw new IllegalArgumentException("The specified Rule is not resolvable.");
		}
		this.source = src;
	}
	
	@Override
	public boolean resolve(){
		for(Claim c : source){
			return c.setTrue_ONLY_Puzzle_AND_Resolvable_MAY_CALL_THIS_METHOD();
		}
		throw new IllegalStateException("This TotalLocalization's factbag has no Claims in it.");
	}
}
