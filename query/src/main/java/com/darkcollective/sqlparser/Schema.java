
package com.darkcollective.sqlparser;

import java.util.*;

/**
 * Represents a database schema with tables and their columns
 */
public class Schema {
    private final Map<String, Table> tables;

    public Schema() {
        this.tables = new HashMap<>();
    }

    public Schema(Map<String, Table> tables) {
        this.tables = new HashMap<>(tables);
    }

    /**
     * Add a table to the schema
     */
    public void addTable(Table table) {
        tables.put(table.getName().toLowerCase(), table);
    }

    /**
     * Get a table by name (case-insensitive)
     */
    public Table getTable(String tableName) {
        return tables.get(tableName.toLowerCase());
    }

    /**
     * Check if table exists (case-insensitive)
     */
    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }

    /**
     * Get all table names
     */
    public Set<String> getTableNames() {
        return new HashSet<>(tables.keySet());
    }

    /**
     * Get all tables
     */
    public Collection<Table> getTables() {
        return Collections.unmodifiableCollection(tables.values());
    }

    /**
     * Represents a database table with its columns
     */
    public static class Table {
        private final String name;
        private final Map<String, Column> columns;

        public Table(String name) {
            this.name = name;
            this.columns = new HashMap<>();
        }

        public Table(String name, List<Column> columns) {
            this.name = name;
            this.columns = new HashMap<>();
            for (Column column : columns) {
                this.columns.put(column.getName().toLowerCase(), column);
            }
        }

        public String getName() {
            return name;
        }

        /**
         * Add a column to the table
         */
        public void addColumn(Column column) {
            columns.put(column.getName().toLowerCase(), column);
        }

        /**
         * Get a column by name (case-insensitive)
         */
        public Column getColumn(String columnName) {
            return columns.get(columnName.toLowerCase());
        }

        /**
         * Check if column exists (case-insensitive)
         */
        public boolean hasColumn(String columnName) {
            return columns.containsKey(columnName.toLowerCase());
        }

        /**
         * Get all column names
         */
        public Set<String> getColumnNames() {
            return new HashSet<>(columns.keySet());
        }

        /**
         * Get all columns
         */
        public Collection<Column> getColumns() {
            return Collections.unmodifiableCollection(columns.values());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Table table = (Table) o;
            return Objects.equals(name, table.name) &&
                   Objects.equals(columns, table.columns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, columns);
        }

        @Override
        public String toString() {
            return "Table{name='" + name + "', columns=" + columns.keySet() + "}";
        }
    }

    /**
     * Represents a table column with its properties
     */
    public static class Column {
        private final String name;
        private final DataType dataType;
        private final boolean nullable;
        private final boolean primaryKey;

        public Column(String name, DataType dataType) {
            this(name, dataType, true, false);
        }

        public Column(String name, DataType dataType, boolean nullable, boolean primaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getName() {
            return name;
        }

        public DataType getDataType() {
            return dataType;
        }

        public boolean isNullable() {
            return nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Column column = (Column) o;
            return nullable == column.nullable &&
                   primaryKey == column.primaryKey &&
                   Objects.equals(name, column.name) &&
                   dataType == column.dataType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, dataType, nullable, primaryKey);
        }

        @Override
        public String toString() {
            return "Column{name='" + name + "', dataType=" + dataType + 
                   ", nullable=" + nullable + ", primaryKey=" + primaryKey + "}";
        }
    }

    /**
     * Common SQL data types
     */
    public enum DataType {
        INTEGER, BIGINT, DECIMAL, FLOAT, DOUBLE,
        VARCHAR, CHAR, TEXT,
        DATE, TIME, TIMESTAMP,
        BOOLEAN,
        BLOB, CLOB
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schema schema = (Schema) o;
        return Objects.equals(tables, schema.tables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tables);
    }

    @Override
    public String toString() {
        return "Schema{tables=" + tables.keySet() + "}";
    }
}