package sudoku;

import java.util.*;

/**
 * Provides services derived from graph theory for 
 * networks of cells, including: 
 * decomposition of a graph into separate connected components; 
 * determining the adjacency matrix for a graph;
 * determining the reachability matrix for a graph;
 * determining the walk length between two cells in a network;
 * decomposing a graph into connected subcomponents of a 
 * specified size, and;
 * determining whether a graph is connected in a linear manner.
 * @author fiveham
 */
public final class GraphTheory {
	
	/**
	 * Returns a list of graphs, each of which is a single 
	 * connected component from the parameter graph.
	 * @param cells				The original graph of cells, 
	 * which is to be decomposed into separate connected 
	 * components.
	 * @param minComponentSize	Minimum size a connected 
	 * component must have to be returned in the list.
	 * @param technique			Analysis technique that 
	 * determines the standard to be used for whether 
	 * two cells are connected.
	 * @return					Returns a list of graphs, 
	 * each of which is a single connected component from 
	 * the parameter graph.
	 */
	public static List<List<Cell>> connectedComponents(List<Cell> cells,
			int minComponentSize, Technique technique){	//TODO replace reference to a technique with a wrapped method reference
		
		List<List<Cell>> listOfComponents = new ArrayList<>();
		
		List<Cell> source = new ArrayList<>(cells);
		
		while( !source.isEmpty() ){
			
			List<Cell> component = new ArrayList<>();
			
			component.add( source.get(0) );
			source.remove( component.get(0) );
			
			boolean changes = false;
			do{
				changes = false;
				
				for(Cell cell : source)
					if( cellConnectsToNetwork(cell, component, technique) )
						changes = component.add(cell);
				
				source.removeAll( component );
				
			}while( changes );
			
			if( component.size() >= minComponentSize )
				listOfComponents.add(component);
		}
		
		return listOfComponents;
	}
	
	/**
	 * Returns a list of graphs, each of which is a single 
	 * connected component from the parameter graph.
	 * @param cells				The original graph of cells, 
	 * which is to be decomposed into separate connected 
	 * components.
	 * @param technique			Analysis technique that 
	 * determines the standard to be used for whether 
	 * two cells are connected.
	 * @return					Returns a list of graphs, 
	 * each of which is a single connected component from 
	 * the parameter graph.
	 */
	public static List<List<Cell>> connectedComponents(List<Cell> cells,
			Technique technique){
		
		final int DEFAULT_MIN_COMPONENT_SIZE = 1;
		
		return connectedComponents(cells, DEFAULT_MIN_COMPONENT_SIZE, technique);
	}
	
	/*
	 * Returns whether the parameter cell connects to the network 
	 * according to the standard for connection provided by the 
	 * parameter technique.
	 * @param cell				The cell to be tested for connection
	 * with the parameter network.
	 * @param network			The network of cells that the parameter 
	 * cell is to be tested against for connection.
	 * @param technique			The technique that provides the standard 
	 * to be used in determining whether two cells connect.
	 * @return					Returns whether the parameter 
	 * cell connects to the network 
	 * according to the standard for connection provided by the 
	 * parameter technique.
	 */
	private static boolean cellConnectsToNetwork( Cell cell, List<Cell> network, Technique technique){
		
		for( Cell networkCell : network )
			if( technique.connect(cell, networkCell) )
				return true;
		
		return false;
	}
	
	/**
	 * Returns an NxN matrix detailing how the cells in the 
	 * parameter graph connect according to the connection-standard 
	 * provided by the parameter technique. Cells are considered 
	 * to connect to themselves. This matrix is meant to indicate 
	 * whether a given cell can be reached from each specific 
	 * other cell.
	 * 
	 * Raising this matrix to the power of N-1 provides a matrix 
	 * detailing whether a given cell can be reached from a given 
	 * cell: 0 indicates unreachability; anything else indicates 
	 * that the cell can be reached.
	 * @param graph				The cells that constitute the graph
	 * for which a reachability matrix is to be generated.
	 * @param technique			The analysis technique that is 
	 * to provide the standard for determining whether two 
	 * cells are connected.
	 * @return					Returns an NxN matrix detailing 
	 * how the cells in the 
	 * parameter graph connect according to the connection-standard 
	 * provided by the parameter technique. Cells are considered 
	 * to connect to themselves. This matrix is meant to indicate 
	 * whether a given cell can be reached from each specific 
	 * other cell.
	 */
	public static int[][] pseudoAdjacencyMatrix(List<Cell> graph, Technique technique){
		
		final int CELLS_CONNECT = 1;
		
		int[][] returnMatrix = adjacencyMatrix(graph, technique);
		
		// set values along the diagonal
		for(int i=0; i<returnMatrix.length; i++)
			returnMatrix[i][i] = CELLS_CONNECT;
		
		return returnMatrix;
	}
	
