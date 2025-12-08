package cc.fastsoft.sql;

import java.util.List;
import java.util.Map;

public class SqlData {
    private List<String> columns;
    private List<Map<String, Object>> rows;

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }
}
