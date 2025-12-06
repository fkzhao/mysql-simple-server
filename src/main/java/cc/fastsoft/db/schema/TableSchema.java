package cc.fastsoft.db.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TableSchema {
    public final String tableName;
    public final List<Column> columns;
    public final List<String> primaryKeyColumns;

    public TableSchema(String tableName,
                       List<Column> columns,
                       List<String> primaryKeyColumns) {
        this.tableName = tableName;
        this.columns = columns;
        this.primaryKeyColumns = primaryKeyColumns;
    }

    public Column getColumn(String name) {
        for (Column c : columns) {
            if (c.name.equals(name)) return c;
        }
        return null;
    }

    public String serialize() {
        String cols = columns.stream()
                .map(c -> c.name + ":" + c.type.name())
                .collect(Collectors.joining(","));
        String pks = String.join(",", primaryKeyColumns);
        return tableName + "|" + cols + "|" + pks;
    }


    public static TableSchema deserialize(String s) {
        String[] parts = s.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid schema string: " + s);
        }
        String tableName = parts[0];
        String colsStr = parts[1];
        String pksStr = parts[2];

        List<Column> cols = new ArrayList<>();
        if (!colsStr.isEmpty()) {
            for (String colDef : colsStr.split(",")) {
                String[] kv = colDef.split(":");
                cols.add(new Column(kv[0], Column.ColumnType.valueOf(kv[1])));
            }
        }
        List<String> pks = new ArrayList<>();
        if (!pksStr.isEmpty()) {
            pks.addAll(Arrays.asList(pksStr.split(",")));
        }
        return new TableSchema(tableName, cols, pks);
    }


}
