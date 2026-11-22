package huffman;

import java.util.Collections;
import java.util.Map;

/**
 * Plain immutable data class holding the comprehensive results of analyzing,
 * compressing, and verifying text using Huffman coding.
 */
public class HuffmanResult {

    private final String originalText;
    private final Map<Character, Integer> frequencyMap;
    private final Map<Character, String> codeMap;
    private final HuffmanNode treeRoot;
    private final byte[] compressedBytes;
    private final byte mode;
    private final long originalSize;
    private final long compressedSize;
    private final long headerSize;
    private final long payloadSize;
    private final double compressionRatio;
    private final double spaceSavedPercentage;
    private final boolean roundTripVerified;

    /**
     * Constructs a complete HuffmanResult instance.
     *
     * @param originalText         the source input text
     * @param frequencyMap         character occurrence count mapping
     * @param codeMap              character to binary prefix code mapping
     * @param treeRoot             the root node of the constructed Huffman tree
     * @param compressedBytes      packed binary file bytes (mode flag + header + payload)
     * @param mode                 0 for Stored mode, 1 for Huffman mode
     * @param originalSize         size of the raw UTF-8 input in bytes
     * @param compressedSize       total size of compressed file on disk in bytes
     * @param headerSize           metadata header overhead in bytes
     * @param payloadSize          compressed bitstream payload in bytes
     * @param compressionRatio     compressed / original ratio (or 0.0 if empty)
     * @param spaceSavedPercentage percent space saved (or 0.0 if empty)
     * @param roundTripVerified    true if decompressing the output produced the exact original text
     */
    public HuffmanResult(
            String originalText,
            Map<Character, Integer> frequencyMap,
            Map<Character, String> codeMap,
            HuffmanNode treeRoot,
            byte[] compressedBytes,
            byte mode,
            long originalSize,
            long compressedSize,
            long headerSize,
            long payloadSize,
            double compressionRatio,
            double spaceSavedPercentage,
            boolean roundTripVerified
    ) {
        this.originalText = originalText != null ? originalText : "";
        this.frequencyMap = frequencyMap != null ? Collections.unmodifiableMap(frequencyMap) : Collections.emptyMap();
        this.codeMap = codeMap != null ? Collections.unmodifiableMap(codeMap) : Collections.emptyMap();
        this.treeRoot = treeRoot;
        this.compressedBytes = compressedBytes != null ? compressedBytes.clone() : new byte[0];
        this.mode = mode;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.headerSize = headerSize;
        this.payloadSize = payloadSize;
        this.compressionRatio = compressionRatio;
        this.spaceSavedPercentage = spaceSavedPercentage;
        this.roundTripVerified = roundTripVerified;
    }

    /**
     * @return the source input text
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * @return an unmodifiable map of character frequencies
     */
    public Map<Character, Integer> getFrequencyMap() {
        return frequencyMap;
    }

    /**
     * @return an unmodifiable map of character to binary prefix code string
     */
    public Map<Character, String> getCodeMap() {
        return codeMap;
    }

    /**
     * @return root node of the built Huffman tree, or null if input was empty
     */
    public HuffmanNode getTreeRoot() {
        return treeRoot;
    }

    /**
     * @return a defensive copy of the binary .huff file bytes
     */
    public byte[] getCompressedBytes() {
        return compressedBytes != null ? compressedBytes.clone() : new byte[0];
    }

    /**
     * @return mode flag (0 for Stored, 1 for Huffman)
     */
    public byte getMode() {
        return mode;
    }

    /**
     * @return size of the original text encoded as UTF-8 in bytes
     */
    public long getOriginalSize() {
        return originalSize;
    }

    /**
     * @return total size of the compressed file/bytes
     */
    public long getCompressedSize() {
        return compressedSize;
    }

    /**
     * @return size of the header metadata in bytes
     */
    public long getHeaderSize() {
        return headerSize;
    }

    /**
     * @return size of the compressed bitstream payload in bytes
     */
    public long getPayloadSize() {
        return payloadSize;
    }

    /**
     * @return compression ratio (compressed / original)
     */
    public double getCompressionRatio() {
        return compressionRatio;
    }

    /**
     * @return space saved percentage
     */
    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    /**
     * Convenience alias for getSpaceSavedPercentage().
     *
     * @return space saved percentage
     */
    public double getSpaceSavedPercent() {
        return spaceSavedPercentage;
    }

    /**
     * @return true if decompression of the compressed bytes matches original text byte-for-byte
     */
    public boolean isRoundTripVerified() {
        return roundTripVerified;
    }

    /**
     * Convenience getter alias for isRoundTripVerified().
     *
     * @return true if round-trip verified
     */
    public boolean getRoundTripVerified() {
        return roundTripVerified;
    }
}
