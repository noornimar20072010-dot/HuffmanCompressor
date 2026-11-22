package huffman;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * High-level service facade providing thread-safe, in-memory Huffman compression,
 * decompression, structural analysis, and file operations.
 *
 * Reuses the single-source-of-truth serialization logic from {@link FileHandler}
 * to ensure 100% byte-for-byte binary compatibility with the .huff file specification.
 */
public class HuffmanService {

    /** Default singleton instance for convenience. */
    public static final HuffmanService INSTANCE = new HuffmanService();

    /**
     * Default constructor. The service is stateless and thread-safe.
     */
    public HuffmanService() {}

    /**
     * Analyzes the character frequencies, constructs the Huffman tree, and derives
     * binary prefix codes for the given text without writing to disk.
     *
     * @param text the input string to analyze
     * @return a {@link HuffmanResult} containing frequencies, codes, and tree root
     */
    public HuffmanResult analyze(String text) {
        String safeText = (text != null) ? text : "";
        byte[] rawUtf8Bytes = safeText.getBytes(StandardCharsets.UTF_8);
        long originalSize = rawUtf8Bytes.length;

        Map<Character, Integer> frequencies = FrequencyCounter.countFrequencies(safeText);
        HuffmanTree tree = new HuffmanTree(frequencies);
        Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);

        if (safeText.isEmpty()) {
            return new HuffmanResult(
                    safeText, frequencies, codeMap, tree.getRoot(),
                    new byte[0], FileHandler.MODE_STORED,
                    0, 1, 1, 0,
                    0.0, 0.0, false
            );
        }

