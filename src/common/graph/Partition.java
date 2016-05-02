package common.graph;

/**
 * <p>A connected component if a Graph being explored so as 
 * to identify which nodes are part of the component and which 
 * are not can be studied as such by choosing a starting node, 
 * accounting for its neighbors, and bulding up a component by 
 * passing the orignal node into an ignored partition, passing 
 * all its neighbors into the start node's original partition, 
 * and moving all those neighbors' neighbors (that aren't already 
 * in a meaningful partition) into the partition that the original 
 * node's neighbors were initially put into. In that manner, a 
 * connected component can be identified explicitly by describing 
 * individual nodes' locations as being one of four partitions.</p>
 * @author fiveham
 *
 */
public enum Partition {
	CORE, EDGE, CUTTINGEDGE, UNASSIGNED;
}
