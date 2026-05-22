import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Viewer extends JPanel implements ActionListener {
    //input filenames. POINTSFILE contains point data, CAMERAFILE contains the original coordinates for the camera
    private static String CAMERAFILE="camera.txt";
    private static String POINTSFILE="cube.txt";

    private Camera camera;
    private List<Vector> points;
    private Vector camPos;
    private double angle;
    private Timer timer;
    private int fps; //approx
    private int size; //  size of points plotted
    private boolean isHPressed = false, isJPressed=false, isKPressed=false, isLPressed=false
    , isPlusPressed=false, isMinusPressed=false, isOPressed=false;
    
    public Viewer(String pointsFile, String cameraFile) {
        points=load(pointsFile);
        camPos=loadCamPos(cameraFile);
        angle=0;
        camera=new Camera(camPos, 60);
        fps=240;
        size=2;

        // keypress detecting methods
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("H"), "pressH");
        this.getActionMap().put("pressH", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isHPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released H"), "releaseH");
        this.getActionMap().put("releaseH", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isHPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("J"), "pressJ");
        this.getActionMap().put("pressJ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isJPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released J"), "releaseJ");
        this.getActionMap().put("releaseJ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isJPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("K"), "pressK");
        this.getActionMap().put("pressK", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isKPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released K"), "releaseK");
        this.getActionMap().put("releaseK", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isKPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("L"), "pressL");
        this.getActionMap().put("pressL", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isLPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released L"), "releaseL");
        this.getActionMap().put("releaseL", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isLPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, false), "press=");
        this.getActionMap().put("press=", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isPlusPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, true), "release=");
        this.getActionMap().put("release=", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isPlusPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false), "press-");
        this.getActionMap().put("press-", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isMinusPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, true), "release-");
        this.getActionMap().put("release-", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isMinusPressed = false;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("O"), "pressO");
        this.getActionMap().put("pressO", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isOPressed = true;
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released O"), "releaseO");
        this.getActionMap().put("releaseO", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isOPressed = false;
            }
        });





        timer = new Timer((int)fps/16, e-> {
            if (isHPressed) {incX(-1);}
            if (isJPressed) {incY(-1);}
            if (isKPressed) {incY(1);}
            if (isLPressed) {incX(1);}
            if (isPlusPressed) {incCam(1);}
            if (isMinusPressed) {incCam(-1);}
            if (isOPressed) {reset();}
            repaint();
        });  // fps/16 gives the approximate fps
        timer.start();
    }
    
    private List<Vector> load(String filename) {
        List<Vector> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(filename)))) {
            String line;
            while ((line=br.readLine())!=null) {//return of br.readLine()
                line=line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts=line.split("\\s+");
                if (parts.length>=3) {
                    double x=Double.parseDouble(parts[0]);
                    double y=Double.parseDouble(parts[1]);
                    double z=Double.parseDouble(parts[2]);
                    result.add(new Vector(x,y,z));
                }
            }
        } 
        catch (Exception e) {
            //System.err.println("ERROR DURING load() method: "+e.getMessage());
            //e.printStackTrace();
        }
        return result;
    } 

    private Vector loadCamPos(String filename) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            String line=br.readLine();
            if (line!=null) {
                line=line.trim();
                String[] parts=line.split("\\s+");

                if (parts.length>=3) {
                    double x=Double.parseDouble(parts[0]);
                    double y=Double.parseDouble(parts[1]);
                    double z=Double.parseDouble(parts[2]);
                    return new Vector(x,y,z);
                }
            }
        } 
        catch (Exception e) {
            //System.err.println("ERROR DURING loadCamPos() method: "+e.getMessage());
            //e.printStackTrace();
        }
        return new Vector(10,6,10); // Default camera position camPos
    }
   



    // MAIN GRAPHICS METHOD
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(),getHeight()); // background is black
        g.setColor(Color.WHITE);  // points are white
        for (Vector point:points) {
            double[] coords=camera.project(point,getWidth(),getHeight());
            if (coords!=null) { g.fillOval((int)coords[0]-2,(int)coords[1]-2,size,size); }
        }


        // display stats
        g.drawString("Points: " + points.size(), 10, 20);
        g.drawString(String.format("Camera: (%.2f, %.2f, %.2f)", 
            camera.getPosition().getx(), 
            camera.getPosition().gety(), 
            camera.getPosition().getz()), 10, 40);
    }

    public void incX(int dir) { // for rotation, height y is fixed
        if (dir > 0) { 
            angle += 0.01; 
        } else { 
            angle-=0.01; 
        }

        Vector pos=camera.getPosition();
        double r=Math.sqrt(pos.getx() * pos.getx() + pos.getz() * pos.getz());
        double y=pos.gety();
        double x=r*Math.cos(angle);
        double z=r*Math.sin(angle);
        
        camera.setPosition(new Vector(x, y, z));
    }
    
    public void incY(int dir) {
        Vector pos=camera.getPosition();
        double deltaY=dir*0.1;
        camera.setPosition(new Vector(pos.getx(), pos.gety()+deltaY, pos.getz()));
    }

    public void incCam(int dir) {
      Vector pos=camera.getPosition();
      
      double scale=1.0+(dir*-0.01);
      double xp = pos.getx()*scale;
      double yp=pos.gety()*scale;
      double zp=pos.getz()*scale;
      double newDistance=Math.sqrt(newX*newX+newY*newY+newZ*newZ);
      
      if (newDistance<0.2 || newDistance>80.0) { return; }
      camera.setPosition(new Vector(xp,yp,zp)); 
    }

    public void reset() { // keypress 'o'
        camera.setPosition(loadCamPos("camera.txt"));
    }

    public void actionPerformed(ActionEvent e) { repaint(); }
    
   




    public static void main(String[] args) {
        JFrame frame=new JFrame("3D visualizer"); //title of program (top)
        frame.add(new Viewer(POINTSFILE, CAMERAFILE)); // at top of this file, can customize
        frame.setSize(800,600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
