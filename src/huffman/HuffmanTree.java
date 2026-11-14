package huffman;

import java.util.Map;
import java.util.PriorityQueue;

/**
 * Builds and stores the Huffman binary tree from a character frequency table.
 *
 * Huffman Coding Algorithm:
 * ------------------------
 * 1. Initialize a min-priority queue (min-heap) where every unique character is
 *    represented as a standalone leaf node, prioritized by its occurrence frequency.
 *
 * 2. Iteratively merge the two nodes with the lowest frequencies:
 *    - Remove the two lowest-frequency nodes (n1, n2).
 *    - Create a new internal node whose frequency is the sum of (n1.frequency + n2.frequency),
 *      with n1 as its left child ('0') and n2 as its right child ('1').
 *    - Insert this new parent node back into the priority queue.
 *
 * 3. Repeat Step 2 until only one node remains in the queue.
 *    This final node becomes the root of the complete Huffman tree.
 *
 * Edge Cases Handled:
 * -------------------
 * - Empty text: The frequency map is empty, resulting in a null root.
 * - Single unique character: If only one character exists, it cannot be paired with a second
 *   node. We create a dummy parent node with the unique leaf as its left child ('0') and a null
 *   right child. This ensures a valid tree path that consistently encodes the character as "0".
 */
public class HuffmanTree {

    private final HuffmanNode root;

    /**
     * Builds a Huffman tree from the provided character frequency map.
     *
     * @param frequencies map of character occurrence counts
     */
    public HuffmanTree(Map<Character, Integer> frequencies) {
        this.root = buildTree(frequencies);
    }

    /**
     * Constructs a Huffman tree directly from an existing root node.
     *
     * @param root the root node of the tree
     */
    public HuffmanTree(HuffmanNode root) {
        this.root = root;
    }

    /**
     * Returns the root node of this Huffman tree.
     *
     * @return the root HuffmanNode, or null if the tree is empty
     */
    public HuffmanNode getRoot() {
        return root;
    }

    private HuffmanNode buildTree(Map<Character, Integer> frequencies) {
        // Edge Case 1: Empty text / no frequencies
        if (frequencies == null || frequencies.isEmpty()) {
            return null;
        }

        // Step 1: Place each character leaf into the min-priority queue
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        // Edge Case 2: Only one unique character in the text
        if (pq.size() == 1) {
            HuffmanNode singleLeaf = pq.poll();
            // Wrap in a parent node where left child produces bit '0'
            return new HuffmanNode(singleLeaf.frequency, singleLeaf, null);
        }

        // Step 2 & 3: Merge lowest two nodes until a single root remains
        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();   // Lowest frequency node -> branch '0'
            HuffmanNode right = pq.poll();  // Second lowest frequency node -> branch '1'

            int combinedFrequency = left.frequency + right.frequency;
            HuffmanNode parent = new HuffmanNode(combinedFrequency, left, right);
            pq.add(parent);
        }

        return pq.poll(); // Final remaining node is the root
    }
}
