package com.darkcollective.sqlparser;

import java.util.List;
import java.util.Collections;

/**
 * Represents a column reference in an expression tree
 */
public class ColumnNode extends ExpressionNode {
    private final String columnName;
    private final String tableName;
    
    public ColumnNode(String originalExpression, String columnName, String tableName, Schema.DataType dataType) {
        super(originalExpression);
        this.columnName = columnName;
        this.tableName = tableName;
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
        return dataType != null; // Valid if we have a type
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public String getTableName() {
        return tableName;
    }
}