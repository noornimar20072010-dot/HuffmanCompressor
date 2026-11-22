package huffman;

/**
 * Custom exception representing errors encountered during Huffman compression,
 * decompression, or binary serialization and deserialization.
 */
public class HuffmanException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a HuffmanException with a descriptive message.
     *
     * @param message human-readable description of the error
     */
    public HuffmanException(String message) {
        super(message);
    }

    /**
     * Constructs a HuffmanException with a descriptive message and cause.
     *
     * @param message human-readable description of the error
     * @param cause   the underlying cause
     */
    public HuffmanException(String message, Throwable cause) {
        super(message, cause);
    }
}
