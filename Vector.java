package lhs.finalproject;

public class Vector {
    //fields w/ x y and z coordinates
    private double x, y, z;
    
    //constructs a new vector
    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    //creates a vector at 0, 0, 0
    public Vector() {
        this(0, 0, 0);
    }

    //creates a copy of a vector
    public Vector(Vector r) {
        this(r.x, r.y, r.z);
    }

    //accessor methods
    public double getx() { 
        return x; 
    }
    public double gety() { 
        return y; 
    }
    public double getz() { 
        return z; 
    }

    //setting coordinates
    public void setx(double x) { 
        this.x = x; 
    }
    public void sety(double y) { 
        this.y = y; 
    }
    public void setz(double z) { 
        this.z = z; 
    }

    //sums two vectors
    public Vector add(Vector other) {
        return new Vector(x+other.x, y+other.y, z+other.z);
    }

    //subtracts two vectors
    public Vector subtract(Vector other) {
        return new Vector(x-other.x, y-other.y, z-other.z);
    }

    //multiplies two vectors
    public Vector multiply(double k) {
        return new Vector(x*k, y*k,z*k);
    }

    //returns the dot product of two vectors
    public double dot(Vector other) {
        return (x*other.x)+(y*other.y)+(z*other.z);
    }

    //returns the cross product of two vectors
    public Vector cross(Vector other) {
        return new Vector(y*other.z - z*other.y, z*other.x - x*other.z, x*other.y - y*other.x);
    }

    //returns a unit vector in the same directon
    public Vector normalize() {
        double mag = magnitude();
        return new Vector(x / mag, y / mag, z / mag);
    }

    //finds the length of the vector
    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    //calculates the distance between two vectors
    public double distance(Vector other) {
        return subtract(other).magnitude();
    }

    //returns string version of the vector
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}



