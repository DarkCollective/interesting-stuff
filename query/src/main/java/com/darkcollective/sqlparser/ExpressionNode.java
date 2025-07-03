
package com.darkcollective.sqlparser;

import java.util.List;

/**
 * Abstract base class for expression nodes in an expression tree
 */
public abstract class ExpressionNode {
    protected Schema.DataType dataType;
    protected String originalExpression;
    
    public ExpressionNode(String originalExpression) {
        this.originalExpression = originalExpression;
    }
    
    public abstract Schema.DataType getDataType();
    public abstract List<ExpressionNode> getChildren();
    public abstract String toString();
    
    public String getOriginalExpression() {
        return originalExpression;
    }
    
    public boolean isLeaf() {
        return getChildren().isEmpty();
    }
    
    /**
     * Validate the expression tree for type compatibility
     */
    public abstract boolean validateTypes();
}