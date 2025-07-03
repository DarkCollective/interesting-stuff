package com.darkcollective.sqlparser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static com.darkcollective.sqlparser.CustomAssertions.assertThat;

import java.util.List;

@DisplayName("Parenthetical Algebra Parser Test Suite")
class ParentheticalAlgebraParserTest {

    @Nested
    @DisplayName("Basic Operator Parsing")
    class BasicOperatorParsingTests {

        @Test
        @DisplayName("Parse TABLE_SCAN without alias")
        void testParseTableScanWithoutAlias() {
            String expression = "TABLE_SCAN(users)";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isTableScanOperator();
            TableScanOperator tableScan = (TableScanOperator) operator;
            assertThat(tableScan).hasTableName("users");
            assertThat(tableScan).hasNoAlias();
        }

        @Test
        @DisplayName("Parse TABLE_SCAN with alias")
        void testParseTableScanWithAlias() {
            String expression = "TABLE_SCAN(users AS u)";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isTableScanOperator();
            TableScanOperator tableScan = (TableScanOperator) operator;
            assertThat(tableScan).hasTableName("users");
            assertThat(tableScan).hasAliasOf("u");
        }

        @Test
        @DisplayName("Parse PROJECTION without DISTINCT")
        void testParseProjectionWithoutDistinct() {
            String expression = "PROJECTION(name, age, TABLE_SCAN(users))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isProjectionOperator();
            ProjectionOperator projection = (ProjectionOperator) operator;
            assertThat(projection.getColumns()).containsExactly("name", "age");
            assertThat(projection).isNotDistinct();
            assertThat(projection.getChildren()).hasSize(1);
            assertThat(projection.getFirstChild()).isTableScanOperator();
        }

        @Test
        @DisplayName("Parse PROJECTION with DISTINCT")
        void testParseProjectionWithDistinct() {
            String expression = "PROJECTION(DISTINCT, department, TABLE_SCAN(employees))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isProjectionOperator();
            ProjectionOperator projection = (ProjectionOperator) operator;
            assertThat(projection.getColumns()).containsExactly("department");
            assertThat(projection).isDistinct();
        }

