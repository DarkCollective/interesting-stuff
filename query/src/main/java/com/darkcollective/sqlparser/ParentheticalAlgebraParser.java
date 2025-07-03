package com.darkcollective.sqlparser;

import java.util.*;

/**
 * Parser for parenthetical notation of relational algebra expressions
 * Example: PROJECTION(name, age, SELECTION(age > 18, TABLE_SCAN(users)))
 */
public class ParentheticalAlgebraParser {

    /**
     * Parse a parenthetical expression into a RelationalOperator tree
     */
    public static RelationalOperator parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }

        TokenIterator tokens = new TokenIterator(tokenize(expression.trim()));
        return parseOperator(tokens);
    }

    /**
     * Parse a single operator from the token stream
     */
    private static RelationalOperator parseOperator(TokenIterator tokens) {
        if (!tokens.hasNext()) {
            throw new IllegalArgumentException("Unexpected end of expression");
        }

        String operatorName = tokens.next();
        
        if (!tokens.hasNext() || !tokens.peek().equals("(")) {
            throw new IllegalArgumentException("Expected '(' after operator name: " + operatorName);
        }
        
        tokens.next(); // consume '('
        
        // Parse based on operator type
        RelationalOperator operator = switch (operatorName.toUpperCase()) {
            case "TABLE_SCAN" -> parseTableScan(tokens);
            case "PROJECTION" -> parseProjection(tokens);
            case "SELECTION" -> parseSelection(tokens);
            case "INNER_JOIN", "LEFT_JOIN", "RIGHT_JOIN", "FULL_JOIN", "CROSS_JOIN" -> parseJoin(operatorName, tokens);
            case "AGGREGATION" -> parseAggregation(tokens);
            case "SORT" -> parseSort(tokens);
            case "SUBQUERY" -> parseSubquery(tokens);
            default -> throw new IllegalArgumentException("Unknown operator: " + operatorName);
        };
        
        if (!tokens.hasNext() || !tokens.next().equals(")")) {
            throw new IllegalArgumentException("Expected ')' to close operator: " + operatorName);
        }
        
        return operator;
    }

    private static TableScanOperator parseTableScan(TokenIterator tokens) {
        String tableSpec = parseStringParameter(tokens);
        
        // Check if it has an alias: "table AS alias" or just "table"
        String[] parts = tableSpec.split("\\s+AS\\s+", 2);
        String tableName = parts[0].trim();
        String alias = parts.length > 1 ? parts[1].trim() : null;
        
        return new TableScanOperator(tableName, alias);
    }

    private static ProjectionOperator parseProjection(TokenIterator tokens) {
        List<String> allParams = parseParameterList(tokens);
        
        if (allParams.isEmpty()) {
            throw new IllegalArgumentException("PROJECTION requires at least one parameter");
        }
        
        boolean distinct = false;
        int columnStartIndex = 0;
        
        // Check if first parameter is DISTINCT
        if (allParams.get(0).equalsIgnoreCase("DISTINCT")) {
            distinct = true;
            columnStartIndex = 1;
        }
        
        // Separate columns from child operators
        List<String> columns = new ArrayList<>();
        List<RelationalOperator> children = new ArrayList<>();
        
        for (int i = columnStartIndex; i < allParams.size(); i++) {
            String param = allParams.get(i);
            if (isOperatorExpression(param)) {
                // Parse child operator
                RelationalOperator child = parse(param);
                children.add(child);
            } else {
                // Regular column
                columns.add(param);
            }
        }
        
        ProjectionOperator projection = new ProjectionOperator(columns, distinct);
        children.forEach(projection::addChild);
        
        return projection;
    }

    private static SelectionOperator parseSelection(TokenIterator tokens) {
        List<String> params = parseParameterList(tokens);
        
        if (params.isEmpty()) {
            throw new IllegalArgumentException("SELECTION requires a condition parameter");
        }
        
        String condition = params.get(0);
        SelectionOperator selection = new SelectionOperator(condition);
        
        // Parse child operators
        for (int i = 1; i < params.size(); i++) {
            String param = params.get(i);
            if (isOperatorExpression(param)) {
                RelationalOperator child = parse(param);
                selection.addChild(child);
            }
        }
        
        return selection;
    }

    private static JoinOperator parseJoin(String joinTypeName, TokenIterator tokens) {
        JoinOperator.JoinType joinType = JoinOperator.JoinType.valueOf(
            joinTypeName.toUpperCase().replace("_JOIN", "")
        );
        
        List<String> params = parseParameterList(tokens);
        
        String condition = null;
        List<RelationalOperator> children = new ArrayList<>();
        
        for (String param : params) {
            if (isOperatorExpression(param)) {
                RelationalOperator child = parse(param);
                children.add(child);
            } else if (condition == null) {
                // First non-operator parameter is the join condition
                condition = param;
            }
        }
        
        JoinOperator join = new JoinOperator(joinType, condition);
        children.forEach(join::addChild);
        
        return join;
    }

    private static AggregationOperator parseAggregation(TokenIterator tokens) {
        List<String> params = parseParameterList(tokens);

        List<String> groupByColumns = new ArrayList<>();
        List<String> aggregateFunctions = new ArrayList<>();
        String havingCondition = null;
        List<RelationalOperator> children = new ArrayList<>();

        for (String param : params) {
            if (isOperatorExpression(param)) {
                RelationalOperator child = parse(param);
                children.add(child);
            } else if (param.startsWith("GROUP_BY:")) {
                String groupByStr = param.substring("GROUP_BY:".length()).trim();
                groupByColumns = parseCommaSeparatedWithFunctions(groupByStr);
            } else if (param.startsWith("AGG:")) {
                String aggStr = param.substring("AGG:".length()).trim();
                aggregateFunctions = parseCommaSeparatedWithFunctions(aggStr);
            } else if (param.startsWith("HAVING:")) {
                havingCondition = param.substring("HAVING:".length()).trim();
            }
        }

        AggregationOperator aggregation = new AggregationOperator(groupByColumns, aggregateFunctions, havingCondition);
        children.forEach(aggregation::addChild);

        return aggregation;
    }

    /**
     * Parse comma-separated values that may contain function calls with parentheses
     */
    private static List<String> parseCommaSeparatedWithFunctions(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '(') {
                parenDepth++;
                current.append(c);
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    private static SortOperator parseSort(TokenIterator tokens) {
        List<String> params = parseParameterList(tokens);
        
        List<String> orderByColumns = new ArrayList<>();
        List<RelationalOperator> children = new ArrayList<>();
        
        for (String param : params) {
            if (isOperatorExpression(param)) {
                RelationalOperator child = parse(param);
                children.add(child);
            } else {
                // Order by column
                orderByColumns.add(param);
            }
        }
        
        SortOperator sort = new SortOperator(orderByColumns);
        children.forEach(sort::addChild);
        
        return sort;
    }

    private static SubqueryOperator parseSubquery(TokenIterator tokens) {
        List<String> params = parseParameterList(tokens);
        
        SubqueryOperator.SubqueryType type = SubqueryOperator.SubqueryType.FROM_CLAUSE;
        String alias = null;
        RelationalOperator subquery = null;
        
        for (String param : params) {
            if (isOperatorExpression(param)) {
                subquery = parse(param);
            } else if (param.startsWith("TYPE:")) {
                String typeStr = param.substring("TYPE:".length()).trim();
                type = SubqueryOperator.SubqueryType.valueOf(typeStr.toUpperCase());
            } else if (param.startsWith("ALIAS:")) {
                alias = param.substring("ALIAS:".length()).trim();
            }
        }
        
        if (subquery == null) {
            throw new IllegalArgumentException("SUBQUERY requires a child operator");
        }
        
        return new SubqueryOperator(subquery, alias, type);
    }

    /**
     * Check if a parameter string represents an operator expression
     */
    private static boolean isOperatorExpression(String param) {
        if (param == null || param.trim().isEmpty()) {
            return false;
        }

        // Check if it starts with a known operator name followed by parentheses
        String trimmed = param.trim();

        // Known operator names - be very specific about what we consider operators
        Set<String> operatorNames = Set.of(
                "TABLE_SCAN", "PROJECTION", "SELECTION",
                "INNER_JOIN", "LEFT_JOIN", "RIGHT_JOIN", "FULL_JOIN", "CROSS_JOIN",
                "AGGREGATION", "SORT", "SUBQUERY", "FUNCTION"
        );

        // Extract the potential operator name (everything before the first opening paren)
        int parenIndex = trimmed.indexOf('(');
        if (parenIndex == -1) {
            return false; // No parentheses, can't be an operator expression
        }

        String potentialOperator = trimmed.substring(0, parenIndex).trim().toUpperCase();
        return operatorNames.contains(potentialOperator);
    }


    /**
     * Parse comma-separated values, handling parentheses and quotes
     */
    private static List<String> parseCommaSeparated(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return SqlExpressionParser.parseArguments(value);
    }

    /**
     * Parse a single string parameter (handling quotes if present)
     */
    private static String parseStringParameter(TokenIterator tokens) {
        List<String> params = parseParameterList(tokens);
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Expected string parameter");
        }
        return params.get(0);
    }

    /**
     * Parse all parameters until the closing parenthesis
     */
    private static List<String> parseParameterList(TokenIterator tokens) {
        List<String> parameters = new ArrayList<>();
        StringBuilder currentParam = new StringBuilder();
        int parenDepth = 0;
        
        while (tokens.hasNext()) {
            String token = tokens.peek();
            
            if (token.equals(")") && parenDepth == 0) {
                // End of parameter list
                if (currentParam.length() > 0) {
                    parameters.add(currentParam.toString().trim());
                }
                break;
            } else if (token.equals(",") && parenDepth == 0) {
                // Parameter separator
                if (!currentParam.isEmpty()) {
                    parameters.add(currentParam.toString().trim());
                    currentParam.setLength(0);
                }
                tokens.next(); // consume comma
            } else {
                // Part of current parameter
                if (token.equals("(")) {
                    parenDepth++;
                } else if (token.equals(")")) {
                    parenDepth--;
                }
                
                if (!currentParam.isEmpty()) {
                  //  currentParam.append("_");
                }
                currentParam.append(tokens.next());
            }
        }
        
        return parameters;
    }

    /**
     * Tokenize the expression into operators, parentheses, commas, and parameters
     */
    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (c == '\'' && !inQuotes) {
                inQuotes = true;
                current.append(c);
            } else if (c == '\'' && inQuotes) {
                inQuotes = false;
                current.append(c);
            } else if (inQuotes) {
                current.append(c);
            } else if (c == '(' || c == ')' || c == ',') {
                if (current.length() > 0) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            tokens.add(current.toString().trim());
        }
        
        return tokens.stream()
            .filter(token -> !token.isEmpty())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Simple iterator wrapper for token list
     */
    private static class TokenIterator {
        private final List<String> tokens;
        private int index = 0;

        public TokenIterator(List<String> tokens) {
            this.tokens = tokens;
        }

        public boolean hasNext() {
            return index < tokens.size();
        }

        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more tokens");
            }
            return tokens.get(index++);
        }

        public String peek() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more tokens");
            }
            return tokens.get(index);
        }
    }
}