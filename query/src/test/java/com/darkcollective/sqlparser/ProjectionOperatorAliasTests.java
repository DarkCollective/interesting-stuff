package com.darkcollective.sqlparser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("Column Aliasing Test Suite")
class ProjectionOperatorAliasTest {

    @Nested
    @DisplayName("Basic Column Aliasing")
    class BasicColumnAliasingTests {

        @Test
        @DisplayName("Simple column alias with AS keyword")
        void testSimpleColumnAliasWithAS() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("name AS full_name");
            
            assertEquals("name", item.getExpression());
            assertEquals("full_name", item.getAlias());
            assertTrue(item.hasAlias());
            assertEquals("full_name", item.getEffectiveColumnName());
            assertEquals("name AS full_name", item.toSql());
        }

        @Test
        @DisplayName("Simple column alias without AS keyword")
        void testSimpleColumnAliasWithoutAS() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("name full_name");
            
            assertEquals("name", item.getExpression());
            assertEquals("full_name", item.getAlias());
            assertTrue(item.hasAlias());
        }

        @Test
        @DisplayName("Column without alias")
        void testColumnWithoutAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("name");
            
            assertEquals("name", item.getExpression());
            assertNull(item.getAlias());
            assertFalse(item.hasAlias());
            assertEquals("name", item.getEffectiveColumnName());
        }

        @Test
        @DisplayName("Table qualified column with alias")
        void testTableQualifiedColumnWithAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("users.name AS user_name");
            
            assertEquals("users.name", item.getExpression());
            assertEquals("user_name", item.getAlias());
            assertTrue(item.hasAlias());
        }
    }

    @Nested
    @DisplayName("Function Aliasing")
    class FunctionAliasingTests {

        @Test
        @DisplayName("Aggregate function with alias")
        void testAggregateFunctionWithAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("COUNT(*) AS total_count");
            
            assertEquals("COUNT(*)", item.getExpression());
            assertEquals("total_count", item.getAlias());
            assertTrue(item.hasAlias());
            assertTrue(item.hasFunctions());
            assertEquals(1, item.getFunctions().size());
            assertEquals("COUNT", item.getFunctions().get(0).getFunctionName());
        }

        @Test
        @DisplayName("String function with alias")
        void testStringFunctionWithAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("UPPER(name) AS upper_name");
            
            assertEquals("UPPER(name)", item.getExpression());
            assertEquals("upper_name", item.getAlias());
            assertTrue(item.hasAlias());
            assertTrue(item.hasFunctions());
        }

        @Test
        @DisplayName("Complex function expression with alias")
        void testComplexFunctionWithAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("CONCAT(first_name, ' ', last_name) AS full_name");
            
            assertEquals("CONCAT(first_name, ' ', last_name)", item.getExpression());
            assertEquals("full_name", item.getAlias());
            assertTrue(item.hasAlias());
            assertTrue(item.hasFunctions());
        }
    }

    @Nested
    @DisplayName("ProjectionOperator with Aliases")
    class ProjectionOperatorWithAliasesTests {

        @Test
        @DisplayName("Projection with mixed aliased and non-aliased columns")
        void testProjectionWithMixedAliases() {
            ProjectionOperator projection = new ProjectionOperator(List.of("name AS full_name", "age", "email AS contact_email"), false);
            
            // Add parsed select items
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("name AS full_name"));
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("age"));
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("email AS contact_email"));
            
            assertTrue(projection.hasAliases());
            
            List<String> outputNames = projection.getOutputColumnNames();
            assertEquals(List.of("full_name", "age", "contact_email"), outputNames);
            
            List<String> expressions = projection.getExpressions();
            assertEquals(List.of("name", "age", "email"), expressions);
        }

        @Test
        @DisplayName("Projection tree string with aliases")
        void testProjectionTreeStringWithAliases() {
            ProjectionOperator projection = new ProjectionOperator(List.of("name AS full_name", "COUNT(*) AS total"), false);
            
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("name AS full_name"));
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("COUNT(*) AS total"));
            
            TableScanOperator tableScan = new TableScanOperator("users", null);
            projection.addChild(tableScan);
            
            String treeString = projection.toTreeString();
            assertTrue(treeString.contains("PROJECTION(name AS full_name, COUNT(*) AS total)"));
        }

        @Test
        @DisplayName("Projection SQL generation with aliases")
        void testProjectionSqlWithAliases() {
            ProjectionOperator projection = new ProjectionOperator(List.of("name AS full_name", "age"), false);
            
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("name AS full_name"));
            projection.addSelectItem(SqlExpressionParser.parseSelectItem("age"));
            
            TableScanOperator tableScan = new TableScanOperator("users", null);
            projection.addChild(tableScan);
            
            String sql = projection.toSql();
            assertTrue(sql.contains("SELECT name AS full_name, age"));
            assertTrue(sql.contains("FROM users"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Alias that looks like SQL keyword should not be treated as alias")
        void testSqlKeywordNotTreatedAsAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("column_name SELECT");
            
            // This should not be parsed as an alias since SELECT is a keyword
            assertEquals("column_name SELECT", item.getExpression());
            assertNull(item.getAlias());
        }

        @Test
        @DisplayName("Expression with operators and alias")
        void testExpressionWithOperatorsAndAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("salary * 1.1 AS increased_salary");
            
            assertEquals("salary * 1.1", item.getExpression());
            assertEquals("increased_salary", item.getAlias());
            assertTrue(item.hasAlias());
        }

        @Test
        @DisplayName("Quoted alias")
        void testQuotedAlias() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("name AS \"full name\"");
            
            assertEquals("name", item.getExpression());
            assertEquals("full name", item.getAlias());
            assertTrue(item.hasAlias());
        }

        @Test
        @DisplayName("Alias with underscores and numbers")
        void testAliasWithUnderscoresAndNumbers() {
            SqlExpressionParser.SelectItem item = SqlExpressionParser.parseSelectItem("value AS result_2024");
            
            assertEquals("value", item.getExpression());
            assertEquals("result_2024", item.getAlias());
            assertTrue(item.hasAlias());
        }
    }
}