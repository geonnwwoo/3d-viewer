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


public class Points extends JPanel implements ActionListener {
    private Camera camera;
    private List<Vector> points;
    private Vector initialCameraPosition;
    private double angle;
    private Timer timer;
    private int fps;
    private int size;
    private boolean isHPressed = false, isJPressed=false, isKPressed=false, isLPressed=false
    , isPlusPressed=false, isMinusPressed=false, isOPressed=false;
    
    public Points(String pointsFile, String cameraFile) {
        points = loadPoints(pointsFile);
        initialCameraPosition = loadInitialCameraPosition(cameraFile);
        angle = 0;
        
        camera = new Camera(initialCameraPosition, 60);
    
        fps=120;
        size=2;


        // 1. Map the "H" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("H"), "pressH");
        this.getActionMap().put("pressH", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isHPressed = true;
            }
        });

        // 2. Map the "H" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released H"), "releaseH");
        this.getActionMap().put("releaseH", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isHPressed = false;
            }
        });

        // 1. Map the "J" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("J"), "pressJ");
        this.getActionMap().put("pressJ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isJPressed = true;
            }
        });

        // 2. Map the "J" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released J"), "releaseJ");
        this.getActionMap().put("releaseJ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isJPressed = false;
            }
        });

        // 1. Map the "K" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("K"), "pressK");
        this.getActionMap().put("pressK", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isKPressed = true;
            }
        });

        // 2. Map the "K" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released K"), "releaseK");
        this.getActionMap().put("releaseK", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isKPressed = false;
            }
        });


        // 1. Map the "L" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("L"), "pressL");
        this.getActionMap().put("pressL", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isLPressed = true;
            }
        });

        // 2. Map the "L" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released L"), "releaseL");
        this.getActionMap().put("releaseL", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isLPressed = false;
            }
        });

        // 1. Map the "=" (Plus) key press
        // Parameters: keyCode, modifiers (0 = none), onKeyRelease (false = press)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, false), "press=");
        this.getActionMap().put("press=", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isPlusPressed = true;
            }
        });

        // 2. Map the "=" (Plus) key release
        // Parameters: keyCode, modifiers (0 = none), onKeyRelease (true = release)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, true), "release=");
        this.getActionMap().put("release=", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isPlusPressed = false;
            }
        });

        // 3. Map the "-" (Minus) key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false), "press-");
        this.getActionMap().put("press-", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isMinusPressed = true;
            }
        });

        // 4. Map the "-" (Minus) key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, true), "release-");
        this.getActionMap().put("release-", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isMinusPressed = false;
            }
        });

        
        // 1. Map the "O" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("O"), "pressO");
        this.getActionMap().put("pressO", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isOPressed = true;
            }
        });

        // 2. Map the "O" key release
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
    
    private List<Vector> loadPoints(String filename) {
        List<Vector> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(filename)))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    result.add(new Vector(x, y, z));
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading points: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    } 

    private Vector loadInitialCameraPosition(String filename) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(filename)))) {

            String line = br.readLine();
            if (line != null) {
                line = line.trim();
                String[] parts = line.split("\\s+");

                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    return new Vector(x, y, z);
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading camera position: " + e.getMessage());
            e.printStackTrace();
        }
        return new Vector(5, 3, 5); // Original
    }
   



    // MAIN GRAPHICS METHOD
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        
        for (Vector point : points) {
            double[] coords = camera.project(point, getWidth(), getHeight());
            if (coords != null) {
                g.fillOval((int)coords[0] - 2, (int)coords[1] - 2, size, size);
            }
        }
        
        g.drawString("Points: " + points.size(), 10, 20);
        g.drawString(String.format("Camera: (%.2f, %.2f, %.2f)", 
            camera.getPosition().getx(), 
            camera.getPosition().gety(), 
            camera.getPosition().getz()), 10, 40);
    }

    public void incX(int dir) {
        // H/L: Rotate left/right around Y-axis (horizontal rotation)
        if (dir > 0) { 
            angle += 0.01; 
        } else { 
            angle -= 0.01; 
        }

        Vector pos = camera.getPosition();
        double radius = Math.sqrt(pos.getx() * pos.getx() + pos.getz() * pos.getz());
        double y = pos.gety();
        
        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        
        camera.setPosition(new Vector(x, y, z));
    }
    
    public void incY(int dir) {
        // J/K: Move camera up/down (vertical movement)
        Vector pos = camera.getPosition();
        double deltaY = dir * 0.1;
        camera.setPosition(new Vector(pos.getx(), pos.gety() + deltaY, pos.getz()));
    }

    public void incCam(int dir) {
      Vector pos = camera.getPosition();
      
      // Move camera along the direction vector from origin to camera
      double scale = 1.0 + (dir * 0.01); // 5% closer or farther each frame
      
      double newX = pos.getx() * scale;
      double newY = pos.gety() * scale;
      double newZ = pos.getz() * scale;
      
      // Optional: add limits
      double newDistance = Math.sqrt(newX * newX + newY * newY + newZ * newZ);
      if (newDistance < 0.2 || newDistance > 80.0) {
          return; // Don't zoom if out of bounds
      }
      
      camera.setPosition(new Vector(newX, newY, newZ)); 
    }

    public void reset() {
        camera.setPosition(loadInitialCameraPosition("camera.txt"));
    }


    public void actionPerformed(ActionEvent e) {
        repaint();
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("3D visualizer");
        frame.add(new Points("cube.txt", "camera.txt"));
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
