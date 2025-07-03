package com.darkcollective.sqlparser;

import org.assertj.core.api.AbstractAssert;

public  class CustomAssertions {

    public static RelationalOperatorAssert assertThat(RelationalOperator actual) {
        return new RelationalOperatorAssert(actual);
    }
    public static TableScanOperatorAssert assertThat(TableScanOperator actual) {
        return new TableScanOperatorAssert(actual);
    }
    public static ProjectionOperatorAssert assertThat(ProjectionOperator actual) {
        return new ProjectionOperatorAssert(actual);
    }
    public static SelectionOperatorAssert assertThat(SelectionOperator actual) {
        return new SelectionOperatorAssert(actual);
    }
    public static SortOperatorAssert assertThat(SortOperator actual) {
        return new SortOperatorAssert(actual);
    }
    public static AggregationOperatorAssert assertThat(AggregationOperator actual) {
        return new AggregationOperatorAssert(actual);
    }
    public static FunctionOperatorAssert assertThat(FunctionOperator actual) {
        return new FunctionOperatorAssert(actual);
    }
    public static JoinOperatorAssert assertThat(JoinOperator actual) {
        return new JoinOperatorAssert(actual);
    }

}

class TableScanOperatorAssert extends AbstractAssert<TableScanOperatorAssert, TableScanOperator> {
    protected TableScanOperatorAssert(TableScanOperator actual) {
        super(actual, TableScanOperatorAssert.class);
    }
    public static TableScanOperatorAssert assertThat(TableScanOperator actual) {
        return new TableScanOperatorAssert(actual);
    }
    public TableScanOperatorAssert hasTableName(String expectedTableName) {
        isNotNull();
        if (!actual.getTableName().equals(expectedTableName)) {
            failWithMessage("Expected TableScanOperator to have table name <%s> but was <%s>",
                    expectedTableName,
                    actual.getTableName());
        }
        return this;
    }
    public TableScanOperatorAssert hasAliasOf(String expectedAlias) {
        isNotNull();
        if (!actual.getAlias().equals(expectedAlias)) {
            failWithMessage("Expected TableScanOperator to have alias <%s> but was <%s>",
                    expectedAlias,
                    actual.getAlias());
        }
        return this;
    }
    public TableScanOperatorAssert hasAlias() {
        isNotNull();
        if (actual.getAlias() == null) {
            failWithMessage("Expected TableScanOperator to have an alias but it did not");
        }
        return this;
    }
    public TableScanOperatorAssert hasNoAlias() {
        isNotNull();
        if (actual.getAlias() != null) {
            failWithMessage("Expected TableScanOperator to not have an alias but it did");
        }
        return this;
    }
}

class RelationalOperatorAssert extends AbstractAssert<RelationalOperatorAssert, RelationalOperator> {


    protected RelationalOperatorAssert(RelationalOperator relationalOperator) {
        super(relationalOperator, RelationalOperatorAssert.class);
    }
    public static RelationalOperatorAssert assertThat(RelationalOperator actual) {
        return new RelationalOperatorAssert(actual);
    }
    public RelationalOperatorAssert isProjectionOperator() {
        isNotNull();
        expectedToBe(ProjectionOperator.class);
        return this;
    }

    public RelationalOperatorAssert isAggregationOperator() {
        isNotNull();
        expectedToBe(AggregationOperator.class);
        return this;
    }

    public RelationalOperatorAssert isFunctionOperator() {
        isNotNull();
        expectedToBe(FunctionOperator.class);
        return this;
    }

    public RelationalOperatorAssert isJoinOperator() {
        isNotNull();
        expectedToBe(JoinOperator.class);
        return this;
    }

    public RelationalOperatorAssert isSelectionOperator() {
        isNotNull();
        expectedToBe(SelectionOperator.class);
        return this;
    }

    public RelationalOperatorAssert isSortOperator() {
        isNotNull();
        expectedToBe(SortOperator.class);
        return this;
    }

    public RelationalOperatorAssert isTableScanOperator() {
        isNotNull();
        expectedToBe(TableScanOperator.class);
        return this;
    }

    public RelationalOperatorAssert isSubQueryOperator() {
        isNotNull();
        expectedToBe(SubqueryOperator.class);
        return this;
    }

    private void expectedToBe(Class<? extends RelationalOperator> expectedClass ) {
        if (!actual.getClass().equals(expectedClass)) {
            failWithMessage("Expected RelationalOperator to be a %s but was a %s",
                    expectedClass.getSimpleName(),
                    actual.getClass().getSimpleName());
        }
    }

}

