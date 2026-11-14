package huffman;

/**
 * Calculates and formats compression efficiency metrics for files processed by Huffman compression.
 * Supports dual-mode reporting and includes safety guards against division by zero for empty files:
 * - Flag 1: Huffman Mode (reports header size, payload size, ratio, and space saved)
 * - Flag 0: Stored Mode (reports "Stored uncompressed (no savings possible)")
 */
public class CompressionStats {

    private final byte mode;
    private final long originalBytes;
    private final long compressedBytes;
    private final long headerBytes;
    private final long payloadBytes;
    private final double compressionRatio;
    private final double spaceSavedPercentage;

    /**
     * Constructs a CompressionStats instance with mode and header breakdown.
     * Safely guards against division by zero if originalBytes is 0.
     *
     * @param mode            0 for stored mode, 1 for Huffman mode
     * @param originalBytes   original file size in bytes
     * @param compressedBytes total compressed file size on disk in bytes
     * @param headerBytes     size of the header metadata in bytes
     * @param payloadBytes    size of the packed compressed payload in bytes
     */
    public CompressionStats(byte mode, long originalBytes, long compressedBytes, long headerBytes, long payloadBytes) {
        this.mode = mode;
        this.originalBytes = originalBytes;
        this.compressedBytes = compressedBytes;
        this.headerBytes = headerBytes;
        this.payloadBytes = payloadBytes;

        if (originalBytes > 0) {
            this.compressionRatio = (double) compressedBytes / (double) originalBytes;
            this.spaceSavedPercentage = (1.0 - this.compressionRatio) * 100.0;
        } else {
            // Guard against division by zero for empty files
            this.compressionRatio = 0.0;
            this.spaceSavedPercentage = 0.0;
        }
    }

    /**
     * Convenience constructor when mode is Huffman (Flag 1) and header breakdown is unknown.
     *
     * @param originalBytes   original file size in bytes
     * @param compressedBytes total compressed file size on disk in bytes
     */
    public CompressionStats(long originalBytes, long compressedBytes) {
        this(FileHandler.MODE_HUFFMAN, originalBytes, compressedBytes, -1, -1);
    }

    /**
     * @return mode flag (0 for Stored, 1 for Huffman)
     */
    public byte getMode() {
        return mode;
    }

    /**
     * @return original file size in bytes
     */
    public long getOriginalBytes() {
        return originalBytes;
    }

    /**
     * @return compressed file size in bytes
     */
    public long getCompressedBytes() {
        return compressedBytes;
    }

    /**
     * @return size of the metadata header in bytes
     */
    public long getHeaderBytes() {
        return headerBytes;
    }

    /**
     * @return size of the compressed payload in bytes
     */
    public long getPayloadBytes() {
        return payloadBytes;
    }

    /**
     * @return compression ratio (compressed / original), or 0.0 if original size is 0
     */
    public double getCompressionRatio() {
        return compressionRatio;
    }

    /**
     * @return space saved as a percentage, or 0.0 if original size is 0
     */
    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    /**
     * Returns a formatted multi-line summary of the compression statistics.
     * Handles empty files safely without displaying NaN or Infinity.
     *
     * @return formatted statistics string
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("================ Compression Statistics ================\n");
        sb.append(String.format("Original Size       : %,d bytes%n", originalBytes));
        sb.append(String.format("Compressed File Size: %,d bytes%n", compressedBytes));

        if (originalBytes == 0) {
            if (mode == FileHandler.MODE_STORED) {
                sb.append("Mode                : Flag 0 (Stored uncompressed (no savings possible))\n");
            } else {
                sb.append("Mode                : Flag 1 (Huffman compressed)\n");
            }
            sb.append("Compression Ratio   : N/A (Original file is empty - 0 bytes)\n");
            sb.append("Space Saved         : 0.00% (Original file is empty)\n");
            sb.append("Status              : Stored uncompressed (no savings possible)\n");
            sb.append("========================================================");
            return sb.toString();
        }

        if (mode == FileHandler.MODE_STORED) {
            sb.append("Mode                : Flag 0 (Stored uncompressed (no savings possible))\n");
            sb.append(String.format("Compression Ratio   : %.4f%n", compressionRatio));
            sb.append("Status              : Stored uncompressed (no savings possible)\n");
        } else {
            sb.append("Mode                : Flag 1 (Huffman compressed)\n");
            if (headerBytes >= 0 && payloadBytes >= 0) {
                sb.append(String.format("  ├── Header Size   : %,d bytes (1-byte flag + varint header)%n", headerBytes));
                sb.append(String.format("  └── Payload Size  : %,d bytes (packed bitstream)%n", payloadBytes));
            }
            sb.append(String.format("Compression Ratio   : %.4f (%.2f%% of original)%n", compressionRatio, compressionRatio * 100.0));

            if (spaceSavedPercentage >= 0) {
                sb.append(String.format("Space Saved         : %.2f%%%n", spaceSavedPercentage));
            } else {
                sb.append(String.format("Space Saved         : %.2f%% (File expanded due to header metadata overhead)%n", spaceSavedPercentage));
            }
        }
        sb.append("========================================================");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedSummary();
    }
}
