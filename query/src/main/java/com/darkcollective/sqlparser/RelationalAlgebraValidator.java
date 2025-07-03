package com.darkcollective.sqlparser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates relational algebra trees against a database schema
 */
public class RelationalAlgebraValidator {
    private final Schema schema;
    private static final Pattern COLUMN_REFERENCE_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\b");

    public RelationalAlgebraValidator(Schema schema) {
        this.schema = schema;
    }

    /**
     * Validate a relational algebra tree
     */
    public ValidationResult validate(RelationalOperator root) {
        ValidationContext context = new ValidationContext();
        validateOperator(root, context);
        return new ValidationResult(context.getErrors(), context.getWarnings());
    }

    /**
     * Recursively validate an operator and its children
     */
    private AvailableColumns validateOperator(RelationalOperator operator, ValidationContext context) {
        if (operator instanceof TableScanOperator) {
            return validateTableScan((TableScanOperator) operator, context);
        } else if (operator instanceof ProjectionOperator) {
            return validateProjection((ProjectionOperator) operator, context);
        } else if (operator instanceof SelectionOperator) {
            return validateSelection((SelectionOperator) operator, context);
        } else if (operator instanceof JoinOperator) {
            return validateJoin((JoinOperator) operator, context);
        } else if (operator instanceof AggregationOperator) {
            return validateAggregation((AggregationOperator) operator, context);
        } else if (operator instanceof SortOperator) {
            return validateSort((SortOperator) operator, context);
        } else if (operator instanceof SubqueryOperator) {
            return validateSubquery((SubqueryOperator) operator, context);
        } else {
            context.addError("Unknown operator type: " + operator.getClass().getSimpleName());
            return new AvailableColumns();
        }
    }

    private AvailableColumns validateTableScan(TableScanOperator tableScan, ValidationContext context) {
        String tableName = tableScan.getTableName();
        String alias = tableScan.getAlias();

        // Check if table exists in schema
        if (!schema.hasTable(tableName)) {
            context.addError("Table '" + tableName + "' does not exist in schema");
            return new AvailableColumns();
        }

        Schema.Table table = schema.getTable(tableName);
        AvailableColumns availableColumns = new AvailableColumns();

        // Add all columns from the table
        String effectiveTableName = alias != null ? alias : tableName;
        for (Schema.Column column : table.getColumns()) {
            availableColumns.addColumn(effectiveTableName, column.getName(), column.getDataType());
            // Also allow unqualified column names if there's no alias conflict
            availableColumns.addColumn(null, column.getName(), column.getDataType());
        }

        return availableColumns;
    }

    private AvailableColumns validateProjection(ProjectionOperator projection, ValidationContext context) {
        // First validate children
        AvailableColumns childColumns = new AvailableColumns();
        for (RelationalOperator child : projection.getChildren()) {
            childColumns.merge(validateOperator(child, context));
        }

        AvailableColumns outputColumns = new AvailableColumns();

        // Validate each projected column/expression
        for (String column : projection.getColumns()) {
            if ("*".equals(column)) {
                // SELECT * - include all available columns
                outputColumns.merge(childColumns);
            } else if (column.contains("(") && column.contains(")")) {
                // Function call - validate function arguments
                validateFunctionInExpression(column, childColumns, context);
                // For now, assume function returns a generic type
                outputColumns.addColumn(null, column, Schema.DataType.VARCHAR);
            } else {
                // Regular column reference
                if (!validateColumnReference(column, childColumns, context)) {
                    context.addError("Column '" + column + "' is not available in projection");
                } else {
                    // Add to output columns
                    ColumnInfo columnInfo = childColumns.findColumn(column);
                    if (columnInfo != null) {
                        outputColumns.addColumn(null, column, columnInfo.dataType);
                    }
                }
            }
        }

        // Validate function calls if any
        for (SqlFunctionCall functionCall : projection.getFunctionCalls()) {
            validateFunctionCall(functionCall, childColumns, context);
        }

        return outputColumns;
    }

