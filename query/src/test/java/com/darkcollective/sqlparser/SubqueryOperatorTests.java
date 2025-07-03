
package com.darkcollective.sqlparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("Subquery Support Test Suite")
class SubqueryTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    @Nested
    @DisplayName("FROM Clause Subqueries")
    class FromClauseSubqueryTests {

        @Test
        @DisplayName("Simple subquery in FROM clause with alias")
        void testSimpleFromSubqueryWithAlias() {
            String sql = "SELECT name FROM (SELECT name, age FROM users) AS u";
            SqlParser.ParsedQuery query = parser.parse(sql);

            assertNotNull(query);
            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(List.of("name"), projection.getColumns());

            assertTrue(projection.getChildren().get(0) instanceof SubqueryOperator);
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);
            assertEquals("u", subquery.getAlias());
            assertEquals(SubqueryOperator.SubqueryType.FROM_CLAUSE, subquery.getType());

            // Check the inner subquery structure
            RelationalOperator innerQuery = subquery.getSubquery();
            assertTrue(innerQuery instanceof ProjectionOperator);
            ProjectionOperator innerProjection = (ProjectionOperator) innerQuery;
            assertEquals(List.of("name", "age"), innerProjection.getColumns());
        }

        @Test
        @DisplayName("Subquery in FROM clause without AS keyword")
        void testFromSubqueryWithoutAs() {
            String sql = "SELECT * FROM (SELECT id, name FROM users) u";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);
            assertEquals("u", subquery.getAlias());
        }

        @Test
        @DisplayName("Nested subqueries in FROM clause")
        void testNestedFromSubqueries() {
            String sql = "SELECT name FROM (SELECT name FROM (SELECT name, age FROM users) AS inner) AS outer";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator outerProjection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator outerSubquery = (SubqueryOperator) outerProjection.getChildren().get(0);
            assertEquals("outer", outerSubquery.getAlias());

            // Check inner subquery
            ProjectionOperator middleProjection = (ProjectionOperator) outerSubquery.getSubquery();
            SubqueryOperator innerSubquery = (SubqueryOperator) middleProjection.getChildren().get(0);
            assertEquals("inner", innerSubquery.getAlias());

            // Check innermost query
            ProjectionOperator innermostProjection = (ProjectionOperator) innerSubquery.getSubquery();
            assertEquals(List.of("name", "age"), innermostProjection.getColumns());
        }

        @Test
        @DisplayName("Subquery with WHERE clause in FROM")
        void testFromSubqueryWithWhere() {
            String sql = "SELECT name FROM (SELECT name FROM users WHERE age > 18) AS adults";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);
            assertEquals("adults", subquery.getAlias());

            // Check that the inner query has a WHERE clause
            ProjectionOperator innerProjection = (ProjectionOperator) subquery.getSubquery();
            assertTrue(innerProjection.getChildren().get(0) instanceof SelectionOperator);
            SelectionOperator selection = (SelectionOperator) innerProjection.getChildren().get(0);
            assertEquals("age > 18", selection.getCondition());
        }

        @Test
        @DisplayName("Subquery with aggregate functions in FROM")
        void testFromSubqueryWithAggregates() {
            String sql = "SELECT dept, avg_salary FROM (SELECT department AS dept, AVG(salary) AS avg_salary FROM employees GROUP BY department) AS dept_avg";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);
            assertEquals("dept_avg", subquery.getAlias());

            // Check that the inner query has aggregation
            ProjectionOperator innerProjection = (ProjectionOperator) subquery.getSubquery();
            assertTrue(innerProjection.getChildren().get(0) instanceof AggregationOperator);
        }
    }

    @Nested
    @DisplayName("SubqueryOperator Unit Tests")
    class SubqueryOperatorUnitTests {

        @Test
        @DisplayName("SubqueryOperator creation with alias")
        void testSubqueryOperatorWithAlias() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name"), false);
            projection.addChild(tableScan);

            SubqueryOperator subquery = new SubqueryOperator(projection, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);

            assertEquals("u", subquery.getAlias());
            assertTrue(subquery.hasAlias());
            assertEquals(projection, subquery.getSubquery());
            assertEquals(SubqueryOperator.SubqueryType.FROM_CLAUSE, subquery.getType());
            assertEquals("u", subquery.getEffectiveTableName());
        }

        @Test
        @DisplayName("SubqueryOperator creation without alias")
        void testSubqueryOperatorWithoutAlias() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name"), false);
            projection.addChild(tableScan);

            SubqueryOperator subquery = new SubqueryOperator(projection, SubqueryOperator.SubqueryType.FROM_CLAUSE);

            assertNull(subquery.getAlias());
            assertFalse(subquery.hasAlias());
            assertTrue(subquery.getEffectiveTableName().startsWith("subquery_"));
        }

        @Test
        @DisplayName("SubqueryOperator toSql")
        void testSubqueryOperatorToSql() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name"), false);
            projection.addChild(tableScan);

            SubqueryOperator subquery = new SubqueryOperator(projection, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);

            String expectedSql = "(SELECT name FROM users) AS u";
            assertEquals(expectedSql, subquery.toSql());
        }

        @Test
        @DisplayName("SubqueryOperator toTreeString")
        void testSubqueryOperatorToTreeString() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name"), false);
            projection.addChild(tableScan);

            SubqueryOperator subquery = new SubqueryOperator(projection, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);

            String treeString = subquery.toTreeString();
            assertTrue(treeString.contains("SUBQUERY(FROM_CLAUSE AS u)"));
            assertTrue(treeString.contains("PROJECTION(name)"));
            assertTrue(treeString.contains("TABLE_SCAN(users)"));
        }

        @Test
        @DisplayName("SubqueryOperator equality")
        void testSubqueryOperatorEquality() {
            TableScanOperator tableScan1 = new TableScanOperator("users", null);
            ProjectionOperator projection1 = new ProjectionOperator(List.of("name"), false);
            projection1.addChild(tableScan1);

            TableScanOperator tableScan2 = new TableScanOperator("users", null);
            ProjectionOperator projection2 = new ProjectionOperator(List.of("name"), false);
            projection2.addChild(tableScan2);

            SubqueryOperator subquery1 = new SubqueryOperator(projection1, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);
            SubqueryOperator subquery2 = new SubqueryOperator(projection2, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);

            assertEquals(subquery1, subquery2);
            assertEquals(subquery1.hashCode(), subquery2.hashCode());
        }

        @Test
        @DisplayName("SubqueryOperator different types not equal")
        void testSubqueryOperatorDifferentTypesNotEqual() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name"), false);
            projection.addChild(tableScan);

            SubqueryOperator subquery1 = new SubqueryOperator(projection, "u", SubqueryOperator.SubqueryType.FROM_CLAUSE);
            SubqueryOperator subquery2 = new SubqueryOperator(projection, "u", SubqueryOperator.SubqueryType.WHERE_EXISTS);

            assertNotEquals(subquery1, subquery2);
        }
    }

    @Nested
    @DisplayName("Complex Subquery Scenarios")
    class ComplexSubqueryTests {

        @Test
        @DisplayName("Subquery with DISTINCT")
        void testSubqueryWithDistinct() {
            String sql = "SELECT * FROM (SELECT DISTINCT department FROM employees) AS depts";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);

            ProjectionOperator innerProjection = (ProjectionOperator) subquery.getSubquery();
            assertTrue(innerProjection.isDistinct());
            assertEquals(List.of("department"), innerProjection.getColumns());
        }

        @Test
        @DisplayName("Subquery with ORDER BY")
        void testSubqueryWithOrderBy() {
            String sql = "SELECT name FROM (SELECT name FROM users ORDER BY name) AS sorted_users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);

            ProjectionOperator innerProjection = (ProjectionOperator) subquery.getSubquery();
            assertTrue(innerProjection.getChildren().get(0) instanceof SortOperator);
            SortOperator sort = (SortOperator) innerProjection.getChildren().get(0);
            assertEquals(List.of("name"), sort.getOrderByColumns());
        }

        @Test
        @DisplayName("Subquery with functions")
        void testSubqueryWithFunctions() {
            String sql = "SELECT upper_name FROM (SELECT UPPER(name) AS upper_name FROM users) AS transformed";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(List.of("upper_name"), projection.getColumns());

            SubqueryOperator subquery = (SubqueryOperator) projection.getChildren().get(0);
            assertEquals("transformed", subquery.getAlias());

            ProjectionOperator innerProjection = (ProjectionOperator) subquery.getSubquery();
            assertEquals(1, innerProjection.getFunctionCalls().size());
            assertEquals("UPPER", innerProjection.getFunctionCalls().get(0).getFunctionName());
        }
    }

    @Nested
    @DisplayName("Subquery Error Handling")
    class SubqueryErrorHandlingTests {

        @Test
        @DisplayName("Invalid subquery syntax should throw exception")
        void testInvalidSubquerySyntax() {
            String sql = "SELECT name FROM (SELECT name FROM users AS incomplete";

            assertThrows(IllegalArgumentException.class, () -> {
                parser.parse(sql);
            });
        }

        @Test
        @DisplayName("Missing closing parenthesis should throw exception")
        void testMissingClosingParenthesis() {
            String sql = "SELECT name FROM (SELECT name FROM users";

            assertThrows(IllegalArgumentException.class, () -> {
                parser.parse(sql);
            });
        }
    }
}