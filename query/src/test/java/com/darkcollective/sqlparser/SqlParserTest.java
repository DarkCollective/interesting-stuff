package com.darkcollective.sqlparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("SQL Parser Comprehensive Test Suite")
class SqlParserTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    @Nested
    @DisplayName("Basic SELECT Queries")
    class BasicSelectTests {

        @Test
        @DisplayName("Simple SELECT with single column")
        void testSimpleSelectSingleColumn() {
            String sql = "SELECT name FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            assertNotNull(query);
            assertNotNull(query.getRootOperator());
            assertTrue(query.getRootOperator() instanceof ProjectionOperator);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(1, projection.getColumns().size());
            assertEquals("name", projection.getColumns().get(0));
            assertFalse(projection.isDistinct());

            assertTrue(projection.getChildren().get(0) instanceof TableScanOperator);
            TableScanOperator tableScan = (TableScanOperator) projection.getChildren().get(0);
            assertEquals("users", tableScan.getTableName());
        }

        @Test
        @DisplayName("SELECT with multiple columns")
        void testSelectMultipleColumns() {
            String sql = "SELECT name, age, email FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(3, projection.getColumns().size());
            assertEquals(List.of("name", "age", "email"), projection.getColumns());
        }

        @Test
        @DisplayName("SELECT with DISTINCT")
        void testSelectDistinct() {
            String sql = "SELECT DISTINCT department FROM employees";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertTrue(projection.isDistinct());
            assertEquals("department", projection.getColumns().get(0));
        }

        @Test
        @DisplayName("SELECT with table alias")
        void testSelectWithTableAlias() {
            String sql = "SELECT u.name FROM users u";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            TableScanOperator tableScan = (TableScanOperator) projection.getChildren().get(0);
            assertEquals("users", tableScan.getTableName());
            assertEquals("u", tableScan.getAlias());
        }

        @Test
        @DisplayName("SELECT with AS table alias")
        void testSelectWithAsTableAlias() {
            String sql = "SELECT u.name FROM users AS u";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            TableScanOperator tableScan = (TableScanOperator) projection.getChildren().get(0);
            assertEquals("users", tableScan.getTableName());
            assertEquals("u", tableScan.getAlias());
        }
    }

    @Nested
    @DisplayName("WHERE Clause Tests")
    class WhereClauseTests {

        @Test
        @DisplayName("Simple WHERE condition")
        void testSimpleWhere() {
            String sql = "SELECT name FROM users WHERE age > 18";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertTrue(projection.getChildren().get(0) instanceof SelectionOperator);

            SelectionOperator selection = (SelectionOperator) projection.getChildren().get(0);
            assertEquals("age > 18", selection.getCondition());
        }

        @Test
        @DisplayName("Complex WHERE condition")
        void testComplexWhere() {
            String sql = "SELECT name FROM users WHERE age > 18 AND status = 'active'";
            SqlParser.ParsedQuery query = parser.parse(sql);

            SelectionOperator selection = (SelectionOperator)
                    ((ProjectionOperator) query.getRootOperator()).getChildren().get(0);
            assertEquals("age > 18 AND status = 'active'", selection.getCondition());
        }

        @Test
        @DisplayName("WHERE with function in condition")
        void testWhereWithFunction() {
            String sql = "SELECT name FROM users WHERE LENGTH(name) > 5";
            SqlParser.ParsedQuery query = parser.parse(sql);

            SelectionOperator selection = (SelectionOperator)
                    ((ProjectionOperator) query.getRootOperator()).getChildren().get(0);
            assertEquals("LENGTH(name) > 5", selection.getCondition());
        }
    }

    @Nested
    @DisplayName("Aggregate Functions Tests")
    class AggregateFunctionTests {

        @Test
        @DisplayName("Simple COUNT function")
        void testCountFunction() {
            String sql = "SELECT COUNT(*) FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals("COUNT(*)", projection.getColumns().get(0));
            assertEquals(1, projection.getFunctionCalls().size());
            assertEquals("COUNT", projection.getFunctionCalls().get(0).getFunctionName());
            assertEquals(SqlFunctionRegistry.FunctionType.AGGREGATE, projection.getFunctionCalls().get(0).getType());
        }

        @Test
        @DisplayName("Multiple aggregate functions")
        void testMultipleAggregateFunctions() {
            String sql = "SELECT COUNT(*), AVG(age), MIN(salary), MAX(salary) FROM employees";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(4, projection.getColumns().size());
            assertEquals(4, projection.getFunctionCalls().size());

            List<SqlFunctionCall> functions = projection.getFunctionCalls();
            assertEquals("COUNT", functions.get(0).getFunctionName());
            assertEquals("AVG", functions.get(1).getFunctionName());
            assertEquals("MIN", functions.get(2).getFunctionName());
            assertEquals("MAX", functions.get(3).getFunctionName());
        }

        @Test
        @DisplayName("GROUP BY with aggregate functions")
        void testGroupByWithAggregates() {
            String sql = "SELECT department, COUNT(*), AVG(salary) FROM employees GROUP BY department";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertTrue(projection.getChildren().get(0) instanceof AggregationOperator);

            AggregationOperator aggregation = (AggregationOperator) projection.getChildren().get(0);
            assertEquals(List.of("department"), aggregation.getGroupByColumns());
            assertTrue(aggregation.getAggregateFunctions().contains("COUNT(*)"));
            assertTrue(aggregation.getAggregateFunctions().contains("AVG(salary)"));
        }

        @Test
        @DisplayName("GROUP BY with HAVING")
        void testGroupByWithHaving() {
            String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            AggregationOperator aggregation = (AggregationOperator) projection.getChildren().get(0);
            assertEquals("COUNT(*) > 5", aggregation.getHavingCondition());
        }
    }

    @Nested
    @DisplayName("String Functions Tests")
    class StringFunctionTests {

        @ParameterizedTest
        @ValueSource(strings = {"UPPER", "LOWER", "TRIM", "LENGTH", "LEN"})
        @DisplayName("Single argument string functions")
        void testSingleArgStringFunctions(String functionName) {
            String sql = String.format("SELECT %s(name) FROM users", functionName);
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            assertEquals(1, projection.getFunctionCalls().size());

            SqlFunctionCall function = projection.getFunctionCalls().get(0);
            assertEquals(functionName, function.getFunctionName());
            assertEquals(SqlFunctionRegistry.FunctionType.STRING, function.getType());
            assertEquals(List.of("name"), function.getArguments());
        }

        @Test
        @DisplayName("CONCAT function with multiple arguments")
        void testConcatFunction() {
            String sql = "SELECT CONCAT(first_name, ' ', last_name) FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SqlFunctionCall function = projection.getFunctionCalls().get(0);
            assertEquals("CONCAT", function.getFunctionName());
            assertEquals(SqlFunctionRegistry.FunctionType.STRING, function.getType());
            assertEquals(3, function.getArguments().size());
        }

        @Test
        @DisplayName("SUBSTR function")
        void testSubstrFunction() {
            String sql = "SELECT SUBSTR(name, 1, 5) FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SqlFunctionCall function = projection.getFunctionCalls().get(0);
            assertEquals("SUBSTR", function.getFunctionName());
            assertEquals(SqlFunctionRegistry.FunctionType.STRING, function.getType());
            assertEquals(List.of("name", "1", "5"), function.getArguments());
        }
    }

    @Nested
    @DisplayName("Numeric Functions Tests")
    class NumericFunctionTests {

        @ParameterizedTest
        @CsvSource({
                "ROUND, salary, 2",
                "FLOOR, price, 0",
                "CEIL, amount, 0",
                "ABS, balance, 0"
        })
        @DisplayName("Numeric functions with arguments")
        void testNumericFunctions(String functionName, String column, String extraArg) {
            String sql = extraArg.equals("0") ?
                    String.format("SELECT %s(%s) FROM table1", functionName, column) :
                    String.format("SELECT %s(%s, %s) FROM table1", functionName, column, extraArg);

            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            SqlFunctionCall function = projection.getFunctionCalls().get(0);
            assertEquals(functionName, function.getFunctionName());
            assertEquals(SqlFunctionRegistry.FunctionType.NUMERIC, function.getType());
        }
    }

    @Nested
    @DisplayName("Function Type Detection Tests")
    class FunctionTypeDetectionTests {

        @Test
        @DisplayName("Aggregate function detection")
        void testAggregateFunctionDetection() {
            String sql = "SELECT COUNT(*), SUM(salary) FROM employees";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            List<SqlFunctionCall> functions = projection.getFunctionCalls();

            assertEquals(2, functions.size());
            assertTrue(functions.stream().allMatch(f -> SqlFunctionRegistry.FunctionType.AGGREGATE.equals(f.getType())));
        }

        @Test
        @DisplayName("String function detection")
        void testStringFunctionDetection() {
            String sql = "SELECT UPPER(name), TRIM(description) FROM users";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            List<SqlFunctionCall> functions = projection.getFunctionCalls();

            assertEquals(2, functions.size());
            assertTrue(functions.stream().allMatch(f -> SqlFunctionRegistry.FunctionType.STRING.equals(f.getType())));
        }

        @Test
        @DisplayName("Mixed function types")
        void testMixedFunctionTypes() {
            String sql = "SELECT COUNT(*), ROUND(salary, 2), UPPER(name) FROM employees";
            SqlParser.ParsedQuery query = parser.parse(sql);

            ProjectionOperator projection = (ProjectionOperator) query.getRootOperator();
            List<SqlFunctionCall> functions = projection.getFunctionCalls();

            assertEquals(3, functions.size());

            long aggregateCount = functions.stream().filter(f -> SqlFunctionRegistry.FunctionType.AGGREGATE.equals(f.getType())).count();
            long stringCount = functions.stream().filter(f -> SqlFunctionRegistry.FunctionType.STRING.equals(f.getType())).count();
            long numericCount = functions.stream().filter(f -> SqlFunctionRegistry.FunctionType.NUMERIC.equals(f.getType())).count();

            assertEquals(1, aggregateCount);
            assertEquals(1, stringCount);
            assertEquals(1, numericCount);
        }
    }

    // Include all other test classes as before (JOIN, ORDER BY, Complex Queries, etc.)
    // but update any references to FunctionCall to SqlFunctionCall and getType() comparisons
}