	/**
	 * Returns an NxN matrix detailing how the cells in the 
	 * parameter graph connect according to the connection-standard 
	 * provided by the parameter technique.
	 * @param graph				The cells that constitute the graph
	 * for which a reachability matrix is to be generated.
	 * @param technique			The analysis technique that is 
	 * to provide the standard for determining whether two 
	 * cells are connected.
	 * @return					Returns an NxN matrix detailing 
	 * how the cells in the 
	 * parameter graph connect according to the connection-standard 
	 * provided by the parameter technique.
	 */
	public static int[][] adjacencyMatrix(List<Cell> graph, Technique technique){
		
		final int CONNECTED = 1, UNCONNECTED = 0;
		
		int[][] returnMatrix = new int[graph.size()][graph.size()];
		
		//Set zeros along the diagonal
		for(int i = 0; i < returnMatrix.length; i++)
			returnMatrix[i][i] = UNCONNECTED;
		
		//set values on a triangular half of the matrix
		for(int i=1; i<returnMatrix.length; i++)
			for(int j=0; j<i; j++){
				// test for cell connectivity
				returnMatrix[i][j] = technique.connect(graph.get(i), graph.get(j))
										? CONNECTED
										: UNCONNECTED;
				//set the value on the other half of the matrix
				returnMatrix[j][i] = returnMatrix[i][j];
			}
		
		return returnMatrix;
	}
	
	/**
	 * Returns the least number of steps needed to get from 
	 * one parameter cell in the parameter network to the 
	 * other parameter cell.
	 * @param cell1				A cell in the parameter network.
	 * @param cell2				A cell in the parameter network.
	 * @param network			List of connected cells including 
	 * the two parameter Cells.
	 * @param technique			The analysis technique that provides
	 * the standard for whether cells connect.
	 * @throws					Throws an IllegalArgumentException if 
	 * the parameter Cells aren't connected in the network, including 
	 * if one or both of them is/are not in the network at all.
	 * @return					Returns the least number of steps 
	 * needed to get from one parameter cell in the 
	 * parameter network to the other parameter cell.
	 */
	public static int walkLengthInNetwork(Cell cell1, Cell cell2, List<Cell> network, Technique technique){
		
		final int CELLS_UNCONNECTED = 0;
		
		final int[][] initAdjMatrix = adjacencyMatrix(network, technique);
		int[][] adjacencyMatrix = initAdjMatrix;
		int index1 = network.indexOf(cell1);
		int index2 = network.indexOf(cell2);
		int MAX_STEPCOUNT = network.size()-1;
		
		//raise the adj matrix to higher and higher powers.
		//At each step check whether the value for the 
		//step count between cell1 and cell2 is non-zero.
		//If it is, then return that power.
		
		for(int i=1; i <= MAX_STEPCOUNT; i++ ){
			
			if(adjacencyMatrix[index1][index2] != CELLS_UNCONNECTED )
				return i;
			
			adjacencyMatrix = Matrix.multiply(adjacencyMatrix, initAdjMatrix);
		}
		
		throw new IllegalArgumentException("The cells are not connected in the network.");
	}
	
