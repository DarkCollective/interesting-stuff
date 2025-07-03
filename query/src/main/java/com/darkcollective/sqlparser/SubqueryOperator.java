package com.darkcollective.sqlparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a subquery (nested SELECT statement) in the relational algebra tree
 */
public class SubqueryOperator extends RelationalOperator {
    private final String alias;
    private final RelationalOperator subquery;
    private final SubqueryType type;

    public enum SubqueryType {
        FROM_CLAUSE,     // Subquery in FROM clause: SELECT * FROM (SELECT ...) AS sub
        WHERE_EXISTS,    // EXISTS subquery: WHERE EXISTS (SELECT ...)
        WHERE_IN,        // IN subquery: WHERE col IN (SELECT ...)
        WHERE_NOT_IN,    // NOT IN subquery: WHERE col NOT IN (SELECT ...)
        WHERE_SCALAR,    // Scalar subquery: WHERE col = (SELECT ...)
        SELECT_SCALAR    // Scalar subquery in SELECT: SELECT (SELECT ...) FROM ...
    }

    public SubqueryOperator(RelationalOperator subquery, String alias, SubqueryType type) {
        this.subquery = subquery;
        this.alias = alias;
        this.type = type;
        // Add the subquery as a child for tree structure
        this.addChild(subquery);
    }

    public SubqueryOperator(RelationalOperator subquery, SubqueryType type) {
        this(subquery, null, type);
    }

    @Override
    protected String getOperatorName() {
        return "SUBQUERY";
    }

    @Override
    protected String getOperatorParameters() {
        List<String> params = new ArrayList<>();
        params.add("TYPE:" + type.name());
        if (alias != null) {
            params.add("ALIAS:" + alias);
        }
        return String.join(", ", params);
    }

    public String getAlias() {
        return alias;
    }

    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }

    public RelationalOperator getSubquery() {
        return subquery;
    }

    public SubqueryType getType() {
        return type;
    }

    /**
     * Get the effective table name for this subquery (alias if present, otherwise generated name)
     */
    public String getEffectiveTableName() {
        if (hasAlias()) {
            return alias;
        }
        return "subquery_" + System.identityHashCode(this);
    }

    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SUBQUERY(").append(type);
        if (hasAlias()) {
            sb.append(" AS ").append(alias);
        }
        sb.append(")\n");

        // Add the subquery tree indented
        sb.append(indent(subquery.toTreeString(), 1));

        return sb.toString();
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("(").append(subquery.toSql()).append(")");
        if (hasAlias()) {
            sql.append(" AS ").append(alias);
        }
        return sql.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubqueryOperator that = (SubqueryOperator) o;
        return Objects.equals(alias, that.alias) &&
                Objects.equals(subquery, that.subquery) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, subquery, type);
    }

    @Override
    public String toString() {
        return "SubqueryOperator{" +
                "alias='" + alias + '\'' +
                ", type=" + type +
                ", subquery=" + subquery +
                '}';
    }
}
