package com.darkcollective.sqlparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProjectionOperator extends RelationalOperator {
    private final List<String> columns;
    private final boolean distinct;
    private final List<SqlFunctionCall> functionCalls;
    private final List<SqlExpressionParser.SelectItem> selectItems;

    public ProjectionOperator(List<String> columns, boolean distinct) {
        this.columns = new ArrayList<>(columns);
        this.distinct = distinct;
        this.functionCalls = new ArrayList<>();
        this.selectItems = new ArrayList<>();
    }
    @Override
    protected String getOperatorName() {
        return "PROJECTION";
    }

    @Override
    protected String getOperatorParameters() {
        StringBuilder params = new StringBuilder();

        if (distinct) {
            params.append("DISTINCT, ");
        }

        if (hasAliases()) {
            params.append(String.join(", ", getColumnsWithAliases()));
        } else {
            params.append(String.join(", ", columns));
        }

        return params.toString();
    }

    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<SqlFunctionCall> getFunctionCalls() {
        return Collections.unmodifiableList(functionCalls);
    }

    public List<SqlExpressionParser.SelectItem> getSelectItems() {
        return Collections.unmodifiableList(selectItems);
    }

    public void addFunctionCall(SqlFunctionCall functionCall) {
        this.functionCalls.add(functionCall);
    }

    public void addSelectItem(SqlExpressionParser.SelectItem selectItem) {
        this.selectItems.add(selectItem);
    }

    /**
     * Get column names with their aliases (if any)
     */
    public List<String> getColumnsWithAliases() {
        if (selectItems.isEmpty()) {
            return getColumns();
        }

        return selectItems.stream()
                .map(item -> item.hasAlias() ?
                        item.getExpression() + " AS " + item.getAlias() :
                        item.getExpression())
                .collect(Collectors.toList());
    }

    /**
     * Get only the alias names (output column names)
     */
    public List<String> getOutputColumnNames() {
        if (selectItems.isEmpty()) {
            return getColumns();
        }

        return selectItems.stream()
                .map(item -> item.hasAlias() ? item.getAlias() : item.getExpression())
                .collect(Collectors.toList());
    }

    /**
     * Get original expressions without aliases
     */
    public List<String> getExpressions() {
        if (selectItems.isEmpty()) {
            return getColumns();
        }

        return selectItems.stream()
                .map(SqlExpressionParser.SelectItem::getExpression)
                .collect(Collectors.toList());
    }

    /**
     * Check if any columns have aliases
     */
    public boolean hasAliases() {
        return selectItems.stream().anyMatch(SqlExpressionParser.SelectItem::hasAlias);
    }

    /**
     * Get only aggregate function calls
     */
    public List<SqlFunctionCall> getAggregateFunctions() {
        return functionCalls.stream()
                .filter(SqlFunctionCall::isAggregate)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Get only string function calls
     */
    public List<SqlFunctionCall> getStringFunctions() {
        return functionCalls.stream()
                .filter(SqlFunctionCall::isString)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Get only numeric function calls
     */
    public List<SqlFunctionCall> getNumericFunctions() {
        return functionCalls.stream()
                .filter(SqlFunctionCall::isNumeric)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECTION(");
        if (distinct) sb.append("DISTINCT ");

        // Use aliased columns if available, otherwise use regular columns
        if (hasAliases()) {
            sb.append(getColumnsWithAliases().stream()
                    .collect(Collectors.joining(", ")));
        } else {
            sb.append(String.join(", ", columns));
        }

        sb.append(")");

        if (!functionCalls.isEmpty()) {
            sb.append("\n");
            sb.append("FUNCTIONS: ");
            sb.append(functionCalls.stream()
                    .map(SqlFunctionCall::toString)
                    .collect(Collectors.joining(", ")));
        }

        sb.append("\n");
        for (RelationalOperator child : children) {
            sb.append(indent(child.toTreeString(), 1)).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        if (distinct) sql.append("DISTINCT ");

        // Use aliased columns for SQL generation
        if (hasAliases()) {
            sql.append(getColumnsWithAliases().stream()
                    .collect(Collectors.joining(", ")));
        } else {
            sql.append(String.join(", ", columns));
        }

        if (!children.isEmpty()) {
            RelationalOperator child = children.get(0);
            if (child instanceof TableScanOperator) {
                sql.append(" FROM ").append(child.toSql());
            } else {
                sql.append(" ").append(child.toSql());
            }
        }

        return sql.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectionOperator that = (ProjectionOperator) o;
        return distinct == that.distinct &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(functionCalls, that.functionCalls) &&
                Objects.equals(selectItems, that.selectItems) &&
                Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, distinct, functionCalls, selectItems, children);
    }

}
