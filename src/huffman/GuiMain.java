package huffman;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for launching the Huffman Compressor Graphical User Interface (Java Swing).
 * Configures the native system look-and-feel and initializes {@link MainFrame} on the
 * Swing Event Dispatch Thread (EDT).
 */
public class GuiMain {

    /**
     * Application entry point for the GUI.
     *
     * @param args optional command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Graceful fallback to default Swing look and feel
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
