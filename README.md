# HuffmanCompressor

A complete, robust, self-contained **Java 17** implementation of the Huffman text compression and decompression engine built with **zero external libraries** and **no build tools**.

---

## How Huffman Coding Works in Simple Words

1. **Count Frequencies**: Count how many times each character appears in the text.
2. **Build a Min-Heap**: Treat every character as a standalone leaf node and place them into a priority queue sorted from lowest to highest frequency.
3. **Merge Bottom-Up**: Take the two nodes with the lowest counts, join them under a new parent node whose count is their sum, and insert the parent back into the queue. Repeat until one root node remains.
4. **Assign Binary Codes**: Starting from the root, going left adds a `0` and going right adds a `1`. Characters that appear frequently (like space or `'e'`) end up near the top with very short codes (e.g. 2–3 bits), while rare characters end up deeper with longer codes.
5. **Pack Bits & Save Dual-Mode Header**: Pack the variable-length bits into 8-bit bytes. If the prospective compressed file (header + payload) is smaller than the original text, save it with **Flag 1 (Huffman Mode)**; otherwise save the raw UTF-8 bytes with **Flag 0 (Stored Mode)** so files never expand unnecessarily.

---

## Project Structure

```
HuffmanCompressor/
├── src/
│   └── huffman/
│       ├── CodeGenerator.java     # Recursive tree traversal generating prefix codes
│       ├── CompressionStats.java  # Metrics calculation (ratios, space saved, zero-guard)
│       ├── Compressor.java        # Encodes text into bit strings and packs them into bytes
│       ├── Decompressor.java      # Restores text from Huffman bitstream or Stored UTF-8 bytes
│       ├── FileHandler.java       # Dual-mode .huff I/O, varint encoding, and corruption guards
│       ├── FrequencyCounter.java  # Character occurrence frequency counter
│       ├── HuffmanNode.java       # Binary tree node (character, frequency, left, right)
│       ├── HuffmanTree.java       # Tree builder using PriorityQueue (handles edge cases)
│       ├── Main.java              # Interactive CLI menu loop (validate input, resilient to bad paths)
│       └── Tests.java             # Comprehensive 21-test validation suite (no JUnit)
├── input.txt                      # Sample text file (319 bytes)
├── big_input.txt                  # Large diverse English text file (~40 KB)
├── .gitignore                     # Git ignore rules (bin/, *.huff, temp files)
└── README.md                      # Complete documentation and usage guide
```

---

## The `.huff` File Format

Every `.huff` file begins with a 1-byte mode flag:

```
[ Mode Flag (1 byte) ] -> 0 = Stored Mode, 1 = Huffman Mode
```

### 1. Stored Mode (Flag 0)
Used when compressed size would be $\ge$ original UTF-8 size:
```
[ Byte 0 ] : 0x00 (Flag 0)
[ Bytes 1..N ] : Raw UTF-8 payload bytes
```

### 2. Huffman Mode (Flag 1)
Used when Huffman compression achieves net disk savings:
```
[ Byte 0 ]     : 0x01 (Flag 1)
[ Bytes 1..4 ] : Unique character count (int, 4 bytes)
[ Header Entries ]: For each unique character:
                 - Character : 2 bytes (char, UTF-16 code unit)
                 - Frequency : 1 to 5 bytes (unsigned 7-bit varint)
[ Padding ]    : Trailing unused bits in last payload byte (int, 4 bytes, 0-7)
[ Payload Len ]: Packed byte count (int, 4 bytes)
[ Payload Data]: Packed bitstream bytes
```

---

## Compile & Run Instructions

From the project root directory (`HuffmanCompressor/`):

### 1. Compile with Zero Warnings
```powershell
javac -Xlint:all -d bin src/huffman/*.java
```

### 2. Run Interactive Menu
```powershell
java -cp bin huffman.Main
```

### 3. Run Automated Test Suite
```powershell
java -cp bin huffman.Tests
```

---

## Full Test Suite Output

Running `java -cp bin huffman.Tests` executes 21 automated validation tests across 5 categories:

```
=================================================
        HuffmanCompressor Validation Suite       
=================================================

--- Part 1: Varint Unit Tests ---
Varint Unit: value = 0 (expect 1 byte)                     [PASS] - Round-tripped 0 exactly in 1 byte
Varint Unit: value = 1 (expect 1 byte)                     [PASS] - Round-tripped 1 exactly in 1 byte
Varint Unit: value = 127 (expect 1 byte)                   [PASS] - Round-tripped 127 exactly in 1 byte
Varint Unit: value = 128 (expect 2 bytes)                  [PASS] - Round-tripped 128 exactly in 2 bytes
Varint Unit: value = 16,383 (expect 2 bytes)               [PASS] - Round-tripped 16,383 exactly in 2 bytes
Varint Unit: value = 16,384 (expect 3 bytes)               [PASS] - Round-tripped 16,384 exactly in 3 bytes
Varint Unit: value = 2,147,483,647 (expect 5 bytes)        [PASS] - Round-tripped 2,147,483,647 exactly in 5 bytes

--- Part 2: Auto-Mode Consistency (Stored Mode Flag 0) ---
Auto-Mode: Empty file -> Flag 0                            [PASS] - Verified Flag 0 (Stored); content matches exactly (0 chars)
Auto-Mode: One-character file -> Flag 0                    [PASS] - Verified Flag 0 (Stored); content matches exactly (1 chars)
Auto-Mode: 5-character file -> Flag 0                      [PASS] - Verified Flag 0 (Stored); content matches exactly (5 chars)

--- Part 3: Forced Huffman Tree Edge Cases (Flag 1) ---
Forced Huffman: Empty file (null root)                     [PASS] - Verified Flag 1 (Huffman); content matches exactly (0 chars)
Forced Huffman: One-character file (code "0")              [PASS] - Verified Flag 1 (Huffman); content matches exactly (1 chars)
Forced Huffman: Single char repeated 1000 times            [PASS] - Verified Flag 1 (Huffman); content matches exactly (1000 chars)
Forced Huffman: Normal text                                [PASS] - Verified Flag 1 (Huffman); content matches exactly (340 chars)
Forced Huffman: Unicode characters and newlines            [PASS] - Verified Flag 1 (Huffman); content matches exactly (302 chars)

--- Part 4: Big-File Test (~40 KB, 3-Byte Varint) ---
Big-File: big_input.txt (~40 KB, 3-byte varint)            [PASS] - Byte-for-byte match! Orig: 40,161 B, Comp: 18,657 B (Hdr: 221 B, Payload: 18,436 B), Ratio: 0.4646, Saved: 53.54% [3-byte varint for ' ': 16,797]

--- Part 5: Edge-Case Safety & Corruption Guards ---
Error reading compressed file: File does not exist at 'non_existent_file_xyz.huff'.
Safety: Missing .huff file returns null                    [PASS] - Handled safely without crashing
Error reading compressed file: File 'test_temp\zero_byte.huff' is empty (0 bytes). A valid .huff file requires at least a 1-byte mode flag.
Safety: 0-byte .huff file returns null                     [PASS] - Handled safely without crashing
Error reading compressed file: Invalid or unsupported mode flag (99) in 'test_temp\corrupt_flag.huff'. Expected 0 (Stored) or 1 (Huffman).
Safety: Corrupt mode flag returns null                     [PASS] - Handled safely without crashing
Error reading compressed file: Truncated header in 'test_temp\truncated.huff'. Premature EOF while reading character/frequency entry 1 of 10.
Safety: Truncated header returns null                      [PASS] - Handled safely without crashing
Safety: CompressionStats division-by-zero guard            [PASS] - No NaN or Infinity present

-------------------------------------------------
Test Summary: 21 / 21 passed.
ALL TESTS PASSED SUCCESSFULLY! [PASS]
-------------------------------------------------
```

---

## Compression Results Comparison

| Metric | Small File (`input.txt`) | Large File (`big_input.txt`) |
| :--- | :--- | :--- |
| **Original File Size** | 319 bytes | 40,161 bytes |
| **Selected Mode** | Flag 1 (Huffman Mode) | Flag 1 (Huffman Mode) |
| **Header Size** | 106 bytes | 221 bytes (includes 3-byte varint for `' '`) |
| **Payload Size** | 170 bytes | 18,436 bytes |
| **Total Compressed Size** | **276 bytes** | **18,657 bytes** |
| **Compression Ratio** | **0.8652 (86.52%)** | **0.4646 (46.46%)** |
| **Space Saved** | **13.48%** | **53.54%** |

### Why Small Files Save Little (or None)
Every Huffman-compressed file must store the dictionary (unique characters and their frequencies) in its header so that the decompressor can reconstruct the binary tree. 
- For `input.txt` (319 bytes), the 106-byte header accounts for **33.2%** of the entire original file size. Although the bitstream payload reduces from 319 bytes to 170 bytes (46.75% bit reduction), adding the 106-byte header leaves a net file saving of only 13.48%.
- For very small files (e.g. 5 bytes), the header would be larger than the original text, triggering **Stored Mode (Flag 0)**.
- For `big_input.txt` (40,161 bytes), the 221-byte header represents only **0.55%** of the original size, allowing the ~54% payload compression to translate almost entirely into a **53.54% net file reduction**.
