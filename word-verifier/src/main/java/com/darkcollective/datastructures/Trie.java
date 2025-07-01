package com.darkcollective.datastructures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Trie {
    private final TrieNode root;
    private final boolean caseSensitive;

    private int wordCount;
    private int totalCharacters;
    private int maxWordLength;

    public Trie() {
        this(false); // default to case-insensitive
    }

    public Trie(boolean caseSensitive) {
        this.root = new TrieNode();
        this.caseSensitive = caseSensitive;

        this.wordCount = 0;
        this.totalCharacters = 0;
        this.maxWordLength = 0;
    }

    protected TrieNode getRoot() {
        return root;
    }

    private String normalizeWord(String word) {
        return caseSensitive ? word : word.toLowerCase();
    }

    public int size() {
        return wordCount;
    }

    // Add for testing/verification
    public boolean isEmpty() {
        return root.getChildren().isEmpty();
    }

    // Add for prefix searching
    public boolean hasPrefix(String prefix) {
        TrieNode node = findNode(prefix.toLowerCase());
        return node != null;
    }

    // Add for getting all words with a prefix
    public List<String> getWordsWithPrefix(String prefix) {
        List<String> words = new ArrayList<>();
        TrieNode node = findNode(prefix.toLowerCase());
        if (node != null) {
            collectWords(node, prefix.toLowerCase(), words);
        }
        return words;
    }

    private void collectWords(TrieNode node, String prefix, List<String> words) {
        if (node.isEndOfWord()) {
            words.add(prefix);
        }
        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            collectWords(entry.getValue(), prefix + entry.getKey(), words);
        }
    }

    // Helper method for finding nodes
    private TrieNode findNode(String prefix) {
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            if (!current.getChildren().containsKey(ch)) {
                return null;
            }
            current = current.getChildren().get(ch);
        }
        return current;
    }

    // Add ability to remove words
    public boolean remove(String word) {
        Objects.requireNonNull(word, "Word cannot be null");
        String normalizedWord = normalizeWord(word);
        boolean result = removeWord(root, normalizedWord, 0);
        if (result) {
            wordCount--;
            totalCharacters -= word.length();
        }
        return result;
    }

    private boolean removeWord(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord()) {
                return false;
            }
            current.setEndOfWord(false);
            return true;
        }

        char ch = word.charAt(index);
        TrieNode child = current.getChildren().get(ch);
        if (child == null) {
            return false;
        }

        boolean shouldRemove = removeWord(child, word, index + 1);

        // If child should be removed, and it has no children, and it's not end of another word
        if (shouldRemove && child.getChildren().isEmpty() && !child.isEndOfWord()) {
            current.getChildren().remove(ch);
        }

        return shouldRemove;
    }


    public void insert(String word) {
        Objects.requireNonNull(word, "Word cannot be null");
        if (!search(word)) {
            wordCount++;
            totalCharacters += word.length();
            maxWordLength = Math.max(maxWordLength, word.length());
        }

        TrieNode current = root;
        for (char ch : normalizeWord(word).toCharArray()) {
            current.getChildren().putIfAbsent(ch, new TrieNode());
            current = current.getChildren().get(ch);
        }
        current.setEndOfWord(true);
    }

    public boolean search(String word) {
        Objects.requireNonNull(word, "Word cannot be null");
        TrieNode node = findNode(word.toLowerCase());
        return node != null && node.isEndOfWord();
    }

    public TrieStatistics getStatistics() {
        return new TrieStatistics(wordCount, totalCharacters, maxWordLength);
    }

    public record TrieStatistics(int wordCount, int totalCharacters, int maxWordLength) {
    }
}

