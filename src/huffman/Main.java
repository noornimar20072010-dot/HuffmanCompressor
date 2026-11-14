package huffman;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive command-line application for the Huffman Compressor.
 * Provides an intuitive menu to compress, decompress, inspect frequency/code tables,
 * and view compression statistics with dual-mode (Huffman vs. Stored) support.
 * Robustly validates all inputs and paths without crashing.
 */
public class Main {

    private static CompressionStats lastStats = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=================================================");
        System.out.println("            Huffman Compressor (Java 17)         ");
        System.out.println("=================================================");

        while (running) {
            printMenu();
            System.out.print("Select an option (1-5): ");

            if (!scanner.hasNextLine()) {
                break; // EOF encountered (e.g., piped input finished)
            }

            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> handleCompress(scanner);
                case "2" -> handleDecompress(scanner);
                case "3" -> handleShowFrequenciesAndCodes(scanner);
                case "4" -> handleShowLastStats();
                case "5" -> {
                    System.out.println("Thank you for using Huffman Compressor. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please enter a number between 1 and 5.\n");
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n----------------- MAIN MENU -----------------");
        System.out.println("1) Compress a file");
        System.out.println("2) Decompress a file");
        System.out.println("3) Show frequency table and codes for a file");
        System.out.println("4) Show compression stats for the last compression");
        System.out.println("5) Exit");
        System.out.println("---------------------------------------------");
    }

    /**
     * Handles compression of a text file into a .huff binary file with auto-fallback to Stored mode.
     */
    private static void handleCompress(Scanner scanner) {
        System.out.print("Enter path of text file to compress (e.g. input.txt): ");
        if (!scanner.hasNextLine()) return;
        String inputPath = scanner.nextLine().trim();

        if (inputPath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }

        Path inPath = Path.of(inputPath);
        if (!Files.exists(inPath)) {
            System.out.println("Error: File does not exist at '" + inputPath + "'.");
            return;
        }
        if (Files.isDirectory(inPath)) {
            System.out.println("Error: Path is a directory, not a file ('" + inputPath + "').");
            return;
        }

        String content = FileHandler.readTextFile(inputPath);
        if (content == null) {
            System.out.println("Compression aborted: Could not read source file.");
            return;
        }

        String defaultOut = inputPath.endsWith(".txt")
                ? inputPath.substring(0, inputPath.length() - 4) + ".huff"
                : inputPath + ".huff";

        System.out.print("Enter destination path for .huff file [Press Enter for '" + defaultOut + "']: ");
        if (!scanner.hasNextLine()) return;
        String outputPath = scanner.nextLine().trim();
        if (outputPath.isEmpty()) {
            outputPath = defaultOut;
        }

        System.out.println("\nAnalyzing and compressing '" + inputPath + "'...");
        byte[] rawUtf8Bytes = content.getBytes(StandardCharsets.UTF_8);
        Map<Character, Integer> frequencies = FrequencyCounter.countFrequencies(content);
        HuffmanTree tree = new HuffmanTree(frequencies);
        Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
        Compressor.CompressionResult result = Compressor.compress(content, codeMap);

        byte selectedMode = FileHandler.writeAutoCompressedFile(
                outputPath,
                rawUtf8Bytes,
                frequencies,
                result.paddingBits(),
                result.compressedBytes()
        );

        if (selectedMode != -1) {
            long originalBytes = FileHandler.getFileSize(inputPath);
            long compressedBytes = FileHandler.getFileSize(outputPath);
            long payloadBytes = (selectedMode == FileHandler.MODE_STORED) ? rawUtf8Bytes.length : result.compressedBytes().length;
            long headerBytes = compressedBytes - payloadBytes;

            lastStats = new CompressionStats(selectedMode, originalBytes, compressedBytes, headerBytes, payloadBytes);

            if (selectedMode == FileHandler.MODE_STORED) {
                System.out.println("Stored uncompressed (no savings possible)");
            } else {
                System.out.println("Successfully compressed with Huffman encoding to '" + outputPath + "'!");
            }
            System.out.println();
            System.out.println(lastStats.getFormattedSummary());
        } else {
            System.out.println("Compression failed: Could not write output file to '" + outputPath + "'.");
        }
    }

    /**
     * Handles decompression of a .huff file back into a plain text file.
     */
    private static void handleDecompress(Scanner scanner) {
        System.out.print("Enter path of .huff file to decompress (e.g. input.huff): ");
        if (!scanner.hasNextLine()) return;
        String huffPath = scanner.nextLine().trim();

        if (huffPath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }

        Path p = Path.of(huffPath);
        if (!Files.exists(p)) {
            System.out.println("Error: File does not exist at '" + huffPath + "'.");
            return;
        }
        if (Files.isDirectory(p)) {
            System.out.println("Error: Path is a directory, not a file ('" + huffPath + "').");
            return;
        }

        FileHandler.CompressedFileData fileData = FileHandler.readCompressedFile(huffPath);
        if (fileData == null) {
            System.out.println("Decompression aborted: The compressed file could not be parsed.");
            return;
        }

        String defaultOut = huffPath.endsWith(".huff")
                ? huffPath.substring(0, huffPath.length() - 5) + "_restored.txt"
                : huffPath + "_restored.txt";

        System.out.print("Enter destination path for restored text file [Press Enter for '" + defaultOut + "']: ");
        if (!scanner.hasNextLine()) return;
        String outputPath = scanner.nextLine().trim();
        if (outputPath.isEmpty()) {
            outputPath = defaultOut;
        }

        if (fileData.mode() == FileHandler.MODE_STORED) {
            System.out.println("\nDetected Stored Mode (Flag 0). Restoring uncompressed UTF-8 bytes...");
        } else {
            System.out.println("\nDetected Huffman Mode (Flag 1). Rebuilding Huffman tree from varint header...");
        }

        String restoredText = Decompressor.decompress(fileData);
        if (restoredText == null) {
            System.out.println("Decompression failed: Corrupted file data encountered.");
            return;
        }

        boolean writeSuccess = FileHandler.writeTextFile(outputPath, restoredText);
        if (writeSuccess) {
            System.out.println("Successfully restored text (" + restoredText.length() + " characters) to '" + outputPath + "'!");
        } else {
            System.out.println("Error: Failed to write restored text to '" + outputPath + "'.");
        }
    }

    /**
     * Reads a text file and prints its character frequency and Huffman code tables.
     */
    private static void handleShowFrequenciesAndCodes(Scanner scanner) {
        System.out.print("Enter path of text file to inspect (e.g. input.txt): ");
        if (!scanner.hasNextLine()) return;
        String path = scanner.nextLine().trim();

        if (path.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }

        Path p = Path.of(path);
        if (!Files.exists(p)) {
            System.out.println("Error: File does not exist at '" + path + "'.");
            return;
        }
        if (Files.isDirectory(p)) {
            System.out.println("Error: Path is a directory, not a file ('" + path + "').");
            return;
        }

        String content = FileHandler.readTextFile(path);
        if (content == null) {
            System.out.println("Could not read file at '" + path + "'.");
            return;
        }

        Map<Character, Integer> frequencies = FrequencyCounter.countFrequencies(content);
        if (frequencies.isEmpty()) {
            System.out.println("The file is empty. Frequency table and codes are not applicable.");
            return;
        }

        System.out.println("\n--- Frequency Table for '" + path + "' (" + content.length() + " chars) ---");
        List<Map.Entry<Character, Integer>> freqList = new ArrayList<>(frequencies.entrySet());
        freqList.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<Character, Integer> entry : freqList) {
            System.out.printf("%-18s : %d%n", FrequencyCounter.formatChar(entry.getKey()), entry.getValue());
        }
        System.out.println("Total unique characters: " + frequencies.size());

        HuffmanTree tree = new HuffmanTree(frequencies);
        Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);

        System.out.println("\n--- Huffman Prefix Codes (Sorted by Code Length) ---");
        List<Map.Entry<Character, String>> codeList = new ArrayList<>(codeMap.entrySet());
        codeList.sort(Comparator.comparingInt((Map.Entry<Character, String> e) -> e.getValue().length())
                                .thenComparing(Map.Entry::getKey));

        System.out.printf("%-18s | %-6s | %s%n", "Character", "Length", "Binary Code");
        System.out.println("--------------------------------------------------");
        for (Map.Entry<Character, String> entry : codeList) {
            System.out.printf("%-18s | %-6d | %s%n",
                    FrequencyCounter.formatChar(entry.getKey()),
                    entry.getValue().length(),
                    entry.getValue());
        }
    }

    /**
     * Displays statistics from the most recent compression run.
     */
    private static void handleShowLastStats() {
        if (lastStats == null) {
            System.out.println("No compression has been executed yet in this session.");
            System.out.println("Please run Option 1 (Compress a file) first.");
        } else {
            System.out.println(lastStats.getFormattedSummary());
        }
    }
}
