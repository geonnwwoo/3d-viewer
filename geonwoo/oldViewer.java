import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static String POINTSFILE="bunny.txt";
    private static String LINESFILE="bunny_lines.txt";
    
    private int lastMouseX, lastMouseY; // mouse drag
    private double panX=215.0, panY=299.0;
    private Camera camera;
    private List<Vector> points;
    private List<int[]> lines; // pairs of points
    private Vector camPos;
    private Timer timer;
    private double theta; // theta and phi for azimuth altitude coordinates
    private double phi;
    private double radius;
    private int fps; //approx
    private int size; //  size of points plotted
    private boolean isHPressed = false, isJPressed=false, isKPressed=false, isLPressed=false
    , isPlusPressed=false, isMinusPressed=false, isOPressed=false;
    
    public Viewer(String pointsFile, String cameraFile,String linesFile) {
        points=load(pointsFile);
        lines = loadLines(linesFile);
        camPos=loadCamPos(cameraFile);
        camera=new Camera(camPos, 60);
        fps=30;
        size=2;
        //define spherical coordinates based on xyz coordinate system
        radius=Math.sqrt(camPos.getx()*camPos.getx()+camPos.gety()*camPos.gety()+camPos.getz()*camPos.getz());
        theta=Math.atan2(camPos.getz(),camPos.getx());
        phi = Math.acos(camPos.gety()/radius);

        // mouse detecting
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX=e.getX();
                lastMouseY=e.getY();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx=e.getX() - lastMouseX;
                int dy=e.getY() - lastMouseY;
                panX+=dx;
                panY+=dy;
                lastMouseX=e.getX();
                lastMouseY=e.getY();
                repaint();
            }
        };
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

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
        List<Vector> result=new ArrayList<>();
        try (BufferedReader br=new BufferedReader(
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
        return new Vector(1,0,0); // Default camera position camPos
    }

    private void update() {
        double x=radius*Math.sin(phi)*Math.cos(theta);
        double y=radius*Math.cos(phi);
        double z=radius*Math.sin(phi)*Math.sin(theta);
        camera.setPosition(new Vector(x,y,z));
    }

    private List<int[]> loadLines(String filename) {
        List<int[]> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                // Assuming lines.txt contains two integers per line (0-based indices matching points.txt)
                if (parts.length >= 2) {
                    int p1 = Integer.parseInt(parts[0]);
                    int p2 = Integer.parseInt(parts[1]);
                    result.add(new int[]{p1, p2});
                }
            }
        }
        catch (Exception e) {
            //jijioasdp
        }
        return result;
    }

    // MAIN GRAPHICS METHOD
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(),getHeight());// background is black
        g.setColor(Color.WHITE); // points are all white
        for (Vector point:points) {
            double[] coords=camera.project(point,getWidth(),getHeight());
            if (coords!=null) { g.fillOval((int)(coords[0]+panX)-2,(int)(coords[1]+panY)-2,size,size); }
        }

        g.setColor(Color.WHITE); //lines also in white, change if wanted
        for (int[] line: lines) {
            if (line[0]>=0 && line[0]<points.size() && line[1]>=0 && line[1]<points.size()) {
                double[] c1=camera.project(points.get(line[0]),getWidth(),getHeight());
                double[] c2=camera.project(points.get(line[1]),getWidth(),getHeight());
                if (c1!=null && c2!=null) {
                    g.drawLine((int)(c1[0]+panX), (int)(c1[1]+panY), (int)(c2[0]+panX), (int)(c2[1]+panY));
                }
            }
        }
        //
        // display stats: number of points, then xyz, a-a, radius for a-a for the camera
        g.drawString("Points: "+points.size(),10,20);
        g.drawString(String.format("Camera xyz: (%.4f, %.4f, %.4f)", camera.getPosition().getx(), camera.getPosition().gety(), camera.getPosition().getz()), 10, 40);
        g.drawString(String.format("Camera A-A: (%.4f, %.4f)", Math.toDegrees(theta), Math.toDegrees(phi)), 10, 60);
        g.drawString(String.format("Camera radius: %.4f", radius),10,80);
        g.drawString(String.format("Origin: (%.2f, %.2f)",panX, panY),10,100);
    }

    public void incX(int dir) { // for rotation, height y is fixed
        theta+=dir*0.05;
        update();
    }
    
    public void incY(int dir) {
        phi+=dir*0.05;
        double epsilon=0.04;
        phi=Math.max(epsilon, Math.min(Math.PI-epsilon,phi));
        update(); 
    }

    public void incCam(int dir) {
        double k=1.0+(dir*-0.04);
        double rp=radius*k;
        // limits to how far camera can zoom out. Can comment out if wanted
        if (rp>80.0 || rp<0.01) {return;}
        radius=rp; update(); 
    }

    public void reset() { // keypress 'o'
        Vector resetPos=loadCamPos("camera.txt");
        camera.setPosition(resetPos);

        // spherical
        radius=Math.sqrt(resetPos.getx()*resetPos.getx() + resetPos.gety()*resetPos.gety() + resetPos.getz()*resetPos.getz());
        theta=Math.atan2(resetPos.getz(), resetPos.getx());
        phi=Math.acos(resetPos.gety()/radius);
        
        // reset offset of origin caused by mouse
        panX=0;panY=0;
    }

    public void actionPerformed(ActionEvent e) { repaint(); }
    
    public static void main(String[] args) {
        JFrame frame=new JFrame("3D visualizer"); //title of program (top)
        frame.add(new Viewer(POINTSFILE, CAMERAFILE, LINESFILE)); // at top of this file, can customize
        frame.setSize(800,600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
