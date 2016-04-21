package anim;

import javafx.scene.shape.Mesh;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.MeshView;

public class Endcap extends MeshView {
	
	/**
	 * The radius of the hotdog shape of a linear factbag.
	 */
	public static final float RAD = 0.3f;
	
	public Endcap() {
		super(buildDome());
	}
	
	private static Mesh buildDome(){
		TriangleMesh result = new TriangleMesh();
		
		
		float[] points = {RAD, 0, 0,
				0, RAD, 0,
				0, -RAD, 0,
				0, 0, RAD,
				0, 0, -RAD};
		float[] texCoords = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
		int[] faces = {0,0, 1,0, 3,0, 
				0,0, 3,0, 2,0, 
				0,0, 2,0, 4,0, 
				0,0, 4,0, 1,0};
		int[] smoothingGroups = {1,1,1,1};

        result.getPoints().addAll(points);
        result.getTexCoords().addAll(texCoords);
        result.getFaces().addAll(faces);
        result.getFaceSmoothingGroups().addAll(smoothingGroups);
        
        
		return result;
	}
	
	/*private static Mesh hotdog(){
        TriangleMesh result = new TriangleMesh();
        
        
        float[] points = {0.0f, 0.0f, 0.0f, 
                1.0f, 0.0f, 0.0f, 
                0.0f, 1.0f, 0.0f, 
                0.0f, 0.0f, 1.0f};
        float texCoords[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        int faces[] = {1,0, 2,0, 3,0,
                0,0, 2,0, 1,0,
                0,0, 1,0, 3,0, 
                0,0, 3,0, 2,0};
        int smoothingGroups[] = {1,1,1,1};
        
        
        result.getPoints().addAll(points);
        result.getTexCoords().addAll(texCoords);
        result.getFaces().addAll(faces);
        result.getFaceSmoothingGroups().addAll(smoothingGroups);
        
        
        return result;
    }*/
}
