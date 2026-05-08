import java.awt.Color;
import java.awt.Graphics;

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
    private boolean isXPressed = false, isYPressed=false, isZPressed=false;
    
    public Points(String pointsFile, String cameraFile) {
        points = loadPoints(pointsFile);
        initialCameraPosition = loadInitialCameraPosition(cameraFile);
        angle = 0;
        
        camera = new Camera(initialCameraPosition, 60);
    
        fps=60;
        size=2;


        // 1. Map the "X" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("X"), "pressX");
        this.getActionMap().put("pressX", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isXPressed = true;
            }
        });

        // 2. Map the "X" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released X"), "releaseX");
        this.getActionMap().put("releaseX", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isXPressed = false;
            }
        });

        // 1. Map the "Y" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Y"), "pressY");
        this.getActionMap().put("pressY", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isYPressed = true;
            }
        });

        // 2. Map the "Y" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released Y"), "releaseY");
        this.getActionMap().put("releaseY", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isYPressed = false;
            }
        });

        // 1. Map the "Z" key press
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Z"), "pressZ");
        this.getActionMap().put("pressZ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isZPressed = true;
            }
        });

        // 2. Map the "Z" key release
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released Z"), "releaseZ");
        this.getActionMap().put("releaseZ", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                isZPressed = false;
            }
        });









        timer = new Timer((int)fps/16, e-> {
            if (isXPressed) {incX();}
            if (isYPressed) {incY();}
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

    public void incX() {
        angle+=0.01;

        double radius = Math.sqrt(
        initialCameraPosition.getx() * initialCameraPosition.getx() + 
        initialCameraPosition.getz() * initialCameraPosition.getz()
        );
        double y = initialCameraPosition.gety(); // Keep same height
        
        // Rotate camera around origin
        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        
        camera.setPosition(new Vector(x, y, z));
        repaint();
    }
    public void incY() {
        camera.setPosition(new Vector(camera.getPosition().getx(), camera.getPosition().gety()+1, camera.getPosition().getz()));
    }
    public void incZ() {
        camera.setPosition(new Vector(camera.getPosition().getx(), camera.getPosition().gety(),camera.getPosition().getz()+1));
    }
   


    public void actionPerformed(ActionEvent e) {
        //angle += 0.02; // Rotation speed
       
        
        


        // Calculate radius from initial position
        double radius = Math.sqrt(
            initialCameraPosition.getx() * initialCameraPosition.getx() + 
            initialCameraPosition.getz() * initialCameraPosition.getz()
        );
        double y = initialCameraPosition.gety(); // Keep same height
        
        // Rotate camera around origin
        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        
        camera.setPosition(new Vector(x, y, z));
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
