package cc.fastsoft.db;

import cc.fastsoft.db.core.DatabaseManager;
import cc.fastsoft.db.core.KeyEncoder;
import cc.fastsoft.db.core.MetadataManager;
import cc.fastsoft.db.core.RowCodec;
import cc.fastsoft.db.core.StorageManager;
import cc.fastsoft.db.schema.Column;
import cc.fastsoft.db.schema.TableSchema;
import cc.fastsoft.storage.rocksdb.RocksDbHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cc.fastsoft.db.schema.Column.ColumnType;

/**
 * Database Engine - Core coordinator for modular database operations
 *
 * Architecture:
 * - MetadataManager: Manages table schema metadata
 * - StorageManager: Handles data storage and retrieval
 * - KeyEncoder: Encodes primary keys and data keys
 * - RowCodec: Serializes and deserializes row data
 */
public class DatabaseEngine implements Closeable {
    private RocksDbHandle rocksDbHandle;
    private RocksDB db;

    // Modular components
    private DatabaseManager databaseManager;
    private MetadataManager metadataManager;
    private StorageManager storageManager;
    private KeyEncoder keyEncoder;
    private RowCodec rowCodec;

    public DatabaseEngine() {
        try {
            initRocksDB();
            initModules();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DatabaseEngine", e);
        }
    }

    /**
     * Initialize RocksDB instance
     */
    void initRocksDB() throws IOException {
        DBOptions dbOptions = new DBOptions();
        dbOptions.setCreateIfMissing(true);
        dbOptions.setCreateMissingColumnFamilies(true);
        ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();

        // Allow custom RocksDB path for testing
        String dbPath = System.getProperty("rocksdb.path", "rocks.db");
        rocksDbHandle =
                new RocksDbHandle(new File(dbPath), dbOptions, columnFamilyOptions, false);
        rocksDbHandle.openDB();
        db = rocksDbHandle.getDb();
    }

    /**
     * Initialize modular components with database hierarchy support
     */
    private void initModules() throws RocksDBException {
        this.keyEncoder = new KeyEncoder();
        this.rowCodec = new RowCodec();
        this.databaseManager = new DatabaseManager(db);
        this.metadataManager = new MetadataManager(db, databaseManager);
        this.storageManager = new StorageManager(db, keyEncoder, rowCodec);

        // Load existing databases and tables from RocksDB on startup
        loadExistingData();
    }

    /**
     * Load all existing databases and tables from RocksDB
     * This ensures that duplicate creation attempts will be detected
     */
    private void loadExistingData() throws RocksDBException {
        System.out.println("Loading existing data from RocksDB...");
        databaseManager.loadAllDatabases();
        metadataManager.loadAllTables();
        System.out.println("Data loading completed.");
    }

    // ==================== Database Management API ====================

    /**
     * Create a new database
     */
    public void createDatabase(String databaseName) throws RocksDBException {
        databaseManager.createDatabase(databaseName);
    }

    /**
     * Drop a database
     */
    public void dropDatabase(String databaseName) throws RocksDBException {
        databaseManager.dropDatabase(databaseName);
    }

    /**
     * Use/switch to a database
     */
    public void useDatabase(String databaseName) throws RocksDBException {
        databaseManager.useDatabase(databaseName);
    }

    /**
     * Get current database name
     */
    public String getCurrentDatabase() {
        return databaseManager.getCurrentDatabase();
    }

    /**
     * List all databases
     */
    public List<String> listDatabases() throws RocksDBException {
        return databaseManager.listDatabases();
    }

    /**
     * Check if database exists
     */
    public boolean databaseExists(String databaseName) {
        return databaseManager.databaseExists(databaseName);
    }

    // ==================== Table Management API ====================

    /**
     * Create a new table in current database
     */
    public void createTable(String tableName,
                            List<Column> columns,
                            List<String> primaryKeyColumns) throws RocksDBException {
        TableSchema schema = new TableSchema(tableName, columns, primaryKeyColumns);
        metadataManager.saveTableSchema(tableName, schema);
    }

