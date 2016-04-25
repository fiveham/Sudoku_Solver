package sudoku;

import java.util.stream.Collectors;

public class Initializer extends Technique {
	
	public Initializer(Sudoku puzzle) {
		super(puzzle);
	}
	
	@Override
	protected SolutionEvent process() {
		for(Fact f : target.factStream().collect(Collectors.toList())){
			if(f instanceof Init){
				Claim c = f.iterator().next();
				SolutionEvent result = new Initialization(c);
				c.setTrue(result);
				return result;
			}
		}
		return null;
	}
	
	public class Initialization extends SolutionEvent{
		private Initialization(Claim c){
			super(c.visibleClaims());
		}
	}
}
