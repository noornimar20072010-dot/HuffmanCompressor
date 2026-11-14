package huffman;

import java.util.Map;

/**
 * Encodes text into a sequence of bits using a Huffman code map and packs them into bytes.
 */
public class Compressor {

    /**
     * Holds the output of the in-memory compression process.
     *
     * @param compressedBytes packed byte array containing the bit stream
     * @param paddingBits     number of unused padding bits added to the final byte (0-7)
     * @param bitString       the raw binary string of '0' and '1' characters
     */
    public record CompressionResult(byte[] compressedBytes, int paddingBits, String bitString) {}

    /**
     * Full compression pipeline: analyzes frequencies, builds the Huffman tree,
     * derives prefix codes, and packs bits into a compressed payload.
     *
     * @param text input source text
     * @return {@link CompressionResult}
     */
    public static CompressionResult compress(String text) {
        if (text == null || text.isEmpty()) {
            return new CompressionResult(new byte[0], 0, "");
        }
        Map<Character, Integer> frequencies = FrequencyCounter.countFrequencies(text);
        HuffmanTree tree = new HuffmanTree(frequencies);
        Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
        return compress(text, codeMap);
    }

    /**
     * Encodes text using the provided code map and packs the resulting bits into a byte array.
     *
     * @param text    the source text to compress
     * @param codeMap the map of character to binary code string
     * @return CompressionResult with packed bytes, padding bit count, and bit string
     */
    public static CompressionResult compress(String text, Map<Character, String> codeMap) {
        if (text == null || text.isEmpty() || codeMap == null || codeMap.isEmpty()) {
            return new CompressionResult(new byte[0], 0, "");
        }

        // 1. Convert characters to a continuous string of '0's and '1's
        StringBuilder bitBuilder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String code = codeMap.get(c);
            if (code == null) {
                throw new IllegalArgumentException("No Huffman code found for character: " + FrequencyCounter.formatChar(c));
            }
            bitBuilder.append(code);
        }

        String bitString = bitBuilder.toString();
        int totalBits = bitString.length();

        // 2. Calculate padding bits required to align to 8-bit byte boundary
        int paddingBits = (totalBits % 8 == 0) ? 0 : 8 - (totalBits % 8);
        int byteCount = (totalBits + 7) / 8;
        byte[] bytes = new byte[byteCount];

        // 3. Pack bits into bytes (MSB to LSB order)
        for (int i = 0; i < totalBits; i++) {
            if (bitString.charAt(i) == '1') {
                int byteIndex = i / 8;
                int bitOffset = 7 - (i % 8);
                bytes[byteIndex] = (byte) (bytes[byteIndex] | (1 << bitOffset));
            }
        }

        return new CompressionResult(bytes, paddingBits, bitString);
    }
}
