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
				SolutionEvent result = new Initialization((Init) f);
				f.validateFinalState(result);
				return result;
			}
		}
		return null;
	}
	
	public static class Initialization extends SolutionEvent{
		private Initialization(Init init){
			super(init.iterator().next().visibleClaims());
		}
	}
}
