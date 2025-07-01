package com.darkcollective.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BKTree Tests")
class BKTreeTest {
    private BKTree bkTree;
    private WordDistance distanceCalculator;

    @BeforeEach
    void setUp() {
        distanceCalculator = new LevenshteinDistance();
        bkTree = new BKTree(distanceCalculator);
    }


    @Nested
    @DisplayName("Insertion Tests")
    class InsertionTests {
        @Test
        @DisplayName("Should insert first word as root")
        void shouldInsertFirstWordAsRoot() {
            bkTree.insert("hello");
            List<String> results = bkTree.search("hello", 0);
            assertEquals(0, results.size(), "Exact match should not be included in results");
        }

        @Test
        @DisplayName("Should handle duplicate words")
        void shouldHandleDuplicateWords() {
            bkTree.insert("hello");
            bkTree.insert("hello");
            List<String> results = bkTree.search("hell", 1);
            assertEquals(1, results.size(), "Should find one similar word");
            assertTrue(results.contains("hello"), "Should find 'hello'");
        }

        @Test
        @DisplayName("Should handle empty string insertion")
        void shouldHandleEmptyString() {
            bkTree.insert("");
            bkTree.insert("test");
            List<String> results = bkTree.search("test", 1);
            assertEquals(0, results.size(), "Should not find exact match");
        }
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {
        @BeforeEach
        void setUpSearchTests() {
            bkTree.insert("hello");
            bkTree.insert("world");
            bkTree.insert("help");
            bkTree.insert("hell");
            bkTree.insert("yellow");
        }

        @Test
        @DisplayName("Should find similar words within distance 1")
        void shouldFindWordsWithinDistanceOne() {
            List<String> results = bkTree.search("hell", 1);
            assertTrue(results.contains("hello"), "Should find 'hello'");
            assertTrue(results.contains("help"), "Should find 'help'");
            assertEquals(2, results.size(), "Should find exactly 2 words");
        }

        @Test
        @DisplayName("Should find words within distance 2")
        void shouldFindWordsWithinDistanceTwo() {
            List<String> results = bkTree.search("helo", 2);
            assertTrue(results.contains("hello"), "Should find 'hello'");
            assertTrue(results.contains("help"), "Should find 'help'");
            assertTrue(results.contains("hell"), "Should find 'hell'");
        }

        @Test
        @DisplayName("Should return empty list for exact match")
        void shouldReturnEmptyListForExactMatch() {
            List<String> results = bkTree.search("hello", 1);
            assertFalse(results.contains("hello"), "Should not include exact match");
        }

        @Test
        @DisplayName("Should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatches() {
            List<String> results = bkTree.search("xyz", 1);
            assertTrue(results.isEmpty(), "Should return empty list when no matches found");
        }

        @Test
        @DisplayName("Should return sorted results")
        void shouldReturnSortedResults() {
            bkTree.insert("helps");
            List<String> results = bkTree.search("helt", 2);
            assertTrue(results.contains("help"), "Should find 'help'");
            assertTrue(results.contains("hell"), "Should find 'hell'");
            assertEquals("hell", results.get(0), "Closest match should be first");
        }
    }

    @Nested
    @DisplayName("Levenshtein Distance Tests")
    class LevenshteinDistanceTests {
        @Test
        @DisplayName("Should calculate distance between identical strings")
        void shouldCalculateDistanceBetweenIdenticalStrings() {
            assertEquals(0,distanceCalculator.calculate("hello", "hello"));
        }

        @Test
        @DisplayName("Should calculate distance with empty strings")
        void shouldCalculateDistanceWithEmptyStrings() {
            assertEquals(5,distanceCalculator.calculate("hello", ""));
            assertEquals(5,distanceCalculator.calculate("", "world"));
            assertEquals(0,distanceCalculator.calculate("", ""));
        }

        @Test
        @DisplayName("Should calculate distance for single character difference")
        void shouldCalculateDistanceForSingleCharacterDifference() {
            assertEquals(1,distanceCalculator.calculate("hello", "helo"));
            assertEquals(1,distanceCalculator.calculate("hello", "hell"));
            assertEquals(1,distanceCalculator.calculate("hello", "hello1"));
        }

        @Test
        @DisplayName("Should calculate distance for multiple character differences")
        void shouldCalculateDistanceForMultipleCharacterDifferences() {
            assertEquals(1,distanceCalculator.calculate("hello", "hllo"));
            assertEquals(2,distanceCalculator.calculate("hello", "help"));
            assertEquals(4,distanceCalculator.calculate("hello", "world"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle null root")
        void shouldHandleNullRoot() {
            List<String> results = bkTree.search("test", 1);
            assertTrue(results.isEmpty(), "Empty tree should return empty results");
        }

        @Test
        @DisplayName("Should handle case sensitivity")
        void shouldHandleCaseSensitivity() {
            bkTree.insert("Hello");
            bkTree.insert("WORLD");
            List<String> results = bkTree.search("hello", 1);
            assertFalse(results.isEmpty(), "Should find case-insensitive matches");
        }

        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            bkTree.insert("hello!");
            bkTree.insert("hello?");
            List<String> results = bkTree.search("hello", 1);
            assertEquals(2, results.size(), "Should find words with special characters");
        }
    }
}