class ProjectionOperatorAssert extends AbstractAssert<ProjectionOperatorAssert, ProjectionOperator> {
    protected ProjectionOperatorAssert(ProjectionOperator actual) {
        super(actual, ProjectionOperatorAssert.class);
    }
    public static ProjectionOperatorAssert assertThat(ProjectionOperator actual) {
        return new ProjectionOperatorAssert(actual);
    }
    public ProjectionOperatorAssert isDistinct() {
        isNotNull();
        if (!actual.isDistinct()) {
            failWithMessage("Expected ProjectionOperator to be distinct but was not");
        }
        return this;
    }
    public ProjectionOperatorAssert isNotDistinct() {
        isNotNull();
        if (actual.isDistinct()) {
            failWithMessage("Expected ProjectionOperator to not be distinct but was");
        }
        return this;
    }
}

class SelectionOperatorAssert extends AbstractAssert<SelectionOperatorAssert, SelectionOperator> {

    protected SelectionOperatorAssert(SelectionOperator actual) {
        super(actual, SelectionOperatorAssert.class);
    }
    public static SelectionOperatorAssert assertThat(SelectionOperator actual) {
        return new SelectionOperatorAssert(actual);
    }
    public SelectionOperatorAssert hasCondition(String expectedCondition) {
        isNotNull();
        if (!actual.getCondition().equals(expectedCondition)) {
            failWithMessage("Expected SelectionOperator to have condition <%s> but was <%s>",
                    expectedCondition,
                    actual.getCondition());
        }
        return this;
    }
}

class SortOperatorAssert extends AbstractAssert<SortOperatorAssert, SortOperator> {

    protected SortOperatorAssert(SortOperator actual) {
        super(actual, SortOperatorAssert.class);
    }
    public static SortOperatorAssert assertThat(SortOperator actual) {
        return new SortOperatorAssert(actual);
    }

}

class AggregationOperatorAssert extends AbstractAssert<AggregationOperatorAssert, AggregationOperator> {

    protected AggregationOperatorAssert(AggregationOperator actual) {
        super(actual, AggregationOperatorAssert.class);
    }
    public static AggregationOperatorAssert assertThat(AggregationOperator actual) {
        return new AggregationOperatorAssert(actual);
    }
}

class FunctionOperatorAssert extends AbstractAssert<FunctionOperatorAssert, FunctionOperator> {

    protected FunctionOperatorAssert(FunctionOperator actual) {
        super(actual, FunctionOperatorAssert.class);
    }
    public static FunctionOperatorAssert assertThat(FunctionOperator actual) {
        return new FunctionOperatorAssert(actual);
    }
}

class JoinOperatorAssert extends AbstractAssert<JoinOperatorAssert, JoinOperator> {

    protected JoinOperatorAssert(JoinOperator actual) {
        super(actual, JoinOperatorAssert.class);
    }
    public static JoinOperatorAssert assertThat(JoinOperator actual) {
        return new JoinOperatorAssert(actual);
    }

    public JoinOperatorAssert isJoinType(JoinOperator.JoinType expectedJoinType) {
        isNotNull();
        expectedJoinType(expectedJoinType);
        return this;
    }

    public JoinOperatorAssert isInnerJoin() {
        isNotNull();
        expectedJoinType(JoinOperator.JoinType.INNER);
        return this;
    }

    public JoinOperatorAssert isLeftOuterJoin() {
        isNotNull();
        expectedJoinType(JoinOperator.JoinType.LEFT);
        return this;
    }

    public JoinOperatorAssert isRightOuterJoin() {
        isNotNull();
        expectedJoinType(JoinOperator.JoinType.RIGHT);
        return this;
    }
    public JoinOperatorAssert isFullOuterJoin() {
        isNotNull();
        expectedJoinType(JoinOperator.JoinType.FULL);
        return this;
    }
    public JoinOperatorAssert isCrossJoin() {
        isNotNull();
        expectedJoinType(JoinOperator.JoinType.CROSS);
        return this;
    }
    public JoinOperatorAssert hasJoinCondition(String expectedCondition) {
        isNotNull();
        if (!actual.getJoinCondition().equals(expectedCondition)) {
            failWithMessage("Expected JoinOperator to have condition <%s> but was <%s>",
                    expectedCondition,
                    actual.getJoinCondition());
        }
        return this;
    }

    private void expectedJoinType(JoinOperator.JoinType expectedJoinType) {
        if (!actual.getJoinType().equals(expectedJoinType)) {
            failWithMessage("Expected JoinOperator to have join type of %s but was a %s",
                    expectedJoinType.name(),
                    actual.getJoinType().name());
        }
    }
}