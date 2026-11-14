package huffman;

import java.util.HashMap;
import java.util.Map;

/**
 * Traverses a HuffmanTree recursively to produce prefix-free binary codes for each character.
 */
public class CodeGenerator {

    /**
     * Traverses the tree and returns a mapping from each character to its binary code string.
     *
     * @param tree the built HuffmanTree
     * @return Map of character to binary string (e.g. 'e' -> "101")
     */
    public static Map<Character, String> generateCodes(HuffmanTree tree) {
        Map<Character, String> codeMap = new HashMap<>();
        if (tree == null || tree.getRoot() == null) {
            return codeMap;
        }

        HuffmanNode root = tree.getRoot();

        // Edge case: Root is itself a leaf (single character)
        if (root.isLeaf()) {
            codeMap.put(root.character, "0");
            return codeMap;
        }

        // Edge case: Single character wrapped with a left child only
        if (root.left != null && root.left.isLeaf() && root.right == null) {
            codeMap.put(root.left.character, "0");
            return codeMap;
        }

        // Recursive tree traversal
        traverse(root, "", codeMap);
        return codeMap;
    }

    private static void traverse(HuffmanNode node, String currentCode, Map<Character, String> codeMap) {
        if (node == null) {
            return;
        }

        // When a leaf is reached, register its accumulated binary code
        if (node.isLeaf()) {
            codeMap.put(node.character, currentCode.isEmpty() ? "0" : currentCode);
            return;
        }

        // Traverse left branch with '0'
        traverse(node.left, currentCode + "0", codeMap);

        // Traverse right branch with '1'
        traverse(node.right, currentCode + "1", codeMap);
    }
}
