package com.darkcollective.sqlparser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("SQL Tokenizer Test Suite")
class SqlTokenizerTest {

    @Nested
    @DisplayName("Basic Tokenization")
    class BasicTokenizationTests {

        @Test
        @DisplayName("Tokenize simple SELECT statement")
        void testSimpleSelect() {
            String sql = "SELECT name FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();
            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize SELECT with multiple columns")
        void testSelectMultipleColumns() {
            String sql = "SELECT name, age, email FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", ",", "age", ",", "email", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Handle case sensitivity")
        void testCaseSensitivity() {
            String sql = "select Name from Users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("select", "Name", "from", "Users"), tokens);
        }
    }

    @Nested
    @DisplayName("String Literal Tokenization")
    class StringLiteralTests {

        @Test
        @DisplayName("Tokenize single-quoted strings")
        void testSingleQuotedStrings() {
            String sql = "SELECT 'Hello World' FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "'Hello World'", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize double-quoted strings")
        void testDoubleQuotedStrings() {
            String sql = "SELECT \"Hello World\" FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "\"Hello World\"", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Handle escaped quotes")
        void testEscapedQuotes() {
            String sql = "SELECT 'John''s Data' FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "'John''s Data'", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Handle empty strings")
        void testEmptyStrings() {
            String sql = "SELECT '' FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "''", "FROM", "users"), tokens);
        }
    }

    @Nested
    @DisplayName("Numeric Literal Tokenization")
    class NumericLiteralTests {

        @Test
        @DisplayName("Tokenize integers")
        void testIntegers() {
            String sql = "SELECT 123 FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "123", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize decimals")
        void testDecimals() {
            String sql = "SELECT 123.456 FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "123.456", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize numbers in expressions")
        void testNumbersInExpressions() {
            String sql = "SELECT 1 + 2.5 * 3 FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "1", "+", "2.5", "*", "3", "FROM", "users"), tokens);
        }
    }

    @Nested
    @DisplayName("Operator Tokenization")
    class OperatorTests {

        @Test
        @DisplayName("Tokenize arithmetic operators")
        void testArithmeticOperators() {
            String sql = "SELECT a + b - c * d / e % f FROM table1";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "a", "+", "b", "-", "c", "*", "d", "/", "e", "%", "f", "FROM", "table1"), tokens);
        }

        @Test
        @DisplayName("Tokenize comparison operators")
        void testComparisonOperators() {
            String sql = "SELECT * FROM users WHERE a = b AND c != d AND e <> f AND g < h AND i > j AND k <= l AND m >= n";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.contains("="));
            assertTrue(tokens.contains("!="));
            assertTrue(tokens.contains("<>"));
            assertTrue(tokens.contains("<"));
            assertTrue(tokens.contains(">"));
            assertTrue(tokens.contains("<="));
            assertTrue(tokens.contains(">="));
        }

        @Test
        @DisplayName("Tokenize string concatenation operator")
        void testStringConcatenation() {
            String sql = "SELECT name || ' - ' || email FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.contains("||"));
        }
    }

    @Nested
    @DisplayName("Delimiter Tokenization")
    class DelimiterTests {

