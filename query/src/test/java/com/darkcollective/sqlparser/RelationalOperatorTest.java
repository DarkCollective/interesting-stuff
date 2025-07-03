package com.darkcollective.sqlparser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("Relational Operators Test Suite")
class RelationalOperatorTest {

    @Nested
    @DisplayName("TableScanOperator Tests")
    class TableScanOperatorTests {

        @Test
        @DisplayName("TableScan without alias")
        void testTableScanWithoutAlias() {
            TableScanOperator tableScan = new TableScanOperator("users", null);

            assertEquals("users", tableScan.getTableName());
            assertNull(tableScan.getAlias());
            assertEquals("TABLE_SCAN(users)", tableScan.toTreeString());
            assertEquals("users", tableScan.toSql());
        }

        @Test
        @DisplayName("TableScan with alias")
        void testTableScanWithAlias() {
            TableScanOperator tableScan = new TableScanOperator("users", "u");

            assertEquals("users", tableScan.getTableName());
            assertEquals("u", tableScan.getAlias());
            assertEquals("TABLE_SCAN(users AS u)", tableScan.toTreeString());
            assertEquals("users AS u", tableScan.toSql());
        }

        @Test
        @DisplayName("TableScan equality")
        void testTableScanEquality() {
            TableScanOperator tableScan1 = new TableScanOperator("users", "u");
            TableScanOperator tableScan2 = new TableScanOperator("users", "u");
            TableScanOperator tableScan3 = new TableScanOperator("posts", "p");

            assertEquals(tableScan1.getTableName(), tableScan2.getTableName());
            assertEquals(tableScan1.getAlias(), tableScan2.getAlias());
            assertNotEquals(tableScan1.getTableName(), tableScan3.getTableName());
        }
    }

    @Nested
    @DisplayName("ProjectionOperator Tests")
    class ProjectionOperatorTests {

        @Test
        @DisplayName("Projection without DISTINCT")
        void testProjectionWithoutDistinct() {
            ProjectionOperator projection = new ProjectionOperator(List.of("name", "age"), false);

            assertEquals(List.of("name", "age"), projection.getColumns());
            assertFalse(projection.isDistinct());
            assertTrue(projection.getFunctionCalls().isEmpty());
            assertTrue(projection.getSelectItems().isEmpty());
        }

        @Test
        @DisplayName("Projection with DISTINCT")
        void testProjectionWithDistinct() {
            ProjectionOperator projection = new ProjectionOperator(List.of("department"), true);

            assertEquals(List.of("department"), projection.getColumns());
            assertTrue(projection.isDistinct());
        }

        @Test
        @DisplayName("Projection with function calls")
        void testProjectionWithFunctions() {
            ProjectionOperator projection = new ProjectionOperator(List.of("COUNT(*)", "UPPER(name)"), false);

            // Add function calls
            SqlFunctionCall countFunc = new SqlFunctionCall("COUNT", List.of("*"), "COUNT(*)");
            SqlFunctionCall upperFunc = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");

            projection.addFunctionCall(countFunc);
            projection.addFunctionCall(upperFunc);

            assertEquals(2, projection.getFunctionCalls().size());
            assertEquals(1, projection.getAggregateFunctions().size());
            assertEquals(1, projection.getStringFunctions().size());
            assertEquals(0, projection.getNumericFunctions().size());
        }

        @Test
        @DisplayName("Projection tree string representation")
        void testProjectionTreeString() {
            ProjectionOperator projection = new ProjectionOperator(List.of("name", "age"), false);
            TableScanOperator tableScan = new TableScanOperator("users", null);
            projection.addChild(tableScan);

            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION(name, age)"));
            assertTrue(treeString.contains("TABLE_SCAN(users)"));
        }

        @Test
        @DisplayName("Projection with DISTINCT tree string")
        void testProjectionDistinctTreeString() {
            ProjectionOperator projection = new ProjectionOperator(List.of("department"), true);
            TableScanOperator tableScan = new TableScanOperator("employees", null);
            projection.addChild(tableScan);

            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION(DISTINCT department)"));
        }

        @Test
        @DisplayName("Projection with functions tree string")
        void testProjectionFunctionsTreeString() {
            ProjectionOperator projection = new ProjectionOperator(List.of("COUNT(*)"), false);
            SqlFunctionCall countFunc = new SqlFunctionCall("COUNT", List.of("*"), "COUNT(*)");
            projection.addFunctionCall(countFunc);

            TableScanOperator tableScan = new TableScanOperator("users", null);
            projection.addChild(tableScan);

            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION(COUNT(*))"));
        }
    }

    @Nested
    @DisplayName("SelectionOperator Tests")
    class SelectionOperatorTests {

