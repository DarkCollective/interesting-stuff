
package com.darkcollective.sqlparser;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Parser that builds expression trees from SQL expressions
 */
public class ExpressionTreeParser {
    
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?\\b");
    
    private final Schema schema;
    
    public ExpressionTreeParser(Schema schema) {
        this.schema = schema;
    }
    
    /**
     * Parse a SQL expression into an expression tree
     */
    public ExpressionNode parseExpression(String expression, List<String> availableColumns) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }

        String trimmed = expression.trim();

        // Handle parentheses
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return parseExpression(trimmed.substring(1, trimmed.length() - 1), availableColumns);
        }

        // Check if it's a function call first (before checking operators)
        if (isFunctionCall(trimmed)) {
            return parseFunctionCall(trimmed, availableColumns);
        }

        // Try to find operators (in order of precedence - lowest first)
        String[] binaryOperators = {"OR", "AND", "=", "!=", "<>", "<", ">", "<=", ">=",
                "LIKE", "NOT LIKE", "+", "-", "*", "/", "%"};

        for (String op : binaryOperators) {
            int opIndex = findOperatorOutsideParentheses(trimmed, op);
            if (opIndex != -1) {
                String left = trimmed.substring(0, opIndex).trim();
                String right = trimmed.substring(opIndex + op.length()).trim();

                OperatorNode operatorNode = new OperatorNode(op, expression);
                operatorNode.addChild(parseExpression(left, availableColumns));
                operatorNode.addChild(parseExpression(right, availableColumns));

                return operatorNode;
            }
        }

        // Check if it's a string literal
        if (isStringLiteral(trimmed)) {
            return LiteralNode.createString(trimmed);
        }

        // Check if it's a numeric literal
        if (isNumericLiteral(trimmed)) {
            return LiteralNode.createNumeric(trimmed);
        }

        // Check if it's a column reference
        if (isIdentifier(trimmed)) {
            return parseColumnReference(trimmed, availableColumns);
        }

        // If we can't parse it, treat as identifier
        return parseColumnReference(trimmed, availableColumns);
    }

    /**
     * Check if the expression is a function call
     */
    private boolean isFunctionCall(String expression) {
        // Look for pattern: identifier followed by parentheses
        int parenIndex = expression.indexOf('(');
        if (parenIndex == -1) {
            return false;
        }

        String beforeParen = expression.substring(0, parenIndex).trim();
        return isIdentifier(beforeParen) && expression.endsWith(")");
    }

    /**
     * Parse a function call into a FunctionNode
     */
    private FunctionNode parseFunctionCall(String expression, List<String> availableColumns) {
        int parenIndex = expression.indexOf('(');
        String functionName = expression.substring(0, parenIndex).trim();
        String argsString = expression.substring(parenIndex + 1, expression.length() - 1).trim();

        List<ExpressionNode> arguments = new ArrayList<>();

        if (!argsString.isEmpty()) {
            // Split arguments by comma, respecting parentheses
            List<String> argStrings = splitFunctionArguments(argsString);

            for (String argString : argStrings) {
                arguments.add(parseExpression(argString.trim(), availableColumns));
            }
        }

        return new FunctionNode(functionName, arguments, expression);
    }

    /**
     * Split function arguments by comma, respecting nested parentheses
     */
    private List<String> splitFunctionArguments(String argsString) {
        List<String> arguments = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);

            if (!inQuotes) {
                if (c == '\'' || c == '"') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == '(') {
                    parenthesesLevel++;
                } else if (c == ')') {
                    parenthesesLevel--;
                } else if (c == ',' && parenthesesLevel == 0) {
                    arguments.add(currentArg.toString().trim());
                    currentArg = new StringBuilder();
                    continue;
                }
            } else {
                if (c == quoteChar) {
                    inQuotes = false;
                }
            }

            currentArg.append(c);
        }

        if (currentArg.length() > 0) {
            arguments.add(currentArg.toString().trim());
        }

        return arguments;
    }

    
    private int findOperatorOutsideParentheses(String expression, String operator) {
        int parenthesesDepth = 0;
        int index = 0;
        
        while (index < expression.length()) {
            char c = expression.charAt(index);
            
            if (c == '(') {
                parenthesesDepth++;
            } else if (c == ')') {
                parenthesesDepth--;
            } else if (parenthesesDepth == 0) {
                // Check if operator matches at this position
                if (index + operator.length() <= expression.length()) {
                    String substring = expression.substring(index, index + operator.length());
                    if (substring.equalsIgnoreCase(operator)) {
                        // Make sure it's a complete word for word operators
                        if (operator.matches("[A-Za-z]+")) {
                            boolean beforeOk = (index == 0) || !Character.isLetterOrDigit(expression.charAt(index - 1));
                            boolean afterOk = (index + operator.length() >= expression.length()) || 
                                            !Character.isLetterOrDigit(expression.charAt(index + operator.length()));
                            if (beforeOk && afterOk) {
                                return index;
                            }
                        } else {
                            return index;
                        }
                    }
                }
            }
            index++;
        }
        
        return -1;
    }
    
    private boolean isStringLiteral(String token) {
        return STRING_LITERAL_PATTERN.matcher(token).matches();
    }
    
    private boolean isNumericLiteral(String token) {
        return NUMERIC_LITERAL_PATTERN.matcher(token).matches();
    }
    
    private boolean isIdentifier(String token) {
        return IDENTIFIER_PATTERN.matcher(token).matches();
    }
    
    private ColumnNode parseColumnReference(String columnRef, List<String> availableColumns) {
        String tableName = null;
        String columnName = columnRef;
        
        // Handle qualified column names (table.column)
        if (columnRef.contains(".")) {
            String[] parts = columnRef.split("\\.");
            if (parts.length == 2) {
                tableName = parts[0];
                columnName = parts[1];
            }
        }
        
        // Try to determine the data type from schema
        Schema.DataType dataType = null;
        
        if (schema != null) {
            if (tableName != null && schema.hasTable(tableName)) {
                Schema.Table table = schema.getTable(tableName);
                if (table.hasColumn(columnName)) {
                    dataType = table.getColumn(columnName).getDataType();
                }
            } else {
                // Search all tables for the column
                for (String tableNameInSchema : schema.getTableNames()) {
                    Schema.Table table = schema.getTable(tableNameInSchema);
                    if (table.hasColumn(columnName)) {
                        dataType = table.getColumn(columnName).getDataType();
                        break;
                    }
                }
            }
        }
        
        // If we couldn't determine the type, default to VARCHAR
        if (dataType == null) {
            dataType = Schema.DataType.VARCHAR;
        }
        
        return new ColumnNode(columnRef, columnName, tableName, dataType);
    }
    
    /**
     * Parse a SELECT item and return both the expression tree and alias information
     */
    public SelectItemWithTree parseSelectItem(String selectItem) {
        SqlExpressionParser.AliasInfo aliasInfo = SqlExpressionParser.parseAlias(selectItem);
        ExpressionNode expressionTree = parseExpression(aliasInfo.getExpression(), new ArrayList<>());
        
        return new SelectItemWithTree(expressionTree, aliasInfo.getAlias(), selectItem);
    }
    
    /**
     * Represents a parsed SELECT item with its expression tree
     */
    public static class SelectItemWithTree {
        private final ExpressionNode expressionTree;
        private final String alias;
        private final String originalExpression;
        
        public SelectItemWithTree(ExpressionNode expressionTree, String alias, String originalExpression) {
            this.expressionTree = expressionTree;
            this.alias = alias;
            this.originalExpression = originalExpression;
        }
        
        public ExpressionNode getExpressionTree() {
            return expressionTree;
        }
        
        public String getAlias() {
            return alias;
        }
        
        public String getOriginalExpression() {
            return originalExpression;
        }
        
        public boolean hasAlias() {
            return alias != null && !alias.isEmpty();
        }
        
        public String getEffectiveColumnName() {
            return hasAlias() ? alias : originalExpression;
        }
        
        public Schema.DataType getDataType() {
            return expressionTree.getDataType();
        }
        
        public boolean isValidExpression() {
            return expressionTree.validateTypes();
        }
    }
}