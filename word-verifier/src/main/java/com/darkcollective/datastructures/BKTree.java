package com.darkcollective.datastructures;

import java.util.*;

public class BKTree {
    private BKNode root;

    public void insert(String word) {
        if (root == null) {
            root = new BKNode(word);
        } else {
            insert(root, word);
        }
    }

    private void insert(BKNode node, String word) {
        int distance = levenshteinDistance(node.word, word);
        if (distance == 0) return; // Word already exists

        if (node.children.containsKey(distance)) {
            insert(node.children.get(distance), word);
        } else {
            node.children.put(distance, new BKNode(word));
        }
    }

    public List<String> search(String word, int maxDistance) {
        List<String> results = new ArrayList<>();
        if (root != null) {
            search(root, word, maxDistance, results);
        }
        // Sort by distance (closest first), then by length
        results.sort((a, b) -> {
            int distA = levenshteinDistance(word, a);
            int distB = levenshteinDistance(word, b);
            if (distA != distB) {
                return Integer.compare(distA, distB);
            }
            return Integer.compare(a.length(), b.length());
        });
        return results;
    }

    private void search(BKNode node, String word, int maxDistance, List<String> results) {
        int distance = levenshteinDistance(node.word, word);

        if (distance <= maxDistance && distance > 0) { // Don't include exact matches
            results.add(node.word);
        }

        // Search children within the possible distance range
        // The key insight: if we're looking for words within maxDistance of 'word',
        // and current node is at distance 'd' from 'word', then we only need to
        // explore children whose edge distance is between (d - maxDistance) and (d + maxDistance)
        int minChildDistance = Math.max(1, distance - maxDistance);
        int maxChildDistance = distance + maxDistance;

        for (Map.Entry<Integer, BKNode> entry : node.children.entrySet()) {
            int childEdgeDistance = entry.getKey();
            if (childEdgeDistance >= minChildDistance && childEdgeDistance <= maxChildDistance) {
                search(entry.getValue(), word, maxDistance, results);
            }
        }
    }

    private int levenshteinDistance(String s1, String s2) {
        if (s1.equals(s2)) return 0;
        if (s1.length() == 0) return s2.length();
        if (s2.length() == 0) return s1.length();

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        // Initialize first row and column
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        // Fill the matrix
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation needed
                } else {
                    dp[i][j] = 1 + Math.min(
                            Math.min(dp[i - 1][j],     // deletion
                                    dp[i][j - 1]),     // insertion
                            dp[i - 1][j - 1]          // substitution
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private static class BKNode {
        String word;
        Map<Integer, BKNode> children = new HashMap<>();

        BKNode(String word) {
            this.word = word;
        }
    }
}
