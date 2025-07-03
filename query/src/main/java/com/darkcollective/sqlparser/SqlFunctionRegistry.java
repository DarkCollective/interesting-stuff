package com.darkcollective.sqlparser;

import java.util.*;

/**
 * Registry for all SQL function definitions and categorization
 */
public class SqlFunctionRegistry {
    
    public enum FunctionType {
        AGGREGATE, STRING, NUMERIC, DATE, CONDITIONAL, UNKNOWN
    }
    
    private static final Set<String> AGGREGATE_FUNCTIONS = Set.of(
            "COUNT", "SUM", "AVG", "MIN", "MAX", "GROUP_CONCAT", "STRING_AGG",
            "STDDEV", "VARIANCE", "MEDIAN"
    );
    
    private static final Set<String> STRING_FUNCTIONS = Set.of(
            "UPPER", "LOWER", "TRIM", "LTRIM", "RTRIM", "SUBSTR", "SUBSTRING",
            "LENGTH", "LEN", "CONCAT", "REPLACE", "LEFT", "RIGHT", "REVERSE",
            "CHARINDEX", "PATINDEX", "STUFF", "REPLICATE"
    );
    
    private static final Set<String> NUMERIC_FUNCTIONS = Set.of(
            "ROUND", "FLOOR", "CEIL", "ABS", "SQRT", "POWER", "MOD", "RAND",
            "SIN", "COS", "TAN", "LOG", "LOG10", "EXP", "PI", "SIGN"
    );
    
    private static final Set<String> DATE_FUNCTIONS = Set.of(
            "NOW", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", 
            "DATEADD", "DATEDIFF", "EXTRACT", "YEAR", "MONTH", "DAY",
            "HOUR", "MINUTE", "SECOND", "GETDATE", "GETUTCDATE"
    );
    
    private static final Set<String> CONDITIONAL_FUNCTIONS = Set.of(
            "CASE", "WHEN", "THEN", "ELSE", "END", "COALESCE", "NULLIF", 
            "ISNULL", "IIF", "CHOOSE"
    );

    // Combined set of all functions
    private static final Set<String> ALL_SQL_FUNCTIONS = new HashSet<>();
    static {
        ALL_SQL_FUNCTIONS.addAll(AGGREGATE_FUNCTIONS);
        ALL_SQL_FUNCTIONS.addAll(STRING_FUNCTIONS);
        ALL_SQL_FUNCTIONS.addAll(NUMERIC_FUNCTIONS);
        ALL_SQL_FUNCTIONS.addAll(DATE_FUNCTIONS);
        ALL_SQL_FUNCTIONS.addAll(CONDITIONAL_FUNCTIONS);
    }

    /**
     * Get all registered SQL functions
     */
    public static Set<String> getAllFunctions() {
        return Collections.unmodifiableSet(ALL_SQL_FUNCTIONS);
    }

    /**
     * Get functions by type
     */
    public static Set<String> getFunctionsByType(FunctionType type) {
        return switch (type) {
            case AGGREGATE -> Collections.unmodifiableSet(AGGREGATE_FUNCTIONS);
            case STRING -> Collections.unmodifiableSet(STRING_FUNCTIONS);
            case NUMERIC -> Collections.unmodifiableSet(NUMERIC_FUNCTIONS);
            case DATE -> Collections.unmodifiableSet(DATE_FUNCTIONS);
            case CONDITIONAL -> Collections.unmodifiableSet(CONDITIONAL_FUNCTIONS);
            case UNKNOWN -> Collections.emptySet();
        };
    }

    /**
     * Determine the type of a function by its name
     */
    public static FunctionType determineFunctionType(String functionName) {
        String upperFunc = functionName.toUpperCase();
        if (AGGREGATE_FUNCTIONS.contains(upperFunc)) return FunctionType.AGGREGATE;
        if (STRING_FUNCTIONS.contains(upperFunc)) return FunctionType.STRING;
        if (NUMERIC_FUNCTIONS.contains(upperFunc)) return FunctionType.NUMERIC;
        if (DATE_FUNCTIONS.contains(upperFunc)) return FunctionType.DATE;
        if (CONDITIONAL_FUNCTIONS.contains(upperFunc)) return FunctionType.CONDITIONAL;
        return FunctionType.UNKNOWN;
    }

    /**
     * Check if a function name is registered
     */
    public static boolean isRegisteredFunction(String functionName) {
        return ALL_SQL_FUNCTIONS.contains(functionName.toUpperCase());
    }

    /**
     * Register a new function (for extensibility)
     */
    public static void registerFunction(String functionName, FunctionType type) {
        String upperFunc = functionName.toUpperCase();
        ALL_SQL_FUNCTIONS.add(upperFunc);
        
        switch (type) {
            case AGGREGATE -> AGGREGATE_FUNCTIONS.add(upperFunc);
            case STRING -> STRING_FUNCTIONS.add(upperFunc);
            case NUMERIC -> NUMERIC_FUNCTIONS.add(upperFunc);
            case DATE -> DATE_FUNCTIONS.add(upperFunc);
            case CONDITIONAL -> CONDITIONAL_FUNCTIONS.add(upperFunc);
        }
    }
}