    private AvailableColumns validateSelection(SelectionOperator selection, ValidationContext context) {
        // First validate children
        AvailableColumns childColumns = new AvailableColumns();
        for (RelationalOperator child : selection.getChildren()) {
            childColumns.merge(validateOperator(child, context));
        }

        // Validate condition
        validateCondition(selection.getCondition(), childColumns, context);

        // Selection doesn't change available columns
        return childColumns;
    }

    private AvailableColumns validateJoin(JoinOperator join, ValidationContext context) {
        if (join.getChildren().size() != 2) {
            context.addError("Join operator must have exactly 2 children");
            return new AvailableColumns();
        }

        AvailableColumns leftColumns = validateOperator(join.getChildren().get(0), context);
        AvailableColumns rightColumns = validateOperator(join.getChildren().get(1), context);

        // Check for column name conflicts
        checkColumnConflicts(leftColumns, rightColumns, context);

        // Validate join condition if present
        if (join.getJoinCondition() != null && !join.getJoinCondition().isEmpty()) {
            AvailableColumns combinedColumns = new AvailableColumns();
            combinedColumns.merge(leftColumns);
            combinedColumns.merge(rightColumns);
            validateCondition(join.getJoinCondition(), combinedColumns, context);
        }

        // Join result includes columns from both sides
        AvailableColumns result = new AvailableColumns();
        result.merge(leftColumns);
        result.merge(rightColumns);
        return result;
    }

    private AvailableColumns validateAggregation(AggregationOperator aggregation, ValidationContext context) {
        // First validate children
        AvailableColumns childColumns = new AvailableColumns();
        for (RelationalOperator child : aggregation.getChildren()) {
            childColumns.merge(validateOperator(child, context));
        }

        // Validate GROUP BY columns
        for (String groupByColumn : aggregation.getGroupByColumns()) {
            if (!validateColumnReference(groupByColumn, childColumns, context)) {
                context.addError("GROUP BY column '" + groupByColumn + "' is not available");
            }
        }

        // Validate aggregate functions
        for (String aggFunction : aggregation.getAggregateFunctions()) {
            validateFunctionInExpression(aggFunction, childColumns, context);
        }

        // After aggregation, only GROUP BY columns and aggregate functions are available
        AvailableColumns postAggregationColumns = new AvailableColumns();
        for (String groupByColumn : aggregation.getGroupByColumns()) {
            ColumnInfo columnInfo = childColumns.findColumn(groupByColumn);
            if (columnInfo != null) {
                postAggregationColumns.addColumn(columnInfo.tableName, columnInfo.columnName, columnInfo.dataType);
            }
        }

        // Add aggregate functions as available columns
        for (String aggFunction : aggregation.getAggregateFunctions()) {
            postAggregationColumns.addColumn(null, aggFunction, Schema.DataType.DECIMAL);
        }

        // Validate HAVING condition - special handling for aggregate functions
        if (aggregation.getHavingCondition() != null) {
            validateHavingCondition(aggregation.getHavingCondition(), postAggregationColumns, aggregation.getAggregateFunctions(), context);
        }

        return postAggregationColumns;
    }

    private void validateHavingCondition(String condition, AvailableColumns availableColumns, List<String> aggregateFunctions, ValidationContext context) {
        // Extract all function calls from the condition
        List<SqlFunctionCall> functions = SqlExpressionParser.extractFunctionCalls(condition);

        // For each function call, validate it
        for (SqlFunctionCall func : functions) {
            if (func.isAggregate()) {
                // Aggregate functions in HAVING are allowed - just validate their arguments exist in original table
                // For simplicity, we'll allow any aggregate function here
            } else {
                // Non-aggregate functions need their arguments validated against available columns
                validateFunctionCall(func, availableColumns, context);
            }
        }

        // Extract column references (excluding those inside function calls)
        String conditionForColumnCheck = condition;
        for (SqlFunctionCall func : functions) {
            conditionForColumnCheck = conditionForColumnCheck.replace(func.toSql(), "");
        }

        Matcher matcher = COLUMN_REFERENCE_PATTERN.matcher(conditionForColumnCheck);
        while (matcher.find()) {
            String columnRef = matcher.group(1);

            if (isLiteralOrKeyword(columnRef)) {
                continue;
            }

            if (!availableColumns.hasColumn(columnRef)) {
                context.addError("Column '" + columnRef + "' in HAVING condition is not available. Only GROUP BY columns and aggregate functions are allowed in HAVING.");
            }
        }
    }


