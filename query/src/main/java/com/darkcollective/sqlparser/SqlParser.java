package com.darkcollective.sqlparser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL Parser - Clean implementation using modular expression parsers
 */
public class SqlParser {
    private static final Set<String> RELATIONAL_OPERATORS = Set.of(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS",
            "GROUP", "BY", "HAVING", "ORDER", "DISTINCT", "ON", "AS"
    );

    public static class ParsedQuery {
        private final RelationalOperator rootOperator;
        private final String originalSql;

        public ParsedQuery(RelationalOperator rootOperator, String originalSql) {
            this.rootOperator = rootOperator;
            this.originalSql = originalSql;
        }

        public RelationalOperator getRootOperator() { return rootOperator; }
        public String getOriginalSql() { return originalSql; }

        public String toTreeString() {
            return rootOperator.toTreeString();
        }

        public String toSql() {
            return reconstructSql(rootOperator);
        }

        private String reconstructSql(RelationalOperator operator) {
            StringBuilder sql = new StringBuilder();
            buildSqlFromTree(operator, sql, new SqlContext());
            return sql.toString().trim();
        }

        private void buildSqlFromTree(RelationalOperator operator, StringBuilder sql, SqlContext context) {
            if (operator instanceof ProjectionOperator) {
                ProjectionOperator proj = (ProjectionOperator) operator;
                sql.append("SELECT ");
                if (proj.isDistinct()) sql.append("DISTINCT ");
                sql.append(String.join(", ", proj.getColumns()));
                context.hasSelect = true;
            } else if (operator instanceof TableScanOperator) {
                TableScanOperator table = (TableScanOperator) operator;
                if (context.hasSelect && !context.hasFrom) {
                    sql.append(" FROM ");
                    context.hasFrom = true;
                }
                sql.append(table.toSql());
            } else if (operator instanceof SubqueryOperator) {
                SubqueryOperator subquery = (SubqueryOperator) operator;
                if (context.hasSelect && !context.hasFrom) {
                    sql.append(" FROM ");
                    context.hasFrom = true;
                }
                sql.append(subquery.toSql());
            } else if (operator instanceof JoinOperator) {
                JoinOperator join = (JoinOperator) operator;
                if (join.getChildren().size() >= 2) {
                    buildSqlFromTree(join.getChildren().get(0), sql, context);
                    sql.append(" ").append(join.getJoinType().name()).append(" JOIN ");
                    buildSqlFromTree(join.getChildren().get(1), sql, context);
                    if (join.getJoinCondition() != null && !join.getJoinCondition().isEmpty()) {
                        sql.append(" ON ").append(join.getJoinCondition());
                    }
                }
            } else if (operator instanceof SelectionOperator) {
                SelectionOperator selection = (SelectionOperator) operator;
                for (RelationalOperator child : operator.getChildren()) {
                    buildSqlFromTree(child, sql, context);
                }
                sql.append(" WHERE ").append(selection.getCondition());
            } else if (operator instanceof AggregationOperator) {
                AggregationOperator agg = (AggregationOperator) operator;
                for (RelationalOperator child : operator.getChildren()) {
                    buildSqlFromTree(child, sql, context);
                }
                if (!agg.getGroupByColumns().isEmpty()) {
                    sql.append(" GROUP BY ").append(String.join(", ", agg.getGroupByColumns()));
                }
                if (agg.getHavingCondition() != null && !agg.getHavingCondition().isEmpty()) {
                    sql.append(" HAVING ").append(agg.getHavingCondition());
                }
            } else if (operator instanceof SortOperator) {
                SortOperator sort = (SortOperator) operator;
                for (RelationalOperator child : operator.getChildren()) {
                    buildSqlFromTree(child, sql, context);
                }
                sql.append(" ORDER BY ").append(String.join(", ", sort.getOrderByColumns()));
            } else {
                for (RelationalOperator child : operator.getChildren()) {
                    buildSqlFromTree(child, sql, context);
                }
            }
        }

