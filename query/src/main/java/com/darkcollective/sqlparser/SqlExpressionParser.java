package com.darkcollective.sqlparser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SQL expressions including functions, literals, and operators
 */
public class SqlExpressionParser {

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'([^']*)'");
    private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*\\b");

    /**
     * Parse a SQL expression and extract all function calls using regex approach
     */
    public static List<SqlFunctionCall> extractFunctionCalls(String expression) {
        List<SqlFunctionCall> functionCalls = new ArrayList<>();

        if (expression == null || expression.trim().isEmpty()) {
            return functionCalls;
        }

        // Create a single regex pattern for all functions
        Set<String> allFunctions = SqlFunctionRegistry.getAllFunctions();

        // Sort by length (longest first) to avoid substring issues
        List<String> sortedFunctions = new ArrayList<>(allFunctions);
        sortedFunctions.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String functionName : sortedFunctions) {
            // Create pattern that matches function name followed by opening parenthesis
            String patternStr = "\\b" + Pattern.quote(functionName) + "\\s*\\(";
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(expression);

            while (matcher.find()) {
                int functionStart = matcher.start();
                int parenStart = matcher.end() - 1; // Position of the '('

                // Find matching closing parenthesis
                int parenEnd = findMatchingParenthesis(expression, parenStart);

                if (parenEnd != -1) {
                    String fullMatch = expression.substring(functionStart, parenEnd + 1);
                    String argumentsString = expression.substring(parenStart + 1, parenEnd);

                    // Check if we already have a function call that overlaps with this one
                    boolean overlaps = false;
                    for (SqlFunctionCall existing : functionCalls) {
                        String existingExpr = existing.getOriginalExpression();
                        int existingStart = expression.indexOf(existingExpr);
                        int existingEnd = existingStart + existingExpr.length() - 1;

                        // Check for overlap
                        if ((functionStart >= existingStart && functionStart <= existingEnd) ||
                                (parenEnd >= existingStart && parenEnd <= existingEnd) ||
                                (functionStart <= existingStart && parenEnd >= existingEnd)) {
                            overlaps = true;
                            break;
                        }
                    }

                    if (!overlaps) {
                        List<String> arguments = parseArguments(argumentsString);

                        SqlFunctionCall functionCall = new SqlFunctionCall(
                                functionName,
                                arguments,
                                fullMatch
                        );

                        functionCalls.add(functionCall);
                    }
                } else {
                    System.out.println("DEBUG: No matching closing parenthesis found for " + functionName);
                }
            }
        }

        return functionCalls;
    }

    /**
     * Find the matching closing parenthesis for an opening parenthesis
     */
    private static int findMatchingParenthesis(String text, int openIndex) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'' && !inQuotes) {
                inQuotes = true;
            } else if (c == '\'' && inQuotes) {
                inQuotes = false;
            } else if (!inQuotes) {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        return -1; // No matching parenthesis found
    }

    /**
     * Parse function arguments, handling nested parentheses and commas
     */
    public static List<String> parseArguments(String argumentsString) {
        List<String> arguments = new ArrayList<>();

        if (argumentsString == null || argumentsString.trim().isEmpty()) {
            return arguments;
        }

        StringBuilder currentArg = new StringBuilder();
        int parenDepth = 0;
        boolean inQuotes = false;

        for (int i = 0; i < argumentsString.length(); i++) {
            char c = argumentsString.charAt(i);

            if (c == '\'' && !inQuotes) {
                inQuotes = true;
                currentArg.append(c);
            } else if (c == '\'' && inQuotes) {
                inQuotes = false;
                currentArg.append(c);
            } else if (inQuotes) {
                currentArg.append(c);
            } else if (c == '(') {
                parenDepth++;
                currentArg.append(c);
            } else if (c == ')') {
                parenDepth--;
                currentArg.append(c);
            } else if (c == ',' && parenDepth == 0) {
                if (currentArg.length() > 0) {
                    arguments.add(currentArg.toString().trim());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }

        // Add the last argument
        if (currentArg.length() > 0) {
            arguments.add(currentArg.toString().trim());
        }

        return arguments;
    }

    /**
     * Extract string literals from an expression
     */
    public static List<String> extractStringLiterals(String expression) {
        List<String> literals = new ArrayList<>();
        Matcher matcher = STRING_LITERAL_PATTERN.matcher(expression);

        while (matcher.find()) {
            literals.add(matcher.group(1)); // Content without quotes
        }

        return literals;
    }

    /**
     * Extract numeric literals from an expression
     */
    public static List<String> extractNumericLiterals(String expression) {
        List<String> literals = new ArrayList<>();
        Matcher matcher = NUMERIC_LITERAL_PATTERN.matcher(expression);

        while (matcher.find()) {
            literals.add(matcher.group(0));
        }

        return literals;
    }

    /**
     * Extract identifiers (column names, table names) from an expression
     */
    public static List<String> extractIdentifiers(String expression) {
        List<String> identifiers = new ArrayList<>();

        // Remove string literals and function calls to avoid false matches
        String cleanExpression = expression;
        cleanExpression = STRING_LITERAL_PATTERN.matcher(cleanExpression).replaceAll("");

        // Remove function calls by replacing them with placeholders
        List<SqlFunctionCall> functions = extractFunctionCalls(expression);
        for (SqlFunctionCall func : functions) {
            cleanExpression = cleanExpression.replace(func.getOriginalExpression(), "FUNC_PLACEHOLDER");
        }

        Matcher matcher = IDENTIFIER_PATTERN.matcher(cleanExpression);

        while (matcher.find()) {
            String identifier = matcher.group(0);
            // Filter out SQL keywords and function names
            if (!SqlFunctionRegistry.isRegisteredFunction(identifier) &&
                    !isSqlKeyword(identifier)) {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    /**
     * Check if a string is a SQL keyword
     */
    private static boolean isSqlKeyword(String word) {
        Set<String> keywords = Set.of(
                "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS",
                "GROUP", "BY", "HAVING", "ORDER", "DISTINCT", "ON", "AS", "AND", "OR", "NOT",
                "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE", "ASC", "DESC"
        );
        return keywords.contains(word.toUpperCase());
    }

    /**
     * Parse alias from expression - handles both explicit AS and implicit aliasing
     */
    public static AliasInfo parseAlias(String expression) {
        String trimmedExpression = expression.trim();

        // Check for explicit AS keyword first
        if (trimmedExpression.toUpperCase().contains(" AS ")) {
            String[] parts = trimmedExpression.split("(?i)\\s+AS\\s+", 2);
            if (parts.length == 2) {
                String expr = parts[0].trim();
                String alias = parts[1].trim();
                // Remove quotes if present
                if ((alias.startsWith("\"") && alias.endsWith("\"")) ||
                        (alias.startsWith("`") && alias.endsWith("`"))) {
                    alias = alias.substring(1, alias.length() - 1);
                }
                return new AliasInfo(expr, alias);
            }
        }

        // Check for implicit alias, but only if we're not inside parentheses
        // This prevents treating function arguments as aliases
        int parenDepth = 0;
        boolean canHaveAlias = true;

        for (char c : trimmedExpression.toCharArray()) {
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            }
        }

        // If parentheses are unbalanced or we have unclosed parentheses,
        // don't try to parse as alias
        if (parenDepth != 0) {
            canHaveAlias = false;
        }

        // Also check if this looks like a function call
        if (trimmedExpression.matches(".*\\w+\\s*\\(.*\\).*")) {
            // This looks like a function call, be more conservative about aliases
            // Only treat as alias if there's a clear word after the closing parenthesis
            int lastParenIndex = trimmedExpression.lastIndexOf(')');
            if (lastParenIndex != -1 && lastParenIndex < trimmedExpression.length() - 1) {
                String afterParen = trimmedExpression.substring(lastParenIndex + 1).trim();
                if (!afterParen.isEmpty() && afterParen.matches("\\w+")) {
                    // There's a word after the closing parenthesis, treat it as alias
                    String expr = trimmedExpression.substring(0, lastParenIndex + 1).trim();
                    return new AliasInfo(expr, afterParen);
                }
            }
            // No alias found after function call
            return new AliasInfo(trimmedExpression, null);
        }

        if (canHaveAlias) {
            String[] words = trimmedExpression.split("\\s+");
            if (words.length >= 2) {
                String potentialAlias = words[words.length - 1];

                // Don't treat SQL keywords as aliases
                if (!isSqlKeyword(potentialAlias) &&
                        !SqlFunctionRegistry.isRegisteredFunction(potentialAlias)) {

                    // Build expression without the last word (the alias)
                    StringBuilder exprBuilder = new StringBuilder();
                    for (int i = 0; i < words.length - 1; i++) {
                        if (i > 0) exprBuilder.append(" ");
                        exprBuilder.append(words[i]);
                    }
                    String expression_part = exprBuilder.toString();

                    // For simple cases like "name full_name", always treat as alias
                    if (words.length == 2 && !expression_part.contains("(") &&
                            !expression_part.contains("+") && !expression_part.contains("-") &&
                            !expression_part.contains("*") && !expression_part.contains("/")) {
                        return new AliasInfo(expression_part, potentialAlias);
                    }

                    // For complex expressions (functions, operators), also treat as alias
                    // but be more careful about parentheses
                    if (!expression_part.contains("(") &&
                            (expression_part.contains("+") || expression_part.contains("-") ||
                                    expression_part.contains("*") || expression_part.contains("/") ||
                                    expression_part.contains("."))) {
                        return new AliasInfo(expression_part, potentialAlias);
                    }

                    // For multi-word expressions without operators, treat as alias
                    if (words.length > 2 && !expression_part.contains("(")) {
                        return new AliasInfo(expression_part, potentialAlias);
                    }
                }
            }
        }

        return new AliasInfo(trimmedExpression, null);
    }

    /**
     * Parse a complete SELECT item (column, expression, or function)
     */
    public static SelectItem parseSelectItem(String item) {
        String trimmedItem = item.trim();

        // Parse alias first
        AliasInfo aliasInfo = parseAlias(trimmedItem);
        String expression = aliasInfo.getExpression();
        String alias = aliasInfo.getAlias();

        // Extract functions from the expression (not the alias)
        List<SqlFunctionCall> functions = extractFunctionCalls(expression);

        // Extract literals from the expression
        List<String> stringLiterals = extractStringLiterals(expression);
        List<String> numericLiterals = extractNumericLiterals(expression);

        // Extract identifiers from the expression
        List<String> identifiers = extractIdentifiers(expression);

        return new SelectItem(expression, alias, functions, stringLiterals, numericLiterals, identifiers);
    }

    /**
     * Helper class to hold alias parsing results
     */
    public static class AliasInfo {
        private final String expression;
        private final String alias;

        public AliasInfo(String expression, String alias) {
            this.expression = expression;
            this.alias = alias;
        }

        public String getExpression() { return expression; }
        public String getAlias() { return alias; }
        public boolean hasAlias() { return alias != null && !alias.isEmpty(); }
    }

    /**
     * Represents a parsed SELECT item with all its components
     */
    public static class SelectItem {
        private final String expression;
        private final String alias;
        private final List<SqlFunctionCall> functions;
        private final List<String> stringLiterals;
        private final List<String> numericLiterals;
        private final List<String> identifiers;

        public SelectItem(String expression, String alias, List<SqlFunctionCall> functions,
                          List<String> stringLiterals, List<String> numericLiterals, List<String> identifiers) {
            this.expression = expression;
            this.alias = alias;
            this.functions = new ArrayList<>(functions);
            this.stringLiterals = new ArrayList<>(stringLiterals);
            this.numericLiterals = new ArrayList<>(numericLiterals);
            this.identifiers = new ArrayList<>(identifiers);
        }

        public String getExpression() { return expression; }
        public String getAlias() { return alias; }
        public List<SqlFunctionCall> getFunctions() { return Collections.unmodifiableList(functions); }
        public List<String> getStringLiterals() { return Collections.unmodifiableList(stringLiterals); }
        public List<String> getNumericLiterals() { return Collections.unmodifiableList(numericLiterals); }
        public List<String> getIdentifiers() { return Collections.unmodifiableList(identifiers); }

        public boolean hasFunctions() { return !functions.isEmpty(); }
        public boolean hasAlias() { return alias != null && !alias.isEmpty(); }

        /**
         * Get the effective column name (alias if present, otherwise expression)
         */
        public String getEffectiveColumnName() {
            return hasAlias() ? alias : expression;
        }

        /**
         * Get the full column representation for SQL
         */
        public String toSql() {
            return hasAlias() ? expression + " AS " + alias : expression;
        }

        @Override
        public String toString() {
            return "SelectItem{" +
                    "expression='" + expression + '\'' +
                    ", alias='" + alias + '\'' +
                    ", functions=" + functions.size() +
                    ", identifiers=" + identifiers +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SelectItem that = (SelectItem) o;
            return Objects.equals(expression, that.expression) &&
                    Objects.equals(alias, that.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expression, alias);
        }
    }
}