    private AvailableColumns validateSort(SortOperator sort, ValidationContext context) {
        // First validate children
        AvailableColumns childColumns = new AvailableColumns();
        for (RelationalOperator child : sort.getChildren()) {
            childColumns.merge(validateOperator(child, context));
        }

        // Validate ORDER BY columns
        for (String orderByColumn : sort.getOrderByColumns()) {
            // Skip ASC/DESC keywords
            if ("ASC".equalsIgnoreCase(orderByColumn) || "DESC".equalsIgnoreCase(orderByColumn)) {
                continue;
            }
            if (!validateColumnReference(orderByColumn, childColumns, context)) {
                context.addError("ORDER BY column '" + orderByColumn + "' is not available");
            }
        }

        // Sort doesn't change available columns
        return childColumns;
    }

    private AvailableColumns validateSubquery(SubqueryOperator subquery, ValidationContext context) {
        // Validate the subquery itself
        AvailableColumns subqueryColumns = validateOperator(subquery.getSubquery(), context);

        // Create new available columns with the subquery alias
        AvailableColumns result = new AvailableColumns();
        String effectiveAlias = subquery.hasAlias() ? subquery.getAlias() : subquery.getEffectiveTableName();

        for (ColumnInfo columnInfo : subqueryColumns.getAllColumns()) {
            result.addColumn(effectiveAlias, columnInfo.columnName, columnInfo.dataType);
        }

        return result;
    }

    private boolean validateColumnReference(String columnRef, AvailableColumns availableColumns, ValidationContext context) {
        return availableColumns.hasColumn(columnRef);
    }


    private void validateCondition(String condition, AvailableColumns availableColumns, ValidationContext context) {
        // First, extract and validate all function calls
        List<SqlFunctionCall> functions = SqlExpressionParser.extractFunctionCalls(condition);
        for (SqlFunctionCall func : functions) {
            validateFunctionCall(func, availableColumns, context);
        }

        // Remove string literals and function calls before extracting column references
        String conditionForColumnCheck = removeStringLiterals(condition);

        // Remove function calls to avoid false column matches
        for (SqlFunctionCall func : functions) {
            conditionForColumnCheck = conditionForColumnCheck.replace(func.getOriginalExpression(), "FUNC_PLACEHOLDER");
        }

        // Extract column references from the cleaned condition
        Matcher matcher = COLUMN_REFERENCE_PATTERN.matcher(conditionForColumnCheck);
        while (matcher.find()) {
            String columnRef = matcher.group(1);

            // Skip SQL keywords, literals, and function placeholders
            if (isLiteralOrKeyword(columnRef) || "FUNC_PLACEHOLDER".equals(columnRef)) {
                continue;
            }

            if (!validateColumnReference(columnRef, availableColumns, context)) {
                context.addError("Column '" + columnRef + "' in condition '" + condition + "' is not available");
            }
        }
    }

    /**
     * Validate a function call and its arguments
     */
    private void validateFunctionCall(SqlFunctionCall functionCall, AvailableColumns availableColumns, ValidationContext context) {
        // Validate each argument of the function
        for (String arg : functionCall.getArguments()) {
            // Skip wildcards and literals
            if ("*".equals(arg) || isLiteralOrKeyword(arg)) {
                continue;
            }

            // Check if argument is a column reference
            if (!validateColumnReference(arg, availableColumns, context)) {
                context.addError("Function argument '" + arg + "' in function '" + functionCall.getFunctionName() + "' is not available");
            }
        }
    }



    private void validateFunctionInExpression(String expression, AvailableColumns availableColumns, ValidationContext context) {
        List<SqlFunctionCall> functions = SqlExpressionParser.extractFunctionCalls(expression);
        for (SqlFunctionCall function : functions) {
            validateFunctionCall(function, availableColumns, context);
        }
    }

