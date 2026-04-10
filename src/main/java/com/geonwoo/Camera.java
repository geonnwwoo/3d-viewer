package com.geonwoo;

public class Camera {
    private Vector position;
    private double fov;
    
    public Camera(Vector position, double fov) {
        this.position = position;
        this.fov = fov;
    }
    
    public Camera() {
        this(new Vector(0, 5, 0), 60);
    }
    
    public Vector getPosition() { return position; }
    public double getFov() { return fov; }
    
    public void setPosition(Vector position) { this.position = position; }
    public void setFov(double fov) { this.fov = fov; }
    
    public double[] project(Vector point, int screenWidth, int screenHeight) {
        Vector forward = new Vector(0, 0, 0).subtract(this.position).normalize();
        Vector worldUp = new Vector(0, 1, 0);
        Vector right = forward.cross(worldUp).normalize();
        Vector up = right.cross(forward).normalize();
        
        Vector translated = point.subtract(this.position);
        
        double x = translated.dot(right);
        double y = translated.dot(up);
        double z = translated.dot(forward);
        
        if (z <= 0) {
            return null;
        }
        
        double fovRadians = Math.toRadians(this.fov);
        double projectionPlaneDistance = 1.0 / Math.tan(fovRadians / 2.0);
        
        double projectedX = (x * projectionPlaneDistance) / z;
        double projectedY = (y * projectionPlaneDistance) / z;
        
        double aspectRatio = (double) screenWidth / screenHeight;
        
        int screenX = (int) ((projectedX / aspectRatio + 1.0) * screenWidth / 2.0);
        int screenY = (int) ((1.0 - projectedY) * screenHeight / 2.0);
        
        return new double[] {screenX, screenY};
    }
}
