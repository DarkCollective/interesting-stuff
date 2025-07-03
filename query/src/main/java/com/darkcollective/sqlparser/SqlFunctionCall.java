package com.darkcollective.sqlparser;

import java.util.*;

/**
 * Represents a SQL function call with its arguments and metadata
 */
public class SqlFunctionCall {
    private final String functionName;
    private final List<String> arguments;
    private final SqlFunctionRegistry.FunctionType type;
    private final String originalExpression;

    public SqlFunctionCall(String functionName, List<String> arguments, String originalExpression) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
        this.type = SqlFunctionRegistry.determineFunctionType(functionName);
        this.originalExpression = originalExpression;
    }

    public SqlFunctionCall(String functionName, List<String> arguments, SqlFunctionRegistry.FunctionType type, String originalExpression) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
        this.type = type;
        this.originalExpression = originalExpression;
    }

    public String getFunctionName() { 
        return functionName; 
    }
    
    public List<String> getArguments() { 
        return Collections.unmodifiableList(arguments); 
    }
    
    public SqlFunctionRegistry.FunctionType getType() { 
        return type; 
    }
    
    public String getOriginalExpression() { 
        return originalExpression; 
    }

    /**
     * Check if this is an aggregate function
     */
    public boolean isAggregate() {
        return type == SqlFunctionRegistry.FunctionType.AGGREGATE;
    }

    /**
     * Check if this is a string function
     */
    public boolean isString() {
        return type == SqlFunctionRegistry.FunctionType.STRING;
    }

    /**
     * Check if this is a numeric function
     */
    public boolean isNumeric() {
        return type == SqlFunctionRegistry.FunctionType.NUMERIC;
    }

    /**
     * Get the number of arguments
     */
    public int getArgumentCount() {
        return arguments.size();
    }

    /**
     * Reconstruct the function call as SQL
     */
    public String toSql() {
        return functionName + "(" + String.join(",", arguments) + ")";
    }

    @Override
    public String toString() {
        return "FUNCTION_CALL(" + type + ":" + functionName + "(" + String.join(",", arguments) + "))";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlFunctionCall that = (SqlFunctionCall) o;
        return Objects.equals(functionName, that.functionName) &&
               Objects.equals(arguments, that.arguments) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionName, arguments, type);
    }
}