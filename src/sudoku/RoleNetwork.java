package sudoku;

import java.util.HashMap;
import common.Pair;

/**
 * <p>Manages Roles for a Puzzle and those roles' connections in an abstract network 
 * that describes the known equalities, known inequalities, and unknownness 
 * of relationships between pairs of Roles in a Puzzle. Any two Roles either 
 * <ul>
 * <li>are the same role,</li>
 * <li>are different roles, or</li>
 * <li>have an unknown relationship.</li>
 * </ul>
 * </p>
 * @author fiveham
 *
 */
public class RoleNetwork {
	
	Puzzle puzzle;
	
	HashMap<Pair<Role,Role>,Sameness> map;
	
	public RoleNetwork(Puzzle puzzle) {
		this.puzzle = puzzle;
		map = new HashMap<>();
	}
	
	public boolean differentiateRoles(Role role1, Role role2){
		return setRoleRelationship(role1, role2, Sameness.DIFFERENT);
	}
	
	private boolean setRoleRelationship(Role role1, Role role2, Sameness sameness){
		return sameness != map.put(new Pair<Role,Role>(role1, role2), sameness);
	}
	
	public Sameness connection(Role role1, Role role2){
		if(role1.puzzle != role2.puzzle){
			throw new IllegalArgumentException("Those two roles do not belong to the same target.");
		}
		
		if(role1.symbol() != role2.symbol()){
			return Sameness.DIFFERENT;
		}
		
		if( role1.localizedRegions().equals(role2.localizedRegions()) ){
			return Sameness.SAME;
		}
		
		return map.get(new Pair<Role,Role>(role1, role2));
	}
}
