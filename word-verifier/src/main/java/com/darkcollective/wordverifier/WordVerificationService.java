package com.darkcollective.wordverifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WordVerificationService {

    @Autowired
    private VocabularyService vocabularyService;

    public String verifyWords(String input) {
        // Parse words from input (split by whitespace and remove punctuation)
  // TODO doesn't look like it handles accented characters.
        List<String> words = Arrays.stream(input.split("\\s+"))
                .map(word -> word.replaceAll("[^a-zA-Z]", ""))
                .filter(word -> !word.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (vocabularyService.isValidWord(word)) {
                // Word found - use checkmark
                result.append("✓ ").append(word).append("\n");
            } else {
                // Word not found - use X mark and find closest matches
                List<String> closestMatches = vocabularyService.findClosestMatches(word, 2);
                result.append("✘ ").append(word).append("; ");
                result.append(String.join(", ", closestMatches));
                result.append("\n");
            }
        }

        return result.toString().trim();
    }
}