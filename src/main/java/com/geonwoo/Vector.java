package com.geonwoo;

public class Vector {
    private double x, y, z;
    
    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector() {
        this(0, 0, 0);
    }
    
    public Vector(Vector r) {
        this(r.x, r.y, r.z);
    }
    
    public double getx() { return x; }
    public double gety() { return y; }
    public double getz() { return z; }
    
    public void setx(double x) { this.x = x; }
    public void sety(double y) { this.y = y; }
    public void setz(double z) { this.z = z; }
    
    public Vector add(Vector other) {
        return new Vector(x + other.x, y + other.y, z + other.z);
    }
    
    public Vector subtract(Vector other) {
        return new Vector(x - other.x, y - other.y, z - other.z);
    }
    
    public Vector multiply(double k) {
        return new Vector(x * k, y * k, z * k);
    }
    
    public double dot(Vector other) {
        return x * other.x + y * other.y + z * other.z;
    }
    
    public Vector cross(Vector other) {
        return new Vector(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        );
    }
    
    public Vector normalize() {
        double mag = magnitude();
        return new Vector(x / mag, y / mag, z / mag);
    }
    
    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public double distance(Vector other) {
        return subtract(other).magnitude();
    }
    
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}