        @Test
        @DisplayName("Tokenize parentheses")
        void testParentheses() {
            String sql = "SELECT COUNT(*) FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "COUNT", "(", "*", ")", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize function calls with multiple arguments")
        void testFunctionWithMultipleArgs() {
            String sql = "SELECT SUBSTR(name, 1, 5) FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "SUBSTR", "(", "name", ",", "1", ",", "5", ")", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize qualified column names")
        void testQualifiedColumnNames() {
            String sql = "SELECT u.name, p.title FROM users u";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "u", ".", "name", ",", "p", ".", "title", "FROM", "users", "u"), tokens);
        }
    }

    @Nested
    @DisplayName("Complex Expression Tokenization")
    class ComplexExpressionTests {

        @Test
        @DisplayName("Tokenize mathematical expressions")
        void testMathematicalExpressions() {
            String sql = "SELECT 1*2+10 AS number FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "1", "*", "2", "+", "10", "AS", "number", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize string concatenation expressions")
        void testStringConcatenationExpressions() {
            String sql = "SELECT \"STRING\"+\"String\" AS string FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "\"STRING\"", "+", "\"String\"", "AS", "string", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Tokenize complex WHERE clause")
        void testComplexWhereClause() {
            String sql = "SELECT * FROM users WHERE (age > 18 AND status = 'active') OR name LIKE 'John%'";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.contains("("));
            assertTrue(tokens.contains(")"));
            assertTrue(tokens.contains("AND"));
            assertTrue(tokens.contains("OR"));
            assertTrue(tokens.contains("LIKE"));
            assertTrue(tokens.contains("'active'"));
            assertTrue(tokens.contains("'John%'"));
        }
    }

    @Nested
    @DisplayName("Comment Handling")
    class CommentTests {

        @Test
        @DisplayName("Skip line comments")
        void testLineComments() {
            String sql = "SELECT name -- This is a comment\nFROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Skip block comments")
        void testBlockComments() {
            String sql = "SELECT name /* This is a block comment */ FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Skip multi-line block comments")
        void testMultiLineBlockComments() {
            String sql = "SELECT name /* This is a\nmulti-line\ncomment */ FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }
    }

    @Nested
    @DisplayName("Whitespace Handling")
    class WhitespaceTests {

        @Test
        @DisplayName("Handle multiple spaces")
        void testMultipleSpaces() {
            String sql = "SELECT    name     FROM    users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }

        @Test
        @DisplayName("Handle tabs and newlines")
        void testTabsAndNewlines() {
            String sql = "SELECT\tname\nFROM\tusers";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertEquals(List.of("SELECT", "name", "FROM", "users"), tokens);
        }
    }

    @Nested
    @DisplayName("Token Classification Tests")
    class TokenClassificationTests {

        @Test
        @DisplayName("Identify keywords correctly")
        void testKeywordIdentification() {
            assertTrue(SqlTokenizer.isKeyword("SELECT"));
            assertTrue(SqlTokenizer.isKeyword("select"));
            assertTrue(SqlTokenizer.isKeyword("FROM"));
            assertTrue(SqlTokenizer.isKeyword("WHERE"));
            assertFalse(SqlTokenizer.isKeyword("name"));
            assertFalse(SqlTokenizer.isKeyword("users"));
        }

        @Test
        @DisplayName("Identify operators correctly")
        void testOperatorIdentification() {
            assertTrue(SqlTokenizer.isOperator("+"));
            assertTrue(SqlTokenizer.isOperator("="));
            assertTrue(SqlTokenizer.isOperator("!="));
            assertTrue(SqlTokenizer.isOperator("<="));
            assertFalse(SqlTokenizer.isOperator("SELECT"));
            assertFalse(SqlTokenizer.isOperator("123"));
        }

        @Test
        @DisplayName("Identify delimiters correctly")
        void testDelimiterIdentification() {
            assertTrue(SqlTokenizer.isDelimiter("("));
            assertTrue(SqlTokenizer.isDelimiter(")"));
            assertTrue(SqlTokenizer.isDelimiter(","));
            assertTrue(SqlTokenizer.isDelimiter("."));
            assertFalse(SqlTokenizer.isDelimiter("SELECT"));
            assertFalse(SqlTokenizer.isDelimiter("+"));
        }

        @Test
        @DisplayName("Identify string literals correctly")
        void testStringLiteralIdentification() {
            assertTrue(SqlTokenizer.isStringLiteral("'Hello'"));
            assertTrue(SqlTokenizer.isStringLiteral("\"World\""));
            assertTrue(SqlTokenizer.isStringLiteral("''"));
            assertFalse(SqlTokenizer.isStringLiteral("Hello"));
            assertFalse(SqlTokenizer.isStringLiteral("123"));
        }

        @Test
        @DisplayName("Identify numeric literals correctly")
        void testNumericLiteralIdentification() {
            assertTrue(SqlTokenizer.isNumericLiteral("123"));
            assertTrue(SqlTokenizer.isNumericLiteral("123.456"));
            assertTrue(SqlTokenizer.isNumericLiteral("0"));
            assertTrue(SqlTokenizer.isNumericLiteral("0.5"));
            assertFalse(SqlTokenizer.isNumericLiteral("abc"));
            assertFalse(SqlTokenizer.isNumericLiteral("'123'"));
        }

        @Test
        @DisplayName("Identify identifiers correctly")
        void testIdentifierIdentification() {
            assertTrue(SqlTokenizer.isIdentifier("name"));
            assertTrue(SqlTokenizer.isIdentifier("user_id"));
            assertTrue(SqlTokenizer.isIdentifier("_private"));
            assertTrue(SqlTokenizer.isIdentifier("table1"));
            assertFalse(SqlTokenizer.isIdentifier("SELECT"));
            assertFalse(SqlTokenizer.isIdentifier("123"));
            assertFalse(SqlTokenizer.isIdentifier("'name'"));
            assertFalse(SqlTokenizer.isIdentifier("+"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handle empty SQL")
        void testEmptySQL() {
            SqlTokenizer tokenizer = new SqlTokenizer("");
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.isEmpty());
        }

        @Test
        @DisplayName("Handle null SQL")
        void testNullSQL() {
            SqlTokenizer tokenizer = new SqlTokenizer(null);
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.isEmpty());
        }

        @Test
        @DisplayName("Handle SQL with only whitespace")
        void testWhitespaceOnlySQL() {
            SqlTokenizer tokenizer = new SqlTokenizer("   \t\n   ");
            List<String> tokens = tokenizer.tokenize();

            assertTrue(tokens.isEmpty());
        }

        @Test
        @DisplayName("Handle unterminated string literal")
        void testUnterminatedStringLiteral() {
            String sql = "SELECT 'unterminated FROM users";
            SqlTokenizer tokenizer = new SqlTokenizer(sql);
            List<String> tokens = tokenizer.tokenize();

            assertThat(tokens).hasSize(2);
            assertThat(tokens).containsExactly("SELECT", "'unterminated FROM users");
        }
    }
}