        @Test
        @DisplayName("Selection with simple condition")
        void testSelectionSimpleCondition() {
            SelectionOperator selection = new SelectionOperator("age > 18");

            assertEquals("age > 18", selection.getCondition());
        }

        @Test
        @DisplayName("Selection with complex condition")
        void testSelectionComplexCondition() {
            SelectionOperator selection = new SelectionOperator("age > 18 AND status = 'active'");

            assertEquals("age > 18 AND status = 'active'", selection.getCondition());
        }

        @Test
        @DisplayName("Selection tree string representation")
        void testSelectionTreeString() {
            SelectionOperator selection = new SelectionOperator("age > 18");
            TableScanOperator tableScan = new TableScanOperator("users", null);
            selection.addChild(tableScan);

            String treeString = selection.toTreeString();
            assertTrue(treeString.contains("SELECTION(age > 18)"));
            assertTrue(treeString.contains("TABLE_SCAN(users)"));
        }

        @Test
        @DisplayName("Selection with function in condition")
        void testSelectionWithFunction() {
            SelectionOperator selection = new SelectionOperator("LENGTH(name) > 5");
            TableScanOperator tableScan = new TableScanOperator("users", null);
            selection.addChild(tableScan);

            String treeString = selection.toTreeString();
            assertTrue(treeString.contains("SELECTION(LENGTH(name) > 5)"));
        }
    }

    @Nested
    @DisplayName("JoinOperator Tests")
    class JoinOperatorTests {

        @Test
        @DisplayName("Inner join")
        void testInnerJoin() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.id = p.user_id");

            assertEquals(JoinOperator.JoinType.INNER, join.getJoinType());
            assertEquals("u.id = p.user_id", join.getJoinCondition());
        }

