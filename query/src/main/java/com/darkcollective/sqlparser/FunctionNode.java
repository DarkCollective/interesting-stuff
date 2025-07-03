package com.darkcollective.sqlparser;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a function call in an expression tree
 */
public class FunctionNode extends ExpressionNode {
    private final String functionName;
    private final List<ExpressionNode> arguments;
    private final SqlFunctionRegistry.FunctionType functionType;

    public FunctionNode(String functionName, List<ExpressionNode> arguments, String originalExpression) {
        super(originalExpression);
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
        this.functionType = SqlFunctionRegistry.determineFunctionType(functionName);
        this.dataType = inferDataType();
    }

    public Schema.DataType getDataType() {
        // Get the function type from the registry
        SqlFunctionRegistry.FunctionType functionType = SqlFunctionRegistry.determineFunctionType(functionName);

        if (functionType == null) {
            // Unknown function - default to VARCHAR
            return Schema.DataType.VARCHAR;
        }

        switch (functionType) {
            case AGGREGATE:
                // Most aggregate functions return numeric types
                if ("COUNT".equalsIgnoreCase(functionName)) {
                    return Schema.DataType.INTEGER;
                } else if ("AVG".equalsIgnoreCase(functionName) ||
                        "SUM".equalsIgnoreCase(functionName) ||
                        "MIN".equalsIgnoreCase(functionName) ||
                        "MAX".equalsIgnoreCase(functionName)) {
                    // For these, try to infer from arguments, otherwise default to DECIMAL
                    if (!arguments.isEmpty()) {
                        Schema.DataType argType = arguments.get(0).getDataType();
                        if (argType != null) {
                            return argType;
                        }
                    }
                    return Schema.DataType.DECIMAL;
                }
                return Schema.DataType.DECIMAL;

            case STRING:
                return Schema.DataType.VARCHAR;

            case NUMERIC:
                // For numeric functions, try to infer from arguments
                if (!arguments.isEmpty()) {
                    Schema.DataType argType = arguments.get(0).getDataType();
                    if (argType == Schema.DataType.INTEGER || argType == Schema.DataType.DECIMAL) {
                        return argType;
                    }
                }
                return Schema.DataType.DECIMAL;

            case DATE:
                return Schema.DataType.DATE;

            default:
                return Schema.DataType.VARCHAR;
        }
     }

    @Override
    public List<ExpressionNode> getChildren() {
        return new ArrayList<>(arguments);
    }

    @Override
    public String toString() {
        List<String> argStrings = new ArrayList<>();
        for (ExpressionNode arg : arguments) {
            argStrings.add(arg.toString());
        }
        return functionName + "(" + String.join(", ", argStrings) + ")";
    }

    @Override
    public boolean validateTypes() {
        // First validate all arguments
        if (!arguments.stream().allMatch(ExpressionNode::validateTypes)) {
            return false;
        }

        // Validate function-specific constraints
        return validateFunctionArguments();
    }

    private Schema.DataType inferDataType() {
        switch (functionType) {
            case AGGREGATE:
                if (functionName.equalsIgnoreCase("COUNT")) {
                    return Schema.DataType.INTEGER;
                } else if (functionName.equalsIgnoreCase("SUM") || functionName.equalsIgnoreCase("AVG")) {
                    // Return the data type of the argument, or DECIMAL if mixed
                    if (!arguments.isEmpty()) {
                        Schema.DataType argType = arguments.get(0).getDataType();
                        return (argType == Schema.DataType.INTEGER || argType == Schema.DataType.DECIMAL) ? 
                               argType : Schema.DataType.DECIMAL;
                    }
                    return Schema.DataType.DECIMAL;
                }
                return Schema.DataType.VARCHAR; // Default for unknown aggregates
                
            case STRING:
                if (functionName.equalsIgnoreCase("LENGTH") || functionName.equalsIgnoreCase("LEN")) {
                    return Schema.DataType.INTEGER;
                }
                return Schema.DataType.VARCHAR;
                
            case NUMERIC:
                return Schema.DataType.DECIMAL; // Most numeric functions return decimal
                
            case DATE:
                return Schema.DataType.TIMESTAMP;
                
            default:
                return Schema.DataType.VARCHAR;
        }
    }

    private boolean validateFunctionArguments() {
        switch (functionType) {
            case STRING:
                // String functions should have string arguments
                return arguments.stream().allMatch(arg -> 
                    arg.getDataType() == Schema.DataType.VARCHAR || 
                    arg.getDataType() == Schema.DataType.TEXT);
                    
            case NUMERIC:
                // Numeric functions should have numeric arguments
                return arguments.stream().allMatch(arg -> 
                    arg.getDataType() == Schema.DataType.INTEGER || 
                    arg.getDataType() == Schema.DataType.DECIMAL ||
                    arg.getDataType() == Schema.DataType.BIGINT);
                    
            case AGGREGATE:
                // Aggregate functions are generally flexible
                return true;
                
            default:
                return true;
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ExpressionNode> getArguments() {
        return new ArrayList<>(arguments);
    }

    public SqlFunctionRegistry.FunctionType getFunctionType() {
        return functionType;
    }

    public SqlFunctionCall toSqlFunctionCall() {
        List<String> argStrings = new ArrayList<>();
        for (ExpressionNode arg : arguments) {
            argStrings.add(arg.getOriginalExpression());
        }
        return new SqlFunctionCall(functionName, argStrings, originalExpression);
    }
}