	/**
	 * Returns a list of subgraphs of a specified size from a graph.
	 * @param network			The graph to be subdivided into 
	 * subgraphs.
	 * @param size				The number of cells that each 
	 * subgraph is to have.
	 * @param technique			The analysis technique that provides
	 * the standard for whether cells connect.
	 * @return					Returns a list of subgraphs of 
	 * a specified size from a graph.
	 */
	public static List<List<Cell>> subgraphs(List<Cell> network, int size, Technique technique){
		
		final int CONNECTED_COMPONENT_COUNT = 1;
		final int MIN_CELLS = 1;
		
		List<List<Cell>> returnList = new ArrayList<>();
		
		//Consider the problem in terms of individual connected components of 
		//the overall network.  If the overall network is not singly connected, 
		//then a lot of effort could be wasted; so, it's better to check.
		for( List<Cell> component : connectedComponents(network, size, technique) ){
			
			//List all the appropriately-sized subsets of nodes in the current connected component
			List<List<Cell>> combosForComponent = combinationsForMagnitude(component, size);
			
			//remove all not-properly-connected combinations of nodes
			for( int i=combosForComponent.size()-1; i>=0; i--){
				//Set<Cell> subcomponent = returnList.get(i);
				
				//Specify the subcomponent (which we hope is an internally connected 
				//subcomponent) of the current overal connected component) that 
				//is being focused on.
				List<Cell> subcomponent = combosForComponent.get(i);
				
				//Make sure that the in-focus subcomponent is singly connected by 
				//enumerating and then counting its subcomponents
				if( connectedComponents(subcomponent, MIN_CELLS, technique).size() != CONNECTED_COMPONENT_COUNT )
					combosForComponent.remove(i);
			}
			
			returnList.addAll( combosForComponent );
		}
		
		return returnList;
	}
	
	/**
 	 * Returns all the combinations of size magnitude 
 	 * of items from the set items.
 	 * @param items				Set of items for which 
 	 * all combinations of size magnitude of items are 
 	 * to be returned.
 	 * @param magnitude			The size of all 
 	 * combinations to be returned.
 	 * @return					Returns all the 
 	 * combinations of size magnitude 
 	 * of items from the set items.
 	 */
	public static <Type> List<List<Type>> combinationsForMagnitude(List<Type> items, int magnitude){
 		List<List<Type>> combinations = new ArrayList<>();
 		
		combinationsForMagnitude(combinations, items, magnitude, new ArrayList<Type>());
		
		return combinations;
	}
	
	/*
	 * Returns a set of all the combinations of items that have 
	 * a certain number of items in them out of the parameter 
	 * set items.
	 */
 	private static <Type> void combinationsForMagnitude(List<List<Type>> combinations, 
			List<Type> items, int magnitude, List<Type> currentCombination){
		
		// When magnitude is 1, recursion needs to not happen
		final int BASE_CASE_MAGNITUDE = 1;
		
		// iterate over the portion of the parameter items list that
		// is valid for this level of iteration as determined by the
		// parameter magnitude
		for( int i=0; i<=items.size()-magnitude; i++ ){
			Type currentItem = items.get(i);
			
			currentCombination.add(currentItem);
			
			if( magnitude == BASE_CASE_MAGNITUDE ){						//recursion shouldn't go any deeper
				combinations.add( new ArrayList<>(currentCombination) );
			}
			else{														//recursion should go deeper
				
				// Create a shortenedItemsList to pass to a recursive copy 
				// of this method. The list contains the items from this 
				// instance's parameter itemsList that have an index higher 
				// than that of the currentItem in the modifiedItemsList
				List<Type> shortenedItemsList = new ArrayList<>();
				int index = items.indexOf(currentItem);
				for(int j=index+1; j<items.size(); j++)
					shortenedItemsList.add(items.get(j));
				
				combinationsForMagnitude(combinations, shortenedItemsList, magnitude-1, currentCombination);
			}
			
			currentCombination.remove( currentItem );
		}
	}
	
	/**
	 * Returns whether the parameter graph is structured 
	 * with two end cells and all other cells connected 
	 * to only two cells.
	 * @param graph				The graph to be analysed.
	 * @param technique			The analysis technique that provides
	 * the standard for whether cells connect.
	 * @return					Returns whether the parameter graph is structured 
	 * with two end cells and all other cells connected 
	 * to only two cells.
	 */
	public static boolean hasLinearTopology(List<Cell> graph, Technique technique){
		
		int[][] adjMat = adjacencyMatrix(graph, technique);
		
		int adjSum = 0;
		for(int i=0; i<adjMat.length; i++)
			for(int j=0; j<adjMat[i].length; j++)
				adjSum += adjMat[i][j];
		
		int sumIfLinear = 2*( graph.size() - 1 );
		
		return adjSum == sumIfLinear;
	}
	