    private void checkColumnConflicts(AvailableColumns left, AvailableColumns right, ValidationContext context) {
        for (ColumnInfo leftCol : left.getAllColumns()) {
            for (ColumnInfo rightCol : right.getAllColumns()) {
                if (leftCol.columnName.equalsIgnoreCase(rightCol.columnName) && 
                    leftCol.tableName == null && rightCol.tableName == null) {
                    context.addWarning("Ambiguous column name '" + leftCol.columnName + 
                                     "' exists in both sides of join");
                }
            }
        }
    }

    private boolean isLiteralOrKeyword(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        String trimmed = token.trim();

        // Check for numeric literals (integers and decimals)
        if (trimmed.matches("\\d+(\\.\\d+)?")) {
            return true;
        }

        // Check for string literals (single quotes)
        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return true;
        }

        // Check for SQL keywords
        Set<String> keywords = Set.of(
                "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS",
                "GROUP", "BY", "HAVING", "ORDER", "DISTINCT", "ON", "AS", "AND", "OR", "NOT",
                "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE", "ASC", "DESC",
                "WHEN", "THEN", "ELSE", "CASE", "END", "UNION", "ALL", "INTERSECT", "EXCEPT"
        );

        return keywords.contains(trimmed.toUpperCase());
    }

    /**
     * Remove string literals from a SQL expression to avoid false column matches
     */
    private String removeStringLiterals(String expression) {
        StringBuilder result = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '\'' && !inDoubleQuotes) {
                if (inSingleQuotes) {
                    // Check for escaped quote
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '\'') {
                        i++; // Skip the next quote
                    } else {
                        inSingleQuotes = false;
                    }
                } else {
                    inSingleQuotes = true;
                }
                result.append(' '); // Replace with space to maintain word boundaries
            } else if (c == '"' && !inSingleQuotes) {
                if (inDoubleQuotes) {
                    // Check for escaped quote
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '"') {
                        i++; // Skip the next quote
                    } else {
                        inDoubleQuotes = false;
                    }
                } else {
                    inDoubleQuotes = true;
                }
                result.append(' '); // Replace with space to maintain word boundaries
            } else if (inSingleQuotes || inDoubleQuotes) {
                result.append(' '); // Replace literal content with space
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
    /**
     * Tracks available columns at each level of the query tree
     */
    private static class AvailableColumns {
        private final List<ColumnInfo> columns = new ArrayList<>();

        public void addColumn(String tableName, String columnName, Schema.DataType dataType) {
            columns.add(new ColumnInfo(tableName, columnName, dataType));
        }

        public void merge(AvailableColumns other) {
            columns.addAll(other.columns);
        }

        public boolean hasColumn(String columnRef) {
            if (columnRef.contains(".")) {
                String[] parts = columnRef.split("\\.", 2);
                String tableAlias = parts[0];
                String columnName = parts[1];
                return columns.stream().anyMatch(col -> 
                    Objects.equals(col.tableName, tableAlias) && 
                    col.columnName.equalsIgnoreCase(columnName));
            } else {
                return columns.stream().anyMatch(col -> 
                    col.columnName.equalsIgnoreCase(columnRef));
            }
        }

        public ColumnInfo findColumn(String columnRef) {
            if (columnRef.contains(".")) {
                String[] parts = columnRef.split("\\.", 2);
                String tableAlias = parts[0];
                String columnName = parts[1];
                return columns.stream()
                    .filter(col -> Objects.equals(col.tableName, tableAlias) && 
                                  col.columnName.equalsIgnoreCase(columnName))
                    .findFirst()
                    .orElse(null);
            } else {
                return columns.stream()
                    .filter(col -> col.columnName.equalsIgnoreCase(columnRef))
                    .findFirst()
                    .orElse(null);
            }
        }

        public List<ColumnInfo> getAllColumns() {
            return new ArrayList<>(columns);
        }
    }

    private static class ColumnInfo {
        final String tableName;
        final String columnName;
        final Schema.DataType dataType;

        ColumnInfo(String tableName, String columnName, Schema.DataType dataType) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.dataType = dataType;
        }
    }

    /**
     * Collects validation errors and warnings
     */
    private static class ValidationContext {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
    }

    /**
     * Result of validation containing errors and warnings
     */
    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{valid=").append(isValid());
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append("}");
            return sb.toString();
        }
    }
}