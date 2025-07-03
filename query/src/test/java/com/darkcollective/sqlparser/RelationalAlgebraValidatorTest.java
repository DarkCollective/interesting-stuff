package com.darkcollective.sqlparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("Relational Algebra Validator Test Suite")
class RelationalAlgebraValidatorTest {

    private Schema schema;
    private RelationalAlgebraValidator validator;

    @BeforeEach
    void setUp() {
        // Create a sample schema
        schema = new Schema();

        // Users table
        Schema.Table users = new Schema.Table("users");
        users.addColumn(new Schema.Column("id", Schema.DataType.INTEGER, false, true));
        users.addColumn(new Schema.Column("name", Schema.DataType.VARCHAR));
        users.addColumn(new Schema.Column("email", Schema.DataType.VARCHAR));
        users.addColumn(new Schema.Column("age", Schema.DataType.INTEGER));
        users.addColumn(new Schema.Column("department_id", Schema.DataType.INTEGER));
        schema.addTable(users);

        // Posts table
        Schema.Table posts = new Schema.Table("posts");
        posts.addColumn(new Schema.Column("id", Schema.DataType.INTEGER, false, true));
        posts.addColumn(new Schema.Column("title", Schema.DataType.VARCHAR));
        posts.addColumn(new Schema.Column("content", Schema.DataType.TEXT));
        posts.addColumn(new Schema.Column("user_id", Schema.DataType.INTEGER));
        posts.addColumn(new Schema.Column("created_at", Schema.DataType.TIMESTAMP));
        schema.addTable(posts);

        // Departments table
        Schema.Table departments = new Schema.Table("departments");
        departments.addColumn(new Schema.Column("id", Schema.DataType.INTEGER, false, true));
        departments.addColumn(new Schema.Column("name", Schema.DataType.VARCHAR));
        departments.addColumn(new Schema.Column("budget", Schema.DataType.DECIMAL));
        schema.addTable(departments);

        validator = new RelationalAlgebraValidator(schema);
    }

    @Nested
    @DisplayName("Table Scan Validation")
    class TableScanValidationTests {

        @Test
        @DisplayName("Valid table scan")
        void testValidTableScan() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            RelationalAlgebraValidator.ValidationResult result = validator.validate(tableScan);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
            assertFalse(result.hasWarnings());
        }

        @Test
        @DisplayName("Valid table scan with alias")
        void testValidTableScanWithAlias() {
            TableScanOperator tableScan = new TableScanOperator("users", "u");
            RelationalAlgebraValidator.ValidationResult result = validator.validate(tableScan);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Invalid table scan - table does not exist")
        void testInvalidTableScan() {
            TableScanOperator tableScan = new TableScanOperator("nonexistent", null);
            RelationalAlgebraValidator.ValidationResult result = validator.validate(tableScan);

            assertFalse(result.isValid());
            assertEquals(1, result.getErrors().size());
            assertTrue(result.getErrors().get(0).contains("does not exist in schema"));
        }
    }

    @Nested
    @DisplayName("Projection Validation")
    class ProjectionValidationTests {