        @Test
        @DisplayName("Left join")
        void testLeftJoin() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.LEFT, "u.id = p.user_id");

            assertEquals(JoinOperator.JoinType.LEFT, join.getJoinType());
            assertEquals("u.id = p.user_id", join.getJoinCondition());
        }

        @Test
        @DisplayName("Right join")
        void testRightJoin() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.RIGHT, "u.id = p.user_id");

            assertEquals(JoinOperator.JoinType.RIGHT, join.getJoinType());
        }

        @Test
        @DisplayName("Full join")
        void testFullJoin() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.FULL, "u.id = p.user_id");

            assertEquals(JoinOperator.JoinType.FULL, join.getJoinType());
        }

        @Test
        @DisplayName("Cross join without condition")
        void testCrossJoin() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.CROSS, null);

            assertEquals(JoinOperator.JoinType.CROSS, join.getJoinType());
            assertNull(join.getJoinCondition());
        }

        @Test
        @DisplayName("Join tree string representation")
        void testJoinTreeString() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.id = p.user_id");
            TableScanOperator leftTable = new TableScanOperator("users", "u");
            TableScanOperator rightTable = new TableScanOperator("posts", "p");
            join.addChild(leftTable);
            join.addChild(rightTable);

            String treeString = join.toTreeString();
            assertTrue(treeString.contains("INNER_JOIN(u.id = p.user_id)"));
            assertTrue(treeString.contains("TABLE_SCAN(users AS u)"));
            assertTrue(treeString.contains("TABLE_SCAN(posts AS p)"));
        }

        @Test
        @DisplayName("Cross join tree string")
        void testCrossJoinTreeString() {
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.CROSS, null);
            TableScanOperator leftTable = new TableScanOperator("users", null);
            TableScanOperator rightTable = new TableScanOperator("posts", null);
            join.addChild(leftTable);
            join.addChild(rightTable);

            String treeString = join.toTreeString();
            assertTrue(treeString.contains("CROSS_JOIN"));
        }
    }

    @Nested
    @DisplayName("AggregationOperator Tests")
    class AggregationOperatorTests {

        @Test
        @DisplayName("Aggregation with GROUP BY")
        void testAggregationWithGroupBy() {
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department"),
                    List.of("COUNT(*)", "AVG(salary)"),
                    null
            );

            assertEquals(List.of("department"), aggregation.getGroupByColumns());
            assertEquals(List.of("COUNT(*)", "AVG(salary)"), aggregation.getAggregateFunctions());
            assertNull(aggregation.getHavingCondition());
        }

        @Test
        @DisplayName("Aggregation with HAVING")
        void testAggregationWithHaving() {
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department"),
                    List.of("COUNT(*)"),
                    "COUNT(*) > 5"
            );

            assertEquals("COUNT(*) > 5", aggregation.getHavingCondition());
        }

        @Test
        @DisplayName("Aggregation without GROUP BY")
        void testAggregationWithoutGroupBy() {
            AggregationOperator aggregation = new AggregationOperator(
                    List.of(),
                    List.of("COUNT(*)", "SUM(salary)"),
                    null
            );

            assertTrue(aggregation.getGroupByColumns().isEmpty());
            assertEquals(2, aggregation.getAggregateFunctions().size());
        }

        @Test
        @DisplayName("Aggregation tree string representation")
        void testAggregationTreeString() {
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department"),
                    List.of("COUNT(*)"),
                    "COUNT(*) > 5"
            );
            TableScanOperator tableScan = new TableScanOperator("employees", null);
            aggregation.addChild(tableScan);

            String treeString = aggregation.toTreeString();
            assertTrue(treeString.contains("AGGREGATION"));
            assertTrue(treeString.contains("GROUP BY: department"));
            assertTrue(treeString.contains("AGG: COUNT(*)"));
            assertTrue(treeString.contains("HAVING: COUNT(*) > 5"));
        }

        @Test
        @DisplayName("Aggregation without HAVING tree string")
        void testAggregationWithoutHavingTreeString() {
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department"),
                    List.of("COUNT(*)"),
                    null
            );
            TableScanOperator tableScan = new TableScanOperator("employees", null);
            aggregation.addChild(tableScan);

            String treeString = aggregation.toTreeString();
            assertTrue(treeString.contains("AGGREGATION"));
            assertTrue(treeString.contains("GROUP BY: department"));
            assertFalse(treeString.contains("HAVING:"));
        }
    }

    @Nested
    @DisplayName("SortOperator Tests")
    class SortOperatorTests {

        @Test
        @DisplayName("Sort with single column")
        void testSortSingleColumn() {
            SortOperator sort = new SortOperator(List.of("name"));

            assertEquals(List.of("name"), sort.getOrderByColumns());
        }

        @Test
        @DisplayName("Sort with multiple columns")
        void testSortMultipleColumns() {
            SortOperator sort = new SortOperator(List.of("age", "name"));

            assertEquals(List.of("age", "name"), sort.getOrderByColumns());
        }

        @Test
        @DisplayName("Sort with ASC/DESC")
        void testSortWithDirection() {
            SortOperator sort = new SortOperator(List.of("name", "ASC", "age", "DESC"));

            assertEquals(List.of("name", "ASC", "age", "DESC"), sort.getOrderByColumns());
        }

        @Test
        @DisplayName("Sort tree string representation")
        void testSortTreeString() {
            SortOperator sort = new SortOperator(List.of("age", "name"));
            TableScanOperator tableScan = new TableScanOperator("users", null);
            sort.addChild(tableScan);

            String treeString = sort.toTreeString();
            assertTrue(treeString.contains("SORT(age, name)"));
            assertTrue(treeString.contains("TABLE_SCAN(users)"));
        }

        @Test
        @DisplayName("Empty sort columns")
        void testEmptySortColumns() {
            SortOperator sort = new SortOperator(List.of());

            assertTrue(sort.getOrderByColumns().isEmpty());
        }
    }

    @Nested
    @DisplayName("SqlFunctionCall Tests")
    class SqlFunctionCallTests {

        @Test
        @DisplayName("Aggregate function call creation")
        void testAggregateFunctionCall() {
            SqlFunctionCall function = new SqlFunctionCall("COUNT", List.of("*"), "COUNT(*)");

            assertEquals("COUNT", function.getFunctionName());
            assertEquals(List.of("*"), function.getArguments());
            assertEquals(SqlFunctionRegistry.FunctionType.AGGREGATE, function.getType());
            assertTrue(function.isAggregate());
            assertFalse(function.isString());
            assertFalse(function.isNumeric());
        }

        @Test
        @DisplayName("String function call creation")
        void testStringFunctionCall() {
            SqlFunctionCall function = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");

            assertEquals("UPPER", function.getFunctionName());
            assertEquals(List.of("name"), function.getArguments());
            assertEquals(SqlFunctionRegistry.FunctionType.STRING, function.getType());
            assertTrue(function.isString());
            assertFalse(function.isAggregate());
            assertFalse(function.isNumeric());
        }

        @Test
        @DisplayName("Numeric function call creation")
        void testNumericFunctionCall() {
            SqlFunctionCall function = new SqlFunctionCall("ROUND", List.of("salary", "2"), "ROUND(salary, 2)");

            assertEquals("ROUND", function.getFunctionName());
            assertEquals(List.of("salary", "2"), function.getArguments());
            assertEquals(SqlFunctionRegistry.FunctionType.NUMERIC, function.getType());
            assertTrue(function.isNumeric());
            assertFalse(function.isAggregate());
            assertFalse(function.isString());
        }

        @Test
        @DisplayName("Function call argument count")
        void testFunctionCallArgumentCount() {
            SqlFunctionCall noArgs = new SqlFunctionCall("NOW", List.of(), "NOW()");
            SqlFunctionCall oneArg = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");
            SqlFunctionCall multiArgs = new SqlFunctionCall("SUBSTR", List.of("name", "1", "5"), "SUBSTR(name, 1, 5)");

            assertEquals(0, noArgs.getArgumentCount());
            assertEquals(1, oneArg.getArgumentCount());
            assertEquals(3, multiArgs.getArgumentCount());
        }

        @Test
        @DisplayName("Function call toSql")
        void testFunctionCallToSql() {
            SqlFunctionCall function = new SqlFunctionCall("CONCAT", List.of("first_name", "' '", "last_name"), "CONCAT(first_name,' ',last_name)");

            assertEquals("CONCAT(first_name,' ',last_name)", function.toSql());
        }

        @Test
        @DisplayName("Function call toString")
        void testFunctionCallToString() {
            SqlFunctionCall function = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");

            String toString = function.toString();
            assertTrue(toString.contains("FUNCTION_CALL"));
            assertTrue(toString.contains("STRING:UPPER"));
            assertTrue(toString.contains("name"));
        }

        @Test
        @DisplayName("Function call equality")
        void testFunctionCallEquality() {
            SqlFunctionCall function1 = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");
            SqlFunctionCall function2 = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");
            SqlFunctionCall function3 = new SqlFunctionCall("LOWER", List.of("name"), "LOWER(name)");

            assertEquals(function1, function2);
            assertNotEquals(function1, function3);
            assertEquals(function1.hashCode(), function2.hashCode());
        }

        @Test
        @DisplayName("Unknown function type")
        void testUnknownFunctionType() {
            SqlFunctionCall function = new SqlFunctionCall("UNKNOWN_FUNC", List.of("arg"), "UNKNOWN_FUNC(arg)");

            assertEquals(SqlFunctionRegistry.FunctionType.UNKNOWN, function.getType());
            assertFalse(function.isAggregate());
            assertFalse(function.isString());
            assertFalse(function.isNumeric());
        }
    }

    @Nested
    @DisplayName("Complex Operator Trees")
    class ComplexOperatorTreeTests {

        @Test
        @DisplayName("Complete query tree")
        void testCompleteQueryTree() {
            // Build: SELECT u.name, COUNT(*) FROM users u WHERE u.age > 18 GROUP BY u.name ORDER BY COUNT(*) DESC

            // Bottom: Table scan
            TableScanOperator tableScan = new TableScanOperator("users", "u");

            // Selection
            SelectionOperator selection = new SelectionOperator("u.age > 18");
            selection.addChild(tableScan);

            // Aggregation
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("u.name"),
                    List.of("COUNT(*)"),
                    null
            );
            aggregation.addChild(selection);

            // Sort
            SortOperator sort = new SortOperator(List.of("COUNT(*)", "DESC"));
            sort.addChild(aggregation);

            // Projection
            ProjectionOperator projection = new ProjectionOperator(List.of("u.name", "COUNT(*)"), false);
            projection.addChild(sort);

            // Verify tree structure
            assertEquals(1, projection.getChildren().size());
            assertTrue(projection.getChildren().get(0) instanceof SortOperator);

            SortOperator sortChild = (SortOperator) projection.getChildren().get(0);
            assertEquals(1, sortChild.getChildren().size());
            assertTrue(sortChild.getChildren().get(0) instanceof AggregationOperator);

            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION"));
            assertTrue(treeString.contains("SORT"));
            assertTrue(treeString.contains("AGGREGATION"));
            assertTrue(treeString.contains("SELECTION"));
            assertTrue(treeString.contains("TABLE_SCAN"));
        }

        @Test
        @DisplayName("Join with projection")
        void testJoinWithProjection() {
            // Build: SELECT u.name, p.title FROM users u INNER JOIN posts p ON u.id = p.user_id

            TableScanOperator leftTable = new TableScanOperator("users", "u");
            TableScanOperator rightTable = new TableScanOperator("posts", "p");

            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.id = p.user_id");
            join.addChild(leftTable);
            join.addChild(rightTable);

            ProjectionOperator projection = new ProjectionOperator(List.of("u.name", "p.title"), false);
            projection.addChild(join);

            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION(u.name, p.title)"));
            assertTrue(treeString.contains("INNER_JOIN(u.id = p.user_id)"));
            assertTrue(treeString.contains("TABLE_SCAN(users AS u)"));
            assertTrue(treeString.contains("TABLE_SCAN(posts AS p)"));
        }
    }
}
