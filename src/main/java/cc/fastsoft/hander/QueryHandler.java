package cc.fastsoft.hander;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Handles SQL query execution
 */
public class QueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(QueryHandler.class);

    /**
     * Handle SQL query
     */
    public void handleQuery(ChannelHandlerContext ctx, String sql, byte sequenceId) {
        String sqlUpper = sql.trim().toUpperCase();

        try {
            if (sql.trim().equalsIgnoreCase("SELECT 1")) {
                sendSimpleResultSet(ctx, new String[]{"1"}, new String[][]{{"1"}}, sequenceId);
            } else if (sqlUpper.equals("SHOW DATABASES") || sqlUpper.equals("SHOW SCHEMAS")) {
                handleShowDatabases(ctx, sequenceId);
            } else if (sqlUpper.startsWith("SELECT @@") || sqlUpper.startsWith("SELECT DATABASE()")) {
                handleSystemVariableQuery(ctx, sql, sequenceId);
            } else if (sqlUpper.startsWith("SHOW ENGINES") || sqlUpper.startsWith("SHOW CHARSET") ||
                       sqlUpper.startsWith("SHOW COLLATION") || sqlUpper.startsWith("SHOW PLUGINS") ||
                       sqlUpper.startsWith("SHOW VARIABLES")) {
                sendEmptyResultSet(ctx, sql, sequenceId);
            } else if (sqlUpper.startsWith("SET ")) {
                PacketHelper.sendOkPacket(ctx, "OK", sequenceId);
            } else {
                PacketHelper.sendOkPacket(ctx, "Query executed: " + sql, sequenceId);
            }
        } catch (Exception e) {
            logger.error("Error handling query: {}", sql, e);
            PacketHelper.sendErrPacket(ctx, "Error: " + e.getMessage(), sequenceId);
        }
    }

    /**
     * Handle SHOW DATABASES command
     */
    private void handleShowDatabases(ChannelHandlerContext ctx, byte sequenceId) {
        String[] databases = {"information_schema", "mysql", "performance_schema", "sys", "test_db", "my_database"};
        String[][] rows = new String[databases.length][1];
        for (int i = 0; i < databases.length; i++) {
            rows[i][0] = databases[i];
        }
        sendSimpleResultSet(ctx, new String[]{"Database"}, rows, sequenceId);
    }

    /**
     * Handle system variable queries
     */
    private void handleSystemVariableQuery(ChannelHandlerContext ctx, String sql, byte sequenceId) {
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.contains("DATABASE()")) {
            sendSimpleResultSet(ctx, new String[]{"DATABASE()"}, new String[][]{{"test_db"}}, sequenceId);
        } else if (sqlUpper.contains("@@VERSION_COMMENT")) {
            sendSimpleResultSet(ctx, new String[]{"@@version_comment"}, new String[][]{{"MySQL Mock Server"}}, sequenceId);
        } else {
            // Return multi-column system variable query result
            String[] columns = extractColumnNames(sql);
            String[][] data = new String[1][columns.length];
            for (int i = 0; i < columns.length; i++) {
                data[0][i] = "mock_value";
            }
            sendSimpleResultSet(ctx, columns, data, sequenceId);
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
    private void sendSimpleResultSet(ChannelHandlerContext ctx, String[] columnNames, String[][] rows, byte sequenceId) {
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
        }

        // Row Data
        for (String[] row : rows) {
            ByteBuf rowBuf = Unpooled.buffer();
            for (String value : row) {
                PacketHelper.writeLengthEncodedString(rowBuf, value == null ? "" : value);
            }
            PacketHelper.sendPacket(ctx, rowBuf, sequenceId++);
        }

        // Send final OK packet
        sendResultSetOkPacket(ctx, rows.length, sequenceId);
    }

    /**
     * Send empty result set
     */
    private void sendEmptyResultSet(ChannelHandlerContext ctx, String sql, byte sequenceId) {
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.startsWith("SHOW ENGINES")) {
            sendSimpleResultSet(ctx, new String[]{"Engine", "Support", "Comment"}, new String[0][0], sequenceId);
        } else if (sqlUpper.startsWith("SHOW CHARSET")) {
            sendSimpleResultSet(ctx, new String[]{"Charset", "Description"}, new String[0][0], sequenceId);
        } else if (sqlUpper.startsWith("SHOW COLLATION")) {
            sendSimpleResultSet(ctx, new String[]{"Collation", "Charset"}, new String[0][0], sequenceId);
        } else if (sqlUpper.startsWith("SHOW PLUGINS")) {
            sendSimpleResultSet(ctx, new String[]{"Name", "Status"}, new String[0][0], sequenceId);
        } else if (sqlUpper.startsWith("SHOW VARIABLES")) {
            sendSimpleResultSet(ctx, new String[]{"Variable_name", "Value"}, new String[0][0], sequenceId);
        } else {
            PacketHelper.sendOkPacket(ctx, "OK", sequenceId);
        }
    }

    /**
     * Send result set OK packet
     */
    private void sendResultSetOkPacket(ChannelHandlerContext ctx, int rowCount, byte sequenceId) {
        ByteBuf ok = Unpooled.buffer();
        ok.writeByte(0xFE); // 0xFE = EOF packet marker
        ok.writeShortLE(0); // warnings
        ok.writeShortLE(0x0002); // SERVER_STATUS_AUTOCOMMIT
        PacketHelper.sendPacket(ctx, ok, sequenceId);
    }
}

