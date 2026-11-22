package huffman;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Main application window for the Huffman Compressor graphical interface.
 * Coordinates user actions, asynchronous SwingWorkers, and visual presentation
 * while delegating all algorithm and file processing entirely to {@link HuffmanService}.
 */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {

    private final HuffmanService huffmanService = HuffmanService.INSTANCE;

    // UI Components
    private JTextArea inputTextArea;
    private JTable codeTable;
    private DefaultTableModel codeTableModel;
    private TreePanel treePanel;
    private FrequencyChartPanel chartPanel;
    private JLabel statusLabel;

    // Statistics Components
    private JLabel lblOriginalSize;
    private JLabel lblCompressedSize;
    private JLabel lblHeaderSize;
    private JLabel lblPayloadSize;
    private JLabel lblRatio;
    private JLabel lblSpaceSaved;
    private JLabel lblMode;
    private JLabel lblModeNote;
    private JLabel lblRoundTrip;

    // State
    private byte[] lastCompressedBytes = null;
    private String currentLoadedFileName = "Untitled";

    public MainFrame() {
        super("Huffman Coding Text Compressor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 520));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Top Toolbar
        add(createToolbar(), BorderLayout.NORTH);

        // 2. Center Split Pane (Editor on Left, Visualizations on Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.45);
        splitPane.setLeftComponent(createEditorPanel());
        splitPane.setRightComponent(createTabbedPane());
        add(splitPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(248, 249, 250));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JButton btnOpenText = new JButton("Open Text File (.txt)");
        btnOpenText.setToolTipText("Load a plain text file into the editor");
        btnOpenText.addActionListener(e -> handleOpenTextFile());

        JButton btnCompress = new JButton("Compress");
        btnCompress.setFont(btnCompress.getFont().deriveFont(Font.BOLD));
        btnCompress.setForeground(new Color(13, 110, 253));
        btnCompress.setToolTipText("Compress editor text and update visualization");
        btnCompress.addActionListener(e -> handleCompress());

        JButton btnSaveHuff = new JButton("Save Compressed (.huff)");
        btnSaveHuff.setToolTipText("Export compressed binary data to disk");
        btnSaveHuff.addActionListener(e -> handleSaveHuffFile());

        JButton btnOpenHuff = new JButton("Open Compressed (.huff)");
        btnOpenHuff.setToolTipText("Load and decompress a .huff file from disk");
        btnOpenHuff.addActionListener(e -> handleOpenHuffFile());

        JButton btnDecompress = new JButton("Decompress");
        btnDecompress.setToolTipText("Restore text from the latest in-memory compressed data");
        btnDecompress.addActionListener(e -> handleDecompress());

        JButton btnSaveRestored = new JButton("Save Text");
        btnSaveRestored.setToolTipText("Save current editor text to disk as .txt");
        btnSaveRestored.addActionListener(e -> handleSaveTextFile());

        JButton btnClear = new JButton("Clear");
        btnClear.setToolTipText("Reset text editor and all visualization tabs");
        btnClear.addActionListener(e -> handleClear());

        toolbar.add(btnOpenText);
        toolbar.add(btnCompress);
        toolbar.add(btnSaveHuff);
        toolbar.addSeparator(new Dimension(10, 20));
        toolbar.add(btnOpenHuff);
        toolbar.add(btnDecompress);
        toolbar.add(btnSaveRestored);
        toolbar.addSeparator(new Dimension(10, 20));
        toolbar.add(btnClear);

        return toolbar;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel titleLabel = new JLabel("Text Source (Type, paste, or load text):");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));
        panel.add(titleLabel, BorderLayout.NORTH);

        inputTextArea = new JTextArea();
        inputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(inputTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Tab 1: Code Table
        tabbedPane.addTab("Code Table", createCodeTablePanel());

        // Tab 2: Huffman Tree
        treePanel = new TreePanel();
        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setBorder(null);
        tabbedPane.addTab("Huffman Tree", treeScroll);

        // Tab 3: Frequency Chart
        chartPanel = new FrequencyChartPanel();
        JScrollPane chartScroll = new JScrollPane(chartPanel);
        chartScroll.setBorder(null);
        tabbedPane.addTab("Frequency Chart", chartScroll);

        // Tab 4: Statistics
        tabbedPane.addTab("Statistics", createStatisticsPanel());

        return tabbedPane;
    }

    @SuppressWarnings("serial")
    private JPanel createCodeTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        String[] columns = {"Character", "Frequency", "Binary Code", "Code Length"};
        codeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1 || columnIndex == 3) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        codeTable = new JTable(codeTableModel);
        codeTable.setAutoCreateRowSorter(true);
        codeTable.setRowHeight(22);
        codeTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // Centered alignment for numbers and codes
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        codeTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        codeTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        codeTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(codeTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Title Header
        JLabel header = new JLabel("Compression Performance Summary");
        header.setFont(new Font("SansSerif", Font.BOLD, 15));
        header.setForeground(new Color(33, 37, 41));
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(header, gbc);
        gbc.gridwidth = 1;

        lblOriginalSize = addStatRow(panel, gbc, row++, "Original File Size:", "-");
        lblCompressedSize = addStatRow(panel, gbc, row++, "Compressed Size:", "-");
        lblHeaderSize = addStatRow(panel, gbc, row++, "  ├── Header Size:", "-");
        lblPayloadSize = addStatRow(panel, gbc, row++, "  └── Payload Size:", "-");
        lblRatio = addStatRow(panel, gbc, row++, "Compression Ratio:", "-");
        lblSpaceSaved = addStatRow(panel, gbc, row++, "Space Saved:", "-");
        lblMode = addStatRow(panel, gbc, row++, "Encoding Mode:", "-");

        // Special Note for Stored Mode
        lblModeNote = new JLabel("");
        lblModeNote.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblModeNote.setForeground(new Color(180, 100, 20));
        gbc.gridx = 1;
        gbc.gridy = row++;
        panel.add(lblModeNote, gbc);

        lblRoundTrip = addStatRow(panel, gbc, row++, "Round-Trip Verification:", "-");

        // Vertical spacer
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JLabel addStatRow(JPanel parent, GridBagConstraints gbc, int row, String labelText, String defaultValue) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel lblName = new JLabel(labelText);
        lblName.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblName.setForeground(new Color(73, 80, 87));
        parent.add(lblName, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        JLabel lblValue = new JLabel(defaultValue);
        lblValue.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblValue.setForeground(new Color(33, 37, 41));
        parent.add(lblValue, gbc);

        return lblValue;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        statusPanel.setBackground(new Color(248, 249, 250));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(108, 117, 125));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        return statusPanel;
    }

    // =========================================================================
    // USER ACTIONS & SWINGWORKER INTEGRATIONS
    // =========================================================================

    private void handleOpenTextFile() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Select Plain Text File");
        chooser.setFileFilter(new FileNameExtensionFilter("Plain Text (*.txt)", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            setBusy(true, "Loading " + selectedFile.getName() + "...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return huffmanService.readTextFile(selectedFile.getAbsolutePath());
                }

                @Override
                protected void done() {
                    setBusy(false, "Ready");
                    try {
                        String content = get();
                        inputTextArea.setText(content);
                        currentLoadedFileName = selectedFile.getName();
                        statusLabel.setText("Loaded '" + currentLoadedFileName + "' (" + String.format("%,d", content.length()) + " characters)");
                    } catch (Exception ex) {
                        showError("Failed to open text file: " + getRootMessage(ex));
                    }
                }
            }.execute();
        }
    }

    private void handleCompress() {
        String text = inputTextArea.getText();
        if (text == null || text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please type or open some text in the editor before compressing.",
                    "Input Empty",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        setBusy(true, "Compressing text...");

        new SwingWorker<HuffmanResult, Void>() {
            @Override
            protected HuffmanResult doInBackground() throws Exception {
                return huffmanService.compress(text);
            }

            @Override
            protected void done() {
                setBusy(false, "Ready");
                try {
                    HuffmanResult result = get();
                    lastCompressedBytes = result.getCompressedBytes();
                    updateVisualizations(result);

                    String modeStr = (result.getMode() == FileHandler.MODE_STORED) ? "Stored Mode" : "Huffman Mode";
                    statusLabel.setText("Compression complete (" + modeStr + ") - "
                            + String.format("%,d", result.getOriginalSize()) + " B -> "
                            + String.format("%,d", result.getCompressedSize()) + " B ("
                            + String.format("%.2f%%", result.getSpaceSavedPercentage()) + " saved)");
                } catch (Exception ex) {
                    showError("Compression error: " + getRootMessage(ex));
                }
            }
        }.execute();
    }

    private void handleSaveHuffFile() {
        if (lastCompressedBytes == null || lastCompressedBytes.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No compressed data available to save. Please click 'Compress' first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Save Compressed .huff File");
        String defaultName = currentLoadedFileName.endsWith(".txt")
                ? currentLoadedFileName.substring(0, currentLoadedFileName.length() - 4) + ".huff"
                : currentLoadedFileName + ".huff";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("Huffman Compressed Files (*.huff)", "huff"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = chooser.getSelectedFile();
            setBusy(true, "Saving " + outFile.getName() + "...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    huffmanService.writeHuffFile(outFile.getAbsolutePath(), lastCompressedBytes);
                    return null;
                }

                @Override
                protected void done() {
                    setBusy(false, "Ready");
                    try {
                        get();
                        statusLabel.setText("Saved compressed file to '" + outFile.getName() + "' (" + String.format("%,d", lastCompressedBytes.length) + " bytes)");
                    } catch (Exception ex) {
                        showError("Failed to save .huff file: " + getRootMessage(ex));
                    }
                }
            }.execute();
        }
    }

    private void handleOpenHuffFile() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Select Compressed .huff File");
        chooser.setFileFilter(new FileNameExtensionFilter("Huffman Compressed Files (*.huff)", "huff"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            setBusy(true, "Opening and decompressing " + selectedFile.getName() + "...");

            new SwingWorker<HuffmanResult, Void>() {
                @Override
                protected HuffmanResult doInBackground() throws Exception {
                    byte[] bytes = huffmanService.readHuffFile(selectedFile.getAbsolutePath());
                    return huffmanService.decompressWithResult(bytes);
                }

                @Override
                protected void done() {
                    setBusy(false, "Ready");
                    try {
                        HuffmanResult result = get();
                        lastCompressedBytes = result.getCompressedBytes();
                        inputTextArea.setText(result.getOriginalText());
                        currentLoadedFileName = selectedFile.getName();
                        updateVisualizations(result);

                        String modeStr = (result.getMode() == FileHandler.MODE_STORED) ? "Stored Mode" : "Huffman Mode";
                        statusLabel.setText("Decompressed '" + selectedFile.getName() + "' (" + modeStr + ") - Restored "
                                + String.format("%,d", result.getOriginalText().length()) + " characters");
                    } catch (Exception ex) {
                        showError("Failed to open or decompress .huff file: " + getRootMessage(ex));
                    }
                }
            }.execute();
        }
    }

    private void handleDecompress() {
        if (lastCompressedBytes == null || lastCompressedBytes.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "No compressed data in memory. Please compress text or open a .huff file first.",
                    "No Compressed Data",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        setBusy(true, "Decompressing...");

        new SwingWorker<HuffmanResult, Void>() {
            @Override
            protected HuffmanResult doInBackground() throws Exception {
                return huffmanService.decompressWithResult(lastCompressedBytes);
            }

            @Override
            protected void done() {
                setBusy(false, "Ready");
                try {
                    HuffmanResult result = get();
                    inputTextArea.setText(result.getOriginalText());
                    updateVisualizations(result);
                    statusLabel.setText("Decompression successful - Restored "
                            + String.format("%,d", result.getOriginalText().length()) + " characters");
                } catch (Exception ex) {
                    showError("Decompression error: " + getRootMessage(ex));
                }
            }
        }.execute();
    }

    private void handleSaveTextFile() {
        String content = inputTextArea.getText();
        if (content == null || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Editor is empty; nothing to save.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Save Restored Text File");
        String defaultName = currentLoadedFileName.endsWith(".huff")
                ? currentLoadedFileName.substring(0, currentLoadedFileName.length() - 5) + "_restored.txt"
                : "restored_text.txt";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("Plain Text (*.txt)", "txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = chooser.getSelectedFile();
            setBusy(true, "Saving text file...");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    huffmanService.writeTextFile(outFile.getAbsolutePath(), content);
                    return null;
                }

                @Override
                protected void done() {
                    setBusy(false, "Ready");
                    try {
                        get();
                        statusLabel.setText("Saved text file to '" + outFile.getName() + "'");
                    } catch (Exception ex) {
                        showError("Failed to save text file: " + getRootMessage(ex));
                    }
                }
            }.execute();
        }
    }

    private void handleClear() {
        inputTextArea.setText("");
        codeTableModel.setRowCount(0);
        treePanel.clear();
        chartPanel.clear();
        lastCompressedBytes = null;
        currentLoadedFileName = "Untitled";

        lblOriginalSize.setText("-");
        lblCompressedSize.setText("-");
        lblHeaderSize.setText("-");
        lblPayloadSize.setText("-");
        lblRatio.setText("-");
        lblSpaceSaved.setText("-");
        lblMode.setText("-");
        lblModeNote.setText("");
        lblRoundTrip.setText("-");

        statusLabel.setText("Cleared all editor contents and visualization panels.");
    }

    private void updateVisualizations(HuffmanResult result) {
        // 1. Update Code Table
        codeTableModel.setRowCount(0);
        Map<Character, Integer> freqs = result.getFrequencyMap();
        Map<Character, String> codes = result.getCodeMap();

        List<Map.Entry<Character, String>> sortedCodes = new ArrayList<>(codes.entrySet());
        sortedCodes.sort(Comparator.comparingInt((Map.Entry<Character, String> e) -> e.getValue().length())
                .thenComparing(Map.Entry::getKey));

        for (Map.Entry<Character, String> entry : sortedCodes) {
            char c = entry.getKey();
            String code = entry.getValue();
            int freq = freqs.getOrDefault(c, 0);
            String displayChar = FrequencyCounter.formatChar(c);
            codeTableModel.addRow(new Object[]{displayChar, freq, code, code.length()});
        }

        // 2. Update Tree Visualization
        treePanel.setTree(result.getTreeRoot());

        // 3. Update Frequency Chart
        chartPanel.setFrequencies(freqs);

        // 4. Update Statistics Tab
        lblOriginalSize.setText(String.format("%,d bytes", result.getOriginalSize()));
        lblCompressedSize.setText(String.format("%,d bytes", result.getCompressedSize()));
        lblHeaderSize.setText(String.format("%,d bytes", result.getHeaderSize()));
        lblPayloadSize.setText(String.format("%,d bytes", result.getPayloadSize()));
        lblRatio.setText(String.format("%.4f (%.2f%% of original)", result.getCompressionRatio(), result.getCompressionRatio() * 100.0));

        if (result.getSpaceSavedPercentage() >= 0) {
            lblSpaceSaved.setText(String.format("%.2f%%", result.getSpaceSavedPercentage()));
            lblSpaceSaved.setForeground(new Color(25, 135, 84)); // Green
        } else {
            lblSpaceSaved.setText(String.format("%.2f%% (File expanded due to metadata)", result.getSpaceSavedPercentage()));
            lblSpaceSaved.setForeground(new Color(220, 53, 69)); // Red
        }

        if (result.getMode() == FileHandler.MODE_STORED) {
            lblMode.setText("Flag 0 (Stored Mode - raw UTF-8)");
            lblModeNote.setText("Stored mode used: Huffman would not save space for this input.");
        } else {
            lblMode.setText("Flag 1 (Huffman Mode - tree encoded)");
            lblModeNote.setText("");
        }

        if (result.isRoundTripVerified()) {
            lblRoundTrip.setText("✔ Verified (Exact byte-for-byte match)");
            lblRoundTrip.setForeground(new Color(25, 135, 84));
        } else {
            lblRoundTrip.setText("✖ Mismatch detected");
            lblRoundTrip.setForeground(new Color(220, 53, 69));
        }
    }

    private void setBusy(boolean busy, String status) {
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        statusLabel.setText(status);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private String getRootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
