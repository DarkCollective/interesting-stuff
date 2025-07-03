package com.darkcollective.sqlparser;

import java.util.*;

// Table scan operator
class TableScanOperator extends RelationalOperator {
    private final String tableName;
    private final String alias;

    @Override
    protected String getOperatorName() {
        return "TABLE_SCAN";
    }

    @Override
    protected String getOperatorParameters() {
        return alias != null ? tableName + " AS " + alias : tableName;
    }

    public TableScanOperator(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
    }

    public String getTableName() { return tableName; }
    public String getAlias() { return alias; }
    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }

    @Override
    public String toTreeString() {
        return "TABLE_SCAN(" + tableName + (alias != null ? " AS " + alias : "") + ")";
    }

    @Override
    public String toSql() {
        return tableName + (alias != null ? " AS " + alias : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableScanOperator that = (TableScanOperator) o;
        return Objects.equals(tableName, that.tableName) &&
                Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, alias);
    }
}


// Selection operator (WHERE clause)
class SelectionOperator extends RelationalOperator {
    private final String condition;

    public SelectionOperator(String condition) {
        this.condition = condition;
    }

    public String getCondition() { return condition; }
    @Override
    protected String getOperatorName() {
        return "SELECTION";
    }

    @Override
    protected String getOperatorParameters() {
        return condition;
    }

    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECTION(").append(condition).append(")\n");
        for (RelationalOperator child : children) {
            sb.append(indent(child.toTreeString(), 1)).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toSql() {
        return children.get(0).toSql();
    }
}

// Join operator
class JoinOperator extends RelationalOperator {
    public enum JoinType {
        INNER, LEFT, RIGHT, FULL, CROSS
    }

    private final JoinType joinType;
    private final String joinCondition;

    public JoinOperator(JoinType joinType, String joinCondition) {
        this.joinType = joinType;
        this.joinCondition = joinCondition;
    }

    @Override
    protected String getOperatorName() {
        return joinType.name() + "_JOIN";
    }

    @Override
    protected String getOperatorParameters() {
        return joinCondition;
    }

    public JoinType getJoinType() { return joinType; }
    public String getJoinCondition() { return joinCondition; }

    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append(joinType.name()).append("_JOIN");
        if (joinCondition != null && !joinCondition.isEmpty()) {
            sb.append("(").append(joinCondition).append(")");
        }
        sb.append("\n");

        for (RelationalOperator child : children) {
            sb.append(indent(child.toTreeString(), 1)).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toSql() {
        if (children.size() < 2) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(children.get(0).toSql());
        sb.append(" ").append(joinType.name());
        if (joinType != JoinType.CROSS) sb.append(" JOIN ");
        else sb.append(" JOIN ");
        sb.append(children.get(1).toSql());

        if (joinCondition != null && !joinCondition.isEmpty()) {
            sb.append(" ON ").append(joinCondition);
        }

        return sb.toString();
    }
}

// Aggregation operator (GROUP BY)
class AggregationOperator extends RelationalOperator {
    private final List<String> groupByColumns;
    private final List<String> aggregateFunctions;
    private final String havingCondition;

    public AggregationOperator(List<String> groupByColumns, List<String> aggregateFunctions, String havingCondition) {
        this.groupByColumns = new ArrayList<>(groupByColumns != null ? groupByColumns : Collections.emptyList());
        this.aggregateFunctions = new ArrayList<>(aggregateFunctions != null ? aggregateFunctions : Collections.emptyList());
        this.havingCondition = havingCondition;
    }

    public List<String> getGroupByColumns() { return groupByColumns; }
    public List<String> getAggregateFunctions() { return aggregateFunctions; }
    public String getHavingCondition() { return havingCondition; }

    @Override
    protected String getOperatorName() {
        return "AGGREGATION";
    }

    @Override
    protected String getOperatorParameters() {
        List<String> params = new ArrayList<>();

        if (!groupByColumns.isEmpty()) {
            params.add("GROUP_BY:" + String.join(",", groupByColumns));
        }
        if (!aggregateFunctions.isEmpty()) {
            params.add("AGG:" + String.join(",", aggregateFunctions));
        }
        if (havingCondition != null) {
            params.add("HAVING:" + havingCondition);
        }

        return String.join(", ", params);
    }


    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AGGREGATION(");
        if (!groupByColumns.isEmpty()) {
            sb.append("GROUP BY: ").append(String.join(", ", groupByColumns));
        }
        if (!aggregateFunctions.isEmpty()) {
            if (!groupByColumns.isEmpty()) sb.append("; ");
            sb.append("AGG: ").append(String.join(", ", aggregateFunctions));
        }
        if (havingCondition != null && !havingCondition.isEmpty()) {
            sb.append("; HAVING: ").append(havingCondition);
        }
        sb.append(")\n");

        for (RelationalOperator child : children) {
            sb.append(indent(child.toTreeString(), 1)).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toSql() {
        return children.get(0).toSql();
    }
}

// Sort operator (ORDER BY)
class SortOperator extends RelationalOperator {
    private final List<String> orderByColumns;

    public SortOperator(List<String> orderByColumns) {
        this.orderByColumns = new ArrayList<>(orderByColumns);
    }

    public List<String> getOrderByColumns() { return orderByColumns; }
    @Override
    protected String getOperatorName() {
        return "SORT";
    }

    @Override
    protected String getOperatorParameters() {
        return String.join(", ", orderByColumns);
    }

    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SORT(").append(String.join(", ", orderByColumns)).append(")\n");
        for (RelationalOperator child : children) {
            sb.append(indent(child.toTreeString(), 1)).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toSql() {
        return children.get(0).toSql();
    }
}

// Function call operator
class FunctionOperator extends RelationalOperator {
    private final String functionName;
    private final List<String> arguments;

    public FunctionOperator(String functionName, List<String> arguments) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
    }

    public String getFunctionName() { return functionName; }
    public List<String> getArguments() { return arguments; }
    @Override
    protected String getOperatorName() {
        return "FUNCTION";
    }

    @Override
    protected String getOperatorParameters() {
        List<String> params = new ArrayList<>();
        params.add("NAME:" + functionName);
        if (!arguments.isEmpty()) {
            params.add("ARGS:" + String.join(",", arguments));
        }
        return String.join(",", params);
    }


    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FUNCTION_CALL(").append(functionName);
        if (!arguments.isEmpty()) {
            sb.append(": ").append(String.join(",", arguments));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toSql() {
        return functionName + "(" + String.join(",", arguments) + ")";
    }
}

