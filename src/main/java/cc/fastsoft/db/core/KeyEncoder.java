package cc.fastsoft.db.core;

import cc.fastsoft.db.schema.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Key Encoder - Handles encoding of primary keys and data keys
 */
public class KeyEncoder {

    /**
     * Encode primary key from row data
     */
    public String encodePrimaryKey(TableSchema schema, Map<String, Object> row) {
        List<String> parts = new ArrayList<>();
        for (String pkCol : schema.primaryKeyColumns) {
            Object value = row.get(pkCol);
            if (value == null) {
                throw new IllegalArgumentException("Primary key column " + pkCol + " is null");
            }
            parts.add(value.toString());
        }
        return String.join("|", parts);
    }

    /**
     * Get data key prefix for a table
     */
    public String getDataKeyPrefix(String tableName) {
        return "data:" + tableName + ":";
    }

    /**
     * Encode full data key
     */
    public byte[] encodeDataKey(String tableName, String primaryKeyEncoded) {
        return (getDataKeyPrefix(tableName) + primaryKeyEncoded).getBytes();
    }

    /**
     * Check if a key starts with given prefix
     */
    public boolean keyStartsWith(byte[] key, byte[] prefix) {
        if (key.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (key[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

