package cc.fastsoft.db.core;

import cc.fastsoft.db.schema.DatabaseSchema;
import cc.fastsoft.db.schema.TableSchema;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metadata Manager - Manages table schema metadata with database hierarchy
 *
 * Note: Tables now belong to databases. Use format: database.table
 */
public class MetadataManager {
    private final RocksDB db;
    private final DatabaseManager databaseManager;
    private final Map<String, TableSchema> schemaCache = new ConcurrentHashMap<>();

    public MetadataManager(RocksDB db, DatabaseManager databaseManager) {
        this.db = db;
        this.databaseManager = databaseManager;
    }

    /**
     * Save table schema to current database
     */
    public void saveTableSchema(String tableName, TableSchema schema) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected. Use 'USE database_name' first.");
        }
        saveTableSchema(currentDb, tableName, schema);
    }

    /**
     * Save table schema to specified database
     */
    public void saveTableSchema(String databaseName, String tableName, TableSchema schema) throws RocksDBException {
        String fullName = getFullTableName(databaseName, tableName);

        if (schemaCache.containsKey(fullName)) {
            throw new IllegalStateException("Table already exists: " + fullName);
        }

        // Get database schema and add table
        DatabaseSchema dbSchema = databaseManager.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }

        dbSchema.addTable(schema);
        databaseManager.saveDatabaseSchema(dbSchema);

        schemaCache.put(fullName, schema);
    }

    /**
     * Get table schema by name (from current database)
     */
    public TableSchema getTableSchema(String tableName) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected. Use 'USE database_name' first.");
        }
        return getTableSchema(currentDb, tableName);
    }

    /**
     * Get table schema by database and table name
     */
    public TableSchema getTableSchema(String databaseName, String tableName) throws RocksDBException {
        String fullName = getFullTableName(databaseName, tableName);

        TableSchema schema = schemaCache.get(fullName);
        if (schema != null) {
            return schema;
        }

        // Load from database schema
        DatabaseSchema dbSchema = databaseManager.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }

        schema = dbSchema.getTable(tableName);
        if (schema == null) {
            throw new IllegalStateException("Table does not exist: " + fullName);
        }

        schemaCache.put(fullName, schema);
        return schema;
    }

    /**
     * Check if table exists in current database
     */
    public boolean tableExists(String tableName) {
        try {
            String currentDb = databaseManager.getCurrentDatabase();
            if (currentDb == null) {
                return false;
            }
            return tableExists(currentDb, tableName);
        } catch (RocksDBException e) {
            return false;
        }
    }

    /**
     * Check if table exists in specified database
     */
    public boolean tableExists(String databaseName, String tableName) throws RocksDBException {
        String fullName = getFullTableName(databaseName, tableName);

        if (schemaCache.containsKey(fullName)) {
            return true;
        }

        DatabaseSchema dbSchema = databaseManager.getDatabaseSchema(databaseName);
        return dbSchema != null && dbSchema.hasTable(tableName);
    }

    /**
     * Drop table schema from current database
     */
    public void dropTableSchema(String tableName) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected. Use 'USE database_name' first.");
        }
        dropTableSchema(currentDb, tableName);
    }

    /**
     * Drop table schema from specified database
     */
    public void dropTableSchema(String databaseName, String tableName) throws RocksDBException {
        String fullName = getFullTableName(databaseName, tableName);

        DatabaseSchema dbSchema = databaseManager.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }

        dbSchema.removeTable(tableName);
        databaseManager.saveDatabaseSchema(dbSchema);

        schemaCache.remove(fullName);
    }

    /**
     * Load all tables from all databases on startup
     * This ensures that existing tables are recognized and cached
     */
    public void loadAllTables() throws RocksDBException {
        List<String> databases = databaseManager.listDatabases();
        int totalTables = 0;

        for (String dbName : databases) {
            DatabaseSchema dbSchema = databaseManager.getDatabaseSchema(dbName);
            if (dbSchema != null) {
                List<TableSchema> tables = dbSchema.getTables();
                for (TableSchema table : tables) {
                    String fullName = getFullTableName(dbName, table.tableName);
                    schemaCache.put(fullName, table);
                    totalTables++;
                }
            }
        }

        if (totalTables > 0) {
            System.out.println("Loaded " + totalTables + " table(s) from " + databases.size() + " database(s)");
        }
    }

    /**
     * Get full table name: database.table
     */
    private String getFullTableName(String databaseName, String tableName) {
        return databaseName + "." + tableName;
    }
}

