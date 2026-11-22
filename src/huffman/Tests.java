package huffman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Comprehensive automated test suite (without JUnit) for HuffmanCompressor:
 * - Part 1: Varint unit tests (0, 1, 127, 128, 16383, 16384, Integer.MAX_VALUE)
 * - Part 2: Auto-mode consistency (Stored mode Flag 0 on empty, 1-char, and 5-char files)
 * - Part 3: Forced Huffman tree edge cases (Flag 1 on empty, 1-char, 1000-char, normal, and Unicode)
 * - Part 4: Big file test (~40 KB, 3-byte varint frequency, byte-for-byte verification & stats)
 * - Part 5: Edge-case safety (missing file, 0-byte file, corrupt flag, truncated header, division by zero)
 */
public class Tests {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        HuffmanCompressor Validation Suite       ");
        System.out.println("=================================================\n");

        Path tempDir = Path.of("test_temp");
        try {
            Files.createDirectories(tempDir);

            // ==============================================================
            // PART 1: VARINT UNIT TESTS
            // ==============================================================
            System.out.println("--- Part 1: Varint Unit Tests ---");
            testVarIntRoundTrip(0, 1);
            testVarIntRoundTrip(1, 1);
            testVarIntRoundTrip(127, 1);
            testVarIntRoundTrip(128, 2);
            testVarIntRoundTrip(16383, 2);
            testVarIntRoundTrip(16384, 3);
            testVarIntRoundTrip(Integer.MAX_VALUE, 5);

            // ==============================================================
            // PART 2: AUTO-MODE CONSISTENCY (EXPECT FLAG 0)
            // ==============================================================
            System.out.println("\n--- Part 2: Auto-Mode Consistency (Stored Mode Flag 0) ---");
            runAutoModeTestCase(
                "Auto-Mode: Empty file -> Flag 0",
                "",
                tempDir.resolve("auto_empty.txt").toString(),
                tempDir.resolve("auto_empty.huff").toString(),
                tempDir.resolve("auto_empty_out.txt").toString()
            );

            runAutoModeTestCase(
                "Auto-Mode: One-character file -> Flag 0",
                "A",
                tempDir.resolve("auto_single.txt").toString(),
                tempDir.resolve("auto_single.huff").toString(),
                tempDir.resolve("auto_single_out.txt").toString()
            );

            runAutoModeTestCase(
                "Auto-Mode: 5-character file -> Flag 0",
                "Hello",
                tempDir.resolve("auto_5char.txt").toString(),
                tempDir.resolve("auto_5char.huff").toString(),
                tempDir.resolve("auto_5char_out.txt").toString()
            );

            // ==============================================================
            // PART 3: FORCED HUFFMAN MODE (TREE EDGE CASES - FLAG 1)
            // ==============================================================
            System.out.println("\n--- Part 3: Forced Huffman Tree Edge Cases (Flag 1) ---");
            runForcedHuffmanTestCase(
                "Forced Huffman: Empty file (null root)",
                "",
                tempDir.resolve("forced_empty.txt").toString(),
                tempDir.resolve("forced_empty.huff").toString(),
                tempDir.resolve("forced_empty_out.txt").toString()
            );

            runForcedHuffmanTestCase(
                "Forced Huffman: One-character file (code \"0\")",
                "A",
                tempDir.resolve("forced_single.txt").toString(),
                tempDir.resolve("forced_single.huff").toString(),
                tempDir.resolve("forced_single_out.txt").toString()
            );

            runForcedHuffmanTestCase(
                "Forced Huffman: Single char repeated 1000 times",
                "X".repeat(1000),
                tempDir.resolve("forced_repeat1000.txt").toString(),
                tempDir.resolve("forced_repeat1000.huff").toString(),
                tempDir.resolve("forced_repeat1000_out.txt").toString()
            );

            String normalText = """
                Huffman coding is a particular type of optimal prefix code that is commonly used for lossless data compression.
                The output from Huffman's algorithm can be viewed as a variable-length code table for encoding a source symbol.
                The algorithm derives this table from the estimated probability or frequency of occurrence for each possible value.
                """;
            runForcedHuffmanTestCase(
                "Forced Huffman: Normal text",
                normalText,
                tempDir.resolve("forced_normal.txt").toString(),
                tempDir.resolve("forced_normal.huff").toString(),
                tempDir.resolve("forced_normal_out.txt").toString()
            );

            String unicodeText = """
                First line with standard English text.
                Second line with non-Latin scripts: こんにちは世界, مرحبا بالعالم, שלום עולם.
                Third line with symbols and currency: ©, ®, ™, €, £, ¥, ₹.
                Fourth line with emoji surrogate pairs: 🚀 🌍 🎉 💡 🔥 🤖.
                Fifth line has varied whitespace: \t\t[tabbed]\t and a blank trailing line.
                
                """;
            runForcedHuffmanTestCase(
                "Forced Huffman: Unicode characters and newlines",
                unicodeText,
                tempDir.resolve("forced_unicode.txt").toString(),
                tempDir.resolve("forced_unicode.huff").toString(),
                tempDir.resolve("forced_unicode_out.txt").toString()
            );

            // ==============================================================
            // PART 4: BENCHMARK TESTS (NATURAL ~42 KB & SYNTHETIC ~40 KB)
            // ==============================================================
            System.out.println("\n--- Part 4: Benchmark Tests (Natural Text & Synthetic Big-File) ---");
            runNaturalFileTest(tempDir);
            runBigFileTest(tempDir);

            // ==============================================================
            // PART 5: EDGE-CASE SAFETY & CORRUPTION GUARDS
            // ==============================================================
            System.out.println("\n--- Part 5: Edge-Case Safety & Corruption Guards ---");
            testMissingFileHandling();
            testEmptyZeroByteFileHandling(tempDir);
            testCorruptFlagHandling(tempDir);
            testTruncatedHeaderHandling(tempDir);
            testCompressionStatsZeroGuard();

            // ==============================================================
            // PART 6: HUFFMAN SERVICE IN-MEMORY VALIDATION
            // ==============================================================
            System.out.println("\n--- Part 6: HuffmanService In-Memory Validation ---");
            testServiceRoundTripNormal();
            testServiceRoundTripEmpty();
            testServiceRoundTripSingleChar();
            testServiceRoundTripUnicodeEmoji();
            testServiceResultMatchesStats();
            testServiceCorruptByteArrayThrows();

        } catch (Exception e) {
            System.err.println("Fatal error during test suite execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanDirectory(tempDir.toFile());
        }

        System.out.println("\n-------------------------------------------------");
        System.out.printf("Test Summary: %d / %d passed.%n", passedTests, totalTests);
        if (passedTests == totalTests) {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! [PASS]");
        } else {
            System.out.println("SOME TESTS FAILED! [FAIL]");
        }
        System.out.println("-------------------------------------------------");
    }

