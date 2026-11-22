package huffman;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Custom JPanel that renders a clean, student-friendly horizontal bar chart
 * of the top 20 character frequencies in the processed text.
 */
@SuppressWarnings("serial")
public final class FrequencyChartPanel extends JPanel {

    private static final int MAX_BARS = 20;
    private static final int ROW_HEIGHT = 28;
    private static final int TOP_PADDING = 30;
    private static final int BOTTOM_PADDING = 30;
    private static final int LEFT_LABEL_WIDTH = 130;
    private static final int RIGHT_VALUE_WIDTH = 110;

    private List<Map.Entry<Character, Integer>> topEntries = new ArrayList<>();
    private int maxFrequency = 0;
    private long totalCharacters = 0;

    public FrequencyChartPanel() {
        setBackground(Color.WHITE);
        setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    /**
     * Updates the chart with a new frequency map and schedules a repaint.
     *
     * @param frequencyMap mapping of characters to occurrence counts
     */
    public void setFrequencies(Map<Character, Integer> frequencyMap) {
        topEntries.clear();
        maxFrequency = 0;
        totalCharacters = 0;

        if (frequencyMap != null && !frequencyMap.isEmpty()) {
            List<Map.Entry<Character, Integer>> sorted = new ArrayList<>(frequencyMap.entrySet());
            sorted.sort(Comparator.comparingInt(Map.Entry<Character, Integer>::getValue).reversed());

            int count = Math.min(MAX_BARS, sorted.size());
            for (int i = 0; i < count; i++) {
                topEntries.add(sorted.get(i));
            }

            for (Map.Entry<Character, Integer> e : sorted) {
                totalCharacters += e.getValue();
            }

            if (!topEntries.isEmpty()) {
                maxFrequency = topEntries.get(0).getValue();
            }
        }

        int desiredHeight = Math.max(300, TOP_PADDING + (topEntries.size() * ROW_HEIGHT) + BOTTOM_PADDING);
        setPreferredSize(new Dimension(500, desiredHeight));
        revalidate();
        repaint();
    }

    /**
     * Clears all frequency data and resets the chart.
     */
    public void clear() {
        setFrequencies(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (topEntries.isEmpty() || maxFrequency <= 0) {
            g2.setColor(new Color(120, 120, 120));
            g2.setFont(new Font("SansSerif", Font.ITALIC, 14));
            String msg = "No frequency data available. Type or load text to view character frequencies.";
            FontMetrics fm = g2.getFontMetrics();
            int msgX = (width - fm.stringWidth(msg)) / 2;
            int msgY = height / 2;
            g2.drawString(msg, Math.max(20, msgX), msgY);
            g2.dispose();
            return;
        }

        // Draw title
        g2.setColor(new Color(33, 37, 41));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String title = "Top " + topEntries.size() + " Character Frequencies (Total: " + String.format("%,d", totalCharacters) + " chars)";
        g2.drawString(title, 20, 20);

        int availableBarWidth = width - LEFT_LABEL_WIDTH - RIGHT_VALUE_WIDTH - 40;
        if (availableBarWidth < 50) {
            availableBarWidth = 50;
        }

        int currentY = TOP_PADDING;
        FontMetrics fm = g2.getFontMetrics(getFont());

        Color barColor = new Color(51, 122, 183);
        Color barBorderColor = new Color(40, 96, 144);

        for (Map.Entry<Character, Integer> entry : topEntries) {
            char c = entry.getKey();
            int freq = entry.getValue();

            // 1. Draw character label
            String charLabel = FrequencyCounter.formatChar(c);
            g2.setColor(new Color(50, 50, 50));
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            int textY = currentY + 16;
            g2.drawString(charLabel, 20, textY);

            // 2. Draw frequency bar
            int barWidth = (int) (((double) freq / (double) maxFrequency) * availableBarWidth);
            barWidth = Math.max(3, barWidth);
            int barX = LEFT_LABEL_WIDTH;
            int barY = currentY + 3;
            int barH = ROW_HEIGHT - 8;

            g2.setColor(barColor);
            g2.fillRoundRect(barX, barY, barWidth, barH, 4, 4);
            g2.setColor(barBorderColor);
            g2.drawRoundRect(barX, barY, barWidth, barH, 4, 4);

            // 3. Draw frequency value and percentage
            double percentage = (totalCharacters > 0) ? ((double) freq / totalCharacters) * 100.0 : 0.0;
            String valStr = String.format("%,d (%.1f%%)", freq, percentage);
            g2.setColor(new Color(70, 70, 70));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(valStr, barX + barWidth + 8, textY);

            currentY += ROW_HEIGHT;
        }

        g2.dispose();
    }
}