        private static class SqlContext {
            boolean hasSelect = false;
            boolean hasFrom = false;
        }
    }

    public ParsedQuery parse(String sql) {
        String normalizedSql = sql.trim().replaceAll("\\s+", " ");
        RelationalOperator root = parseSql(normalizedSql);
        return new ParsedQuery(root, sql);
    }

    private RelationalOperator parseSql(String sql) {
        SqlTokenizer tokenizer = new SqlTokenizer(sql);
        List<String> tokens = tokenizer.tokenize();
        return parseQuery(tokens);
    }

    private RelationalOperator parseQuery(List<String> tokens) {
        int selectIndex = findKeyword(tokens, "SELECT");
        int fromIndex = findKeyword(tokens, "FROM");
        int whereIndex = findTopLevelKeyword(tokens, "WHERE");
        int groupByIndex = findTopLevelKeywordSequence(tokens, "GROUP", "BY");
        int havingIndex = findTopLevelKeyword(tokens, "HAVING");
        int orderByIndex = findTopLevelKeywordSequence(tokens, "ORDER", "BY");

        // Parse SELECT clause
        List<String> selectColumns = new ArrayList<>();
        List<SqlFunctionCall> functionCalls = new ArrayList<>();
        List<SqlExpressionParser.SelectItem> selectItems = new ArrayList<>();
        boolean distinct = false;

        if (selectIndex != -1) {
            int start = selectIndex + 1;
            if (start < tokens.size() && "DISTINCT".equalsIgnoreCase(tokens.get(start))) {
                distinct = true;
                start++;
            }

            int end = fromIndex != -1 ? fromIndex : tokens.size();
            List<String> selectTokens = tokens.subList(start, end);

            parseSelectItems(selectTokens, selectColumns, functionCalls, selectItems);
        }

        // Parse FROM clause and joins - IMPROVED TO HANDLE SUBQUERIES
        RelationalOperator fromOperator = null;
        if (fromIndex != -1) {
            // For FROM clause, we need to be smarter about where it ends
            // especially when there are subqueries with their own clauses
            int fromEnd = findFromClauseEnd(tokens, fromIndex, whereIndex, groupByIndex, orderByIndex);
            fromOperator = parseFromClause(tokens.subList(fromIndex + 1, fromEnd));
        }

        // Parse WHERE clause - ONLY if it exists at the top level
        if (whereIndex != -1 && fromOperator != null) {
            int end = groupByIndex != -1 ? groupByIndex :
                    orderByIndex != -1 ? orderByIndex : tokens.size();

            // Use reconstructExpression to properly handle function calls in WHERE clauses
            String whereCondition = reconstructExpression(String.join(" ", tokens.subList(whereIndex + 1, end)));
            SelectionOperator selection = new SelectionOperator(whereCondition);
            selection.addChild(fromOperator);
            fromOperator = selection;
        }

        // Parse GROUP BY and HAVING - ONLY if they exist at the top level (not inside subqueries)
        if (groupByIndex != -1 && fromOperator != null) {
            int start = groupByIndex + 2;
            int havingStart = havingIndex != -1 ? havingIndex :
                    orderByIndex != -1 ? orderByIndex : tokens.size();

            List<String> groupByColumns = new ArrayList<>();
            for (int i = start; i < havingStart; i++) {
                if (!",".equals(tokens.get(i))) {
                    groupByColumns.add(tokens.get(i));
                }
            }

            String havingCondition = null;
            if (havingIndex != -1) {
                int havingEnd = orderByIndex != -1 ? orderByIndex : tokens.size();
                // Use reconstructExpression for HAVING conditions too
                havingCondition = reconstructExpression(String.join(" ", tokens.subList(havingIndex + 1, havingEnd)));
            }

            List<String> aggregateFunctions = extractAggregateFunctions(functionCalls);

            AggregationOperator aggregation = new AggregationOperator(groupByColumns, aggregateFunctions, havingCondition);
            aggregation.addChild(fromOperator);
            fromOperator = aggregation;
        }

        // Parse ORDER BY - ONLY if it exists at the top level
        if (orderByIndex != -1 && fromOperator != null) {
            List<String> orderByColumns = new ArrayList<>();
            for (int i = orderByIndex + 2; i < tokens.size(); i++) {
                if (!",".equals(tokens.get(i))) {
                    orderByColumns.add(tokens.get(i));
                }
            }

            SortOperator sort = new SortOperator(orderByColumns);
            sort.addChild(fromOperator);
            fromOperator = sort;
        }

        // Create projection operator
        if (!selectColumns.isEmpty() && fromOperator != null) {
            ProjectionOperator projection = new ProjectionOperator(selectColumns, distinct);

            for (SqlFunctionCall func : functionCalls) {
                projection.addFunctionCall(func);
            }

            for (SqlExpressionParser.SelectItem item : selectItems) {
                projection.addSelectItem(item);
            }

            projection.addChild(fromOperator);
            return projection;
        }

        return fromOperator != null ? fromOperator : new TableScanOperator("unknown", null);
    }

