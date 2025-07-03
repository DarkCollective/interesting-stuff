
package com.darkcollective.sqlparser;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Tokenizes SQL strings into individual tokens for parsing
 */
public class SqlTokenizer {

    private final String sql;

    // SQL keywords that should be treated as single tokens
    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER",
            "ON", "GROUP", "BY", "ORDER", "HAVING", "DISTINCT", "AS", "AND", "OR", "NOT",
            "IN", "LIKE", "BETWEEN", "IS", "NULL", "COUNT", "SUM", "AVG", "MIN", "MAX",
            "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TABLE", "INDEX",
            "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT",
            "UNION", "INTERSECT", "EXCEPT", "LIMIT", "OFFSET", "ASC", "DESC"
    ));

    // Operators that should be treated as single tokens
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            "=", "!=", "<>", "<", ">", "<=", ">=", "+", "-", "*", "/", "%",
            "||", "&&", "!", "^", "&", "|", "<<", ">>", "~"
    ));

    // Special characters that should be treated as separate tokens
    private static final Set<Character> DELIMITERS = new HashSet<>(Arrays.asList(
            '(', ')', ',', ';', '.', '[', ']', '{', '}'
    ));

    public SqlTokenizer(String sql) {
        this.sql = sql != null ? sql.trim() : "";
    }

    /**
     * Tokenize the SQL string into a list of tokens
     */
    public List<String> tokenize() {
        if (sql.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < sql.length()) {
            char c = sql.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Handle string literals (single quotes)
            if (c == '\'') {
                String stringLiteral = extractStringLiteral(i, '\'');
                tokens.add(stringLiteral);
                i += stringLiteral.length();
                continue;
            }

            // Handle string literals (double quotes)
            if (c == '"') {
                String stringLiteral = extractStringLiteral(i, '"');
                tokens.add(stringLiteral);
                i += stringLiteral.length();
                continue;
            }

            // Handle comments (-- style)
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                i = skipLineComment(i);
                continue;
            }

            // Handle comments (/* */ style)
            if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(i);
                continue;
            }

            // Handle multi-character operators
            String multiCharOp = extractMultiCharOperator(i);
            if (multiCharOp != null) {
                tokens.add(multiCharOp);
                i += multiCharOp.length();
                continue;
            }

            // Handle single character delimiters
            if (DELIMITERS.contains(c)) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // Handle single character operators
            if (OPERATORS.contains(String.valueOf(c))) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // Handle numbers
            if (Character.isDigit(c) || c == '.') {
                String number = extractNumber(i);
                tokens.add(number);
                i += number.length();
                continue;
            }

            // Handle identifiers and keywords
            if (Character.isLetter(c) || c == '_') {
                String identifier = extractIdentifier(i);
                tokens.add(identifier);
                i += identifier.length();
                continue;
            }

            // If we get here, it's an unexpected character - treat as single token
            tokens.add(String.valueOf(c));
            i++;
        }

        return tokens;
    }

    private String extractStringLiteral(int start, char quoteChar) {
        StringBuilder sb = new StringBuilder();
        sb.append(quoteChar);
        int i = start + 1;

        while (i < sql.length()) {
            char c = sql.charAt(i);
            sb.append(c);

            if (c == quoteChar) {
                // Check for escaped quote (doubled quote)
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quoteChar) {
                    sb.append(quoteChar);
                    i += 2;
                } else {
                    i++;
                    break;
                }
            } else {
                i++;
            }
        }

        return sb.toString();
    }

    private String extractNumber(int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        boolean hasDecimalPoint = false;

        while (i < sql.length()) {
            char c = sql.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(c);
                i++;
            } else if (c == '.' && !hasDecimalPoint) {
                hasDecimalPoint = true;
                sb.append(c);
                i++;
            } else {
                break;
            }
        }

        return sb.toString();
    }

    private String extractIdentifier(int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;

        while (i < sql.length()) {
            char c = sql.charAt(i);

            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                i++;
            } else {
                break;
            }
        }

        return sb.toString();
    }

    private String extractMultiCharOperator(int start) {
        // Check for two-character operators first
        if (start + 1 < sql.length()) {
            String twoChar = sql.substring(start, start + 2);
            if (OPERATORS.contains(twoChar)) {
                return twoChar;
            }
        }

        return null;
    }

    private int skipLineComment(int start) {
        int i = start;
        while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
            i++;
        }
        return i;
    }

    private int skipBlockComment(int start) {
        int i = start + 2; // Skip /*

        while (i + 1 < sql.length()) {
            if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
                return i + 2; // Skip */
            }
            i++;
        }

        return sql.length(); // Unterminated comment, go to end
    }

    /**
     * Check if a token is a SQL keyword
     */
    public static boolean isKeyword(String token) {
        return SQL_KEYWORDS.contains(token.toUpperCase());
    }

    /**
     * Check if a token is an operator
     */
    public static boolean isOperator(String token) {
        return OPERATORS.contains(token);
    }

    /**
     * Check if a token is a delimiter
     */
    public static boolean isDelimiter(String token) {
        return token.length() == 1 && DELIMITERS.contains(token.charAt(0));
    }

    /**
     * Check if a token is a string literal
     */
    public static boolean isStringLiteral(String token) {
        return (token.startsWith("'") && token.endsWith("'")) ||
                (token.startsWith("\"") && token.endsWith("\""));
    }

    /**
     * Check if a token is a numeric literal
     */
    public static boolean isNumericLiteral(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if a token is an identifier (column name, table name, etc.)
     */
    public static boolean isIdentifier(String token) {
        if (token.isEmpty() || isKeyword(token) || isOperator(token) ||
                isDelimiter(token) || isStringLiteral(token) || isNumericLiteral(token)) {
            return false;
        }

        // Must start with letter or underscore
        char first = token.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return false;
        }

        // Rest must be letters, digits, or underscores
        for (int i = 1; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }
}
