
package com.darkcollective.sqlparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

@DisplayName("Expression Tree Parser Test Suite")
class ExpressionTreeParserTest {

    private ExpressionTreeParser parser;
    private Schema schema;

    @BeforeEach
    void setUp() {
        schema = new Schema();
        parser = new ExpressionTreeParser(schema);
    }

    @Nested
    @DisplayName("Numeric Expression Tests")
    class NumericExpressionTests {

        @Test
        @DisplayName("Parse simple numeric addition")
        void testSimpleNumericAddition() {
            String expression = "1 + 2";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            OperatorNode opNode = (OperatorNode) node;
            assertEquals("+", opNode.getOperator());
            assertEquals(Schema.DataType.INTEGER, opNode.getDataType());
            assertTrue(opNode.validateTypes());

            List<ExpressionNode> children = opNode.getChildren();
            assertEquals(2, children.size());
            assertTrue(children.get(0) instanceof LiteralNode);
            assertTrue(children.get(1) instanceof LiteralNode);
        }

        @Test
        @DisplayName("Parse complex numeric expression")
        void testComplexNumericExpression() {
            String expression = "1 * 2 + 10";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            OperatorNode addNode = (OperatorNode) node;
            assertEquals("+", addNode.getOperator());
            assertEquals(Schema.DataType.INTEGER, addNode.getDataType());
            assertTrue(addNode.validateTypes());

            List<ExpressionNode> children = addNode.getChildren();
            assertEquals(2, children.size());
            
            // First child should be multiplication
            assertTrue(children.get(0) instanceof OperatorNode);
            OperatorNode multiplyNode = (OperatorNode) children.get(0);
            assertEquals("*", multiplyNode.getOperator());
            
            // Second child should be literal 10
            assertTrue(children.get(1) instanceof LiteralNode);
        }
    }

    @Nested
    @DisplayName("String Expression Tests")
    class StringExpressionTests {

        @Test
        @DisplayName("Parse string concatenation")
        void testStringConcatenation() {
            String expression = "\"STRING\" + \"String\"";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            OperatorNode opNode = (OperatorNode) node;
            assertEquals("+", opNode.getOperator());
            assertEquals(Schema.DataType.VARCHAR, opNode.getDataType());
            assertTrue(opNode.validateTypes());

            List<ExpressionNode> children = opNode.getChildren();
            assertEquals(2, children.size());
            assertTrue(children.get(0) instanceof LiteralNode);
            assertTrue(children.get(1) instanceof LiteralNode);

            LiteralNode left = (LiteralNode) children.get(0);
            LiteralNode right = (LiteralNode) children.get(1);
            assertEquals(Schema.DataType.VARCHAR, left.getDataType());
            assertEquals(Schema.DataType.VARCHAR, right.getDataType());
        }

        @Test
        @DisplayName("Parse string with single quotes")
        void testStringWithSingleQuotes() {
            String expression = "'Hello' + 'World'";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            OperatorNode opNode = (OperatorNode) node;
            assertEquals(Schema.DataType.VARCHAR, opNode.getDataType());
            assertTrue(opNode.validateTypes());
        }
    }

    @Nested
    @DisplayName("SELECT Item Parsing Tests")
    class SelectItemParsingTests {

        @Test
        @DisplayName("Parse SELECT items with aliases")
        void testSelectItemsWithAliases() {
            String selectItem1 = "1*2+10 AS number";
            String selectItem2 = "\"STRING\"+\"String\" as string";

            ExpressionTreeParser.SelectItemWithTree item1 = parser.parseSelectItem(selectItem1);
            ExpressionTreeParser.SelectItemWithTree item2 = parser.parseSelectItem(selectItem2);

            // Test numeric expression
            assertEquals("number", item1.getAlias());
            assertEquals(Schema.DataType.INTEGER, item1.getDataType());
            assertTrue(item1.isValidExpression());

            // Test string expression
            assertEquals("string", item2.getAlias());
            assertEquals(Schema.DataType.VARCHAR, item2.getDataType());
            assertTrue(item2.isValidExpression());
        }
    }

    @Nested
    @DisplayName("Type Validation Tests")
    class TypeValidationTests {

        @Test
        @DisplayName("Invalid mixed type expression should fail validation")
        void testInvalidMixedTypeExpression() {
            String expression = "1 + \"string\"";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            assertFalse(node.validateTypes());
        }

        @Test
        @DisplayName("Valid numeric expression should pass validation")
        void testValidNumericExpression() {
            String expression = "1.5 + 2";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            assertEquals(Schema.DataType.DECIMAL, node.getDataType());
            assertTrue(node.validateTypes());
        }

        @Test
        @DisplayName("Valid string expression should pass validation")
        void testValidStringExpression() {
            String expression = "\"Hello\" + \"World\"";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            assertEquals(Schema.DataType.VARCHAR, node.getDataType());
            assertTrue(node.validateTypes());
        }
    }

    @Nested
    @DisplayName("Expression Tree Structure Tests")
    class ExpressionTreeStructureTests {

        @Test
        @DisplayName("Expression tree maintains proper structure")
        void testExpressionTreeStructure() {
            String expression = "(1 + 2) * 3";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            assertTrue(node instanceof OperatorNode);
            OperatorNode multiplyNode = (OperatorNode) node;
            assertEquals("*", multiplyNode.getOperator());

            List<ExpressionNode> children = multiplyNode.getChildren();
            assertEquals(2, children.size());
            
            // First child should be addition
            assertTrue(children.get(0) instanceof OperatorNode);
            OperatorNode addNode = (OperatorNode) children.get(0);
            assertEquals("+", addNode.getOperator());
            
            // Second child should be literal 3
            assertTrue(children.get(1) instanceof LiteralNode);
        }

        @Test
        @DisplayName("Expression tree toString produces readable output")
        void testExpressionTreeToString() {
            String expression = "1 + 2";
            ExpressionNode node = parser.parseExpression(expression, new ArrayList<>());

            String treeString = node.toString();
            assertNotNull(treeString);
            assertTrue(treeString.contains("+"));
            assertTrue(treeString.contains("1"));
            assertTrue(treeString.contains("2"));
        }
    }
}