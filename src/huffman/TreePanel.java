package huffman;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom JPanel for visualizing the Huffman binary tree.
 * Renders nodes as circular glyphs with connecting branch lines,
 * labeled edges ('0' for left, '1' for right), interactive zoom buttons (+ / - / Reset),
 * and automatic scrollbar integration.
 */
@SuppressWarnings("serial")
public final class TreePanel extends JPanel {

    private static final int NODE_DIAMETER = 44;
    private static final int NODE_RADIUS = NODE_DIAMETER / 2;
    private static final int VERTICAL_GAP = 72;
    private static final int HORIZONTAL_SPACING = 62;
    private static final int TOP_MARGIN = 55;
    private static final int SIDE_MARGIN = 45;

    private HuffmanNode root;
    private double zoomFactor = 1.0;
    private int leafCount = 0;
    private int treeDepth = 0;

    // Computed coordinates of nodes
    private final Map<HuffmanNode, Point> nodeCoordinates = new HashMap<>();
    private final TreeCanvas canvas;

    public TreePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        canvas = new TreeCanvas();
        add(createControlBar(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
    }

    private JToolBar createControlBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(245, 247, 250));
        toolbar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JButton zoomInBtn = new JButton("Zoom In (+)");
        zoomInBtn.setToolTipText("Increase tree view scale");
        zoomInBtn.setFocusPainted(false);
        zoomInBtn.addActionListener(e -> setZoom(zoomFactor + 0.15));

        JButton zoomOutBtn = new JButton("Zoom Out (-)");
        zoomOutBtn.setToolTipText("Decrease tree view scale");
        zoomOutBtn.setFocusPainted(false);
        zoomOutBtn.addActionListener(e -> setZoom(zoomFactor - 0.15));

        JButton resetZoomBtn = new JButton("Reset Zoom (100%)");
        resetZoomBtn.setToolTipText("Reset scale to default 100%");
        resetZoomBtn.setFocusPainted(false);
        resetZoomBtn.addActionListener(e -> setZoom(1.0));

        toolbar.add(zoomInBtn);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(zoomOutBtn);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(resetZoomBtn);

