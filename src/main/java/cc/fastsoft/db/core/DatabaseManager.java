package cc.fastsoft.db.core;

import cc.fastsoft.db.schema.DatabaseSchema;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database Manager - Manages database-level operations
 *
 * Hierarchy:
 * - Database (contains multiple tables)
 *   - Table (contains multiple rows)
 *     - Row (contains columns)
 */
public class DatabaseManager {
    private final RocksDB db;
    private final Map<String, DatabaseSchema> databaseCache = new ConcurrentHashMap<>();
    private String currentDatabase = null;

    public DatabaseManager(RocksDB db) {
        this.db = db;
    }

    /**
     * Load all databases from RocksDB on startup
     * This ensures that existing databases are recognized and cached
     */
    public void loadAllDatabases() throws RocksDBException {
        List<String> databases = listDatabases();
        for (String dbName : databases) {
            // Load each database into cache
            getDatabaseSchema(dbName);
        }
        if (!databases.isEmpty()) {
            System.out.println("Loaded " + databases.size() + " database(s) from RocksDB: " + databases);
        }
    }

    /**
     * Create a new database
     */
    public void createDatabase(String databaseName) throws RocksDBException {
        if (databaseCache.containsKey(databaseName)) {
            throw new IllegalStateException("Database already exists: " + databaseName);
        }

        DatabaseSchema schema = new DatabaseSchema(databaseName);
        String serialized = schema.serialize();
        db.put(metaDatabaseKey(databaseName), serialized.getBytes());
        databaseCache.put(databaseName, schema);
    }

    /**
     * Drop a database and all its tables
     */
    public void dropDatabase(String databaseName) throws RocksDBException {
        DatabaseSchema schema = getDatabaseSchema(databaseName);
        if (schema == null) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }

        // Delete database metadata
        db.delete(metaDatabaseKey(databaseName));
        databaseCache.remove(databaseName);

        // If current database is being dropped, set to null
        if (databaseName.equals(currentDatabase)) {
            currentDatabase = null;
        }
    }

    /**
     * Get database schema
     */
    public DatabaseSchema getDatabaseSchema(String databaseName) throws RocksDBException {
        DatabaseSchema schema = databaseCache.get(databaseName);
        if (schema != null) {
            return schema;
        }

        // Try to load from RocksDB
        byte[] value = db.get(metaDatabaseKey(databaseName));
        if (value == null) {
            return null;
        }

        schema = DatabaseSchema.deserialize(new String(value));
        databaseCache.put(databaseName, schema);
        return schema;
    }

    /**
     * Check if database exists
     */
    public boolean databaseExists(String databaseName) {
        if (databaseCache.containsKey(databaseName)) {
            return true;
        }

        try {
            byte[] value = db.get(metaDatabaseKey(databaseName));
            return value != null;
        } catch (RocksDBException e) {
            return false;
        }
    }

    /**
     * List all databases
     */
    public List<String> listDatabases() throws RocksDBException {
        List<String> databases = new ArrayList<>();
        byte[] prefix = "meta:db:".getBytes();

        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefix); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();

                // Check if key still matches prefix
                if (!startsWith(key, prefix)) {
                    break;
                }

                // Extract database name from key: meta:db:<dbName>
                String keyStr = new String(key);
                String dbName = keyStr.substring("meta:db:".length());
                databases.add(dbName);
            }
        }

        return databases;
    }

    /**
     * Use/switch to a database
     */
    public void useDatabase(String databaseName) throws RocksDBException {
        if (!databaseExists(databaseName)) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }
        this.currentDatabase = databaseName;
    }

    /**
     * Get current database name
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * Get current database schema
     */
    public DatabaseSchema getCurrentDatabaseSchema() throws RocksDBException {
        if (currentDatabase == null) {
            throw new IllegalStateException("No database selected. Use 'USE database_name' first.");
        }
        return getDatabaseSchema(currentDatabase);
    }

    /**
     * Save database schema (after modifications like adding/dropping tables)
     */
    public void saveDatabaseSchema(DatabaseSchema schema) throws RocksDBException {
        String serialized = schema.serialize();
        db.put(metaDatabaseKey(schema.databaseName), serialized.getBytes());
        databaseCache.put(schema.databaseName, schema);
    }

    /**
     * Get database metadata key
     */
    private byte[] metaDatabaseKey(String databaseName) {
        return ("meta:db:" + databaseName).getBytes();
    }

    /**
     * Check if byte array starts with prefix
     */
    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