        // Calculate expected payload size without full serialization
        long totalBits = 0;
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
            String code = codeMap.get(entry.getKey());
            if (code != null) {
                totalBits += (long) entry.getValue() * code.length();
            }
        }
        int payloadBytes = (int) ((totalBits + 7) / 8);

        byte prospectiveMode = FileHandler.determineAutoMode(rawUtf8Bytes, frequencies, payloadBytes);
        long prospectiveCompressedSize = (prospectiveMode == FileHandler.MODE_HUFFMAN)
                ? FileHandler.calculateHuffmanFileSize(frequencies, payloadBytes)
                : (originalSize + 1);

        long headerSize = (prospectiveMode == FileHandler.MODE_HUFFMAN)
                ? (prospectiveCompressedSize - payloadBytes)
                : 1;

        long actualPayloadSize = (prospectiveMode == FileHandler.MODE_HUFFMAN)
                ? payloadBytes
                : originalSize;

        CompressionStats stats = new CompressionStats(
                prospectiveMode,
                originalSize,
                prospectiveCompressedSize,
                headerSize,
                actualPayloadSize
        );

        return new HuffmanResult(
                safeText,
                frequencies,
                codeMap,
                tree.getRoot(),
                new byte[0],
                prospectiveMode,
                originalSize,
                prospectiveCompressedSize,
                headerSize,
                actualPayloadSize,
                stats.getCompressionRatio(),
                stats.getSpaceSavedPercentage(),
                false
        );
    }

    /**
     * Executes the complete compression pipeline in-memory:
     * 1. Analyzes frequencies and builds tree and prefix codes.
     * 2. Packs bits into bytes.
     * 3. Selects optimal mode (Stored mode Flag 0 vs Huffman mode Flag 1).
     * 4. Serializes complete .huff file bytes.
     * 5. Decompresses the result in-memory to verify round-trip integrity.
     *
     * @param text input source text
     * @return a {@link HuffmanResult} with packed bytes, metrics, and round-trip verification
     * @throws HuffmanException if compression or serialization fails
     */
    public HuffmanResult compress(String text) throws HuffmanException {
        String safeText = (text != null) ? text : "";
        byte[] rawUtf8Bytes = safeText.getBytes(StandardCharsets.UTF_8);
        long originalBytes = rawUtf8Bytes.length;

        Map<Character, Integer> frequencies = FrequencyCounter.countFrequencies(safeText);
        HuffmanTree tree = new HuffmanTree(frequencies);
        Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
        Compressor.CompressionResult packed = Compressor.compress(safeText, codeMap);

        byte selectedMode = FileHandler.determineAutoMode(rawUtf8Bytes, frequencies, packed.compressedBytes().length);
        byte[] huffBytes;
        try {
            huffBytes = FileHandler.serializeCompressedData(
                    selectedMode,
                    rawUtf8Bytes,
                    frequencies,
                    packed.paddingBits(),
                    packed.compressedBytes()
            );
        } catch (IOException e) {
            throw new HuffmanException("Failed to serialize compressed .huff data: " + e.getMessage(), e);
        }

        long compressedBytesLength = huffBytes.length;
        long payloadBytes = (selectedMode == FileHandler.MODE_STORED)
                ? rawUtf8Bytes.length
                : packed.compressedBytes().length;
        long headerBytes = compressedBytesLength - payloadBytes;

        CompressionStats stats = new CompressionStats(
                selectedMode,
                originalBytes,
                compressedBytesLength,
                headerBytes,
                payloadBytes
        );

        // Round-trip verification: decompress and compare with original text
        boolean roundTripVerified;
        try {
            String restored = decompress(huffBytes);
            roundTripVerified = safeText.equals(restored);
        } catch (Exception e) {
            roundTripVerified = false;
        }

        return new HuffmanResult(
                safeText,
                frequencies,
                codeMap,
                tree.getRoot(),
                huffBytes,
                selectedMode,
                originalBytes,
                compressedBytesLength,
                headerBytes,
                payloadBytes,
                stats.getCompressionRatio(),
                stats.getSpaceSavedPercentage(),
                roundTripVerified
        );
    }

    /**
     * Decompresses serialized .huff binary bytes in-memory back to the original UTF-8 string.
     *
     * @param huffBytes serialized .huff binary bytes
     * @return restored plain text string
     * @throws HuffmanException if bytes are corrupted, invalid, or empty
     */
    public String decompress(byte[] huffBytes) throws HuffmanException {
        if (huffBytes == null || huffBytes.length == 0) {
            throw new HuffmanException("Cannot decompress: input byte buffer is null or 0 bytes.");
        }
        try {
            FileHandler.CompressedFileData fileData = FileHandler.readCompressedBytes(huffBytes);
            return Decompressor.decompressOrThrow(fileData);
        } catch (IOException e) {
            throw new HuffmanException("I/O error during in-memory decompression: " + e.getMessage(), e);
        }
    }

    /**
     * Decompresses serialized .huff binary bytes and reconstructs the full {@link HuffmanResult}
     * model (including rebuilt tree, frequency table, and code map for Huffman mode).
     *
     * @param huffBytes serialized .huff binary bytes
     * @return {@link HuffmanResult} containing restored text, tree, frequencies, codes, and metrics
     * @throws HuffmanException if decompression fails or data is corrupted
     */
    public HuffmanResult decompressWithResult(byte[] huffBytes) throws HuffmanException {
        if (huffBytes == null || huffBytes.length == 0) {
            throw new HuffmanException("Cannot decompress: input byte buffer is null or 0 bytes.");
        }
        try {
            FileHandler.CompressedFileData fileData = FileHandler.readCompressedBytes(huffBytes);
            String restoredText = Decompressor.decompressOrThrow(fileData);
            byte mode = fileData.mode();
            byte[] rawUtf8Bytes = restoredText.getBytes(StandardCharsets.UTF_8);
            long originalSize = rawUtf8Bytes.length;
            long compressedSize = huffBytes.length;

            Map<Character, Integer> freqs;
            HuffmanTree tree;
            Map<Character, String> codeMap;
            long headerSize;
            long payloadSize;

            if (mode == FileHandler.MODE_HUFFMAN) {
                freqs = fileData.frequencies();
                tree = new HuffmanTree(freqs);
                codeMap = CodeGenerator.generateCodes(tree);
                payloadSize = (fileData.compressedBytes() != null) ? fileData.compressedBytes().length : 0;
                headerSize = compressedSize - payloadSize;
            } else {
                // Stored mode: analyze restored text to populate visualization tabs
                freqs = FrequencyCounter.countFrequencies(restoredText);
                tree = new HuffmanTree(freqs);
                codeMap = CodeGenerator.generateCodes(tree);
                payloadSize = (fileData.rawBytes() != null) ? fileData.rawBytes().length : 0;
                headerSize = 1;
            }

            CompressionStats stats = new CompressionStats(
                    mode,
                    originalSize,
                    compressedSize,
                    headerSize,
                    payloadSize
            );

            return new HuffmanResult(
                    restoredText,
                    freqs,
                    codeMap,
                    tree.getRoot(),
                    huffBytes,
                    mode,
                    originalSize,
                    compressedSize,
                    headerSize,
                    payloadSize,
                    stats.getCompressionRatio(),
                    stats.getSpaceSavedPercentage(),
                    true
            );
        } catch (IOException e) {
            throw new HuffmanException("I/O error during in-memory decompression: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FILE HELPER METHODS (UTF-8 Plain Text & Binary .huff)
    // =========================================================================

    /**
     * Reads a UTF-8 encoded text file from disk.
     *
     * @param path file path string
     * @return file contents as a String
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if file path is invalid or missing
     */
    public String readTextFile(String path) throws IOException, HuffmanException {
        return FileHandler.readTextFileOrThrow(path);
    }

    /**
     * Reads a UTF-8 encoded text file from disk.
     *
     * @param path {@link Path} object
     * @return file contents as a String
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if file path is invalid or missing
     */
    public String readTextFile(Path path) throws IOException, HuffmanException {
        if (path == null) {
            throw new HuffmanException("File path cannot be null.");
        }
        return readTextFile(path.toString());
    }

    /**
     * Writes plain text to disk encoded as UTF-8.
     *
     * @param path    destination path
     * @param content text content to write
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if path is empty or invalid
     */
    public void writeTextFile(String path, String content) throws IOException, HuffmanException {
        FileHandler.writeTextFileOrThrow(path, content);
    }

    /**
     * Writes plain text to disk encoded as UTF-8.
     *
     * @param path    destination {@link Path}
     * @param content text content to write
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if path is null
     */
    public void writeTextFile(Path path, String content) throws IOException, HuffmanException {
        if (path == null) {
            throw new HuffmanException("Destination path cannot be null.");
        }
        writeTextFile(path.toString(), content);
    }

    /**
     * Reads complete binary .huff file bytes from disk.
     *
     * @param path path to .huff file
     * @return raw binary bytes of the file
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if file is missing or invalid
     */
    public byte[] readHuffFile(String path) throws IOException, HuffmanException {
        if (path == null || path.isBlank()) {
            throw new HuffmanException("Path is empty or blank.");
        }
        Path p = Path.of(path);
        if (!Files.exists(p)) {
            throw new HuffmanException("File does not exist at '" + path + "'.");
        }
        if (Files.isDirectory(p)) {
            throw new HuffmanException("Path is a directory, not a file at '" + path + "'.");
        }
        return Files.readAllBytes(p);
    }

    /**
     * Writes .huff binary bytes to disk.
     *
     * @param path      destination path
     * @param huffBytes binary bytes to write
     * @throws IOException      if disk I/O fails
     * @throws HuffmanException if path is invalid
     */
    public void writeHuffFile(String path, byte[] huffBytes) throws IOException, HuffmanException {
        if (path == null || path.isBlank()) {
            throw new HuffmanException("Destination path is empty.");
        }
        try (FileOutputStream fos = new FileOutputStream(path);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            if (huffBytes != null && huffBytes.length > 0) {
                bos.write(huffBytes);
            }
            bos.flush();
        }
    }

    /**
     * Compresses a text file on disk into a .huff file.
     *
     * @param inputPath  source text file path
     * @param outputPath destination .huff file path
     * @return resulting {@link HuffmanResult}
     * @throws IOException      if file reading or writing fails
     * @throws HuffmanException if input data is invalid
     */
    public HuffmanResult compressFile(String inputPath, String outputPath) throws IOException, HuffmanException {
        String text = readTextFile(inputPath);
        HuffmanResult result = compress(text);
        writeHuffFile(outputPath, result.getCompressedBytes());
        return result;
    }

    /**
     * Decompresses a .huff file on disk into a restored UTF-8 plain text file.
     *
     * @param inputPath  source .huff file path
     * @param outputPath destination text file path
     * @return restored text string
     * @throws IOException      if file reading or writing fails
     * @throws HuffmanException if .huff file is corrupted or invalid
     */
    public String decompressFile(String inputPath, String outputPath) throws IOException, HuffmanException {
        FileHandler.CompressedFileData fileData = FileHandler.readCompressedFileOrThrow(inputPath);
        String restoredText = Decompressor.decompressOrThrow(fileData);
        writeTextFile(outputPath, restoredText);
        return restoredText;
    }
}
