package com.darkcollective.datastructures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BKTree {
    private static final int MAX_RESULTS = 5;
    private final WordDistance distanceCalculator;
    private BKNode root;

    public BKTree() {
        this(new LevenshteinDistance());
    }

    public BKTree(WordDistance distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public void insert(String word) {
        if (root == null) {
            root = new BKNode(word);
        } else {
            insertRecursive(root, word);
        }
    }

    private void insertRecursive(BKNode node, String word) {
        int distance = distanceCalculator.calculate(node.getWord(), word);
        if (distance == 0) return;

        if (node.hasChild(distance)) {
            insertRecursive(node.getChild(distance), word);
        } else {
            node.addChild(distance, new BKNode(word));
        }
    }

    public List<String> search(String word, int maxDistance) {
        List<String> results = new ArrayList<>();
        if (root != null) {
            searchSimilarWords(root, word, maxDistance, results);
        }
        return sortResults(results, word);
    }

    private void searchSimilarWords(BKNode node, String word, int maxDistance, List<String> results) {
        int distance = distanceCalculator.calculate(node.getWord(), word);
        if (distance <= maxDistance && distance > 0) {
            results.add(node.getWord());
        }

        int minChildDistance = Math.max(1, distance - maxDistance);
        int maxChildDistance = distance + maxDistance;

        node.getChildren().forEach((childDistance, childNode) -> {
            if (childDistance >= minChildDistance && childDistance <= maxChildDistance) {
                searchSimilarWords(childNode, word, maxDistance, results);
            }
        });
    }

    private List<String> sortResults(List<String> results, String targetWord) {
        return results.stream().sorted(Comparator.<String, Integer>comparing(word -> distanceCalculator.calculate(targetWord, word)).thenComparing(String::length)).limit(MAX_RESULTS).toList();
    }
}
