package cc.fastsoft.db.core;

import cc.fastsoft.db.schema.Column;
import cc.fastsoft.db.schema.TableSchema;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Row Codec - Handles serialization and deserialization of row data
 */
public class RowCodec {

    /**
     * Encode row as simple key-value format: "col1=val1;col2=val2;..."
     */
    public String encodeRow(TableSchema schema, Map<String, Object> row) {
        return schema.columns.stream()
                .map(column -> encodeColumn(column, row.get(column.name)))
                .collect(Collectors.joining(";"));
    }

    /**
     * Decode row from string format
     */
    public Map<String, Object> decodeRow(TableSchema schema, String data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return result;
        }

        String[] pairs = data.split(";");
        Map<String, String> keyValueMap = new HashMap<>();

        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                keyValueMap.put(parts[0], parts[1]);
            }
        }

        for (Column column : schema.columns) {
            String rawValue = keyValueMap.get(column.name);
            Object decodedValue = decodeColumnValue(column, rawValue);
            result.put(column.name, decodedValue);
        }

        return result;
    }

    /**
     * Encode a single column value
     */
    private String encodeColumn(Column column, Object value) {
        String valueStr = (value == null) ? "NULL" : value.toString();
        return column.name + "=" + valueStr;
    }

    /**
     * Decode a single column value based on its type
     */
    private Object decodeColumnValue(Column column, String rawValue) {
        if (rawValue == null || rawValue.equals("NULL")) {
            return null;
        }

        switch (column.type) {
            case LONG:
                return Long.parseLong(rawValue);
            case INT:
                return Integer.parseInt(rawValue);
            case DOUBLE:
                return Double.parseDouble(rawValue);
            case BOOLEAN:
                return Boolean.parseBoolean(rawValue);
            case STRING:
            case TEXT:
            default:
                return rawValue;
        }
    }
}