        @Test
        @DisplayName("Parse SELECTION")
        void testParseSelection() {
            String expression = "SELECTION(age > 18, TABLE_SCAN(users))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isSelectionOperator();
            SelectionOperator selection = (SelectionOperator) operator;
            assertThat(selection).hasCondition("age > 18");
            assertThat(selection.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("Parse INNER_JOIN")
        void testParseInnerJoin() {
            String expression = "INNER_JOIN(users.id = posts.user_id, TABLE_SCAN(users), TABLE_SCAN(posts))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isJoinOperator();
            JoinOperator join = (JoinOperator) operator;

            assertThat(join).isInnerJoin();
            assertThat(join).hasJoinCondition("users.id = posts.user_id");
            assertThat(join.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("Parse AGGREGATION")
        void testParseAggregation() {
            String expression = "AGGREGATION(GROUP_BY:department, AGG:COUNT(*), TABLE_SCAN(employees))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isAggregationOperator();
            AggregationOperator aggregation = (AggregationOperator) operator;
            assertThat(aggregation.getGroupByColumns()).containsExactly("department");
            assertThat(aggregation.getAggregateFunctions()).containsExactly("COUNT(*)");
        }

        @Test
        @DisplayName("Parse SORT")
        void testParseSort() {
            String expression = "SORT(name ASC, age DESC, TABLE_SCAN(users))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isSortOperator();
            SortOperator sort = (SortOperator) operator;
            assertThat(sort.getOrderByColumns()).containsExactly("name ASC", "age DESC");
            assertThat(sort.getChildren()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Complex Expression Parsing")
    class ComplexExpressionParsingTests {

        @Test
        @DisplayName("Parse nested projection and selection")
        void testParseNestedProjectionAndSelection() {
            String expression = "PROJECTION(name, age, SELECTION(age > 18, TABLE_SCAN(users)))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isProjectionOperator();
            ProjectionOperator projection = (ProjectionOperator) operator;

            assertThat(projection.getColumns()).containsExactly("name", "age");
            assertThat(projection.getChildren()).hasSize(1);

            assertThat(projection.getFirstChild()).isSelectionOperator();
            
            SelectionOperator selection = (SelectionOperator) projection.getFirstChild();
            assertThat(selection).hasCondition("age > 18");
            assertThat(selection.getChildren()).hasSize(1);
            assertThat(selection.getFirstChild()).isTableScanOperator();
        }

        @Test
        @DisplayName("Parse complex join with multiple conditions")
        void testParseComplexJoin() {
            String expression = "INNER_JOIN(users.id = posts.user_id AND users.status = 'active', TABLE_SCAN(users AS u), TABLE_SCAN(posts AS p))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isJoinOperator();
            JoinOperator join = (JoinOperator) operator;
            assertThat(join).hasJoinCondition("users.id = posts.user_id AND users.status = 'active'");
            assertThat(join.getChildren()).hasSize(2);

            TableScanOperator leftTable = (TableScanOperator) join.getFirstChild();
            TableScanOperator rightTable = (TableScanOperator) join.getLastChild();
            assertThat(leftTable).hasAliasOf("u");
            assertThat(rightTable).hasAliasOf("p");
        }

        @Test
        @DisplayName("Parse aggregation with having clause")
        void testParseAggregationWithHaving() {
            String expression = "AGGREGATION(GROUP_BY:department, AGG:COUNT(*), HAVING:COUNT(*)>5, TABLE_SCAN(employees))";
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isAggregationOperator();
            AggregationOperator aggregation = (AggregationOperator) operator;
            assertThat(aggregation.getGroupByColumns()).containsExactly("department");

            assertThat(aggregation.getAggregateFunctions()).containsExactly("COUNT(*)");
            assertThat(aggregation.getHavingCondition()).isEqualTo("COUNT(*)>5");
        }

        @Test
        @DisplayName("Parse full query tree")
        void testParseFullQueryTree() {
            String expression = """
                SORT(department ASC, 
                  PROJECTION(department, COUNT(*), 
                    AGGREGATION(GROUP_BY:department, AGG:COUNT(*), HAVING:COUNT(*)>5, 
                      SELECTION(age > 25, 
                        TABLE_SCAN(employees)))))
                """;
            
            RelationalOperator operator = ParentheticalAlgebraParser.parse(expression);

            assertThat(operator).isSortOperator();

            SortOperator sort = (SortOperator) operator;
            assertThat(sort.getOrderByColumns()).containsExactly("department ASC");

            assertThat(sort.getFirstChild()).isProjectionOperator();
            ProjectionOperator projection = (ProjectionOperator) sort.getFirstChild();
            assertThat(projection.getColumns()).containsExactly("department", "COUNT(*)");

            assertThat(projection.getFirstChild()).isAggregationOperator();

            AggregationOperator aggregation = (AggregationOperator) projection.getFirstChild();
            assertThat(aggregation.getHavingCondition()).isEqualTo("COUNT(*)>5");

            assertThat(aggregation.getFirstChild()).isSelectionOperator();
            SelectionOperator selection = (SelectionOperator) aggregation.getFirstChild();
            assertThat(selection).hasCondition("age > 25");

            assertThat(selection.getFirstChild()).isTableScanOperator();
        }
    }

    @Nested
    @DisplayName("Round-trip Conversion Tests")
    class RoundTripConversionTests {

        @Test
        @DisplayName("Round-trip: simple table scan")
        void testRoundTripSimpleTableScan() {
            TableScanOperator original = new TableScanOperator("users", "u");
            String parenthetical = original.toParenthetical();
            RelationalOperator parsed = ParentheticalAlgebraParser.parse(parenthetical);
            assertThat(parsed).isEqualTo(original);
            //assertEquals(original, parsed);
        }

        @Test
        @DisplayName("Round-trip: projection over table scan")
        void testRoundTripProjectionOverTableScan() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator original = new ProjectionOperator(List.of("name", "age"), false);
            original.addChild(tableScan);

            String parenthetical = original.toParenthetical();
            RelationalOperator parsed = ParentheticalAlgebraParser.parse(parenthetical);

            assertThat(parsed).isProjectionOperator();
            ProjectionOperator parsedProjection = (ProjectionOperator) parsed;
            assertThat(original.getColumns()).isEqualTo(parsedProjection.getColumns());
            //assertEquals(original.getColumns(), parsedProjection.getColumns());
            assertThat(parsedProjection.isDistinct()).isEqualTo(original.isDistinct());
            assertThat(parsedProjection.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("Round-trip: complex query")
        void testRoundTripComplexQuery() {
            // Build complex operator tree
            TableScanOperator tableScan = new TableScanOperator("employees", null);
            SelectionOperator selection = new SelectionOperator("age > 25");
            selection.addChild(tableScan);
            
            AggregationOperator aggregation = new AggregationOperator(
                List.of("department"), List.of("COUNT(*)"), "COUNT(*) > 5");
            aggregation.addChild(selection);
            
            ProjectionOperator projection = new ProjectionOperator(List.of("department", "COUNT(*)"), false);
            projection.addChild(aggregation);

            String parenthetical = projection.toParenthetical();
            RelationalOperator parsed = ParentheticalAlgebraParser.parse(parenthetical);

            // Verify structure is preserved
            assertThat(parsed).isProjectionOperator();
            assertThat(parsed.getChildren()).hasSize(1);
            assertThat(parsed.getFirstChild()).isAggregationOperator();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Handle empty expression")
        void testHandleEmptyExpression() {
            assertThatThrownBy(() -> ParentheticalAlgebraParser.parse(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Handle null expression")
        void testHandleNullExpression() {
            assertThatThrownBy(() -> ParentheticalAlgebraParser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Handle malformed parentheses")
        void testHandleMalformedParentheses() {
            assertThatThrownBy(() -> ParentheticalAlgebraParser.parse("PROJECTION(name, age"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Handle unknown operator")
        void testHandleUnknownOperator() {
            assertThatThrownBy(() -> ParentheticalAlgebraParser.parse("UNKNOWN_OPERATOR(param"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Handle missing parameters")
        void testHandleMissingParameters() {
            assertThatThrownBy(() -> ParentheticalAlgebraParser.parse("PROJECTION("))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}