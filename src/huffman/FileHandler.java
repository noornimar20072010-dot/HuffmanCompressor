package huffman;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing file I/O operations for plain text files and compact
 * Huffman-compressed binary (.huff) files with dual-mode support and robust corruption guards:
 * - Flag 1 (Huffman Mode): Serializes frequency table using varints + bitstream payload.
 * - Flag 0 (Stored Mode) : Stores raw UTF-8 bytes when Huffman encoding cannot achieve net size reduction.
 */
public class FileHandler {

    /** Mode flag indicating uncompressed raw UTF-8 storage. */
    public static final byte MODE_STORED = 0;

    /** Mode flag indicating Huffman-compressed storage with varint header. */
    public static final byte MODE_HUFFMAN = 1;

    /**
     * Immutable data transfer record holding deserialized data from a .huff file.
     *
     * @param mode            0 for stored raw bytes, 1 for Huffman compressed
     * @param rawBytes        original UTF-8 bytes (non-null in stored mode)
     * @param frequencies     reconstructed character frequency map (non-null in Huffman mode)
     * @param paddingBits     number of unused padding bits in the final packed byte (Huffman mode)
     * @param compressedBytes packed payload containing Huffman-encoded bitstream (Huffman mode)
     * @param headerBytes     size of the header metadata in bytes (1 for stored mode)
     */
    public record CompressedFileData(
            byte mode,
            byte[] rawBytes,
            Map<Character, Integer> frequencies,
            int paddingBits,
            byte[] compressedBytes,
            int headerBytes
    ) {}

