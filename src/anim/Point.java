package anim;

public class Point {
	
	public final float x, y, z;
	
	public Point(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("(").append(x).append(", ").append(y).append(", ").append(z).append(")");
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Point){
			Point p = (Point) o;
			return p.x == x && p.y == y && p.z == z;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return Float.hashCode(x) ^ Float.hashCode(y) ^ Float.hashCode(z);
	}
}
