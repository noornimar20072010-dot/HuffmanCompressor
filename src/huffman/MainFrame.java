package huffman;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Menu Bar
        setJMenuBar(createMenuBar());

        // 2. Top Toolbar
        add(createToolbar(), BorderLayout.NORTH);

        // 3. Center Split Pane (Editor on Left, Visualizations on Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.42);
        splitPane.setLeftComponent(createEditorPanel());
        splitPane.setRightComponent(createTabbedPane());
        add(splitPane, BorderLayout.CENTER);

        // 4. Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem openTextItem = new JMenuItem("Open Text File...");
        openTextItem.setMnemonic('O');
        openTextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openTextItem.addActionListener(e -> handleOpenTextFile());
        fileMenu.add(openTextItem);

        JMenuItem saveHuffItem = new JMenuItem("Save Compressed (.huff)...");
        saveHuffItem.setMnemonic('S');
        saveHuffItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveHuffItem.addActionListener(e -> handleSaveHuffFile());
        fileMenu.add(saveHuffItem);

        fileMenu.addSeparator();

        JMenuItem openHuffItem = new JMenuItem("Open Compressed (.huff)...");
        openHuffItem.addActionListener(e -> handleOpenHuffFile());
        fileMenu.add(openHuffItem);

        JMenuItem saveTextItem = new JMenuItem("Save Restored Text As...");
        saveTextItem.addActionListener(e -> handleSaveTextFile());
        fileMenu.add(saveTextItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Actions Menu
        JMenu actionMenu = new JMenu("Actions");
        actionMenu.setMnemonic('A');

        JMenuItem compressItem = new JMenuItem("Compress Editor Text");
        compressItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
        compressItem.addActionListener(e -> handleCompress());
        actionMenu.add(compressItem);

        JMenuItem decompressItem = new JMenuItem("Decompress In-Memory Data");
        decompressItem.addActionListener(e -> handleDecompress());
        actionMenu.add(decompressItem);

        actionMenu.addSeparator();

        JMenuItem clearItem = new JMenuItem("Clear All");
        clearItem.addActionListener(e -> handleClear());
        actionMenu.add(clearItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("About Huffman Compressor");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(actionMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(248, 249, 250));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JButton btnOpenText = createToolbarButton("Open Text File (.txt)", "Load a plain text file from disk into the editor", false, false);
        btnOpenText.addActionListener(e -> handleOpenTextFile());

        JButton btnCompress = createToolbarButton("Compress", "Compress editor text using adaptive Huffman coding", true, false);
        btnCompress.addActionListener(e -> handleCompress());

        JButton btnSaveHuff = createToolbarButton("Save Compressed (.huff)", "Save compressed binary .huff file to disk", false, false);
        btnSaveHuff.addActionListener(e -> handleSaveHuffFile());

        JButton btnOpenHuff = createToolbarButton("Open Compressed (.huff)", "Open and decompress a binary .huff file from disk", false, false);
        btnOpenHuff.addActionListener(e -> handleOpenHuffFile());

        JButton btnDecompress = createToolbarButton("Decompress", "Decompress current in-memory binary payload back to text", false, true);
        btnDecompress.addActionListener(e -> handleDecompress());

        JButton btnSaveRestored = createToolbarButton("Save Text", "Save current editor text to disk as a .txt file", false, false);
        btnSaveRestored.addActionListener(e -> handleSaveTextFile());

        JButton btnClear = createToolbarButton("Clear", "Reset editor contents, table, tree, chart, and statistics", false, false);
        btnClear.addActionListener(e -> handleClear());

        JButton btnAbout = createToolbarButton("About", "View information about this Huffman Compressor project", false, false);
        btnAbout.addActionListener(e -> showAboutDialog());

        toolbar.add(btnOpenText);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(btnCompress);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(btnSaveHuff);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.addSeparator(new Dimension(2, 24));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnOpenHuff);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(btnDecompress);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(btnSaveRestored);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.addSeparator(new Dimension(2, 24));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnClear);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btnAbout);

        return toolbar;
    }

    private JButton createToolbarButton(String text, String tooltip, boolean isPrimary, boolean isSecondary) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", (isPrimary || isSecondary) ? Font.BOLD : Font.PLAIN, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color bgNormal;
        Color bgHover;
        Color fgNormal;
        Color borderColor;

        if (isPrimary) {
            // Compress: Prominent Blue
            bgNormal = new Color(13, 110, 253);
            bgHover = new Color(11, 94, 215);
            fgNormal = Color.WHITE;
            borderColor = new Color(10, 88, 202);
        } else if (isSecondary) {
            // Decompress: Prominent Green / Teal
            bgNormal = new Color(25, 135, 84);
            bgHover = new Color(21, 115, 71);
            fgNormal = Color.WHITE;
            borderColor = new Color(20, 108, 67);
        } else {
            // Standard action button
            bgNormal = new Color(248, 249, 250);
            bgHover = new Color(233, 236, 239);
            fgNormal = new Color(33, 37, 41);
            borderColor = new Color(206, 212, 218);
        }

        btn.setBackground(bgNormal);
        btn.setForeground(fgNormal);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bgHover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgNormal);
            }
        });

        return btn;
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
        codeTable.setRowHeight(26);
        codeTable.setShowGrid(true);
        codeTable.setGridColor(new Color(230, 233, 236));
        codeTable.setSelectionBackground(new Color(224, 231, 255));
        codeTable.setSelectionForeground(new Color(30, 41, 59));
        codeTable.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Header Style
        JTableHeader header = codeTable.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(new Color(30, 41, 59));
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setReorderingAllowed(false);

        // Custom Zebra Renderer with padding and alignments
        class ZebraRenderer extends DefaultTableCellRenderer {
            ZebraRenderer(int alignment) {
                setHorizontalAlignment(alignment);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                if (!isSelected) {
                    if (row % 2 == 0) {
                        comp.setBackground(Color.WHITE);
                    } else {
                        comp.setBackground(new Color(248, 250, 252));
                    }
                    comp.setForeground(new Color(30, 41, 59));
                }
                return comp;
            }
        }

        codeTable.getColumnModel().getColumn(0).setCellRenderer(new ZebraRenderer(SwingConstants.LEFT));
        codeTable.getColumnModel().getColumn(1).setCellRenderer(new ZebraRenderer(SwingConstants.CENTER));
        codeTable.getColumnModel().getColumn(2).setCellRenderer(new ZebraRenderer(SwingConstants.CENTER));
        codeTable.getColumnModel().getColumn(3).setCellRenderer(new ZebraRenderer(SwingConstants.CENTER));

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
        lblModeNote.setForeground(new Color(217, 119, 6)); // Warm amber
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

    private String formatBytes(long count) {
        if (count == 1 || count == -1) {
            return String.format("%,d byte", count);
        }
        return String.format("%,d bytes", count);
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
                            + formatBytes(result.getOriginalSize()) + " -> "
                            + formatBytes(result.getCompressedSize()) + " ("
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
                        statusLabel.setText("Saved compressed file to '" + outFile.getName() + "' (" + formatBytes(lastCompressedBytes.length) + ")");
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

    private void showAboutDialog() {
        String message = """
                Huffman Coding Text Compressor
                College Semester 3 Java Project (Java 17)
                
                • Dual Interfaces: Java Swing GUI & CLI Menu
                • Zero External Libraries / Zero Build Tools (Plain javac)
                • Adaptive Dual-Mode Encoding (Stored Mode & Huffman Mode)
                • Unsigned 7-Bit Varint Frequency Table Compression
                • Dynamic Zoomable Tree Visualizer & Frequency Chart
                • Full Automated 29-Test Validation Suite
                """;
        JOptionPane.showMessageDialog(this, message, "About Huffman Compressor", JOptionPane.INFORMATION_MESSAGE);
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
        lblOriginalSize.setText(formatBytes(result.getOriginalSize()));
        lblCompressedSize.setText(formatBytes(result.getCompressedSize()));
        lblHeaderSize.setText(formatBytes(result.getHeaderSize()));
        lblPayloadSize.setText(formatBytes(result.getPayloadSize()));
        lblRatio.setText(String.format("%.4f (%.2f%% of original)", result.getCompressionRatio(), result.getCompressionRatio() * 100.0));

        if (result.getMode() == FileHandler.MODE_STORED) {
            lblMode.setText("Flag 0 (Stored Mode - raw UTF-8)");
            lblMode.setForeground(new Color(217, 119, 6)); // Warm amber
            lblModeNote.setText("Stored mode: raw text plus a 1-byte mode flag. Huffman would not save space for this input.");
            lblSpaceSaved.setText("0.00% (Stored mode: raw text plus a 1-byte mode flag. Huffman would not save space for this input.)");
            lblSpaceSaved.setForeground(new Color(217, 119, 6)); // Warm amber
        } else {
            lblMode.setText("Flag 1 (Huffman Mode - tree encoded)");
            lblMode.setForeground(new Color(13, 110, 253)); // Blue
            lblModeNote.setText("");
            if (result.getSpaceSavedPercentage() >= 0) {
                lblSpaceSaved.setText(String.format("%.2f%%", result.getSpaceSavedPercentage()));
                lblSpaceSaved.setForeground(new Color(25, 135, 84)); // Green
            } else {
                lblSpaceSaved.setText(String.format("%.2f%% (File expanded due to metadata)", result.getSpaceSavedPercentage()));
                lblSpaceSaved.setForeground(new Color(220, 53, 69)); // Red
            }
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
