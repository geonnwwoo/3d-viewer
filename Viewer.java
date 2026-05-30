package lhs.finalproject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class Viewer extends JPanel {

    private static final double ROT_SPEED  = 2.2;
    private static final double ZOOM_SPEED = 1.4;
    private static final double INPUT_RAMP = 12.0;
    private static final double PAN_DECAY  = 5.0;
    private static final int    FRAME_MS   = 16;

    private int lastMouseX, lastMouseY;
    private double panX = 400.0, panY = 300.0;
    private double panXVel = 0, panYVel = 0;
    private long lastDragNano = 0;

    private Camera camera;
    private List<Vector> points;
    private List<int[]> mesh;
    private Vector initialCamPos;
    private Timer timer;

    private double theta;
    private double phi;
    private double radius;

    private double thetaVel = 0, phiVel = 0, zoomRate = 0;
    private long lastFrameNano = 0;

    private boolean isHPressed = false, isJPressed = false, isKPressed = false, isLPressed = false,
            isPlusPressed = false, isMinusPressed = false, isOPressed = false;

    public Viewer(float[][][] voxelGrid) {
        points = new ArrayList<>();
        mesh = new ArrayList<>();

        buildMeshFromVoxels(voxelGrid);

        // Auto-pick a camera position based on grid size so the object fits in view
        int gridZ = voxelGrid.length;
        int gridY = (gridZ > 0) ? voxelGrid[0].length : 0;
        int gridX = (gridY > 0) ? voxelGrid[0][0].length : 0;
        double maxDim = Math.max(1, Math.max(gridX, Math.max(gridY, gridZ)));
        Vector camPos = new Vector(maxDim * 2.0, maxDim * 2.0, maxDim * 2.0);
        initialCamPos = camPos;

        camera = new Camera(camPos, 60);

        radius = Math.sqrt(camPos.getx() * camPos.getx() + camPos.gety() * camPos.gety() + camPos.getz() * camPos.getz());
        theta = Math.atan2(camPos.getz(), camPos.getx());
        phi = Math.acos(camPos.gety() / radius);

        // --- Mouse: drag to pan, with momentum on release ---
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                // grabbing the canvas kills any leftover momentum
                panXVel = 0;
                panYVel = 0;
                lastDragNano = System.nanoTime();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                panX += dx;
                panY += dy;

                // Track pan velocity via exponential moving average so a single
                // jittery event doesn't dominate the post-release coast.
                long now = System.nanoTime();
                double dragDt = (now - lastDragNano) / 1e9;
                if (dragDt > 0.001 && dragDt < 0.1) {
                    double alpha = 0.4;
                    panXVel = (1 - alpha) * panXVel + alpha * (dx / dragDt);
                    panYVel = (1 - alpha) * panYVel + alpha * (dy / dragDt);
                }
                lastDragNano = now;

                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        };
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        // --- Key bindings (HJKL rotate, +/- zoom, O reset) ---
        bindKey("H", () -> isHPressed = true,  () -> isHPressed = false);
        bindKey("J", () -> isJPressed = true,  () -> isJPressed = false);
        bindKey("K", () -> isKPressed = true,  () -> isKPressed = false);
        bindKey("L", () -> isLPressed = true,  () -> isLPressed = false);
        bindKey("O", () -> isOPressed = true,  () -> isOPressed = false);
        bindKeyCode(KeyEvent.VK_EQUALS, () -> isPlusPressed = true,  () -> isPlusPressed = false);
        bindKeyCode(KeyEvent.VK_MINUS,  () -> isMinusPressed = true, () -> isMinusPressed = false);

        lastFrameNano = System.nanoTime();
        timer = new Timer(FRAME_MS, e -> tick());
        timer.start();
    }

    // Helper to deduplicate the repetitive key-binding boilerplate.
    private void bindKey(String key, Runnable onPress, Runnable onRelease) {
        String pressName = "press_" + key;
        String releaseName = "release_" + key;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), pressName);
        getActionMap().put(pressName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onPress.run(); }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released " + key), releaseName);
        getActionMap().put(releaseName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onRelease.run(); }
        });
    }

    private void bindKeyCode(int keyCode, Runnable onPress, Runnable onRelease) {
        String pressName = "press_code_" + keyCode;
        String releaseName = "release_code_" + keyCode;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, false), pressName);
        getActionMap().put(pressName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onPress.run(); }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, 0, true), releaseName);
        getActionMap().put(releaseName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onRelease.run(); }
        });
    }

    // --- Per-frame update: delta-time based, with smoothed input ramping ---
    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastFrameNano) / 1e9;
        lastFrameNano = now;
        if (dt > 0.1) dt = 0.1; // cap dt to avoid huge jumps after a window stall

        if (isOPressed) {
            reset();
            repaint();
            return;
        }

        // Ramp velocities toward target (soft accel/coast feel)
        double rampFactor = 1.0 - Math.exp(-INPUT_RAMP * dt);

        double targetThetaVel = ((isLPressed ? 1.0 : 0.0) - (isHPressed ? 1.0 : 0.0)) * ROT_SPEED;
        double targetPhiVel   = ((isKPressed ? 1.0 : 0.0) - (isJPressed ? 1.0 : 0.0)) * ROT_SPEED;
        double targetZoomRate = ((isMinusPressed ? 1.0 : 0.0) - (isPlusPressed ? 1.0 : 0.0)) * ZOOM_SPEED;

        thetaVel += (targetThetaVel - thetaVel) * rampFactor;
        phiVel   += (targetPhiVel   - phiVel)   * rampFactor;
        zoomRate += (targetZoomRate - zoomRate) * rampFactor;

        // Integrate
        theta += thetaVel * dt;
        phi   += phiVel   * dt;

        // Clamp phi to avoid camera flipping at the poles
        double eps = 0.04;
        if (phi < eps) { phi = eps; phiVel = 0; }
        if (phi > Math.PI - eps) { phi = Math.PI - eps; phiVel = 0; }

        // Exponential zoom keeps the feel uniform regardless of current radius
        double newRadius = radius * Math.exp(zoomRate * dt);
        if (newRadius > 100000.0) { newRadius = 100000.0; zoomRate = 0; }
        if (newRadius < 0.01)     { newRadius = 0.01;     zoomRate = 0; }
        radius = newRadius;

        // Pan momentum
        panX += panXVel * dt;
        panY += panYVel * dt;
        double panDecayFactor = Math.exp(-PAN_DECAY * dt);
        panXVel *= panDecayFactor;
        panYVel *= panDecayFactor;
        // snap tiny velocities to zero so we don't twitch forever
        if (Math.abs(panXVel) < 0.5) panXVel = 0;
        if (Math.abs(panYVel) < 0.5) panYVel = 0;

        updateCameraPosition();
        repaint();
    }

    private void updateCameraPosition() {
        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.cos(phi);
        double z = radius * Math.sin(phi) * Math.sin(theta);
        camera.setPosition(new Vector(x, y, z));
    }

    // --- Mesh building (unchanged) ---
    private void buildMeshFromVoxels(float[][][] voxelGrid) {
        if (voxelGrid == null || voxelGrid.length == 0) return;
        int gridZ = voxelGrid.length;
        int gridY = voxelGrid[0].length;
        int gridX = voxelGrid[0][0].length;

        double offsetX = gridX / 2.0;
        double offsetY = gridY / 2.0;
        double offsetZ = gridZ / 2.0;

        for (int z = 0; z < gridZ; z++) {
            for (int y = 0; y < gridY; y++) {
                for (int x = 0; x < gridX; x++) {
                    if (voxelGrid[z][y][x] != 0f) {
                        addVoxelFaces(voxelGrid, x, y, z, gridX, gridY, gridZ, offsetX, offsetY, offsetZ);
                    }
                }
            }
        }
    }

    private boolean isEmpty(float[][][] grid, int x, int y, int z, int gridX, int gridY, int gridZ) {
        if (x < 0 || x >= gridX || y < 0 || y >= gridY || z < 0 || z >= gridZ) return true;
        return grid[z][y][x] == 0f;
    }

    private void addVoxelFaces(float[][][] grid, int x, int y, int z,
                               int gridX, int gridY, int gridZ,
                               double offX, double offY, double offZ) {
        double fx = x - offX;
        double fy = y - offY;
        double fz = z - offZ;
        double s = 1.0;

        int base = points.size();
        points.add(new Vector(fx,     fy,     fz));      // 0
        points.add(new Vector(fx + s, fy,     fz));      // 1
        points.add(new Vector(fx + s, fy + s, fz));      // 2
        points.add(new Vector(fx,     fy + s, fz));      // 3
        points.add(new Vector(fx,     fy,     fz + s));  // 4
        points.add(new Vector(fx + s, fy,     fz + s));  // 5
        points.add(new Vector(fx + s, fy + s, fz + s));  // 6
        points.add(new Vector(fx,     fy + s, fz + s));  // 7

        if (isEmpty(grid, x, y, z - 1, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 0, base + 2, base + 1});
            mesh.add(new int[]{base + 0, base + 3, base + 2});
        }
        if (isEmpty(grid, x, y, z + 1, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 4, base + 5, base + 6});
            mesh.add(new int[]{base + 4, base + 6, base + 7});
        }
        if (isEmpty(grid, x - 1, y, z, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 0, base + 4, base + 7});
            mesh.add(new int[]{base + 0, base + 7, base + 3});
        }
        if (isEmpty(grid, x + 1, y, z, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 1, base + 2, base + 6});
            mesh.add(new int[]{base + 1, base + 6, base + 5});
        }
        if (isEmpty(grid, x, y - 1, z, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 0, base + 1, base + 5});
            mesh.add(new int[]{base + 0, base + 5, base + 4});
        }
        if (isEmpty(grid, x, y + 1, z, gridX, gridY, gridZ)) {
            mesh.add(new int[]{base + 3, base + 7, base + 6});
            mesh.add(new int[]{base + 3, base + 6, base + 2});
        }
    }

    private static class Triangle {
        int[] indices;
        double avgDepth;
        Triangle(int[] indices, double avgDepth) {
            this.indices = indices;
            this.avgDepth = avgDepth;
        }
    }

    // --- Rendering ---
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Stats overlay
        g2.setColor(Color.WHITE);
        g2.drawString("Points: " + points.size(), 10, 20);
        g2.drawString("Triangles: " + mesh.size(), 10, 40);
        g2.drawString(String.format("Camera xyz: (%.4f, %.4f, %.4f)",
                camera.getPosition().getx(), camera.getPosition().gety(), camera.getPosition().getz()), 10, 60);
        g2.drawString(String.format("Camera A-A: (%.4f, %.4f)", Math.toDegrees(theta), Math.toDegrees(phi)), 10, 80);
        g2.drawString(String.format("Camera radius: %.4f", radius), 10, 100);
        g2.drawString(String.format("Origin: (%.2f, %.2f)", panX, panY), 10, 120);

        if (mesh == null || mesh.isEmpty()) return;

        Vector camPos = camera.getPosition();
        List<Triangle> sortedTriangles = new ArrayList<>(mesh.size());

        for (int[] face : mesh) {
            if (face[0] < 0 || face[0] >= points.size()
                    || face[1] < 0 || face[1] >= points.size()
                    || face[2] < 0 || face[2] >= points.size()) continue;

            Vector p1 = points.get(face[0]);
            Vector p2 = points.get(face[1]);
            Vector p3 = points.get(face[2]);

            // Backface cull: outward normal pointing away from camera -> skip.
            // Mesh is generated with consistent winding so outward normals are correct.
            Vector e1 = p2.subtract(p1);
            Vector e2 = p3.subtract(p1);
            Vector rawNormal = e1.cross(e2);
            Vector camToFace = p1.subtract(camPos);
            if (rawNormal.dot(camToFace) > 0) continue;

            double depth = (p1.subtract(camPos).magnitude()
                    + p2.subtract(camPos).magnitude()
                    + p3.subtract(camPos).magnitude()) / 3.0;
            sortedTriangles.add(new Triangle(face, depth));
        }

        sortedTriangles.sort(Comparator.comparingDouble((Triangle t) -> t.avgDepth).reversed());

        Vector lightDir = new Vector(0.5, -0.5, -0.7).normalize();
        int w = getWidth();
        int h = getHeight();

        for (Triangle tri : sortedTriangles) {
            int[] face = tri.indices;
            double[] c1 = camera.project(points.get(face[0]), w, h);
            double[] c2 = camera.project(points.get(face[1]), w, h);
            double[] c3 = camera.project(points.get(face[2]), w, h);
            if (c1 == null || c2 == null || c3 == null) continue;

            Vector p1 = points.get(face[0]);
            Vector p2 = points.get(face[1]);
            Vector p3 = points.get(face[2]);
            Vector normal = p2.subtract(p1).cross(p3.subtract(p1)).normalize();

            double lightIntensity = Math.max(0.2, Math.abs(normal.dot(lightDir)));
            int color = (int) (lightIntensity * 200);
            g2.setColor(new Color(color, color, color));

            int[] xPoints = {(int)(c1[0] + panX), (int)(c2[0] + panX), (int)(c3[0] + panX)};
            int[] yPoints = {(int)(c1[1] + panY), (int)(c2[1] + panY), (int)(c3[1] + panY)};
            g2.fillPolygon(xPoints, yPoints, 3);
        }
    }

    public void reset() {
        Vector resetPos = initialCamPos;
        camera.setPosition(resetPos);
        radius = Math.sqrt(resetPos.getx() * resetPos.getx()
                + resetPos.gety() * resetPos.gety()
                + resetPos.getz() * resetPos.getz());
        theta = Math.atan2(resetPos.getz(), resetPos.getx());
        phi = Math.acos(resetPos.gety() / radius);
        thetaVel = 0;
        phiVel = 0;
        zoomRate = 0;
        panX = 400;
        panY = 300;
        panXVel = 0;
        panYVel = 0;
    }

    public static void main(String[] args) {
        try {
            BatchImageServer.startServer();
            System.out.println("\nPress ENTER once photos have uploaded...");
            new java.util.Scanner(System.in).nextLine();

            MainRunner runner = new MainRunner();
            float[][][] voxelGrid = runner.runPipeline();

            JFrame frame = new JFrame("3D visualizer");
            frame.add(new Viewer(voxelGrid));
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error: Pipeline failed.");
            e.printStackTrace();
        }
    }
}