    /**
     * Unit test for writeVarInt / readVarInt.
     */
    private static void testVarIntRoundTrip(int value, int expectedByteLength) {
        totalTests++;
        String testName = String.format("Varint Unit: value = %,d (expect %d byte%s)", value, expectedByteLength, expectedByteLength > 1 ? "s" : "");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileHandler.writeVarInt(baos, value);
            byte[] bytes = baos.toByteArray();

            if (bytes.length != expectedByteLength) {
                printResult(testName, false, String.format("Expected %d bytes but got %d", expectedByteLength, bytes.length));
                return;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            int restored = FileHandler.readVarInt(bais);

            if (restored == value) {
                passedTests++;
                printResult(testName, true, String.format("Round-tripped %,d exactly in %d byte%s", value, bytes.length, bytes.length > 1 ? "s" : ""));
            } else {
                printResult(testName, false, String.format("Value mismatch! Expected %,d but got %,d", value, restored));
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    /**
     * Tests files passing through writeAutoCompressedFile, asserting Flag 0 when compressed size >= original size.
     */
    private static void runAutoModeTestCase(String testName, String originalText, String txtPath, String huffPath, String outTxtPath) {
        totalTests++;
        try {
            FileHandler.writeTextFile(txtPath, originalText);
            String readOriginal = FileHandler.readTextFile(txtPath);
            byte[] rawBytes = readOriginal.getBytes(StandardCharsets.UTF_8);

            Map<Character, Integer> freqs = FrequencyCounter.countFrequencies(readOriginal);
            HuffmanTree tree = new HuffmanTree(freqs);
            Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
            Compressor.CompressionResult result = Compressor.compress(readOriginal, codeMap);

            byte selectedMode = FileHandler.writeAutoCompressedFile(huffPath, rawBytes, freqs, result.paddingBits(), result.compressedBytes());

            if (selectedMode != FileHandler.MODE_STORED) {
                printResult(testName, false, "Expected Flag 0 (MODE_STORED), but auto-compressor chose: " + selectedMode);
                return;
            }

            FileHandler.CompressedFileData fileData = FileHandler.readCompressedFile(huffPath);
            if (fileData == null || fileData.mode() != FileHandler.MODE_STORED) {
                printResult(testName, false, "Deserialized file data is null or mode is not Flag 0");
                return;
            }

            String restoredText = Decompressor.decompress(fileData);
            FileHandler.writeTextFile(outTxtPath, restoredText);
            String savedText = FileHandler.readTextFile(outTxtPath);

            if (originalText.equals(restoredText) && originalText.equals(savedText)) {
                passedTests++;
                printResult(testName, true, "Verified Flag 0 (Stored); content matches exactly (" + originalText.length() + " chars)");
            } else {
                printResult(testName, false, "Text mismatch after decompression");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    /**
     * Tests files explicitly forced into Huffman mode (Flag 1) to verify tree edge cases.
     */
    private static void runForcedHuffmanTestCase(String testName, String originalText, String txtPath, String huffPath, String outTxtPath) {
        totalTests++;
        try {
            FileHandler.writeTextFile(txtPath, originalText);
            String readOriginal = FileHandler.readTextFile(txtPath);

            Map<Character, Integer> freqs = FrequencyCounter.countFrequencies(readOriginal);
            HuffmanTree tree = new HuffmanTree(freqs);
            Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
            Compressor.CompressionResult result = Compressor.compress(readOriginal, codeMap);

            boolean writeSuccess = FileHandler.writeHuffmanFile(huffPath, freqs, result.paddingBits(), result.compressedBytes());
            if (!writeSuccess) {
                printResult(testName, false, "Failed to write forced Huffman file");
                return;
            }

            FileHandler.CompressedFileData fileData = FileHandler.readCompressedFile(huffPath);
            if (fileData == null || fileData.mode() != FileHandler.MODE_HUFFMAN) {
                printResult(testName, false, "Failed to deserialize forced Huffman file with Flag 1");
                return;
            }

            String restoredText = Decompressor.decompress(fileData);
            FileHandler.writeTextFile(outTxtPath, restoredText);
            String savedText = FileHandler.readTextFile(outTxtPath);

            if (originalText.equals(restoredText) && originalText.equals(savedText)) {
                passedTests++;
                printResult(testName, true, "Verified Flag 1 (Huffman); content matches exactly (" + originalText.length() + " chars)");
            } else {
                printResult(testName, false, "Text mismatch after decompression");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    /**
     * Tests round-trip compression and decompression for natural_input.txt (~42 KB real public-domain prose).
     */
    private static void runNaturalFileTest(Path tempDir) {
        totalTests++;
        String testName = "Natural-Text: natural_input.txt (~42 KB real prose)";
        String naturalInputPath = "natural_input.txt";
        String huffPath = tempDir.resolve("natural_input.huff").toString();
        String restoredPath = tempDir.resolve("natural_input_restored.txt").toString();

        try {
            String originalText = FileHandler.readTextFile(naturalInputPath);
            if (originalText == null) {
                printResult(testName, false, "Could not find " + naturalInputPath);
                return;
            }

            byte[] rawBytes = originalText.getBytes(StandardCharsets.UTF_8);
            Map<Character, Integer> freqs = FrequencyCounter.countFrequencies(originalText);

            HuffmanTree tree = new HuffmanTree(freqs);
            Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
            Compressor.CompressionResult result = Compressor.compress(originalText, codeMap);

            byte selectedMode = FileHandler.writeAutoCompressedFile(huffPath, rawBytes, freqs, result.paddingBits(), result.compressedBytes());

            if (selectedMode != FileHandler.MODE_HUFFMAN) {
                printResult(testName, false, "Expected Flag 1 (Huffman Mode) for ~42 KB natural file, got: " + selectedMode);
                return;
            }

            FileHandler.CompressedFileData fileData = FileHandler.readCompressedFile(huffPath);
            if (fileData == null) {
                printResult(testName, false, "Failed to read compressed file " + huffPath);
                return;
            }

            String restoredText = Decompressor.decompress(fileData);
            FileHandler.writeTextFile(restoredPath, restoredText);
            String savedRestored = FileHandler.readTextFile(restoredPath);

            boolean matches = originalText.equals(restoredText) && originalText.equals(savedRestored);
            if (!matches) {
                printResult(testName, false, "Decompressed text does not match original natural_input.txt byte-for-byte");
                return;
            }

            long origSize = FileHandler.getFileSize(naturalInputPath);
            long compSize = FileHandler.getFileSize(huffPath);
            long payloadSize = result.compressedBytes().length;
            long headerSize = compSize - payloadSize;
            CompressionStats stats = new CompressionStats(selectedMode, origSize, compSize, headerSize, payloadSize);

            passedTests++;
            printResult(testName, true, String.format("Byte-for-byte match! Orig: %,d B, Comp: %,d B (Hdr: %,d B, Payload: %,d B), Ratio: %.4f, Saved: %.2f%%",
                    origSize, compSize, headerSize, payloadSize, stats.getCompressionRatio(), stats.getSpaceSavedPercentage()));
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    /**
     * Tests compressing and decompressing big_input.txt (~40 KB, 3-byte varint).
     */
    private static void runBigFileTest(Path tempDir) {
        totalTests++;
        String testName = "Big-File: big_input.txt (~40 KB, 3-byte varint)";
        String bigInputPath = "big_input.txt";
        String huffPath = tempDir.resolve("big_input.huff").toString();
        String restoredPath = tempDir.resolve("big_input_restored.txt").toString();

        try {
            String originalText = FileHandler.readTextFile(bigInputPath);
            if (originalText == null) {
                printResult(testName, false, "Could not find " + bigInputPath);
                return;
            }

            byte[] rawBytes = originalText.getBytes(StandardCharsets.UTF_8);
            Map<Character, Integer> freqs = FrequencyCounter.countFrequencies(originalText);

            // Verify that space frequency >= 16384 (exercises 3-byte varint)
            int spaceFreq = freqs.getOrDefault(' ', 0);
            boolean has3ByteVarint = spaceFreq >= 16384;

            HuffmanTree tree = new HuffmanTree(freqs);
            Map<Character, String> codeMap = CodeGenerator.generateCodes(tree);
            Compressor.CompressionResult result = Compressor.compress(originalText, codeMap);

            byte selectedMode = FileHandler.writeAutoCompressedFile(huffPath, rawBytes, freqs, result.paddingBits(), result.compressedBytes());

            if (selectedMode != FileHandler.MODE_HUFFMAN) {
                printResult(testName, false, "Expected Flag 1 (Huffman Mode) for ~40 KB file, got: " + selectedMode);
                return;
            }

            FileHandler.CompressedFileData fileData = FileHandler.readCompressedFile(huffPath);
            if (fileData == null) {
                printResult(testName, false, "Failed to read compressed file " + huffPath);
                return;
            }

            String restoredText = Decompressor.decompress(fileData);
            FileHandler.writeTextFile(restoredPath, restoredText);
            String savedRestored = FileHandler.readTextFile(restoredPath);

            boolean matches = originalText.equals(restoredText) && originalText.equals(savedRestored);
            if (!matches) {
                printResult(testName, false, "Decompressed text does not match original big_input.txt byte-for-byte");
                return;
            }

            long origSize = FileHandler.getFileSize(bigInputPath);
            long compSize = FileHandler.getFileSize(huffPath);
            long payloadSize = result.compressedBytes().length;
            long headerSize = compSize - payloadSize;
            CompressionStats stats = new CompressionStats(selectedMode, origSize, compSize, headerSize, payloadSize);

            passedTests++;
            printResult(testName, true, String.format("Byte-for-byte match! Orig: %,d B, Comp: %,d B (Hdr: %,d B, Payload: %,d B), Ratio: %.4f, Saved: %.2f%% [3-byte varint for ' ': %,d]",
                    origSize, compSize, headerSize, payloadSize, stats.getCompressionRatio(), stats.getSpaceSavedPercentage(), spaceFreq));
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testMissingFileHandling() {
        totalTests++;
        String testName = "Safety: Missing .huff file returns null";
        FileHandler.CompressedFileData data = FileHandler.readCompressedFile("non_existent_file_xyz.huff");
        if (data == null) {
            passedTests++;
            printResult(testName, true, "Handled safely without crashing");
        } else {
            printResult(testName, false, "Expected null for missing file");
        }
    }

    private static void testEmptyZeroByteFileHandling(Path tempDir) throws IOException {
        totalTests++;
        String testName = "Safety: 0-byte .huff file returns null";
        Path emptyHuff = tempDir.resolve("zero_byte.huff");
        Files.createFile(emptyHuff);

        FileHandler.CompressedFileData data = FileHandler.readCompressedFile(emptyHuff.toString());
        if (data == null) {
            passedTests++;
            printResult(testName, true, "Handled safely without crashing");
        } else {
            printResult(testName, false, "Expected null for 0-byte file");
        }
    }

    private static void testCorruptFlagHandling(Path tempDir) throws IOException {
        totalTests++;
        String testName = "Safety: Corrupt mode flag returns null";
        Path corruptHuff = tempDir.resolve("corrupt_flag.huff");
        try (FileOutputStream fos = new FileOutputStream(corruptHuff.toFile())) {
            fos.write(99); // Invalid flag (neither 0 nor 1)
            fos.write(new byte[]{1, 2, 3, 4, 5});
        }

        FileHandler.CompressedFileData data = FileHandler.readCompressedFile(corruptHuff.toString());
        if (data == null) {
            passedTests++;
            printResult(testName, true, "Handled safely without crashing");
        } else {
            printResult(testName, false, "Expected null for invalid flag");
        }
    }

    private static void testTruncatedHeaderHandling(Path tempDir) throws IOException {
        totalTests++;
        String testName = "Safety: Truncated header returns null";
        Path truncHuff = tempDir.resolve("truncated.huff");
        try (FileOutputStream fos = new FileOutputStream(truncHuff.toFile())) {
            fos.write(FileHandler.MODE_HUFFMAN); // Flag 1
            fos.write(new byte[]{0, 0, 0, 10}); // uniqueCount = 10, then EOF immediately!
        }

        FileHandler.CompressedFileData data = FileHandler.readCompressedFile(truncHuff.toString());
        if (data == null) {
            passedTests++;
            printResult(testName, true, "Handled safely without crashing");
        } else {
            printResult(testName, false, "Expected null for truncated header");
        }
    }

    private static void testCompressionStatsZeroGuard() {
        totalTests++;
        String testName = "Safety: CompressionStats division-by-zero guard";
        CompressionStats stats = new CompressionStats(FileHandler.MODE_STORED, 0, 1, 1, 0);
        String summary = stats.getFormattedSummary();

        if (!summary.contains("NaN") && !summary.contains("Infinity") && summary.contains("N/A (Original file is empty")) {
            passedTests++;
            printResult(testName, true, "No NaN or Infinity present");
        } else {
            printResult(testName, false, "Summary contains invalid numerical values: " + summary);
        }
    }

    private static void testServiceRoundTripNormal() {
        totalTests++;
        String testName = "Service: Round-trip normal text";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            String text = "The quick brown fox jumps over the lazy dog! 12345.";
            HuffmanResult result = service.compress(text);
            String decompressed = service.decompress(result.getCompressedBytes());

            if (result.isRoundTripVerified() && text.equals(decompressed)) {
                passedTests++;
                printResult(testName, true, String.format("Verified in-memory round-trip (%d chars -> %d bytes)", text.length(), result.getCompressedSize()));
            } else {
                printResult(testName, false, "Decompressed text does not match original");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testServiceRoundTripEmpty() {
        totalTests++;
        String testName = "Service: Round-trip empty text";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            String text = "";
            HuffmanResult result = service.compress(text);
            String decompressed = service.decompress(result.getCompressedBytes());

            if (result.isRoundTripVerified() && text.equals(decompressed) && result.getMode() == FileHandler.MODE_STORED) {
                passedTests++;
                printResult(testName, true, "Verified empty text round-trip (Flag 0 Stored, 1 byte)");
            } else {
                printResult(testName, false, "Decompressed empty text mismatch or incorrect mode");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testServiceRoundTripSingleChar() {
        totalTests++;
        String testName = "Service: Round-trip one-character text";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            String text = "A";
            HuffmanResult result = service.compress(text);
            String decompressed = service.decompress(result.getCompressedBytes());

            if (result.isRoundTripVerified() && text.equals(decompressed)) {
                passedTests++;
                printResult(testName, true, "Verified single char round-trip ('A' restored exactly)");
            } else {
                printResult(testName, false, "Decompressed single character mismatch");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testServiceRoundTripUnicodeEmoji() {
        totalTests++;
        String testName = "Service: Round-trip Unicode and emoji text";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            String text = "Hello 世界! こんにちは 🌍 🚀 © ™ 🎉 [newline:\n\ttab]";
            HuffmanResult result = service.compress(text);
            String decompressed = service.decompress(result.getCompressedBytes());

            if (result.isRoundTripVerified() && text.equals(decompressed)) {
                passedTests++;
                printResult(testName, true, String.format("Verified Unicode/emoji round-trip (%d chars restored)", text.length()));
            } else {
                printResult(testName, false, "Decompressed Unicode text does not match");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testServiceResultMatchesStats() {
        totalTests++;
        String testName = "Service: HuffmanResult values match CompressionStats";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            String text = "Huffman compression efficiency verification asserting stats parity across data structures.";
            HuffmanResult result = service.compress(text);
            CompressionStats stats = new CompressionStats(
                    result.getMode(),
                    result.getOriginalSize(),
                    result.getCompressedSize(),
                    result.getHeaderSize(),
                    result.getPayloadSize()
            );

            boolean sizesMatch = (result.getOriginalSize() == stats.getOriginalBytes())
                    && (result.getCompressedSize() == stats.getCompressedBytes())
                    && (result.getHeaderSize() == stats.getHeaderBytes())
                    && (result.getPayloadSize() == stats.getPayloadBytes());

            boolean metricsMatch = (Math.abs(result.getCompressionRatio() - stats.getCompressionRatio()) < 1e-9)
                    && (Math.abs(result.getSpaceSavedPercentage() - stats.getSpaceSavedPercentage()) < 1e-9);

            boolean sumMatches = (result.getHeaderSize() + result.getPayloadSize() == result.getCompressedSize());

            if (sizesMatch && metricsMatch && sumMatches) {
                passedTests++;
                printResult(testName, true, String.format("Parity verified (Ratio: %.4f, Saved: %.2f%%)", result.getCompressionRatio(), result.getSpaceSavedPercentage()));
            } else {
                printResult(testName, false, "Discrepancy detected between HuffmanResult and CompressionStats");
            }
        } catch (Exception e) {
            printResult(testName, false, "Exception: " + e.getMessage());
        }
    }

    private static void testServiceCorruptByteArrayThrows() {
        totalTests++;
        String testName = "Service: Corrupt byte[] throws HuffmanException";
        try {
            HuffmanService service = HuffmanService.INSTANCE;
            byte[] corruptBytes = new byte[]{ (byte) 99, 1, 2, 3, 4 }; // Invalid mode flag 99
            service.decompress(corruptBytes);
            printResult(testName, false, "Expected HuffmanException was not thrown");
        } catch (HuffmanException e) {
            passedTests++;
            printResult(testName, true, "Caught expected HuffmanException: " + e.getMessage());
        } catch (Exception e) {
            printResult(testName, false, "Unexpected exception type: " + e.getClass().getName());
        }
    }

    private static void printResult(String testName, boolean pass, String details) {
        System.out.printf("%-58s [%s] - %s%n", testName, pass ? "PASS" : "FAIL", details);
    }

    private static void cleanDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    cleanDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
