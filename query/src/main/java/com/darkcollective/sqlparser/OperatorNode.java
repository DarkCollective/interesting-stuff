
package com.darkcollective.sqlparser;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents an operator in an expression tree
 */
public class OperatorNode extends ExpressionNode {
    private final String operator;
    private final List<ExpressionNode> children;
    
    // Define operator types
    private static final Set<String> NUMERIC_OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "%", "=", "!=", "<>", "<", ">", "<=", ">="
    ));
    
    private static final Set<String> STRING_OPERATORS = new HashSet<>(Arrays.asList(
        "+", "||", "LIKE", "NOT LIKE", "=", "!=", "<>"
    ));
    
    private static final Set<String> COMPARISON_OPERATORS = new HashSet<>(Arrays.asList(
        "=", "!=", "<>", "<", ">", "<=", ">="
    ));
    
    public OperatorNode(String operator, String originalExpression) {
        super(originalExpression);
        this.operator = operator;
        this.children = new ArrayList<>();
    }
    
    public void addChild(ExpressionNode child) {
        children.add(child);
    }
    
    @Override
    public List<ExpressionNode> getChildren() {
        return new ArrayList<>(children);
    }
    
    @Override
    public Schema.DataType getDataType() {
        if (dataType != null) {
            return dataType;
        }
        
        // Determine data type based on operator and operands
        if (COMPARISON_OPERATORS.contains(operator)) {
            dataType = Schema.DataType.BOOLEAN;
        } else if (operator.equals("+") || operator.equals("||")) {
            // For concatenation, check if all operands are strings
            boolean allStrings = children.stream()
                .allMatch(child -> child.getDataType() == Schema.DataType.VARCHAR || 
                                 child.getDataType() == Schema.DataType.TEXT);
            
            if (allStrings) {
                dataType = Schema.DataType.VARCHAR;
            } else {
                // If mixing types, check if it's valid numeric addition
                boolean allNumeric = children.stream()
                    .allMatch(child -> isNumericType(child.getDataType()));
                
                if (allNumeric) {
                    dataType = determineNumericResultType();
                } else {
                    // Mixed types - invalid
                    dataType = null;
                }
            }
        } else if (NUMERIC_OPERATORS.contains(operator)) {
            dataType = determineNumericResultType();
        } else {
            dataType = Schema.DataType.VARCHAR; // Default for unknown operators
        }
        
        return dataType;
    }
    
    private boolean isNumericType(Schema.DataType type) {
        return type == Schema.DataType.INTEGER || 
               type == Schema.DataType.DECIMAL || 
               type == Schema.DataType.BIGINT;
    }
    
    private Schema.DataType determineNumericResultType() {
        boolean hasDecimal = children.stream()
            .anyMatch(child -> child.getDataType() == Schema.DataType.DECIMAL);
        
        return hasDecimal ? Schema.DataType.DECIMAL : Schema.DataType.INTEGER;
    }
    
    @Override
    public boolean validateTypes() {
        // First validate all children
        if (!children.stream().allMatch(ExpressionNode::validateTypes)) {
            return false;
        }
        
        if (children.isEmpty()) {
            return false;
        }
        
        Schema.DataType resultType = getDataType();
        if (resultType == null) {
            return false; // Type inference failed
        }
        
        // Validate specific operator constraints
        if (operator.equals("+")) {
            // Addition can be numeric or string concatenation
            boolean allNumeric = children.stream()
                .allMatch(child -> isNumericType(child.getDataType()));
            
            boolean allString = children.stream()
                .allMatch(child -> child.getDataType() == Schema.DataType.VARCHAR || 
                                 child.getDataType() == Schema.DataType.TEXT);
            
            return allNumeric || allString;
        }
        
        if (NUMERIC_OPERATORS.contains(operator) && !operator.equals("+")) {
            // All other numeric operators require numeric operands
            return children.stream()
                .allMatch(child -> isNumericType(child.getDataType()));
        }
        
        if (operator.equals("||")) {
            // String concatenation requires string operands
            return children.stream()
                .allMatch(child -> child.getDataType() == Schema.DataType.VARCHAR || 
                                 child.getDataType() == Schema.DataType.TEXT);
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        if (children.size() == 2) {
            return "(" + children.get(0) + " " + operator + " " + children.get(1) + ")";
        } else if (children.size() == 1) {
            return operator + children.get(0);
        }
        return operator + "(" + String.join(", ", children.stream()
            .map(Object::toString).toArray(String[]::new)) + ")";
    }
    
    public String getOperator() {
        return operator;
    }
}