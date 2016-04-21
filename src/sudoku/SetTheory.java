package sudoku;

import java.util.*;

public class SetTheory {
	
	public static <T> List<T> intersection(List<T> list1, List<T> list2){
		List<T> retList = new ArrayList<>();
		
		for(int i=0; i<list1.size(); i++){
			if(list2.contains(list1.get(i))){
				retList.add(list1.get(i));
			}
		}
		
		return retList;
	}
}
