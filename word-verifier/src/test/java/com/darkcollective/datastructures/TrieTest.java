package com.darkcollective.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Trie Tests")
class TrieTest {
    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
    }

    @Nested
    class GeneralTests {
        @Test
        @DisplayName("Should properly remove words and clean up nodes")
        void shouldProperlyRemoveWords() {
            trie.insert("hello");
            assertTrue(trie.search("hello"), "Word should be found after insertion");
            assertEquals(1, trie.size(), "Size should be 1 after insertion");

            assertTrue(trie.remove("hello"), "Remove should return true for existing word");
            assertFalse(trie.search("hello"), "Word should not be found after removal");
            assertEquals(0, trie.size(), "Size should be 0 after removal");
            assertTrue(trie.isEmpty(), "Trie should be empty after removing only word");

            // Test that the tree structure is properly cleaned up
            TrieNode root = trie.getRoot(); // You might need to add a getter for root
            assertTrue(root.getChildren().isEmpty(), "Root should have no children after removal");
        }


        @Test
        @DisplayName("Should properly track statistics")
        void shouldCollectStatistics() {
            Trie.TrieStatistics stats = trie.getStatistics();
            assertEquals(0, stats.wordCount());
            assertEquals(0, stats.totalCharacters());
            assertEquals(0, stats.maxWordLength());


            trie.insert("hello");
            trie.insert("world");
            trie.insert("hello"); // duplicate should not affect counts

            stats = trie.getStatistics();
            assertEquals(2, stats.wordCount());
            assertEquals(10, stats.totalCharacters());
            assertEquals(5, stats.maxWordLength());

            // Additional verification
            assertEquals(2, trie.size());

            // Test removal
            trie.remove("hello");
            stats = trie.getStatistics();
            assertEquals(1, stats.wordCount());
            assertEquals(5, stats.totalCharacters());
            assertEquals(5, stats.maxWordLength());
        }

    }


    @Nested
    @DisplayName("Insertion Tests")
    class InsertionTests {
        @Test
        @DisplayName("Should insert and find a single word")
        void shouldInsertAndFindSingleWord() {
            trie.insert("hello");
            assertTrue(trie.search("hello"), "Should find inserted word");
        }

        @Test
        @DisplayName("Should handle multiple word insertions")
        void shouldHandleMultipleWordInsertions() {
            trie.insert("hello");
            trie.insert("world");
            trie.insert("help");

            assertTrue(trie.search("hello"), "Should find 'hello'");
            assertTrue(trie.search("world"), "Should find 'world'");
            assertTrue(trie.search("help"), "Should find 'help'");
        }

        @Test
        @DisplayName("Should handle duplicate insertions")
        void shouldHandleDuplicateInsertions() {
            trie.insert("hello");
            trie.insert("hello");
            assertTrue(trie.search("hello"), "Should find word after duplicate insertions");
        }

        @Test
        @DisplayName("Should handle empty string insertion")
        void shouldHandleEmptyStringInsertion() {
            trie.insert("");
            assertTrue(trie.search(""), "Should find empty string after insertion");
        }

        @Test
        @DisplayName("Should handle case-insensitive insertions")
        void shouldHandleCaseInsensitiveInsertions() {
            trie.insert("Hello");
            trie.insert("WORLD");

            assertTrue(trie.search("hello"), "Should find 'hello' in lowercase");
            assertTrue(trie.search("world"), "Should find 'world' in lowercase");
            assertTrue(trie.search("HELLO"), "Should find 'HELLO' in uppercase");
            assertTrue(trie.search("World"), "Should find 'World' in mixed case");
        }
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {
        @BeforeEach
        void setUpSearchTests() {
            trie.insert("hello");
            trie.insert("help");
            trie.insert("world");
            trie.insert("word");
        }

        @Test
        @DisplayName("Should find exact matches")
        void shouldFindExactMatches() {
            assertTrue(trie.search("hello"), "Should find exact match 'hello'");
            assertTrue(trie.search("help"), "Should find exact match 'help'");
            assertTrue(trie.search("world"), "Should find exact match 'world'");
        }

        @Test
        @DisplayName("Should not find non-existent words")
        void shouldNotFindNonExistentWords() {
            assertFalse(trie.search("hell"), "Should not find prefix 'hell'");
            assertFalse(trie.search("helping"), "Should not find extended word 'helping'");
            assertFalse(trie.search("xyz"), "Should not find non-existent word 'xyz'");
        }

        @Test
        @DisplayName("Should handle case-insensitive searches")
        void shouldHandleCaseInsensitiveSearches() {
            assertTrue(trie.search("HELLO"), "Should find uppercase 'HELLO'");
            assertTrue(trie.search("Help"), "Should find capitalized 'Help'");
            assertTrue(trie.search("WoRlD"), "Should find mixed case 'WoRlD'");
        }

        @Test
        @DisplayName("Should not find partial matches")
        void shouldNotFindPartialMatches() {
            assertFalse(trie.search("hel"), "Should not find partial word 'hel'");
            assertFalse(trie.search("wor"), "Should not find partial word 'wor'");
        }

    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String[] specialChars = {"hello!", "hello?", "hello-world", "hello@world"};

            for (String word : specialChars) {
                trie.insert(word);
                assertTrue(trie.search(word), "Should find word with special character: " + word);
            }
        }

        @Test
        @DisplayName("Should handle numeric characters")
        void shouldHandleNumericCharacters() {
            String[] numericWords = {"hello123", "123hello", "1234", "h3ll0"};

            for (String word : numericWords) {
                trie.insert(word);
                assertTrue(trie.search(word), "Should find word with numeric characters: " + word);
            }
        }

        @Test
        @DisplayName("Should handle whitespace")
        void shouldHandleWhitespace() {
            String[] wordsWithSpace = {"hello world", " hello", "hello ", "  "};

            for (String word : wordsWithSpace) {
                trie.insert(word);
                assertTrue(trie.search(word), "Should find word with whitespace: " + word);
            }
        }

        @Test
        @DisplayName("Should handle very long words")
        void shouldHandleVeryLongWords() {
            String longWord = "a".repeat(1000);
            trie.insert(longWord);
            assertTrue(trie.search(longWord), "Should find very long word");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        @Test
        @DisplayName("Should handle large number of insertions")
        void shouldHandleLargeNumberOfInsertions() {
            int numWords = 10000;
            for (int i = 0; i < numWords; i++) {
                String word = "word" + i;
                trie.insert(word);
            }

            assertTrue(trie.search("word9999"), "Should find last inserted word");
            assertFalse(trie.search("word10000"), "Should not find word beyond inserted range");
        }

        @Test
        @DisplayName("Should handle words with common prefixes")
        void shouldHandleWordsWithCommonPrefixes() {
            String[] prefixWords = {
                    "pre", "prefix", "prefixer", "prefixing",
                    "prefixed", "prefixable", "prefixation"
            };

            for (String word : prefixWords) {
                trie.insert(word);
            }

            for (String word : prefixWords) {
                assertTrue(trie.search(word), "Should find word with common prefix: " + word);
            }
            assertFalse(trie.search("pref"), "Should not find incomplete prefix");
        }
    }
}