        @Test
        @DisplayName("Valid projection with existing columns")
        void testValidProjection() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("name", "email"), false);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Valid projection with qualified columns")
        void testValidProjectionWithQualifiedColumns() {
            TableScanOperator tableScan = new TableScanOperator("users", "u");
            ProjectionOperator projection = new ProjectionOperator(List.of("u.name", "u.email"), false);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Valid projection with SELECT *")
        void testValidProjectionWithStar() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("*"), false);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid projection - column does not exist")
        void testInvalidProjectionNonexistentColumn() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("nonexistent_column"), false);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertFalse(result.isValid());
            assertEquals(1, result.getErrors().size());
            assertTrue(result.getErrors().get(0).contains("is not available in projection"));
        }

        @Test
        @DisplayName("Valid projection with functions")
        void testValidProjectionWithFunctions() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("UPPER(name)", "LENGTH(email)"), false);

            SqlFunctionCall upperFunc = new SqlFunctionCall("UPPER", List.of("name"), "UPPER(name)");
            SqlFunctionCall lengthFunc = new SqlFunctionCall("LENGTH", List.of("email"), "LENGTH(email)");
            projection.addFunctionCall(upperFunc);
            projection.addFunctionCall(lengthFunc);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid projection with function using nonexistent column")
        void testInvalidProjectionWithFunction() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            ProjectionOperator projection = new ProjectionOperator(List.of("UPPER(nonexistent)"), false);

            SqlFunctionCall upperFunc = new SqlFunctionCall("UPPER", List.of("nonexistent"), "UPPER(nonexistent)");
            projection.addFunctionCall(upperFunc);
            projection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent") && error.contains("is not available")));
        }
    }

    @Nested
    @DisplayName("Selection Validation")
    class SelectionValidationTests {

        @Test
        @DisplayName("Valid selection with simple condition")
        void testValidSelection() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SelectionOperator selection = new SelectionOperator("age > 18");
            selection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(selection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Valid selection with qualified column")
        void testValidSelectionWithQualifiedColumn() {
            TableScanOperator tableScan = new TableScanOperator("users", "u");
            SelectionOperator selection = new SelectionOperator("u.age > 18");
            selection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(selection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid selection - column does not exist")
        void testInvalidSelectionNonexistentColumn() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SelectionOperator selection = new SelectionOperator("nonexistent > 18");
            selection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(selection);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent") && error.contains("is not available")));
        }

        @Test
        @DisplayName("Valid selection with function in condition")
        void testValidSelectionWithFunction() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SelectionOperator selection = new SelectionOperator("LENGTH(name) > 5");
            selection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(selection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Valid selection with complex condition")
        void testValidSelectionComplexCondition() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SelectionOperator selection = new SelectionOperator("age > 18 AND name LIKE 'John%'");
            selection.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(selection);

            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("Join Validation")
    class JoinValidationTests {

        @Test
        @DisplayName("Valid inner join")
        void testValidInnerJoin() {
            TableScanOperator leftTable = new TableScanOperator("users", "u");
            TableScanOperator rightTable = new TableScanOperator("posts", "p");
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.id = p.user_id");
            join.addChild(leftTable);
            join.addChild(rightTable);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(join);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Join with ambiguous column names - should warn")
        void testJoinWithAmbiguousColumns() {
            TableScanOperator leftTable = new TableScanOperator("users", null);
            TableScanOperator rightTable = new TableScanOperator("posts", null);
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "users.id = posts.user_id");
            join.addChild(leftTable);
            join.addChild(rightTable);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(join);

            assertTrue(result.isValid());
            // Should have warnings about ambiguous 'id' column
            assertTrue(result.hasWarnings());
        }

        @Test
        @DisplayName("Invalid join condition - nonexistent column")
        void testInvalidJoinCondition() {
            TableScanOperator leftTable = new TableScanOperator("users", "u");
            TableScanOperator rightTable = new TableScanOperator("posts", "p");
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.nonexistent = p.user_id");
            join.addChild(leftTable);
            join.addChild(rightTable);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(join);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent") && error.contains("is not available")));
        }

        @Test
        @DisplayName("Join with wrong number of children")
        void testJoinWrongChildCount() {
            TableScanOperator table = new TableScanOperator("users", null);
            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "condition");
            join.addChild(table); // Only one child instead of two

            RelationalAlgebraValidator.ValidationResult result = validator.validate(join);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("must have exactly 2 children")));
        }
    }

    @Nested
    @DisplayName("Aggregation Validation")
    class AggregationValidationTests {

        @Test
        @DisplayName("Valid aggregation with GROUP BY")
        void testValidAggregationWithGroupBy() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department_id"),
                    List.of("COUNT(*)", "AVG(age)"),
                    null
            );
            aggregation.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(aggregation);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Valid aggregation with HAVING")
        void testValidAggregationWithHaving() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department_id"),
                    List.of("COUNT(*)"),
                    "COUNT(*) > 5"
            );
            aggregation.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(aggregation);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid GROUP BY column")
        void testInvalidGroupByColumn() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("nonexistent_column"),
                    List.of("COUNT(*)"),
                    null
            );
            aggregation.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(aggregation);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent_column") && error.contains("is not available")));
        }

        @Test
        @DisplayName("Invalid aggregate function argument")
        void testInvalidAggregateFunctionArgument() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department_id"),
                    List.of("AVG(nonexistent_column)"),
                    null
            );
            aggregation.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(aggregation);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent_column") && error.contains("is not available")));
        }
    }

    @Nested
    @DisplayName("Sort Validation")
    class SortValidationTests {

        @Test
        @DisplayName("Valid sort")
        void testValidSort() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SortOperator sort = new SortOperator(List.of("name", "ASC", "age", "DESC"));
            sort.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(sort);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid ORDER BY column")
        void testInvalidOrderByColumn() {
            TableScanOperator tableScan = new TableScanOperator("users", null);
            SortOperator sort = new SortOperator(List.of("nonexistent_column"));
            sort.addChild(tableScan);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(sort);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("nonexistent_column") && error.contains("is not available")));
        }
    }

    @Nested
    @DisplayName("Complex Query Validation")
    class ComplexQueryValidationTests {

        @Test
        @DisplayName("Valid complex query with join and aggregation")
        void testValidComplexQuery() {
            // SELECT u.department_id, COUNT(*) FROM users u
            // INNER JOIN posts p ON u.id = p.user_id
            // WHERE u.age > 18
            // GROUP BY u.department_id

            TableScanOperator usersTable = new TableScanOperator("users", "u");
            TableScanOperator postsTable = new TableScanOperator("posts", "p");

            JoinOperator join = new JoinOperator(JoinOperator.JoinType.INNER, "u.id = p.user_id");
            join.addChild(usersTable);
            join.addChild(postsTable);

            SelectionOperator selection = new SelectionOperator("u.age > 18");
            selection.addChild(join);

            AggregationOperator aggregation = new AggregationOperator(
                    List.of("u.department_id"),
                    List.of("COUNT(*)"),
                    null
            );
            aggregation.addChild(selection);

            ProjectionOperator projection = new ProjectionOperator(
                    List.of("u.department_id", "COUNT(*)"), false);
            projection.addChild(aggregation);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Invalid complex query - column not available after aggregation")
        void testInvalidComplexQueryColumnNotAvailableAfterAggregation() {
            // SELECT name, COUNT(*) FROM users GROUP BY department_id
            // This is invalid because 'name' is not in GROUP BY

            TableScanOperator tableScan = new TableScanOperator("users", null);

            AggregationOperator aggregation = new AggregationOperator(
                    List.of("department_id"),
                    List.of("COUNT(*)"),
                    null
            );
            aggregation.addChild(tableScan);

            ProjectionOperator projection = new ProjectionOperator(
                    List.of("name", "COUNT(*)"), false);
            projection.addChild(aggregation);

            RelationalAlgebraValidator.ValidationResult result = validator.validate(projection);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(error ->
                    error.contains("name") && error.contains("is not available")));
        }
    }

    @Nested
    @DisplayName("Schema Validation")
    class SchemaValidationTests {

        @Test
        @DisplayName("Schema with tables and columns")
        void testSchemaCreation() {
            assertNotNull(schema);
            assertTrue(schema.hasTable("users"));
            assertTrue(schema.hasTable("posts"));
            assertTrue(schema.hasTable("departments"));

            Schema.Table usersTable = schema.getTable("users");
            assertNotNull(usersTable);
            assertTrue(usersTable.hasColumn("name"));
            assertTrue(usersTable.hasColumn("email"));
            assertTrue(usersTable.hasColumn("age"));

            Schema.Column nameColumn = usersTable.getColumn("name");
            assertNotNull(nameColumn);
            assertEquals(Schema.DataType.VARCHAR, nameColumn.getDataType());
        }

        @Test
        @DisplayName("Case insensitive table and column names")
        void testCaseInsensitiveNames() {
            assertTrue(schema.hasTable("USERS"));
            assertTrue(schema.hasTable("Users"));

            Schema.Table usersTable = schema.getTable("USERS");
            assertNotNull(usersTable);
            assertTrue(usersTable.hasColumn("NAME"));
            assertTrue(usersTable.hasColumn("Name"));
        }
    }
}
