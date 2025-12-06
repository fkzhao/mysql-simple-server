package cc.fastsoft.db.schema;

public class Column {
    public final String name;
    public final ColumnType type;

    public Column(String name, ColumnType type) {
        this.name = name;
        this.type = type;
    }
    public static enum ColumnType {
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        VARCHAR,
        STRING,
        TEXT,
        DATE,
        DATETIME,
        BOOLEAN,
    }
}