    /**
     * Create a new table in specified database
     */
    public void createTable(String databaseName, String tableName,
                            List<Column> columns,
                            List<String> primaryKeyColumns) throws RocksDBException {
        TableSchema schema = new TableSchema(tableName, columns, primaryKeyColumns);
        metadataManager.saveTableSchema(databaseName, tableName, schema);
    }

    /**
     * Insert a row into table
     */
    public void insert(String tableName, Map<String, Object> row) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        TableSchema schema = metadataManager.getTableSchema(tableName);
        String fullTableName = currentDb + "." + tableName;
        storageManager.insertRow(fullTableName, schema, row);
    }

    /**
     * Select a row by primary key
     */
    public Map<String, Object> selectByPrimaryKey(String tableName,
                                                  Map<String, Object> pkValues) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        TableSchema schema = metadataManager.getTableSchema(tableName);
        String fullTableName = currentDb + "." + tableName;
        return storageManager.selectByPrimaryKey(fullTableName, schema, pkValues);
    }

    /**
     * Select all rows from a table
     */
    public List<Map<String, Object>> selectAll(String tableName) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        TableSchema schema = metadataManager.getTableSchema(tableName);
        String fullTableName = currentDb + "." + tableName;
        return storageManager.selectAll(fullTableName, schema);
    }

    /**
     * Update a row by primary key
     */
    public void update(String tableName,
                      Map<String, Object> pkValues,
                      Map<String, Object> newValues) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        TableSchema schema = metadataManager.getTableSchema(tableName);
        String fullTableName = currentDb + "." + tableName;
        storageManager.updateByPrimaryKey(fullTableName, schema, pkValues, newValues);
    }

    /**
     * Delete a row by primary key
     */
    public void delete(String tableName, Map<String, Object> pkValues) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        TableSchema schema = metadataManager.getTableSchema(tableName);
        String fullTableName = currentDb + "." + tableName;
        storageManager.deleteByPrimaryKey(fullTableName, schema, pkValues);
    }

    /**
     * Drop a table (delete schema and all data)
     */
    public void dropTable(String tableName) throws RocksDBException {
        String currentDb = databaseManager.getCurrentDatabase();
        if (currentDb == null) {
            throw new IllegalStateException("No database selected");
        }
        String fullTableName = currentDb + "." + tableName;
        storageManager.deleteAll(fullTableName);
        metadataManager.dropTableSchema(tableName);
    }

    /**
     * Check if table exists
     */
    public boolean tableExists(String tableName) {
        return metadataManager.tableExists(tableName);
    }

    /**
     * Get table schema
     */
    public TableSchema getTableSchema(String tableName) throws RocksDBException {
        return metadataManager.getTableSchema(tableName);
    }

    // ==================== Getters for Modules ====================

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public KeyEncoder getKeyEncoder() {
        return keyEncoder;
    }

    public RowCodec getRowCodec() {
        return rowCodec;
    }

    @Override
    public void close() {
        if (rocksDbHandle != null) {
            rocksDbHandle.getDb().close();
        }
    }

    public static void main(String[] args) {
        DatabaseEngine engine = new DatabaseEngine();
        try {
            // Example usage
            engine.createDatabase("demo");
            engine.useDatabase("demo");

            // Define table schema
            List<Column> columns = List.of(
                    new Column("id", ColumnType.INT),
                    new Column("name", ColumnType.STRING),
                    new Column("age", ColumnType.INT),
                    new Column("type", ColumnType.STRING),
                    new Column("created_at", ColumnType.LONG)
            );
            List<String> pkColumns = List.of("id");

            // Create table
            engine.createTable("users", columns, pkColumns);

            // Insert a row
            for (int i = 1; i <= 10; i++) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", i);
                row.put("name", "User" + i);
                row.put("age", 20 + i);
                row.put("type", i % 2 == 0 ? "admin" : "guest");
                row.put("created_at", System.currentTimeMillis());
                engine.insert("users", row);
            }

            // Select the row
            Map<String, Object> pkValues = new HashMap<>();
            pkValues.put("id", 1);
            Map<String, Object> selectedRow = engine.selectByPrimaryKey("users", pkValues);
            System.out.println("Selected Row: " + selectedRow);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            engine.close();
        }
    }
}