    /**
     * Find a keyword at the top level (not inside subqueries)
     */
    private int findTopLevelKeyword(List<String> tokens, String keyword) {
        int parenDepth = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if ("(".equals(token)) {
                parenDepth++;
            } else if (")".equals(token)) {
                parenDepth--;
            } else if (parenDepth == 0 && keyword.equalsIgnoreCase(token)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find a keyword sequence at the top level (not inside subqueries)
     */
    private int findTopLevelKeywordSequence(List<String> tokens, String first, String second) {
        int parenDepth = 0;

        for (int i = 0; i < tokens.size() - 1; i++) {
            String token = tokens.get(i);
            if ("(".equals(token)) {
                parenDepth++;
            } else if (")".equals(token)) {
                parenDepth--;
            } else if (parenDepth == 0 && first.equalsIgnoreCase(token) &&
                    i + 1 < tokens.size() && second.equalsIgnoreCase(tokens.get(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find where the FROM clause ends, accounting for subqueries that may contain their own clauses
     */
    private int findFromClauseEnd(List<String> tokens, int fromIndex, int whereIndex, int groupByIndex, int orderByIndex) {
        // Start after FROM keyword
        int start = fromIndex + 1;
        int parenDepth = 0;

        // Walk through tokens and track parentheses depth
        for (int i = start; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if ("(".equals(token)) {
                parenDepth++;
            } else if (")".equals(token)) {
                parenDepth--;
            } else if (parenDepth == 0) {
                // Only consider clause keywords when we're not inside parentheses
                if (("WHERE".equalsIgnoreCase(token) && i == whereIndex) ||
                        ("GROUP".equalsIgnoreCase(token) && i == groupByIndex) ||
                        ("ORDER".equalsIgnoreCase(token) && i == orderByIndex)) {
                    return i;
                }
            }
        }

        // If we didn't find a clause boundary, return end of tokens
        return tokens.size();
    }

    private void parseSelectItems(List<String> tokens, List<String> selectColumns,
                                  List<SqlFunctionCall> functionCalls,
                                  List<SqlExpressionParser.SelectItem> selectItems) {
        StringBuilder currentItem = new StringBuilder();
        int parenDepth = 0;

        for (String token : tokens) {
            // First, update parentheses depth BEFORE checking for comma
            if (token.contains("(")) {
                parenDepth += countChar(token, '(');
            }
            if (token.contains(")")) {
                parenDepth -= countChar(token, ')');
            }

            // Only treat comma as separator when we're at depth 0
            if (",".equals(token) && parenDepth == 0) {
                if (currentItem.length() > 0) {
                    String item = reconstructExpression(currentItem.toString().trim());
                    selectColumns.add(item);

                    SqlExpressionParser.SelectItem parsedItem = SqlExpressionParser.parseSelectItem(item);
                    selectItems.add(parsedItem);
                    functionCalls.addAll(parsedItem.getFunctions());

                    currentItem.setLength(0);
                }
            } else {
                // Add token to current item - be smart about spaces around parentheses
                if (currentItem.length() > 0 && !token.equals("(") && !currentItem.toString().endsWith("(")) {
                    currentItem.append(" ");
                }
                currentItem.append(token);
            }
        }

        // Handle last item
        if (currentItem.length() > 0) {
            String item = reconstructExpression(currentItem.toString().trim());
            selectColumns.add(item);

            SqlExpressionParser.SelectItem parsedItem = SqlExpressionParser.parseSelectItem(item);
            selectItems.add(parsedItem);
            functionCalls.addAll(parsedItem.getFunctions());
        }
    }

    /**
     * Reconstruct an expression by properly handling parentheses spacing
     */
    static String reconstructExpression(String expression) {
        // Only remove spaces around parentheses for function calls
        // Pattern: word followed by optional spaces, opening paren, optional spaces
        // This preserves spaces around operators like > < = etc.
        return expression
                .replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*", "$1(")  // function_name ( -> function_name(
                .replaceAll("\\s*\\)\\s*([^><=!\\s])", ")$1")              // ) word -> )word (but not ) > or ) =)
                .replaceAll("\\s*\\)\\s*$", ")")                           // ) at end -> )
                .replaceAll("\\s*\\)\\s*([><=!]+)", ") $1");               // ) > -> ) >
    }


    private int countChar(String str, char ch) {
        return (int) str.chars().filter(c -> c == ch).count();
    }

    private List<String> extractAggregateFunctions(List<SqlFunctionCall> functionCalls) {
        return functionCalls.stream()
                .filter(SqlFunctionCall::isAggregate)
                .map(SqlFunctionCall::toSql)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Check if a token list contains a subquery (starts with SELECT)
     */
    private boolean containsSubquery(List<String> tokens) {
        if (tokens.isEmpty()) return false;

        // Look for opening parenthesis followed by SELECT (allowing for whitespace)
        for (int i = 0; i < tokens.size(); i++) {
            if ("(".equals(tokens.get(i))) {
                // Look for SELECT after the opening parenthesis
                for (int j = i + 1; j < Math.min(i + 3, tokens.size()); j++) {
                    if ("SELECT".equalsIgnoreCase(tokens.get(j))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Parse FROM clause - handles both regular tables/joins and subqueries
     */
    private RelationalOperator parseFromClause(List<String> tokens) {
        if (tokens.isEmpty()) {
            return null;
        }

        // Check for subquery in FROM clause FIRST
        if (containsSubquery(tokens)) {
            return parseFromClauseWithSubquery(tokens);
        }

        // Rest of the original logic for regular tables and joins
        List<Integer> joinPositions = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toUpperCase();
            if ("JOIN".equals(token) ||
                    (i > 0 && (("INNER".equals(tokens.get(i-1).toUpperCase()) ||
                            "LEFT".equals(tokens.get(i-1).toUpperCase()) ||
                            "RIGHT".equals(tokens.get(i-1).toUpperCase()) ||
                            "FULL".equals(tokens.get(i-1).toUpperCase()) ||
                            "CROSS".equals(tokens.get(i-1).toUpperCase())) && "JOIN".equals(token)))) {
                joinPositions.add(i);
            }
        }

        if (joinPositions.isEmpty()) {
            String tableName = tokens.get(0);
            String alias = tokens.size() > 2 && "AS".equalsIgnoreCase(tokens.get(1)) ? tokens.get(2) :
                    tokens.size() > 1 && !"AS".equalsIgnoreCase(tokens.get(1)) ? tokens.get(1) : null;
            return new TableScanOperator(tableName, alias);
        }

        // Handle joins (existing logic)
        String firstTable = tokens.get(0);
        String firstAlias = null;
        int firstTableEnd = 1;

        if (firstTableEnd < tokens.size() && "AS".equalsIgnoreCase(tokens.get(firstTableEnd))) {
            firstTableEnd++;
            if (firstTableEnd < tokens.size()) {
                firstAlias = tokens.get(firstTableEnd);
                firstTableEnd++;
            }
        } else if (firstTableEnd < tokens.size() && !isJoinKeyword(tokens.get(firstTableEnd))) {
            firstAlias = tokens.get(firstTableEnd);
            firstTableEnd++;
        }

        RelationalOperator currentOperator = new TableScanOperator(firstTable, firstAlias);

        for (int joinPos : joinPositions) {
            JoinOperator.JoinType joinType = JoinOperator.JoinType.INNER;

            if (joinPos > 0) {
                String prevToken = tokens.get(joinPos - 1).toUpperCase();
                switch (prevToken) {
                    case "LEFT": joinType = JoinOperator.JoinType.LEFT; break;
                    case "RIGHT": joinType = JoinOperator.JoinType.RIGHT; break;
                    case "FULL": joinType = JoinOperator.JoinType.FULL; break;
                    case "CROSS": joinType = JoinOperator.JoinType.CROSS; break;
                    case "INNER": joinType = JoinOperator.JoinType.INNER; break;
                }
            }

            int tableStart = joinPos + 1;
            String joinTable = tokens.get(tableStart);
            String joinAlias = null;
            int tableEnd = tableStart + 1;

            if (tableEnd < tokens.size() && "AS".equalsIgnoreCase(tokens.get(tableEnd))) {
                tableEnd++;
                if (tableEnd < tokens.size()) {
                    joinAlias = tokens.get(tableEnd);
                    tableEnd++;
                }
            } else if (tableEnd < tokens.size() &&
                    !"ON".equalsIgnoreCase(tokens.get(tableEnd)) &&
                    !isJoinKeyword(tokens.get(tableEnd))) {
                joinAlias = tokens.get(tableEnd);
                tableEnd++;
            }

            String joinCondition = null;
            int onIndex = -1;
            for (int i = tableEnd; i < tokens.size(); i++) {
                if ("ON".equalsIgnoreCase(tokens.get(i))) {
                    onIndex = i;
                    break;
                }
            }

            if (onIndex != -1) {
                int conditionEnd = tokens.size();
                for (int i = onIndex + 1; i < tokens.size(); i++) {
                    if (isJoinKeyword(tokens.get(i)) ||
                            (i > 0 && isJoinKeyword(tokens.get(i-1)) && "JOIN".equalsIgnoreCase(tokens.get(i)))) {
                        conditionEnd = i - 1;
                        break;
                    }
                }

                if (onIndex + 1 < conditionEnd) {
                    joinCondition = String.join(" ", tokens.subList(onIndex + 1, conditionEnd));
                }
            }

            JoinOperator join = new JoinOperator(joinType, joinCondition);
            join.addChild(currentOperator);
            join.addChild(new TableScanOperator(joinTable, joinAlias));
            currentOperator = join;
        }

        return currentOperator;
    }

    /**
     * Parse FROM clause that contains subqueries
     */
    private RelationalOperator parseFromClauseWithSubquery(List<String> tokens) {
        // Find the subquery - look for the pattern more flexibly
        int startParen = -1;
        int endParen = -1;
        int parenDepth = 0;
        boolean foundSelect = false;

        // First, find the opening parenthesis that starts a subquery
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if ("(".equals(token)) {
                // Check if this parenthesis is followed by SELECT (within a few tokens)
                for (int j = i + 1; j < Math.min(i + 5, tokens.size()); j++) {
                    if ("SELECT".equalsIgnoreCase(tokens.get(j))) {
                        startParen = i;
                        foundSelect = true;
                        parenDepth = 1;
                        break;
                    }
                }
                if (foundSelect) break;
            }
        }

        if (!foundSelect || startParen == -1) {
            throw new IllegalArgumentException("Invalid subquery syntax in FROM clause: no SELECT found after opening parenthesis. Tokens: " + tokens);
        }

        // Now find the matching closing parenthesis
        for (int i = startParen + 1; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if ("(".equals(token)) {
                parenDepth++;
            } else if (")".equals(token)) {
                parenDepth--;
                if (parenDepth == 0) {
                    endParen = i;
                    break;
                }
            }
        }

        if (endParen == -1) {
            throw new IllegalArgumentException("Invalid subquery syntax in FROM clause: missing closing parenthesis. Tokens: " + tokens);
        }

        // Extract subquery tokens (without parentheses)
        List<String> subqueryTokens = tokens.subList(startParen + 1, endParen);
        String subqueryString = String.join(" ", subqueryTokens);

        // Parse the subquery recursively
        ParsedQuery subqueryParsed = parse(subqueryString);

        // Look for alias after the closing parenthesis
        String alias = null;
        if (endParen + 1 < tokens.size()) {
            String nextToken = tokens.get(endParen + 1);
            if ("AS".equalsIgnoreCase(nextToken) && endParen + 2 < tokens.size()) {
                alias = tokens.get(endParen + 2);
            } else if (!isJoinKeyword(nextToken) && !isSqlKeyword(nextToken)) {
                alias = nextToken;
            }
        }

        return new SubqueryOperator(subqueryParsed.getRootOperator(), alias, SubqueryOperator.SubqueryType.FROM_CLAUSE);
    }

    /**
     * Check if a string is a SQL keyword
     */
    private boolean isSqlKeyword(String word) {
        if (word == null) return false;

        Set<String> keywords = Set.of(
                "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS",
                "GROUP", "BY", "HAVING", "ORDER", "DISTINCT", "ON", "AS", "AND", "OR", "NOT",
                "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE", "ASC", "DESC"
        );
        return keywords.contains(word.toUpperCase());
    }

    private boolean isJoinKeyword(String token) {
        String upper = token.toUpperCase();
        return "JOIN".equals(upper) || "INNER".equals(upper) || "LEFT".equals(upper) ||
                "RIGHT".equals(upper) || "FULL".equals(upper) || "CROSS".equals(upper);
    }

    private int findKeyword(List<String> tokens, String keyword) {
        for (int i = 0; i < tokens.size(); i++) {
            if (keyword.equalsIgnoreCase(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findKeywordSequence(List<String> tokens, String first, String second) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (first.equalsIgnoreCase(tokens.get(i)) && second.equalsIgnoreCase(tokens.get(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    private static class SqlTokenizer {
        private final String sql;

        public SqlTokenizer(String sql) {
            this.sql = sql;
        }

        public List<String> tokenize() {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);

                if (c == '\'' && !inQuotes) {
                    inQuotes = true;
                    current.append(c);
                } else if (c == '\'' && inQuotes) {
                    inQuotes = false;
                    current.append(c);
                } else if (inQuotes) {
                    current.append(c);
                } else if (c == '(' || c == ')') {
                    // Always create separate tokens for parentheses
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    tokens.add(String.valueOf(c));
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else if (c == ',') {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    tokens.add(",");
                } else {
                    current.append(c);
                }
            }

            if (current.length() > 0) {
                tokens.add(current.toString());
            }

            return tokens;
        }
    }

    public static ParsedQuery fromTreeString(String treeString) {
        try {
            RelationalOperator rootOperator = ParentheticalAlgebraParser.parse(treeString);
            return new ParsedQuery(rootOperator, null); // No original SQL since we're parsing from tree
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse tree string: " + e.getMessage(), e);
        }
    }

}
