# HuffmanCompressor

A complete, robust, self-contained **Java 17** implementation of the Huffman text compression and decompression engine built with **zero external libraries** and **no build tools** (plain `javac`). Features dual interfaces (Java Swing GUI and Console CLI), an in-memory service architecture, and an automated 29-test validation suite.

---

## Features

- **Dual Graphical & CLI Interfaces**: Launch either a rich Java Swing desktop GUI (`GuiMain`) or an interactive terminal menu (`Main`).
- **In-Memory Service Layer**: `HuffmanService` provides non-blocking, thread-safe methods for in-memory compression, decompression, structural analysis, and automatic round-trip verification (`HuffmanResult`).
- **Interactive Visualizations**:
  - **Code Table**: Sortable `JTable` displaying character glyphs, frequencies, variable-length binary prefix codes, and code lengths.
  - **Huffman Tree**: Custom zoomable and scrollable binary tree canvas with labeled branches (`0` left, `1` right), character/frequency leaves, and combined internal nodes.
  - **Frequency Chart**: Clean bar chart of top 20 character frequencies with exact counts and percentages.
  - **Live Statistics**: Real-time breakdown of original size, compressed size, metadata header size, payload size, compression ratio, space saved %, and round-trip verification status.
- **Dual-Mode File Storage**:
  - **Flag 1 (Huffman Mode)**: Compresses text using optimal prefix codes when net disk savings are achieved.
  - **Flag 0 (Stored Mode)**: Stores raw UTF-8 bytes when Huffman metadata overhead would exceed payload savings, ensuring files never expand by more than 1 byte (the mode flag overhead).
- **Varint Metadata Encoding**: Frequency table entries are encoded using unsigned 7-bit variable-length integers (1–5 bytes) instead of standard 4-byte integers, minimizing header overhead.
- **Asynchronous SwingWorker Architecture**: Long-running operations run in the background, keeping the user interface completely responsive on large inputs.
- **Robust Safety & Corruption Guards**: Built-in validation guarding against 0-byte files, truncated headers, missing files, corrupted bitstreams, and division-by-zero on empty inputs.

---

## How Huffman Coding Works in Simple Words

1. **Count Frequencies**: Count how many times each character appears in the text ($O(n)$ time).
2. **Build a Min-Heap**: Treat every unique character as a standalone leaf node and place them into a priority queue ordered by ascending frequency.
3. **Merge Bottom-Up**: Repeatedly remove the two lowest-frequency nodes, join them under a new parent node whose frequency is their sum, and reinsert the parent back into the queue. Repeat until one root node remains ($O(k \log k)$ time for $k$ unique characters).
4. **Assign Binary Codes**: Starting from the root, going left adds a `0` and going right adds a `1`. Frequent characters (such as space or `'e'`) remain near the top with short codes (2–4 bits), while rare characters end up deeper with longer codes.
5. **Pack Bits & Save Dual-Mode Header**: Pack the variable-length bits into 8-bit bytes. If the prospective compressed file (header + payload) is smaller than the original text, save it with **Flag 1 (Huffman Mode)**; otherwise save the raw UTF-8 bytes with **Flag 0 (Stored Mode)** so files never expand by more than 1 byte.

---

## Time and Space Complexity

- **Frequency Counting**: $\mathcal{O}(n)$, where $n$ is the total number of characters in the input text. Performs a single linear pass over the text.
- **Tree Construction**: $\mathcal{O}(k \log k)$, where $k$ is the number of unique characters ($k \le 65{,}536$, typically $k \le 128$ for English/ASCII). The min-heap (`PriorityQueue`) holds $k$ elements, and each of the $k-1$ merge operations takes $\mathcal{O}(\log k)$ time.
- **Prefix Code Generation**: $\mathcal{O}(k)$, recursively traversing the $2k-1$ nodes of the Huffman tree.
- **Bitstream Encoding**: $\mathcal{O}(n)$, replacing each character with its precomputed binary code string and bit-packing into bytes.
- **Decompression**: $\mathcal{O}(n)$, walking down the binary tree from the root for each bit in the payload to restore the original character.
- **Overall Time Complexity**: $\mathcal{O}(n + k \log k)$, which runs in near-linear time with respect to the input text size.
- **Space Complexity**: $\mathcal{O}(k)$ auxiliary memory for the tree and frequency tables, and $\mathcal{O}(n)$ memory for the input and output byte arrays.

---

## Project Structure

