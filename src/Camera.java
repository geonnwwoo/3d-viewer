public Camera {
  // Camera always points towards (0,0,0)
  Vector position;
  double fov; // in degrees, default=60 deg

  // Constructors
  public Camera(Vector position, double fov) {
    this.position=position;
    this.fov=fov;
  }
  public Camera {
    position=new Vector(0,5,0);
    fov=60;
  }
  
  // Getters
  public Vector getPosition() {
    return position;
  }
  public double getFov() {
    return fov;
  }

  // Setters
  public void setPosition(Vector position) {
    this.position=position;
  }
  public void setFov(double fov) {
    this.fov=fov;
  }
}
