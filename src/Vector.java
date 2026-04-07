public class Vector {
  private double x,y,z;
  
  // Constructor
  public Vector(double x, double y, double z) {
    this.x=x;
    this.y=y;
    this.z=z;
  }
  public Vector() {
    x=0; y=0; z=0;
  }
  public Vector(Vector r) {
    x=r.getx();
    y=r.gety();
    z=r.getz();
  }

  // Getters
  public double getx() {
    return x;
  }
  public double gety() {
    return y;
  }
  public double getz() {
    return z;
  }

  // Setters
  public void setx(double x) {
    this.x=x;
  }
  public void sety(double y) {
    this.y=y;
  }
  public void setz(double z) {
    this.z=z;
  }

  // Vector addition/subtraction/scalar multiple/dot product/cross product
  public Vector add(Vector other) {
    return new Vector(other.getx()+x, other.gety()+y, other.getz()+z);
  }
  public Vector subtract(Vector other) {
    return new Vector(x-other.getx(),y-other.gety(),z-other.getz());
  }
  public Vector multiply(double k) {
    return new Vector(x*k,y*k,z*k);
  }
  public double dot(Vector other) {
    return ((other.getx()*x)+(other.gety()*y)+(other.getz()*z));
  }
  public Vector cross(Vector other) {
    return new Vector(
      y*other.getz()-z*other.gety(),
      z*other.getx()-x*other.getz(),
      x*other.gety()-y*other.getx()
    )
  }

  // Vector magnitude/Distance
  public double magnitude() {
    return Math.sqrt(x*x+y*y+z*z);
  }
  public Vector distance(Vector other) {
    return subtract(other).magnitude();
  }

  // toString()
  @Override
  public String toString() {
    return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
  }
}
