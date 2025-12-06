package cc.fastsoft.db.core;

import cc.fastsoft.db.schema.TableSchema;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Storage Manager - Handles data storage and retrieval operations
 */
public class StorageManager {
    private final RocksDB db;
    private final KeyEncoder keyEncoder;
    private final RowCodec rowCodec;

    public StorageManager(RocksDB db, KeyEncoder keyEncoder, RowCodec rowCodec) {
        this.db = db;
        this.keyEncoder = keyEncoder;
        this.rowCodec = rowCodec;
    }

    /**
     * Insert a row into the table
     */
    public void insertRow(String tableName, TableSchema schema, Map<String, Object> row)
            throws RocksDBException {
        // Encode primary key
        String primaryKey = keyEncoder.encodePrimaryKey(schema, row);

        // Encode row data
        String rowData = rowCodec.encodeRow(schema, row);

        // Store in RocksDB
        byte[] key = keyEncoder.encodeDataKey(tableName, primaryKey);
        db.put(key, rowData.getBytes());
    }

    /**
     * Select a row by primary key
     */
    public Map<String, Object> selectByPrimaryKey(String tableName, TableSchema schema,
                                                   Map<String, Object> primaryKeyValues)
            throws RocksDBException {
        String primaryKey = keyEncoder.encodePrimaryKey(schema, primaryKeyValues);
        byte[] key = keyEncoder.encodeDataKey(tableName, primaryKey);
        byte[] value = db.get(key);

        if (value == null) {
            return null;
        }

        return rowCodec.decodeRow(schema, new String(value));
    }

    /**
     * Select all rows from a table
     */
    public List<Map<String, Object>> selectAll(String tableName, TableSchema schema)
            throws RocksDBException {
        List<Map<String, Object>> results = new ArrayList<>();
        String prefix = keyEncoder.getDataKeyPrefix(tableName);
        byte[] prefixBytes = prefix.getBytes();

        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefixBytes); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();

                // Check if key still matches prefix
                if (!keyEncoder.keyStartsWith(key, prefixBytes)) {
                    break;
                }

                byte[] value = iterator.value();
                Map<String, Object> row = rowCodec.decodeRow(schema, new String(value));
                results.add(row);
            }
        }

        return results;
    }

    /**
     * Delete a row by primary key
     */
    public void deleteByPrimaryKey(String tableName, TableSchema schema,
                                   Map<String, Object> primaryKeyValues)
            throws RocksDBException {
        String primaryKey = keyEncoder.encodePrimaryKey(schema, primaryKeyValues);
        byte[] key = keyEncoder.encodeDataKey(tableName, primaryKey);
        db.delete(key);
    }

    /**
     * Update a row by primary key
     */
    public void updateByPrimaryKey(String tableName, TableSchema schema,
                                   Map<String, Object> primaryKeyValues,
                                   Map<String, Object> newValues)
            throws RocksDBException {
        // First check if row exists
        Map<String, Object> existingRow = selectByPrimaryKey(tableName, schema, primaryKeyValues);
        if (existingRow == null) {
            throw new IllegalStateException("Row not found for update");
        }

        // Merge new values into existing row
        existingRow.putAll(newValues);

        // Update in storage
        insertRow(tableName, schema, existingRow);
    }

    /**
     * Delete all rows from a table
     */
    public void deleteAll(String tableName) throws RocksDBException {
        String prefix = keyEncoder.getDataKeyPrefix(tableName);
        byte[] prefixBytes = prefix.getBytes();
        List<byte[]> keysToDelete = new ArrayList<>();

        try (RocksIterator iterator = db.newIterator()) {
            for (iterator.seek(prefixBytes); iterator.isValid(); iterator.next()) {
                byte[] key = iterator.key();
                if (!keyEncoder.keyStartsWith(key, prefixBytes)) {
                    break;
                }
                keysToDelete.add(key);
            }
        }

        // Delete all collected keys
        for (byte[] key : keysToDelete) {
            db.delete(key);
        }
    }
}