	/**
	 * Provides matrix math methods.
	 * @author fiveham
	 */
	public static final class Matrix{
		
		/**
		 * Returns a matrix equal to the parameter matrix
		 * raised to the power of the parameter exponent.
		 * @param a					Matrix to be raised to a power.
		 * @param exponent			Power to which to raise a matrix.
		 * @return					Returns a matrix equal 
		 * to the parameter matrix
		 * raised to the power of the parameter exponent.
		 */
		public static int[][] raise(int[][] a, int exponent){
			int[][] returnArray = a;
			
			for(int i=1; i<exponent; i++)
				returnArray = multiply(returnArray, a);
			
			return returnArray;
		}
		
		/**
		 * Returns a matrix equal to the result of 
		 * the first parameter matrix multiplied by 
		 * the second parameter matrix under standard 
		 * matrix multiplication.
		 * @param a					First matrix to be multiplied.
		 * @param b					Second matrix to be multiplied.
		 * @return					Returns a matrix 
		 * equal to the result of 
		 * the first parameter matrix multiplied by 
		 * the second parameter matrix under standard 
		 * matrix multiplication.
		 */
		public static int[][] multiply(int[][] a, int[][] b){
			int[][] returnArray = new int[a.length][b[0].length];
			
			for(int i=0; i<returnArray.length; i++)
				for(int j=0; j<returnArray[0].length; j++)
					returnArray[i][j] = dotProduct( row(a, i), column(b, j) );
			
			return returnArray;
		}
		
		/**
		 * Returns a matrix equal to the result of adding
		 * the first and second parameter matrices.
		 * @param a					First matrix to be added.
		 * @param b					Second matrix to be added.
		 * @return					Returns a matrix equal to the result of adding
		 * the first and second parameter matrices.
		 */
		public static int[][] add(int[][] a, int[][] b){
			if(a.length != b.length)
				throw new IllegalArgumentException("mismatched matrices");
			
			int[][] returnMatrix = new int[a.length][a[0].length];
			
			for(int i=0; i<a.length; i++){
				if(a[i].length != b[i].length)
					throw new IllegalArgumentException("mismatched matrices");
				for(int j=0; j<a[i].length; j++)
					returnMatrix[i][j] = a[i][j] + b[i][j];
			}
			
			return returnMatrix;
		}
		
		/**
		 * Returns the Nth row of the parameter matrix,
		 * where N is the parameter row index.
		 * @param matrix			Matrix from which 
		 * to extract a row.
		 * @param rowIndex			IndexValue of the row 
		 * to be extracted from the parameter matrix.
		 * @return					Returns the Nth 
		 * row of the parameter matrix,
		 * where N is the parameter row index.
		 */
		public static int[] row(int[][] matrix, int rowIndex){
			int[] returnArray = new int[matrix.length];
			for(int i=0; i<returnArray.length; i++)
				returnArray[i] = matrix[rowIndex][i];
			return matrix[rowIndex];
		}
		
		/**
		 * Returns the Nth column of the parameter matrix,
		 * where N is the parameter column index.
		 * @param matrix			Matrix from which 
		 * to extract a column.
		 * @param columnIndex		IndexValue of the column 
		 * to be extracted from the parameter matrix.
		 * @return					Returns the Nth 
		 * column of the parameter matrix,
		 * where N is the parameter column index.
		 */
		public static int[] column(int[][] matrix, int columnIndex){
			int[] returnArray = new int[matrix.length];
			
			for(int i=0; i<returnArray.length; i++)
				returnArray[i] = matrix[i][columnIndex];
			
			return returnArray;
		}
		
		/**
		 * Returns the dot product of two vectors.
		 * @param a					First vector 
		 * to be used in determining a dot product.
		 * @param b					Second vector 
		 * to be used in determining a dot product.
		 * @return					Returns the 
		 * dot product of two vectors.
		 */
		public static int dotProduct( int[] a, int[] b){
			int returnValue = 0;
			for( int i=0; i<a.length; i++)
				returnValue += a[i]*b[i];
			
			return returnValue;
		}
	}
}
