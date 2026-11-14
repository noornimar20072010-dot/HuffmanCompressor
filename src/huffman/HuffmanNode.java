package huffman;

/**
 * Represents a node within the Huffman tree.
 * Can be a leaf node (containing a character) or an internal node (combining child frequencies).
 */
public class HuffmanNode implements Comparable<HuffmanNode> {

    public final Character character; // null for internal routing nodes
    public final int frequency;
    public final HuffmanNode left;
    public final HuffmanNode right;

    /**
     * Leaf node constructor.
     *
     * @param character the character stored in this leaf
     * @param frequency occurrence count of the character
     */
    public HuffmanNode(char character, int frequency) {
        this.character = character;
        this.frequency = frequency;
        this.left = null;
        this.right = null;
    }

    /**
     * Internal node constructor.
     *
     * @param frequency combined frequency of left and right subtrees
     * @param left      left child (representing bit '0')
     * @param right     right child (representing bit '1')
     */
    public HuffmanNode(int frequency, HuffmanNode left, HuffmanNode right) {
        this.character = null;
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    /**
     * Checks if this node is a leaf (has no children and holds a character).
     *
     * @return true if this node is a leaf, false otherwise
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }

    /**
     * Orders nodes by ascending frequency for use in a PriorityQueue (min-heap).
     *
     * @param other the other HuffmanNode to compare against
     * @return negative if this node has lower frequency, positive if higher, 0 if equal
     */
    @Override
    public int compareTo(HuffmanNode other) {
        return Integer.compare(this.frequency, other.frequency);
    }
}
