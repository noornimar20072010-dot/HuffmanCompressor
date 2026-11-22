package huffman;

import java.nio.charset.StandardCharsets;

/**
 * Reconstructs original text from compressed files or memory buffers, supporting both
 * Stored mode (Flag 0, raw UTF-8) and Huffman mode (Flag 1, tree traversal).
 * Includes robust validation against null or corrupted inputs.
 */
public class Decompressor {

    /**
     * Decompresses data directly from a parsed {@link FileHandler.CompressedFileData} instance,
     * throwing a {@link HuffmanException} on corrupted or invalid input.
     *
     * @param fileData parsed compressed file data
     * @return original decoded text
     * @throws HuffmanException if fileData is null, missing frequency table, or corrupted
     */
    public static String decompressOrThrow(FileHandler.CompressedFileData fileData) throws HuffmanException {
        if (fileData == null) {
            throw new HuffmanException("Provided compressed file data is null.");
        }

        // Mode 0: Stored uncompressed
        if (fileData.mode() == FileHandler.MODE_STORED) {
            byte[] raw = fileData.rawBytes();
            return (raw == null || raw.length == 0) ? "" : new String(raw, StandardCharsets.UTF_8);
        }

        // Mode 1: Huffman compressed
        if (fileData.frequencies() == null) {
            throw new HuffmanException("Missing frequency table for Huffman mode.");
        }

        HuffmanTree rebuiltTree = new HuffmanTree(fileData.frequencies());
        return decompressOrThrow(fileData.compressedBytes(), fileData.paddingBits(), rebuiltTree);
    }

    /**
     * Decompresses data directly from a parsed {@link FileHandler.CompressedFileData} instance.
     * Evaluates the 1-byte mode flag:
     * - Flag 0: Decodes the raw UTF-8 bytes directly.
     * - Flag 1: Reconstructs the Huffman tree from the varint frequency header and walks bits.
     *
     * @param fileData parsed compressed file data
     * @return original decoded text, or null if decompression fails
     */
    public static String decompress(FileHandler.CompressedFileData fileData) {
        try {
            return decompressOrThrow(fileData);
        } catch (HuffmanException e) {
            System.err.println("Decompression error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decompresses packed bytes back into the original string by walking the Huffman tree,
     * throwing a {@link HuffmanException} on corrupted bitstreams.
     *
     * @param compressedBytes byte array holding the packed bits
     * @param paddingBits     number of padding bits at the end of the last byte to ignore
     * @param tree            the HuffmanTree used to decode bits
     * @return original decoded text
     * @throws HuffmanException if corrupted or invalid tree/bytes
     */
    public static String decompressOrThrow(byte[] compressedBytes, int paddingBits, HuffmanTree tree) throws HuffmanException {
        if (tree == null || tree.getRoot() == null) {
            if (compressedBytes != null && compressedBytes.length > 0) {
                throw new HuffmanException("Empty tree with non-empty payload cannot be decompressed.");
            }
            return "";
        }

        if (compressedBytes == null || compressedBytes.length == 0) {
            return "";
        }

        HuffmanNode root = tree.getRoot();
        int totalValidBits = (compressedBytes.length * 8) - paddingBits;
        if (totalValidBits <= 0) {
            return "";
        }

        // Direct single-character leaf root edge case
        if (root.isLeaf()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < totalValidBits; i++) {
                sb.append(root.character);
            }
            return sb.toString();
        }

        StringBuilder decodedText = new StringBuilder();
        HuffmanNode current = root;

        for (int i = 0; i < totalValidBits; i++) {
            int byteIndex = i / 8;
            int bitOffset = 7 - (i % 8);
            int bit = (compressedBytes[byteIndex] >> bitOffset) & 1;

            current = (bit == 0) ? current.left : current.right;

            if (current == null) {
                throw new HuffmanException("Corrupted bitstream encountered at bit position " + i + ". Invalid branch taken.");
            }

            if (current.isLeaf()) {
                decodedText.append(current.character);
                current = root; // Reset to root to decode next character
            }
        }

        return decodedText.toString();
    }

    /**
     * Decompresses packed bytes back into the original string by walking the Huffman tree.
     * Guards against corrupted bitstreams without throwing uncaught exceptions.
     *
     * @param compressedBytes byte array holding the packed bits
     * @param paddingBits     number of padding bits at the end of the last byte to ignore
     * @param tree            the HuffmanTree used to decode bits
     * @return original decoded text, or null if corrupted
     */
    public static String decompress(byte[] compressedBytes, int paddingBits, HuffmanTree tree) {
        try {
            return decompressOrThrow(compressedBytes, paddingBits, tree);
        } catch (HuffmanException e) {
            System.err.println("Decompression error: " + e.getMessage());
            return null;
        }
    }
}
