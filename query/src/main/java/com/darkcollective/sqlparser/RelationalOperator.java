package com.darkcollective.sqlparser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Base class for all relational algebra operators
public abstract class RelationalOperator {
    protected List<RelationalOperator> children = new ArrayList<>();

    public void addChild(RelationalOperator child) {
        children.add(child);
    }

    public List<RelationalOperator> getChildren() {
        return children;
    }

    public RelationalOperator getFirstChild() {
        return children.isEmpty() ? null : children.get(0);
    }
    public RelationalOperator getLastChild() {
        return children.isEmpty() ? null : children.get(children.size() - 1);
    }

    public abstract String toSql();
    public abstract String toTreeString();
    /**
     * Generate parenthetical notation representation
     */
    public String toParenthetical() {
        StringBuilder sb = new StringBuilder();
        sb.append(getOperatorName()).append("(");

        // Add operator-specific parameters
        String params = getOperatorParameters();
        if (params != null && !params.isEmpty()) {
            sb.append(params);
        }

        // Add children
        if (!children.isEmpty()) {
            if (params != null && !params.isEmpty()) {
                sb.append(", ");
            }
            sb.append(children.stream()
                    .map(RelationalOperator::toParenthetical)
                    .collect(Collectors.joining(", ")));
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Get the operator name for parenthetical notation
     */
    protected abstract String getOperatorName();

    /**
     * Get operator-specific parameters for parenthetical notation
     */
    protected abstract String getOperatorParameters();

    protected String indent(String text, int level) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");
        for (String line : lines) {
            for (int i = 0; i < level; i++) {
                sb.append("  ");
            }
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }





}

