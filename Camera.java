package lhs.finalproject;

//creates a camera in a 3D space for a 2D screen
public class Camera {
    private Vector position;
    private double fov;

    //creates a camera with a set position and field of view
    public Camera(Vector position, double fov) {
        this.position = position;
        this.fov = fov;
    }

    //creates a default camera at a position and default field of view
    public Camera() {
        this(new Vector(0,5,0), 60); // Viewer.java overrides this im pretty sure
    }

    //getter methods
    public Vector getPosition() { 
        return position; 
    }
    public double getFov() { 
        return fov; 
    }

    //setter methods
    public void setPosition(Vector position) { 
        this.position = position; 
    }
    public void setFov(double fov) { 
        this.fov = fov; 
    }

    //projects a 3D point on a 2D screen w/ perspective projection
    public double[] project(Vector point, int screenWidth, int screenHeight) {
        Vector forward=new Vector(0,0,0).subtract(this.position).normalize();
        Vector worldUp=new Vector(0,1,0);
        Vector right=forward.cross(worldUp).normalize();
        Vector up=right.cross(forward).normalize();
        Vector translated = point.subtract(this.position);
        double x=translated.dot(right);
        double y=translated.dot(up);
        double z=translated.dot(forward);

        if (z<=0) { return null; }

        double fovRadians=Math.toRadians(this.fov);
        double projectionPlaneDistance = 1.0/Math.tan(fovRadians/2.0);
        double projectedX=(x*projectionPlaneDistance)/z;
        double projectedY=(y*projectionPlaneDistance)/z;

        double aspectRatio=(double) screenWidth/screenHeight;

        int xa=(int) ((projectedX/aspectRatio+1.0)*screenWidth/2.0);
        int ya=(int) ((1.0-projectedY)*screenHeight/2.0);

        return new double[] {xa,ya};
    }
}



