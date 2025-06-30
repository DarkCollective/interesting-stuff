package com.darkcollective.wordverifier;

import com.darkcollective.datastructures.BKTree;
import com.darkcollective.datastructures.Trie;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class VocabularyService {
    private Trie trie;
    private BKTree bkTree;

    @PostConstruct
    public void init() throws IOException {
        trie = new Trie();
        bkTree = new BKTree();
        loadVocabulary();
    }

    private void loadVocabulary() throws IOException {
        ClassPathResource resource = new ClassPathResource("vocabulary.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String word;
            while ((word = reader.readLine()) != null) {
                word = word.trim().toLowerCase();
                if (!word.isEmpty()) {
                    trie.insert(word);
                    bkTree.insert(word);
                }
            }
        }
    }

    public boolean isValidWord(String word) {
        return trie.search(word.toLowerCase());
    }

    public List<String> findClosestMatches(String word, int maxDistance) {
        List<String> matches = bkTree.search(word.toLowerCase(), maxDistance);
        // Limit to top 5 matches and sort by length (shorter words first as they're likely closer)
        return matches.stream()
                .sorted((a, b) -> Integer.compare(a.length(), b.length()))
                .limit(5)
                .toList();
    }
}
