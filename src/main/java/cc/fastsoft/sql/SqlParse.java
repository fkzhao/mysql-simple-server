package cc.fastsoft.sql;


import cc.fastsoft.db.DatabaseEngine;
import cc.fastsoft.db.schema.TableSchema;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SqlParse {
    private static Logger logger = LoggerFactory.getLogger(SqlParse.class);


    public static SqlData parseSql(String sql, DatabaseEngine databaseEngine) throws Exception {
        // Parse SQL statement
        Statement stmt = CCJSqlParserUtil.parse(sql);
        logger.info("Parsed SQL Statement: {}", stmt.getClass().getSimpleName());

        if (stmt instanceof Select) {
            return handleSelectStatement((Select) stmt, databaseEngine);
        } else if (stmt instanceof Insert) {
            return handleInsertStatement((Insert) stmt, databaseEngine);
        } else if (stmt instanceof Update) {
            return handleUpdateStatement((Update) stmt, databaseEngine);
        } else if (stmt instanceof Delete) {
            return handleDeleteStatement((Delete) stmt, databaseEngine);
        } else {
            logger.warn("Unsupported SQL statement type: {}", stmt.getClass().getSimpleName());
            throw new Exception("Unsupported SQL statement type: " + stmt.getClass().getSimpleName());
        }
    }

    private static SqlData handleSelectStatement(Select selectStmt, DatabaseEngine databaseEngine) throws Exception {
        logger.info("Handling SELECT statement");

        PlainSelect plainSelect = selectStmt.getPlainSelect();
        if (plainSelect == null) {
            throw new Exception("Only simple SELECT statements are supported");
        }

        // Get table name
        String tableName = plainSelect.getFromItem().toString();
        logger.info("Table name: {}", tableName);

        // Get table schema
        TableSchema schema = databaseEngine.getTableSchema(tableName);
        if (schema == null) {
            throw new Exception("Table not found: " + tableName);
        }

        // Get column names (handle SELECT * and specific columns with aliases)
        List<String> columnNames = new ArrayList<>();
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();

        boolean isSelectAll = false;
        for (SelectItem<?> item : selectItems) {
            // Check if it's SELECT *
            String itemStr = item.toString();
            if ("*".equals(itemStr) || item.toString().contains("*")) {
                // SELECT *
                isSelectAll = true;
                columnNames = schema.getColumns().stream()
                        .map(c -> c.name)
                        .collect(Collectors.toList());
                break;
            } else {
                // SELECT column or SELECT column AS alias
                String fullExpression = item.toString();

                // Check if there's an alias (pattern: "column AS alias" or "column alias")
                if (fullExpression.toLowerCase().contains(" as ")) {
                    // Extract alias after AS
                    String[] parts = fullExpression.split("(?i)\\s+as\\s+");
                    columnNames.add(parts[1].trim());
                } else {
                    // No alias, use column name
                    Expression expr = item.getExpression();
                    if (expr instanceof Column) {
                        columnNames.add(((Column) expr).getColumnName());
                    } else {
                        columnNames.add(expr.toString());
                    }
                }
            }
        }

        logger.info("Column names: {}", columnNames);

        // Fetch all rows
        List<Map<String, Object>> allRows = databaseEngine.selectAll(tableName);
        logger.info("Fetched {} rows", allRows.size());

        // Handle WHERE clause if present
        Expression where = plainSelect.getWhere();
        if (where != null) {
            allRows = filterRows(allRows, where);
            logger.info("After WHERE filter: {} rows", allRows.size());
        }

        // Handle LIMIT clause if present
        Limit limit = plainSelect.getLimit();
        if (limit != null && limit.getRowCount() instanceof net.sf.jsqlparser.expression.LongValue) {
            long limitCount = ((net.sf.jsqlparser.expression.LongValue) limit.getRowCount()).getValue();
            if (allRows.size() > limitCount) {
                allRows = allRows.subList(0, (int) limitCount);
            }
            logger.info("After LIMIT: {} rows", allRows.size());
        }

        // Project columns (handle column selection and aliases)
        List<Map<String, Object>> projectedRows = new ArrayList<>();
        if (!isSelectAll) {
            for (Map<String, Object> row : allRows) {
                Map<String, Object> projectedRow = new HashMap<>();
                int index = 0;
                for (SelectItem<?> item : selectItems) {
                    Expression expr = item.getExpression();
                    String outputName = columnNames.get(index);
                    String sourceColumn = null;

                    if (expr instanceof Column) {
                        sourceColumn = ((Column) expr).getColumnName();
                    }

                    if (sourceColumn != null && row.containsKey(sourceColumn)) {
                        projectedRow.put(outputName, row.get(sourceColumn));
                    }
                    index++;
                }
                projectedRows.add(projectedRow);
            }
        } else {
            projectedRows = allRows;
        }

        SqlData result = new SqlData();
        result.setColumns(columnNames);
        result.setRows(projectedRows);

        return result;
    }

    private static SqlData handleInsertStatement(Insert insertStmt, DatabaseEngine databaseEngine) throws Exception {
        logger.info("Handling INSERT statement");

        String tableName = insertStmt.getTable().getName();
        logger.info("Table name: {}", tableName);

        // Get table schema
        TableSchema schema = databaseEngine.getTableSchema(tableName);
        if (schema == null) {
            throw new Exception("Table not found: " + tableName);
        }

        // Get column names from INSERT statement
        List<Column> columns = insertStmt.getColumns();
        List<String> columnNames;
        if (columns != null && !columns.isEmpty()) {
            columnNames = columns.stream().map(Column::getColumnName).collect(Collectors.toList());
        } else {
            columnNames = schema.getColumns().stream().map(c -> c.name).collect(Collectors.toList());
        }

        // Parse the INSERT statement string to extract values
        // This is a simplified approach for jsqlparser 5.3
        String insertStr = insertStmt.toString();
        logger.info("INSERT string: {}", insertStr);

        // Check if this is INSERT ... SELECT
        if (insertStr.toUpperCase().contains("INSERT") &&
            insertStr.toUpperCase().contains("SELECT") &&
            !insertStr.toUpperCase().contains("VALUES")) {
            throw new Exception("INSERT ... SELECT is not supported yet");
        }

        // Extract values between VALUES and the closing parenthesis
        int valuesIdx = insertStr.toUpperCase().indexOf("VALUES");
        if (valuesIdx < 0) {
            throw new Exception("No VALUES clause found in INSERT statement");
        }

        String valuesStr = insertStr.substring(valuesIdx + 6).trim();
        // Remove leading ( and trailing )
        if (valuesStr.startsWith("(")) {
            valuesStr = valuesStr.substring(1);
        }
        if (valuesStr.endsWith(")")) {
            valuesStr = valuesStr.substring(0, valuesStr.length() - 1);
        }

        // Split by comma (simple parsing - doesn't handle nested commas in strings properly)
        String[] valueParts = valuesStr.split(",");

        if (columnNames.size() != valueParts.length) {
            throw new Exception("Column count doesn't match value count: " + columnNames.size() + " vs " + valueParts.length);
        }

        // Build row map
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            String valueStr = valueParts[i].trim();
            Object value = parseValueString(valueStr);
            row.put(colName, value);
        }

        logger.info("Inserting row: {}", row);

        // Insert into database
        databaseEngine.insert(tableName, row);

        // Return empty result (INSERT doesn't return data)
        SqlData result = new SqlData();
        result.setColumns(Collections.emptyList());
        result.setRows(Collections.emptyList());

        return result;
    }

    private static SqlData handleUpdateStatement(Update updateStmt, DatabaseEngine databaseEngine) throws Exception {
        logger.info("Handling UPDATE statement");

        String tableName = updateStmt.getTable().getName();
        logger.info("Table name: {}", tableName);

        // Get table schema
        TableSchema schema = databaseEngine.getTableSchema(tableName);
        if (schema == null) {
            throw new Exception("Table not found: " + tableName);
        }

        // Get all rows to update
        List<Map<String, Object>> allRows = databaseEngine.selectAll(tableName);

        // Filter rows by WHERE clause
        Expression where = updateStmt.getWhere();
        List<Map<String, Object>> rowsToUpdate = where != null
                ? filterRows(allRows, where)
                : allRows;

        logger.info("Found {} rows to update", rowsToUpdate.size());

        // Get update values
        List<UpdateSet> updateSets = updateStmt.getUpdateSets();
        Map<String, Object> newValues = new HashMap<>();

        for (UpdateSet updateSet : updateSets) {
            List<Column> columns = updateSet.getColumns();
            // Get expressions from the update set
            // In jsqlparser 5.x, we need to parse from toString or use getList
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).getColumnName();
                // Parse from string representation as a workaround
                String updateStr = updateSet.toString();
                String[] parts = updateStr.split("=");
                if (parts.length == 2) {
                    String valueStr = parts[1].trim();
                    Object value = parseValueString(valueStr);
                    newValues.put(colName, value);
                }
            }
        }

        logger.info("Update values: {}", newValues);

        // Update each matching row
        for (Map<String, Object> row : rowsToUpdate) {
            // Extract primary key values
            Map<String, Object> pkValues = new HashMap<>();
            for (String pkCol : schema.getPrimaryKeyColumns()) {
                pkValues.put(pkCol, row.get(pkCol));
            }

            // Update the row
            databaseEngine.update(tableName, pkValues, newValues);
        }

        // Return empty result (UPDATE doesn't return data)
        SqlData result = new SqlData();
        result.setColumns(Collections.emptyList());
        result.setRows(Collections.emptyList());

        return result;
    }

    private static SqlData handleDeleteStatement(Delete deleteStmt, DatabaseEngine databaseEngine) throws Exception {
        logger.info("Handling DELETE statement");

        String tableName = deleteStmt.getTable().getName();
        logger.info("Table name: {}", tableName);

        // Get table schema
        TableSchema schema = databaseEngine.getTableSchema(tableName);
        if (schema == null) {
            throw new Exception("Table not found: " + tableName);
        }

        // Get all rows
        List<Map<String, Object>> allRows = databaseEngine.selectAll(tableName);

        // Filter rows by WHERE clause
        Expression where = deleteStmt.getWhere();
        List<Map<String, Object>> rowsToDelete = where != null
                ? filterRows(allRows, where)
                : allRows;

        logger.info("Found {} rows to delete", rowsToDelete.size());

        // Delete each matching row
        for (Map<String, Object> row : rowsToDelete) {
            // Extract primary key values
            Map<String, Object> pkValues = new HashMap<>();
            for (String pkCol : schema.getPrimaryKeyColumns()) {
                pkValues.put(pkCol, row.get(pkCol));
            }

            // Delete the row
            databaseEngine.delete(tableName, pkValues);
        }

        // Return empty result (DELETE doesn't return data)
        SqlData result = new SqlData();
        result.setColumns(Collections.emptyList());
        result.setRows(Collections.emptyList());

        return result;
    }

    /**
     * Filter rows based on WHERE clause expression
     */
    private static List<Map<String, Object>> filterRows(List<Map<String, Object>> rows, Expression where) {
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (evaluateCondition(row, where)) {
                filtered.add(row);
            }
        }

        return filtered;
    }

    /**
     * Evaluate WHERE condition for a row
     */
    private static boolean evaluateCondition(Map<String, Object> row, Expression expr) {
        if (expr instanceof EqualsTo) {
            EqualsTo equals = (EqualsTo) expr;
            Expression left = equals.getLeftExpression();
            Expression right = equals.getRightExpression();

            if (left instanceof Column) {
                String colName = ((Column) left).getColumnName();
                Object rowValue = row.get(colName);
                Object compareValue = extractValue(right);

                return Objects.equals(rowValue, compareValue);
            }
        }

        // For unsupported expressions, return true (no filtering)
        logger.warn("Unsupported WHERE expression type: {}", expr.getClass().getSimpleName());
        return true;
    }

    /**
     * Extract value from SQL expression
     */
    private static Object extractValue(Expression expr) {
        String str = expr.toString();

        // Remove quotes for string values
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }

        // Try to parse as number
        try {
            if (str.contains(".")) {
                return Double.parseDouble(str);
            } else {
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            return str;
        }
    }

    /**
     * Parse string value from SQL INSERT/UPDATE statement
     */
    private static Object parseValueString(String valueStr) {
        valueStr = valueStr.trim();

        // Remove quotes for string values
        if (valueStr.startsWith("'") && valueStr.endsWith("'")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // Try to parse as number
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            return valueStr;
        }
    }

}