    /**
     * Writes an integer as an unsigned variable-length integer (varint, 7 bits per byte).
     * The most significant bit (MSB 0x80) serves as a continuation flag.
     *
     * @param out   output stream
     * @param value non-negative integer to write
     * @throws IOException if an I/O error occurs
     */
    public static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value & 0x7F);
    }

    /**
     * Reads an unsigned variable-length integer (varint, 7 bits per byte).
     *
     * @param in input stream
     * @return deserialized integer
     * @throws IOException if an I/O error or unexpected EOF occurs
     */
    public static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException("Unexpected EOF while reading varint");
            }
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift >= 35) {
                throw new IllegalArgumentException("Varint exceeds 32 bits");
            }
        }
        return result;
    }

    /**
     * Calculates the byte length required to encode an integer as a 7-bit varint.
     *
     * @param value the non-negative integer value
     * @return number of bytes required (1 to 5 bytes)
     */
    public static int getVarIntLength(int value) {
        int len = 0;
        while ((value & ~0x7F) != 0) {
            len++;
            value >>>= 7;
        }
        return len + 1;
    }

    /**
     * Calculates the total prospective file size for Huffman compression (1-byte flag + header + payload).
     *
     * @param frequencies        character frequency map
     * @param payloadBytesLength length of packed payload in bytes
     * @return total file size in bytes
     */
    public static int calculateHuffmanFileSize(Map<Character, Integer> frequencies, int payloadBytesLength) {
        int size = 1; // 1-byte mode flag (MODE_HUFFMAN)
        size += 4;    // unique character count int
        if (frequencies != null) {
            for (int freq : frequencies.values()) {
                size += 2; // 2-byte character
                size += getVarIntLength(freq); // 1-5 bytes varint frequency
            }
        }
        size += 4; // paddingBits int
        size += 4; // payloadLength int
        size += payloadBytesLength;
        return size;
    }

    /**
     * Reads the entire contents of a text file from disk decoded as UTF-8.
     * Safely validates that the file exists and is not a directory.
     *
     * @param path path to the text file
     * @return file contents as a String, or null if an error occurs
     */
    public static String readTextFile(String path) {
        if (path == null || path.isBlank()) {
            System.err.println("Error reading text file: File path is empty or blank.");
            return null;
        }
        Path filePath = Path.of(path);
        if (!Files.exists(filePath)) {
            System.err.println("Error reading text file: File does not exist at '" + path + "'.");
            return null;
        }
        if (Files.isDirectory(filePath)) {
            System.err.println("Error reading text file: Path is a directory, not a file at '" + path + "'.");
            return null;
        }
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error reading text file at '" + path + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Writes string content to a text file on disk encoded as UTF-8.
     *
     * @param path    destination file path
     * @param content text content to write
     * @return true if successful, false on error
     */
    public static boolean writeTextFile(String path, String content) {
        if (path == null || path.isBlank()) {
            System.err.println("Error writing text file: Destination path is empty.");
            return false;
        }
        try {
            Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("Error writing text file at '" + path + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the size of a file in bytes, or -1 if the file cannot be accessed.
     *
     * @param path path to the file
     * @return file size in bytes, or -1 on error
     */
    public static long getFileSize(String path) {
        if (path == null || path.isBlank()) {
            return -1L;
        }
        Path p = Path.of(path);
        if (!Files.exists(p) || Files.isDirectory(p)) {
            return -1L;
        }
        try {
            return Files.size(p);
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Writes an uncompressed file with mode flag 0 (MODE_STORED) followed by raw UTF-8 bytes.
     *
     * @param path     destination file path
     * @param rawBytes raw UTF-8 bytes to store
     * @return true if written successfully, false otherwise
     */
    public static boolean writeStoredFile(String path, byte[] rawBytes) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
            out.writeByte(MODE_STORED);
            if (rawBytes != null && rawBytes.length > 0) {
                out.write(rawBytes);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing stored file at '" + path + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Explicitly writes a Huffman-compressed file with mode flag 1 (MODE_HUFFMAN).
     *
     * @param path            destination path for the .huff file
     * @param frequencies     character frequency table
     * @param paddingBits     number of padding bits in the last byte
     * @param compressedBytes compressed payload bytes
     * @return true if file was written successfully, false otherwise
     */
    public static boolean writeHuffmanFile(String path, Map<Character, Integer> frequencies, int paddingBits, byte[] compressedBytes) {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
            // 1. Write mode flag (Flag 1)
            out.writeByte(MODE_HUFFMAN);

            // 2. Write unique character count
            out.writeInt(frequencies == null ? 0 : frequencies.size());

            // 3. Write character and varint frequency pairs
            if (frequencies != null) {
                for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                    out.writeChar(entry.getKey());
                    writeVarInt(out, entry.getValue());
                }
            }

            // 4. Write padding bits count
            out.writeInt(paddingBits);

            // 5. Write payload length and packed bytes
            out.writeInt(compressedBytes == null ? 0 : compressedBytes.length);
            if (compressedBytes != null && compressedBytes.length > 0) {
                out.write(compressedBytes);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error writing Huffman file at '" + path + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Automatically selects mode: compares prospective Huffman file size (1-byte flag + header + payload)
     * against the original file's UTF-8 byte size. If Huffman file size is >= original size, stores raw
     * UTF-8 bytes with Flag 0 (MODE_STORED). Otherwise, stores Huffman-encoded data with Flag 1 (MODE_HUFFMAN).
     *
     * @param path            destination .huff path
     * @param rawUtf8Bytes    original raw UTF-8 bytes
     * @param frequencies     character frequency table
     * @param paddingBits     padding bits in the last payload byte
     * @param compressedBytes packed payload bytes
     * @return the mode selected (0 = MODE_STORED, 1 = MODE_HUFFMAN), or -1 on error
     */
    public static byte writeAutoCompressedFile(String path, byte[] rawUtf8Bytes, Map<Character, Integer> frequencies, int paddingBits, byte[] compressedBytes) {
        int originalSize = (rawUtf8Bytes == null) ? 0 : rawUtf8Bytes.length;
        int prospectiveHuffmanSize = calculateHuffmanFileSize(frequencies, compressedBytes == null ? 0 : compressedBytes.length);

        // Strict rule: Stored mode (Flag 0) must be used whenever Huffman file size is >= original UTF-8 byte size.
        if (prospectiveHuffmanSize < originalSize) {
            boolean ok = writeHuffmanFile(path, frequencies, paddingBits, compressedBytes);
            return ok ? MODE_HUFFMAN : -1;
        } else {
            boolean ok = writeStoredFile(path, rawUtf8Bytes);
            return ok ? MODE_STORED : -1;
        }
    }

    /**
     * Default write method: writes in Huffman mode (Flag 1).
     *
     * @param path            destination path
     * @param frequencies     frequency map
     * @param paddingBits     padding bits
     * @param compressedBytes compressed payload
     * @return true if written successfully, false otherwise
     */
    public static boolean writeCompressedFile(String path, Map<Character, Integer> frequencies, int paddingBits, byte[] compressedBytes) {
        return writeHuffmanFile(path, frequencies, paddingBits, compressedBytes);
    }

    /**
     * Reads a .huff file from disk, detecting the 1-byte mode flag with comprehensive safety checks:
     * - Validates that the file exists, is readable, and is non-empty.
     * - Validates mode flag is either 0 (MODE_STORED) or 1 (MODE_HUFFMAN).
     * - In Huffman mode, guards against truncated headers, negative/out-of-range counts, corrupted varints,
     *   and premature end-of-file without throwing uncaught stack traces.
     *
     * @param path path to the .huff file
     * @return parsed {@link CompressedFileData}, or null if corrupted, missing, empty, or unreadable
     */
    public static CompressedFileData readCompressedFile(String path) {
        if (path == null || path.isBlank()) {
            System.err.println("Error reading compressed file: Path is empty or blank.");
            return null;
        }
        Path filePath = Path.of(path);
        if (!Files.exists(filePath)) {
            System.err.println("Error reading compressed file: File does not exist at '" + path + "'.");
            return null;
        }
        if (Files.isDirectory(filePath)) {
            System.err.println("Error reading compressed file: Path is a directory, not a file at '" + path + "'.");
            return null;
        }

        long totalFileSize;
        try {
            totalFileSize = Files.size(filePath);
        } catch (IOException e) {
            System.err.println("Error checking compressed file size at '" + path + "': " + e.getMessage());
            return null;
        }

        if (totalFileSize == 0) {
            System.err.println("Error reading compressed file: File '" + path + "' is empty (0 bytes). A valid .huff file requires at least a 1-byte mode flag.");
            return null;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath.toFile())))) {
            // Read 1-byte mode flag
            int modeByte = in.read();
            if (modeByte == -1) {
                System.err.println("Error reading compressed file: Unexpected end of file while reading mode flag in '" + path + "'.");
                return null;
            }

            if (modeByte == MODE_STORED) {
                // Read all remaining bytes as raw UTF-8 payload
                byte[] rawBytes = in.readAllBytes();
                return new CompressedFileData(MODE_STORED, rawBytes, null, 0, null, 1);
            } else if (modeByte == MODE_HUFFMAN) {
                // 1. Read unique character count
                int uniqueCount;
                try {
                    uniqueCount = in.readInt();
                } catch (EOFException e) {
                    System.err.println("Error reading compressed file: Truncated header in '" + path + "'. Missing unique character count.");
                    return null;
                }

                if (uniqueCount < 0 || uniqueCount > 65536) {
                    System.err.println("Error reading compressed file: Corrupted header in '" + path + "'. Invalid unique character count: " + uniqueCount + ".");
                    return null;
                }

                Map<Character, Integer> frequencies = new HashMap<>();

                // 2. Read each 2-byte character and varint frequency pair
                for (int i = 0; i < uniqueCount; i++) {
                    char c;
                    int freq;
                    try {
                        c = in.readChar();
                        freq = readVarInt(in);
                    } catch (EOFException e) {
                        System.err.println("Error reading compressed file: Truncated header in '" + path + "'. Premature EOF while reading character/frequency entry " + (i + 1) + " of " + uniqueCount + ".");
                        return null;
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error reading compressed file: Corrupted varint frequency in '" + path + "': " + e.getMessage());
                        return null;
                    }

                    if (freq <= 0) {
                        System.err.println("Error reading compressed file: Corrupted frequency (" + freq + ") for character " + FrequencyCounter.formatChar(c) + " in '" + path + "'. Frequencies must be positive integers.");
                        return null;
                    }
                    frequencies.put(c, freq);
                }

                // 3. Read padding bit count
                int paddingBits;
                try {
                    paddingBits = in.readInt();
                } catch (EOFException e) {
                    System.err.println("Error reading compressed file: Truncated header in '" + path + "'. Missing padding bits count.");
                    return null;
                }

                if (paddingBits < 0 || paddingBits > 7) {
                    System.err.println("Error reading compressed file: Corrupted padding bits count (" + paddingBits + ") in '" + path + "'. Expected value between 0 and 7.");
                    return null;
                }

                // 4. Read payload byte length and data
                int byteLength;
                try {
                    byteLength = in.readInt();
                } catch (EOFException e) {
                    System.err.println("Error reading compressed file: Truncated header in '" + path + "'. Missing payload length.");
                    return null;
                }

                if (byteLength < 0) {
                    System.err.println("Error reading compressed file: Corrupted payload length (" + byteLength + ") in '" + path + "'.");
                    return null;
                }

                byte[] compressedBytes = new byte[byteLength];
                try {
                    in.readFully(compressedBytes);
                } catch (EOFException e) {
                    System.err.println("Error reading compressed file: Truncated payload in '" + path + "'. Expected " + byteLength + " bytes, but file ended prematurely.");
                    return null;
                }

                int headerBytes = (int) (totalFileSize - byteLength);
                return new CompressedFileData(MODE_HUFFMAN, null, frequencies, paddingBits, compressedBytes, headerBytes);
            } else {
                System.err.println("Error reading compressed file: Invalid or unsupported mode flag (" + modeByte + ") in '" + path + "'. Expected 0 (Stored) or 1 (Huffman).");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error reading compressed file at '" + path + "': " + e.getMessage());
            return null;
        }
    }
}
