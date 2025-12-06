package cc.fastsoft.db.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Database Schema - Represents a database containing multiple tables
 */
public class DatabaseSchema {
    public final String databaseName;
    private final Map<String, TableSchema> tables;

    public DatabaseSchema(String databaseName) {
        this.databaseName = databaseName;
        this.tables = new HashMap<>();
    }

    /**
     * Add a table to this database
     */
    public void addTable(TableSchema table) {
        if (tables.containsKey(table.tableName)) {
            throw new IllegalStateException("Table already exists: " + table.tableName);
        }
        tables.put(table.tableName, table);
    }

    /**
     * Get a table by name
     */
    public TableSchema getTable(String tableName) {
        return tables.get(tableName);
    }

    /**
     * Check if table exists
     */
    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName);
    }

    /**
     * Remove a table
     */
    public void removeTable(String tableName) {
        tables.remove(tableName);
    }

    /**
     * Get all table names
     */
    public List<String> getTableNames() {
        return new ArrayList<>(tables.keySet());
    }

    /**
     * Get all tables
     */
    public List<TableSchema> getTables() {
        return new ArrayList<>(tables.values());
    }

    /**
     * Get table count
     */
    public int getTableCount() {
        return tables.size();
    }

    /**
     * Serialize database schema to string
     * Format: databaseName|table1_serialized||table2_serialized||...
     */
    public String serialize() {
        String tablesStr = tables.values().stream()
                .map(TableSchema::serialize)
                .collect(Collectors.joining("||"));
        return databaseName + "|" + tablesStr;
    }

    /**
     * Deserialize from string
     */
    public static DatabaseSchema deserialize(String s) {
        int firstPipe = s.indexOf('|');
        if (firstPipe == -1) {
            throw new IllegalArgumentException("Invalid database schema string: " + s);
        }

        String dbName = s.substring(0, firstPipe);
        DatabaseSchema schema = new DatabaseSchema(dbName);

        String tablesStr = s.substring(firstPipe + 1);
        if (!tablesStr.isEmpty()) {
            String[] tableParts = tablesStr.split("\\|\\|");
            for (String tablePart : tableParts) {
                if (!tablePart.isEmpty()) {
                    TableSchema table = TableSchema.deserialize(tablePart);
                    schema.addTable(table);
                }
            }
        }

        return schema;
    }

    @Override
    public String toString() {
        return "DatabaseSchema{" +
                "name='" + databaseName + '\'' +
                ", tables=" + tables.size() +
                '}';
    }
}