        return toolbar;
    }

    /**
     * Updates the displayed tree from the given root node.
     *
     * @param root the root node of the Huffman tree
     */
    public void setTree(HuffmanNode root) {
        this.root = root;
        this.nodeCoordinates.clear();
        this.leafCount = 0;
        this.treeDepth = 0;

        if (root != null) {
            calculateTreeMetrics(root, 1);
            int[] nextX = new int[]{SIDE_MARGIN};
            layoutTree(root, 0, nextX);
        }

        updateCanvasSize();
        canvas.repaint();
    }

    /**
     * Clears the displayed tree.
     */
    public void clear() {
        setTree(null);
    }

    /**
     * Changes the current zoom factor and updates canvas size.
     *
     * @param newZoom scale factor
     */
    public void setZoom(double newZoom) {
        this.zoomFactor = Math.max(0.25, Math.min(3.0, newZoom));
        updateCanvasSize();
        canvas.repaint();
    }

    private void calculateTreeMetrics(HuffmanNode node, int depth) {
        if (node == null) return;
        treeDepth = Math.max(treeDepth, depth);
        if (node.isLeaf()) {
            leafCount++;
        }
        calculateTreeMetrics(node.left, depth + 1);
        calculateTreeMetrics(node.right, depth + 1);
    }

    private int layoutTree(HuffmanNode node, int depth, int[] nextX) {
        if (node == null) return 0;

        int y = TOP_MARGIN + (depth * VERTICAL_GAP);

        if (node.isLeaf()) {
            int x = nextX[0];
            nextX[0] += HORIZONTAL_SPACING;
            nodeCoordinates.put(node, new Point(x, y));
            return x;
        }

        // Internal node: left child
        int leftX = 0;
        boolean hasLeft = (node.left != null);
        if (hasLeft) {
            leftX = layoutTree(node.left, depth + 1, nextX);
        }

        // Internal node: right child
        int rightX = 0;
        boolean hasRight = (node.right != null);
        if (hasRight) {
            rightX = layoutTree(node.right, depth + 1, nextX);
        }

        int x;
        if (hasLeft && hasRight) {
            x = (leftX + rightX) / 2;
        } else if (hasLeft) {
            x = leftX + (HORIZONTAL_SPACING / 2);
        } else if (hasRight) {
            x = rightX - (HORIZONTAL_SPACING / 2);
        } else {
            x = nextX[0];
            nextX[0] += HORIZONTAL_SPACING;
        }

        nodeCoordinates.put(node, new Point(x, y));
        return x;
    }

    private void updateCanvasSize() {
        int maxX = SIDE_MARGIN * 2;
        int maxY = TOP_MARGIN * 2;

        for (Point p : nodeCoordinates.values()) {
            if (p.x > maxX) maxX = p.x;
            if (p.y > maxY) maxY = p.y;
        }

        int scaledWidth = (int) ((maxX + SIDE_MARGIN + NODE_DIAMETER) * zoomFactor);
        int scaledHeight = (int) ((maxY + TOP_MARGIN + NODE_DIAMETER) * zoomFactor);

        scaledWidth = Math.max(500, scaledWidth);
        scaledHeight = Math.max(400, scaledHeight);

        canvas.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
        canvas.revalidate();
    }

    /**
     * Inner painting canvas that renders nodes and branch connections under zoom.
     */
    @SuppressWarnings("serial")
    private final class TreeCanvas extends JPanel {

        TreeCanvas() {
            setBackground(Color.WHITE);

            // Tooltip support for hovered nodes
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (root == null || nodeCoordinates.isEmpty()) {
                        setToolTipText(null);
                        return;
                    }

                    int unscaledX = (int) (e.getX() / zoomFactor);
                    int unscaledY = (int) (e.getY() / zoomFactor);

                    for (Map.Entry<HuffmanNode, Point> entry : nodeCoordinates.entrySet()) {
                        Point p = entry.getValue();
                        double dx = unscaledX - p.x;
                        double dy = unscaledY - p.y;
                        if ((dx * dx + dy * dy) <= (NODE_RADIUS * NODE_RADIUS)) {
                            HuffmanNode node = entry.getKey();
                            if (node.isLeaf()) {
                                setToolTipText("Leaf: " + FrequencyCounter.formatChar(node.character) + " (Freq: " + node.frequency + ")");
                            } else {
                                setToolTipText("Internal Node (Combined Freq: " + node.frequency + ")");
                            }
                            return;
                        }
                    }
                    setToolTipText(null);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (root == null) {
                g2.setColor(new Color(130, 130, 130));
                g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
                String emptyMsg = "No Huffman tree built. Type or load text and press Compress to view the tree.";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(emptyMsg)) / 2;
                int y = getHeight() / 2;
                g2.drawString(emptyMsg, Math.max(20, x), y);
                g2.dispose();
                return;
            }

            // Warning for extremely large trees (> 120 unique characters)
            if (leafCount > 120) {
                g2.setColor(new Color(255, 243, 205));
                g2.fillRect(10, 10, getWidth() - 20, 26);
                g2.setColor(new Color(133, 100, 4));
                g2.drawRect(10, 10, getWidth() - 20, 26);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.drawString("⚠️ Extremely large tree (" + leafCount + " leaves). Use Zoom Out (-) and scrollbars to navigate.", 20, 27);
            }

            // Apply zoom scaling
            g2.scale(zoomFactor, zoomFactor);

            // 1. Draw connecting branches first (behind nodes)
            drawBranches(g2, root);

            // 2. Draw nodes on top
            drawNodes(g2, root);

            g2.dispose();
        }

        private void drawBranches(Graphics2D g2, HuffmanNode node) {
            if (node == null) return;
            Point parentPoint = nodeCoordinates.get(node);
            if (parentPoint == null) return;

            // Draw branch to left child (Bit '0')
            if (node.left != null) {
                Point leftPoint = nodeCoordinates.get(node.left);
                if (leftPoint != null) {
                    drawBranchLineAndLabel(g2, parentPoint, leftPoint, "0", true);
                    drawBranches(g2, node.left);
                }
            }

            // Draw branch to right child (Bit '1')
            if (node.right != null) {
                Point rightPoint = nodeCoordinates.get(node.right);
                if (rightPoint != null) {
                    drawBranchLineAndLabel(g2, parentPoint, rightPoint, "1", false);
                    drawBranches(g2, node.right);
                }
            }
        }

        private void drawBranchLineAndLabel(Graphics2D g2, Point parent, Point child, String label, boolean isLeft) {
            double dx = child.x - parent.x;
            double dy = child.y - parent.y;
            double dist = Math.hypot(dx, dy);

            if (dist <= 0) return;

            // Calculate perimeter start and end points outside parent and child circles
            int startX = (int) Math.round(parent.x + dx * (NODE_RADIUS / dist));
            int startY = (int) Math.round(parent.y + dy * (NODE_RADIUS / dist));
            int endX = (int) Math.round(child.x - dx * (NODE_RADIUS / dist));
            int endY = (int) Math.round(child.y - dy * (NODE_RADIUS / dist));

            // Draw connecting branch line
            g2.setColor(new Color(156, 163, 175));
            g2.drawLine(startX, startY, endX, endY);

            // Calculate midpoint of visible segment
            int midX = (startX + endX) / 2;
            int midY = (startY + endY) / 2;

            // Offset label perpendicularly away from the line so it never touches the line or circles
            int badgeX = isLeft ? (midX - 11) : (midX + 4);
            int badgeY = midY + 4;

            // Badge background for clean contrast
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRoundRect(badgeX - 3, badgeY - 11, 14, 14, 4, 4);

            // Branch label ('0' red, '1' blue)
            g2.setColor(isLeft ? new Color(220, 38, 38) : new Color(37, 99, 235));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(label, badgeX, badgeY);
        }

        private void drawNodes(Graphics2D g2, HuffmanNode node) {
            if (node == null) return;
            Point p = nodeCoordinates.get(node);
            if (p == null) return;

            int nodeX = p.x - NODE_RADIUS;
            int nodeY = p.y - NODE_RADIUS;

            if (node.isLeaf()) {
                // Leaf node: soft green circle
                g2.setColor(new Color(223, 240, 216));
                g2.fillOval(nodeX, nodeY, NODE_DIAMETER, NODE_DIAMETER);
                g2.setColor(new Color(92, 184, 92));
                g2.drawOval(nodeX, nodeY, NODE_DIAMETER, NODE_DIAMETER);

                // Leaf text: character & frequency
                String charDisp = formatLeafChar(node.character);
                String freqDisp = String.valueOf(node.frequency);

                g2.setColor(new Color(30, 80, 30));
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int cx = p.x - (fm.stringWidth(charDisp) / 2);
                int cy = p.y - 2;
                g2.drawString(charDisp, cx, cy);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                FontMetrics fm2 = g2.getFontMetrics();
                int fx = p.x - (fm2.stringWidth(freqDisp) / 2);
                int fy = p.y + 12;
                g2.drawString(freqDisp, fx, fy);

            } else {
                // Internal node: soft blue/grey circle
                g2.setColor(new Color(230, 242, 255));
                g2.fillOval(nodeX, nodeY, NODE_DIAMETER, NODE_DIAMETER);
                g2.setColor(new Color(66, 139, 202));
                g2.drawOval(nodeX, nodeY, NODE_DIAMETER, NODE_DIAMETER);

                // Combined frequency
                String freqDisp = String.valueOf(node.frequency);
                g2.setColor(new Color(40, 70, 110));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int fx = p.x - (fm.stringWidth(freqDisp) / 2);
                int fy = p.y + (fm.getAscent() / 2) - 2;
                g2.drawString(freqDisp, fx, fy);
            }

            drawNodes(g2, node.left);
            drawNodes(g2, node.right);
        }

        private String formatLeafChar(Character c) {
            if (c == null) return "";
            return switch (c) {
                case ' ' -> "␣";
                case '\n' -> "\\n";
                case '\t' -> "\\t";
                case '\r' -> "\\r";
                default -> String.valueOf(c);
            };
        }
    }
}
