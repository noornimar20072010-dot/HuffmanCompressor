package huffman;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts character frequencies from input text.
 */
public class FrequencyCounter {

    /**
     * Scans the given text and returns a map of each character to its frequency count.
     *
     * @param text the input string to analyze
     * @return map of character to occurrence count
     */
    public static Map<Character, Integer> countFrequencies(String text) {
        Map<Character, Integer> frequencies = new HashMap<>();
        if (text == null || text.isEmpty()) {
            return frequencies;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
        }

        return frequencies;
    }

    /**
     * Helper to display whitespace and control characters in a human-readable form.
     *
     * @param c character to format
     * @return readable representation of the character
     */
    public static String formatChar(char c) {
        return switch (c) {
            case '\n' -> "\\n (newline)";
            case '\r' -> "\\r (carriage return)";
            case '\t' -> "\\t (tab)";
            case ' '  -> "' ' (space)";
            default   -> "'" + c + "'";
        };
    }
}
