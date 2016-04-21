package anim;

public class Face {
	
	public final Point a, b, c;
	
	public Face(Point a, Point b, Point c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public boolean hasPoint(Point existingPoint){
		return existingPoint == a || existingPoint == b || existingPoint == c;
	}
	
	public void switchPoint(Point existingPoint, Point newPoint){
		if(existingPoint == a){
			
		} else if(existingPoint == b){
			
		} else if(existingPoint == c){
			
		} else{
			throw new IllegalArgumentException("The specified point ("+existingPoint.toString()+") is not one of the points of this face: "+toString());
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("[");
		
		sb.append(a).append(", ").append(b).append(", ").append(c);
		
		sb.append("]");
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Face){
			Face f = (Face) o;
			return a.equals(f.a) && b.equals(f.b) && c.equals(f.c);  
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return a.hashCode() ^ b.hashCode() ^ c.hashCode();
	}
}