```
HuffmanCompressor/
├── src/
│   └── huffman/
│       ├── CodeGenerator.java        # Recursive tree traversal generating prefix codes
│       ├── CompressionStats.java     # Metrics calculation (ratios, space saved, zero-guard)
│       ├── Compressor.java           # Encodes text into bit strings and packs them into bytes
│       ├── Decompressor.java         # Restores text from Huffman bitstream or Stored UTF-8 bytes
│       ├── FileHandler.java          # Dual-mode .huff I/O, varint encoding, shared streaming, and corruption guards
│       ├── FrequencyChartPanel.java  # Custom Swing JPanel rendering top 20 character frequencies bar chart
│       ├── FrequencyCounter.java     # Character occurrence frequency counter
│       ├── GuiMain.java              # Entry point for the Swing Graphical User Interface
│       ├── HuffmanException.java     # Custom exception for typed compression/decompression error reporting
│       ├── HuffmanNode.java          # Binary tree node (character, frequency, left, right, getters)
│       ├── HuffmanResult.java        # Immutable data container for compression results and verification
│       ├── HuffmanService.java       # High-level in-memory facade for compression, analysis, and file helpers
│       ├── HuffmanTree.java          # Tree builder using PriorityQueue (handles edge cases)
│       ├── Main.java                 # Interactive CLI menu loop (resilient to bad paths and inputs)
│       ├── MainFrame.java            # Main Swing window with toolbar, split view, tabs, and status bar
│       ├── Tests.java                # Comprehensive 29-test validation suite (no JUnit)
│       └── TreePanel.java            # Custom Swing JPanel rendering the Huffman tree with zoom controls
├── input.txt                         # Short sample text file (319 bytes)
├── natural_input.txt                 # Authentic natural English prose (~42 KB from Arthur Conan Doyle)
├── big_input.txt                     # Synthetic test file (~40 KB) exercising 3-byte varint frequencies
├── .gitignore                        # Git ignore rules (bin/, *.huff, temp files)
└── README.md                         # Complete documentation and usage guide
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
[ Byte 0 ]     : 0x00 (Flag 0)
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

### 2. Run Graphical User Interface (GUI)
```powershell
java -cp bin huffman.GuiMain
```

### 3. Run Interactive Console Menu (CLI)
```powershell
java -cp bin huffman.Main
```

### 4. Run Automated Test Suite (29 Tests)
```powershell
java -cp bin huffman.Tests
```

---

## Full Test Suite Output

Running `java -cp bin huffman.Tests` executes 29 automated validation tests across 6 categories:

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

--- Part 4: Benchmark Tests (Natural Text & Synthetic Big-File) ---
Natural-Text: natural_input.txt (~42 KB real prose)        [PASS] - Byte-for-byte match! Orig: 42,967 B, Comp: 24,363 B (Hdr: 256 B, Payload: 24,107 B), Ratio: 0.5670, Saved: 43.30%
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

--- Part 6: HuffmanService In-Memory Validation ---
Service: Round-trip normal text (Huffman Mode)             [PASS] - Verified Flag 1! 504 chars -> 336 bytes (Saved: 33.33%)
Service: Round-trip empty text (Stored Mode)               [PASS] - Verified empty text round-trip (Flag 0 Stored, 1 byte)
Service: Round-trip one-character text (Stored Mode)       [PASS] - Verified single char round-trip ('A' restored exactly, Flag 0)
Service: Round-trip Unicode & emoji (Huffman Mode)         [PASS] - Verified Flag 1! 675 chars (1020 B -> 502 B, Saved: 50.78%)
Service: HuffmanResult values match CompressionStats       [PASS] - Parity verified (Flag 1, Ratio: 0.6920, Saved: 30.80%)
Service: natural_input.txt benchmark (expect 24,363 B)     [PASS] - Byte-for-byte match! Orig: 42,967 B, Comp: 24,363 B (expected 24,363 B), Saved: 43.30%
Service: Corrupt byte[] throws HuffmanException            [PASS] - Caught expected HuffmanException: Invalid or unsupported mode flag (99) in 'memory buffer'. Expected 0 (Stored) or 1 (Huffman).

-------------------------------------------------
Test Summary: 29 / 29 passed.
ALL TESTS PASSED SUCCESSFULLY! [PASS]
-------------------------------------------------
```

---

## Benchmark Results Comparison

| Metric | Short Sample (`input.txt`) | Natural Prose (`natural_input.txt`) | Synthetic Test (`big_input.txt`) |
| :--- | :--- | :--- | :--- |
| **Description** | 4-sentence overview | Authentic English prose (*Sherlock Holmes*) | Synthetic text to exercise 3-byte varints |
| **Original File Size** | 319 bytes | **42,967 bytes** (~42 KB) | 40,161 bytes (~40 KB) |
| **Selected Mode** | Flag 1 (Huffman) | **Flag 1 (Huffman)** | Flag 1 (Huffman) |
| **Header Size** | 106 bytes | **256 bytes** | 221 bytes (exercises 3-byte varint for `' '`) |
| **Payload Size** | 170 bytes | **24,107 bytes** | 18,436 bytes |
| **Compressed Size** | **276 bytes** | **24,363 bytes** | **18,657 bytes** |
| **Compression Ratio** | **0.8652 (86.52%)** | **0.5670 (56.70%)** | **0.4646 (46.46%)** |
| **Space Saved** | **13.48%** | **43.30%** | **53.54%** |

> **Note on Line Endings (CRLF vs. LF)**: On Windows systems, text files cloned or created with Windows-style CRLF (`\r\n`) line endings consume 2 bytes per line break instead of 1 byte (LF `\n`). Consequently, `input.txt` may measure 323 bytes rather than 319 bytes, and its exact compression ratio may slightly differ (~85.4% vs. 86.5%) from the Unix-standard LF benchmark table above.

### Benchmark Analysis: Natural English vs. Small & Synthetic Files
- **Natural English (`natural_input.txt`)**: Represents realistic performance on authentic English literature. Character distributions follow standard English letter frequencies, resulting in a typical Huffman space savings of **43.30%** (compression ratio of `0.5670`).
- **Short Sample (`input.txt`)**: For short files (319 bytes), the 106-byte dictionary header accounts for **33.2%** of the original size. Although the payload itself shrinks by 46.75%, the fixed metadata overhead leaves a net file saving of **13.48%**. For files smaller than ~25 bytes, this overhead exceeds bit savings entirely, triggering **Stored Mode (Flag 0)**.
- **Synthetic Test (`big_input.txt`)**: Specifically designed to stress-test 3-byte varint deserialization by deliberately inflating space occurrence to 16,797 ($\ge 16,384$). Because the space character dominates over 40% of the text, it is assigned a very short Huffman code (1–2 bits), yielding an artificially high compression ratio (53.54%) compared to natural English.
