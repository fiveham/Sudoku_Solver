package anim;

import sudoku.Claim;
import sudoku.Puzzle;
import sudoku.Solver;
import java.io.FileNotFoundException;
import java.util.List;

public class AnimCreator {
	
	public static void main(String[] args) throws FileNotFoundException{
		Solver solver = new Solver(args[0]);
		solver.solve();
		Puzzle p = solver.getPuzzle();
		List<List<List<Claim>>> solveEventFrames = p.getSolveEvents();
		
		generateAnimation(solveEventFrames.get(0).get(0));
		
		//for each solveEvent, compose an animation.
		//but, then combine those animations that need to happen at the same time
		//Once you've got a list of animations that are distinct from each other, 
		//actually put together the animations, the Timeline, etc. and so forth.
		
		//for now, the process has been refined and redefined:
		//First remove falsified claims from factbags (change the factbags' shapes 
		//all at once), and THEN retract the factbags to remove unnecessary reservoir 
		//tips
		
		
		
		
	}
	
	private static void generateAnimation(List<Claim> list){
		
	}
	
	public AnimCreator() {
		
	}
	
	public static final double SCALE_FULL_TO_EMPTY = 0.3;
	
	/*public static Scale emptyVoxel(BagModel.Voxel vox){
		BagModel.Cuboid cub = vox.getCuboid();
		return vox.isConnectedVertical() 
				? new Scale(SCALE_FULL_TO_EMPTY, 1, 1, cub.lowerX(), 0, 0) 
				: new Scale(1, 1, SCALE_FULL_TO_EMPTY, 0, 0, cub.lowerZ());
	}*/
	
}
