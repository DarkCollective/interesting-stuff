package com.darkcollective.sqlparser;

import java.util.List;
import java.util.Collections;

/**
 * Represents a literal value in an expression tree
 */
public class LiteralNode extends ExpressionNode {
    private final Object value;
    
    public LiteralNode(String originalExpression, Object value, Schema.DataType dataType) {
        super(originalExpression);
        this.value = value;
        this.dataType = dataType;
    }
    
    @Override
    public Schema.DataType getDataType() {
        return dataType;
    }
    
    @Override
    public List<ExpressionNode> getChildren() {
        return Collections.emptyList();
    }
    
    @Override
    public String toString() {
        return originalExpression;
    }
    
    @Override
    public boolean validateTypes() {
        return true; // Literals are always valid
    }
    
    public Object getValue() {
        return value;
    }
    
    public static LiteralNode createNumeric(String expression) {
        try {
            if (expression.contains(".")) {
                return new LiteralNode(expression, Double.parseDouble(expression), Schema.DataType.DECIMAL);
            } else {
                return new LiteralNode(expression, Integer.parseInt(expression), Schema.DataType.INTEGER);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric literal: " + expression);
        }
    }
    
    public static LiteralNode createString(String expression) {
        // Remove quotes from string literals
        String value = expression;
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            value = expression.substring(1, expression.length() - 1);
        } else if (expression.startsWith("'") && expression.endsWith("'")) {
            value = expression.substring(1, expression.length() - 1);
        }
        return new LiteralNode(expression, value, Schema.DataType.VARCHAR);
    }
}