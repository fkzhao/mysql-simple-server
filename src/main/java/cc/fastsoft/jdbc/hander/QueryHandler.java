package cc.fastsoft.jdbc.hander;

import cc.fastsoft.db.DatabaseEngine;
import cc.fastsoft.db.schema.TableSchema;
import cc.fastsoft.jdbc.protocol.PacketHelper;
import cc.fastsoft.sql.SqlData;
import cc.fastsoft.sql.SqlParse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Handles SQL query execution
 */
public class QueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(QueryHandler.class);
    private static final DatabaseEngine databaseEngine = new DatabaseEngine();

    /**
     * Handle SQL query
     */
    public void handleQuery(ChannelHandlerContext ctx, String sql, byte sequenceId, int clientCapabilities) {
        String sqlUpper = sql.trim().toUpperCase();

        try {
            if (sqlUpper.startsWith("SELECT 1")) {
                sendResultSet(ctx, new String[]{"value"}, new String[][]{{"1"}}, sequenceId, clientCapabilities);
            } else if (sqlUpper.equals("SHOW DATABASES") || sqlUpper.equals("SHOW SCHEMAS")) {
                handleShowDatabases(ctx, sequenceId, clientCapabilities);
            } else if (sqlUpper.startsWith("SELECT @@") || sqlUpper.startsWith("SELECT DATABASE()")) {
                handleSystemVariableQuery(ctx, sql, sequenceId, clientCapabilities);
            } else if (sqlUpper.startsWith("SHOW ENGINES") || sqlUpper.startsWith("SHOW CHARSET") ||
                    sqlUpper.startsWith("SHOW COLLATION") || sqlUpper.startsWith("SHOW PLUGINS")) {
                sendEmptyResultSet(ctx, sql, sequenceId, clientCapabilities);
            } else if (sqlUpper.startsWith("SHOW VARIABLES")) {
                handleShowVariables(ctx, sql, sequenceId, clientCapabilities);
            } else if (sqlUpper.startsWith("SET ")) {
                PacketHelper.sendOkPacket(ctx, "OK", sequenceId);
            } else if (sqlUpper.startsWith("SELECT * FROM USERS - MOCK_DB")) {
                handleMockDbQuery(ctx, sequenceId, clientCapabilities, sql);
            } else {
                SqlData sqlData = SqlParse.parseSql(sql, databaseEngine);
                String[] columnNames = sqlData.getColumns().toArray(new String[0]);
                List<Map<String, Object>> selectedRow = sqlData.getRows();
                String[][] dataList = new String[selectedRow.size()][columnNames.length];
                for (int i = 0; i < selectedRow.size(); i++) {
                    Map<String, Object> row = selectedRow.get(i);
                    for (int j = 0; j < columnNames.length; j++) {
                        Object value = row.get(columnNames[j]);
                        dataList[i][j] = value == null ? "" : value.toString();
                    }
                }
                sendResultSet(ctx, columnNames, dataList, sequenceId, clientCapabilities);
            }
        } catch (Exception e) {
            logger.error("Error handling query: {}", sql, e);
            PacketHelper.sendErrPacket(ctx, "Error: " + e.getMessage(), sequenceId);
        }
    }

    /**
     * Handle SHOW DATABASES command
     */
    private void handleShowDatabases(ChannelHandlerContext ctx, byte sequenceId, int clientCapabilities) {
        String[] databases = {"information_schema", "mysql", "performance_schema", "sys", "test_db", "my_database"};
        String[][] rows = new String[databases.length][1];
        for (int i = 0; i < databases.length; i++) {
            rows[i][0] = databases[i];
        }
        sendResultSet(ctx, new String[]{"Database"}, rows, sequenceId, clientCapabilities);
    }

    /**
     * Handle system variable queries
     */
    private void handleSystemVariableQuery(ChannelHandlerContext ctx, String sql, byte sequenceId, int clientCapabilities) {
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.contains("DATABASE()")) {
            sendResultSet(ctx, new String[]{"DATABASE()"}, new String[][]{{"test_db"}}, sequenceId, clientCapabilities);
        } else if (sqlUpper.contains("@@VERSION_COMMENT")) {
            sendResultSet(ctx, new String[]{"@@version_comment"}, new String[][]{{"MySQL Mock Server"}}, sequenceId, clientCapabilities);
        } else {
            // Return multi-column system variable query result
            String[] columns = extractColumnNames(sql);
            String[][] data = new String[1][columns.length];
            for (int i = 0; i < columns.length; i++) {
                data[0][i] = "mock_value";
            }
            sendResultSet(ctx, columns, data, sequenceId, clientCapabilities);
        }
    }

    /**
     * Extract column names from SQL
     */
    private String[] extractColumnNames(String sql) {
        String[] parts = sql.toUpperCase().split("SELECT")[1].split("FROM")[0].split(",");
        String[] columns = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String col = parts[i].trim();
            if (col.contains(" AS ")) {
                col = col.split(" AS ")[1].trim();
            } else if (col.startsWith("@@")) {
                col = col.substring(2);
            }
            columns[i] = col;
        }
        return columns;
    }

    /**
     * Send simple result set
     */
    private void sendResultSet(ChannelHandlerContext ctx, String[] columnNames, String[][] rows, byte sequenceId, int clientCapabilities) {
        boolean deprecateEof = (clientCapabilities & 0x01000000) != 0; // CLIENT_DEPRECATE_EOF

        // Column Count
        ByteBuf columnCount = Unpooled.buffer().writeByte(columnNames.length);
        PacketHelper.sendPacket(ctx, columnCount, sequenceId++);

        // Column Definitions
        for (String colName : columnNames) {
            ByteBuf colDef = Unpooled.buffer();
            PacketHelper.writeLengthEncodedString(colDef, "def"); // catalog
            PacketHelper.writeLengthEncodedString(colDef, ""); // schema
            PacketHelper.writeLengthEncodedString(colDef, ""); // table
            PacketHelper.writeLengthEncodedString(colDef, ""); // org_table
            PacketHelper.writeLengthEncodedString(colDef, colName); // name
            PacketHelper.writeLengthEncodedString(colDef, colName); // org_name
            colDef.writeByte(0x0C); // next_length
            colDef.writeShortLE(33); // character_set
            colDef.writeIntLE(256); // column_length
            colDef.writeByte(0xFD); // column_type (VAR_STRING)
            colDef.writeShortLE(0); // flags
            colDef.writeByte(0); // decimals
            colDef.writeBytes(new byte[2]); // filler
            PacketHelper.sendPacket(ctx, colDef, sequenceId++);
            logger.debug("Sent column definition for '{}', seq={}", colName, sequenceId - 1);
        }

        // EOF after column definitions (only if CLIENT_DEPRECATE_EOF is NOT set)
        if (!deprecateEof) {
            PacketHelper.sendEofPacket(ctx, sequenceId++);
        }

        // Row Data
        for (String[] row : rows) {
            ByteBuf rowBuf = Unpooled.buffer();
            for (String value : row) {
                PacketHelper.writeLengthEncodedString(rowBuf, value == null ? "" : value);
            }
            PacketHelper.sendPacket(ctx, rowBuf, sequenceId++);
        }

        // Final packet: OK if CLIENT_DEPRECATE_EOF, otherwise EOF
        if (deprecateEof) {
            PacketHelper.sendResultSetOkPacket(ctx, sequenceId);
        } else {
            PacketHelper.sendEofPacket(ctx, sequenceId);
        }
    }

    /**
     * Handle SHOW VARIABLES command
     */
    private void handleShowVariables(ChannelHandlerContext ctx, String sql, byte sequenceId, int clientCapabilities) {
        // Check if it's filtered with LIKE clause
        String likePattern = null;
        if (sql.toUpperCase().contains(" LIKE ")) {
            String[] parts = sql.split("(?i)LIKE");
            if (parts.length > 1) {
                likePattern = parts[1].trim().replaceAll("'", "").replaceAll("%", ".*").toLowerCase();
            }
        }

        // Common MySQL variables
        String[][] allVariables = {
                {"autocommit", "ON"},
                {"auto_increment_increment", "1"},
                {"character_set_client", "utf8mb4"},
                {"character_set_connection", "utf8mb4"},
                {"character_set_database", "utf8mb4"},
                {"character_set_results", "utf8mb4"},
                {"character_set_server", "utf8mb4"},
                {"collation_connection", "utf8mb4_general_ci"},
                {"collation_database", "utf8mb4_general_ci"},
                {"collation_server", "utf8mb4_general_ci"},
                {"init_connect", ""},
                {"interactive_timeout", "28800"},
                {"license", "GPL"},
                {"lower_case_table_names", "0"},
                {"max_allowed_packet", "67108864"},
                {"max_connections", "151"},
                {"net_write_timeout", "60"},
                {"performance_schema", "ON"},
                {"port", "2883"},
                {"protocol_version", "10"},
                {"query_cache_size", "0"},
                {"query_cache_type", "OFF"},
                {"server_id", "1"},
                {"sql_mode", "STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION"},
                {"system_time_zone", "UTC"},
                {"time_zone", "SYSTEM"},
                {"transaction_isolation", "REPEATABLE-READ"},
                {"version", "5.7+"},
                {"version_comment", "MySQL Mock Server"},
                {"wait_timeout", "28800"}
        };

        // Filter variables if LIKE pattern exists
        if (likePattern != null) {
            java.util.List<String[]> filtered = new java.util.ArrayList<>();
            final String pattern = likePattern;
            for (String[] var : allVariables) {
                if (var[0].toLowerCase().matches(pattern)) {
                    filtered.add(var);
                }
            }
            String[][] filteredArray = filtered.toArray(new String[0][0]);
            sendResultSet(ctx, new String[]{"Variable_name", "Value"}, filteredArray, sequenceId, clientCapabilities);
        } else {
            sendResultSet(ctx, new String[]{"Variable_name", "Value"}, allVariables, sequenceId, clientCapabilities);
        }
    }

    private void handleMockDbQuery(ChannelHandlerContext ctx, byte sequenceId, int clientCapabilities, String query) throws RocksDBException {
        databaseEngine.useDatabase("demo");
        TableSchema tableSchema = databaseEngine.getTableSchema("users");
        String[] columnNames = tableSchema.getColumns().stream().map(c -> c.name).toArray(String[]::new);
        List<Map<String, Object>> selectedRow = databaseEngine.selectAll("users");
        String[][] dataList = new String[selectedRow.size()][columnNames.length];
        for (int i = 0; i < selectedRow.size(); i++) {
            Map<String, Object> row = selectedRow.get(i);
            logger.info("Row: {}", row);
            for (int j = 0; j < columnNames.length; j++) {
                Object value = row.get(columnNames[j]);
                dataList[i][j] = value == null ? "" : value.toString();
            }
        }
        sendResultSet(ctx, columnNames, dataList, sequenceId, clientCapabilities);
    }

    /**
     * Send empty result set
     */
    private void sendEmptyResultSet(ChannelHandlerContext ctx, String sql, byte sequenceId, int clientCapabilities) {
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.startsWith("SHOW ENGINES")) {
            sendResultSet(ctx, new String[]{"Engine", "Support", "Comment"}, new String[0][0], sequenceId, clientCapabilities);
        } else if (sqlUpper.startsWith("SHOW CHARSET")) {
            sendResultSet(ctx, new String[]{"Charset", "Description"}, new String[0][0], sequenceId, clientCapabilities);
        } else if (sqlUpper.startsWith("SHOW COLLATION")) {
            sendResultSet(ctx, new String[]{"Collation", "Charset"}, new String[0][0], sequenceId, clientCapabilities);
        } else if (sqlUpper.startsWith("SHOW PLUGINS")) {
            sendResultSet(ctx, new String[]{"Name", "Status"}, new String[0][0], sequenceId, clientCapabilities);
        } else if (sqlUpper.startsWith("SHOW VARIABLES")) {
            handleShowVariables(ctx, sql, sequenceId, clientCapabilities);
        } else {
            PacketHelper.sendOkPacket(ctx, "OK", sequenceId);
        }
    }

    /**
     * Handle COM_INIT_DB command (USE database)
     */
    public void handleInitDb(ChannelHandlerContext ctx, String databaseName, byte sequenceId) {
        try {
            logger.info("Switching to database: {}", databaseName);
            databaseEngine.useDatabase(databaseName);
            PacketHelper.sendOkPacket(ctx, "Database changed", sequenceId);
        } catch (Exception e) {
            logger.error("Error switching database: {}", databaseName, e);
            PacketHelper.sendErrPacket(ctx, "Unknown database '" + databaseName + "'", sequenceId);
        }
    }


}

