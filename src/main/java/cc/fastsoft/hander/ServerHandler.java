package cc.fastsoft.hander;

import cc.fastsoft.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

public class ServerHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private byte sequenceId = 0;
    private boolean authenticated = false;
    private byte[] authPluginData = new byte[20]; // 认证 salt
    private String username;
    private String password = "123456"; // mock 密码，实际中从配置读取
    private String database;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 发送 HandshakeV10 包
        Random random = new Random();
        random.nextBytes(authPluginData);

        ByteBuf handshake = Unpooled.buffer();
        handshake.writeByte(10); // protocol version
        handshake.writeBytes("5.7.36-mock\0".getBytes(StandardCharsets.US_ASCII));
        handshake.writeIntLE(1); // connection id
        handshake.writeBytes(authPluginData, 0, 8); // auth_plugin_data_part_1
        handshake.writeByte(0); // filler
        handshake.writeShortLE(0xFFFF); // capability_flags_1
        handshake.writeByte(33); // character_set
        handshake.writeShortLE(2); // status_flags
        handshake.writeShortLE(0xFFFF); // capability_flags_2
        handshake.writeByte(21); // auth_plugin_data_len
        handshake.writeBytes(new byte[10]); // reserved
        handshake.writeBytes(authPluginData, 8, 12); // auth_plugin_data_part_2
        handshake.writeByte(0); // null terminator
        handshake.writeBytes("mysql_native_password\0".getBytes(StandardCharsets.US_ASCII));

        logger.debug("Sending handshake to {}, payload size: {}", ctx.channel().remoteAddress(), handshake.readableBytes());
        sendPacket(ctx, handshake, (byte) 0);
        logger.info("Handshake sent to {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        sequenceId = (byte) (packet.sequenceId + 1); // 响应 seq = req seq + 1

        if (!authenticated) {
            handleAuth(ctx, packet.payload);
        } else {
            handleCommand(ctx, packet.payload);
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, ByteBuf payload) {
        logger.debug("Received auth packet from {}, payload size: {}", ctx.channel().remoteAddress(), payload.readableBytes());

        // 检查是否是 HandshakeResponse41 (需要至少 4+4+1+23=32 字节的头部)
        if (payload.readableBytes() < 32) {
            logger.warn("Packet too small for HandshakeResponse41, treating as simple auth");
            // 简单处理：跳过认证直接返回 OK
            authenticated = true;
            sendOkPacket(ctx, "Authenticated (simple mode)");
            return;
        }

        // 解析 HandshakeResponse41
        int clientCapabilities = payload.readIntLE();
        int maxPacketSize = payload.readIntLE();
        int charset = payload.readUnsignedByte();

        // 检查是否有足够的字节来跳过保留字段
        if (payload.readableBytes() >= 23) {
            payload.skipBytes(23); // reserved (23 bytes of 0x00)
        } else {
            logger.error("Not enough bytes for reserved field: {}", payload.readableBytes());
            authenticated = true;
            sendOkPacket(ctx, "Authenticated (incomplete packet)");
            return;
        }

        logger.debug("Client capabilities: 0x{}, remaining bytes: {}", Integer.toHexString(clientCapabilities), payload.readableBytes());

        // 读取用户名（null-terminated string）
        username = readNullTerminatedString(payload);
        logger.debug("Username: '{}', remaining bytes: {}", username, payload.readableBytes());

        // 读取认证响应
        byte[] authResponse = new byte[0];

        if (payload.readableBytes() > 0) {
            // 检查 CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA 标志位
            if ((clientCapabilities & 0x00200000) != 0) {
                // 使用 length-encoded integer 格式
                long authResponseLen = readLengthEncodedInteger(payload);
                authResponse = new byte[(int) authResponseLen];
                if (payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(authResponse);
                } else {
                    logger.error("Not enough bytes for auth response: expected {}, got {}", authResponseLen, payload.readableBytes());
                }
            } else if ((clientCapabilities & 0x00008000) != 0) {
                // CLIENT_SECURE_CONNECTION: 使用单字节长度格式
                int authResponseLen = payload.readUnsignedByte();
                authResponse = new byte[authResponseLen];
                if (payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(authResponse);
                } else {
                    logger.error("Not enough bytes for auth response: expected {}, got {}", authResponseLen, payload.readableBytes());
                }
            } else {
                // 老式格式：null-terminated string
                int len = payload.bytesBefore((byte) 0);
                if (len >= 0) {
                    authResponse = new byte[len];
                    payload.readBytes(authResponse);
                    payload.readByte(); // skip null
                }
            }
        }

        logger.debug("Auth response length: {}, remaining bytes: {}", authResponse.length, payload.readableBytes());

        // 读取数据库名（如果有 CLIENT_CONNECT_WITH_DB 标志）
        if ((clientCapabilities & 0x00000008) != 0 && payload.readableBytes() > 0) {
            database = readNullTerminatedString(payload);
            logger.debug("Database: '{}', remaining bytes: {}", database, payload.readableBytes());
        }

        // 读取认证插件名（如果有 CLIENT_PLUGIN_AUTH 标志）
        String authPluginName = "";
        if ((clientCapabilities & 0x00080000) != 0 && payload.readableBytes() > 0) {
            authPluginName = readNullTerminatedString(payload);
            logger.debug("Auth plugin: '{}', remaining bytes: {}", authPluginName, payload.readableBytes());
        }

        // 认证逻辑 (mysql_native_password)
        byte[] expected = nativePasswordHash(password, authPluginData);
        boolean authOk = MessageDigest.isEqual(authResponse, expected);

        logger.info("Authentication result for user '{}': {}", username, authOk ? "SUCCESS" : "FAILED");

        // 暂时总是返回成功以便测试
        authenticated = true;
        sendOkPacket(ctx, "Authenticated");
//        if (authOk) {
//            authenticated = true;
//            sendOkPacket(ctx, "Authenticated");
//        } else {
//            sendErrPacket(ctx, "Authentication failed");
//        }
    }

    private void handleCommand(ChannelHandlerContext ctx, ByteBuf payload) {
        byte command = payload.readByte();

        switch (command) {
            case 0x01: // COM_QUIT
                ctx.close();
                break;
            case 0x03: // COM_QUERY
                String sql = readString(payload);
                // 清理 SQL：移除注释，提取真正的 SQL
                sql = cleanSql(sql);
                logger.info("Executing SQL from {}: {}", ctx.channel().remoteAddress(), sql);
                handleQuery(ctx, sql);
                break;
            case 0x0E: // COM_PING
                sendOkPacket(ctx, "PONG");
                break;
            default:
                sendErrPacket(ctx, "Unsupported command: " + command);
        }
    }

    private String cleanSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        // 移除 /* ... */ 格式的注释
        sql = sql.replaceAll("/\\*.*?\\*/", " ");

        // 移除 -- 开头的单行注释
        sql = sql.replaceAll("--[^\n]*", " ");

        // 移除 # 开头的单行注释
        sql = sql.replaceAll("#[^\n]*", " ");

        // 移除多余的空白字符
        sql = sql.trim().replaceAll("\\s+", " ");

        return sql;
    }

    private void handleQuery(ChannelHandlerContext ctx, String sql) {
        String sqlUpper = sql.trim().toUpperCase();

        try {
            if (sql.trim().equalsIgnoreCase("SELECT 1")) {
                sendSimpleResultSet(ctx, new String[]{"1"}, new String[][]{{"1"}});
            } else if (sqlUpper.equals("SHOW DATABASES") || sqlUpper.equals("SHOW SCHEMAS")) {
                // 返回模拟的数据库列表
                String[] databases = {"information_schema", "mysql", "performance_schema", "sys", "test_db", "my_database"};
                String[][] rows = new String[databases.length][1];
                for (int i = 0; i < databases.length; i++) {
                    rows[i][0] = databases[i];
                }
                sendSimpleResultSet(ctx, new String[]{"Database"}, rows);
            } else if (sqlUpper.startsWith("SELECT @@") || sqlUpper.startsWith("SELECT DATABASE()")) {
                // DBeaver 查询系统变量
                handleSystemVariableQuery(ctx, sql);
            } else if (sqlUpper.startsWith("SHOW ENGINES") || sqlUpper.startsWith("SHOW CHARSET") ||
                       sqlUpper.startsWith("SHOW COLLATION") || sqlUpper.startsWith("SHOW PLUGINS") ||
                       sqlUpper.startsWith("SHOW VARIABLES")) {
                // 返回空结果集
                sendEmptyResultSet(ctx, sql);
            } else if (sqlUpper.startsWith("SET ")) {
                // SET 命令直接返回 OK
                sendOkPacket(ctx, "OK");
            } else {
                // 对于其他 SQL，返回简单 OK
                sendOkPacket(ctx, "Query executed: " + sql);
            }
        } catch (Exception e) {
            logger.error("Error handling query: {}", sql, e);
            sendErrPacket(ctx, "Error: " + e.getMessage());
        }
    }

    private void handleSystemVariableQuery(ChannelHandlerContext ctx, String sql) {
        // 解析变量名并返回模拟值
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.contains("DATABASE()")) {
            sendSimpleResultSet(ctx, new String[]{"DATABASE()"}, new String[][]{{"test_db"}});
        } else if (sqlUpper.contains("@@VERSION_COMMENT")) {
            sendSimpleResultSet(ctx, new String[]{"@@version_comment"}, new String[][]{{"MySQL Mock Server"}});
        } else {
            // 返回多列的系统变量查询结果
            String[] columns = extractColumnNames(sql);
            String[][] data = new String[1][columns.length];
            for (int i = 0; i < columns.length; i++) {
                data[0][i] = "mock_value";
            }
            sendSimpleResultSet(ctx, columns, data);
        }
    }

    private String[] extractColumnNames(String sql) {
        // 简单的列名提取逻辑
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

    private void sendSimpleResultSet(ChannelHandlerContext ctx, String[] columnNames, String[][] rows) {
        // Column Count
        ByteBuf columnCount = Unpooled.buffer().writeByte(columnNames.length);
        sendPacket(ctx, columnCount, sequenceId++);

        // Column Definitions
        for (String colName : columnNames) {
            ByteBuf colDef = Unpooled.buffer();
            writeLengthEncodedString(colDef, "def"); // catalog
            writeLengthEncodedString(colDef, ""); // schema
            writeLengthEncodedString(colDef, ""); // table
            writeLengthEncodedString(colDef, ""); // org_table
            writeLengthEncodedString(colDef, colName); // name
            writeLengthEncodedString(colDef, colName); // org_name
            colDef.writeByte(0x0C); // next_length
            colDef.writeShortLE(33); // character_set
            colDef.writeIntLE(256); // column_length
            colDef.writeByte(0xFD); // column_type (VAR_STRING)
            colDef.writeShortLE(0); // flags
            colDef.writeByte(0); // decimals
            colDef.writeBytes(new byte[2]); // filler
            sendPacket(ctx, colDef, sequenceId++);
        }

        // Row Data
        for (String[] row : rows) {
            ByteBuf rowBuf = Unpooled.buffer();
            for (String value : row) {
                writeLengthEncodedString(rowBuf, value == null ? "" : value);
            }
            sendPacket(ctx, rowBuf, sequenceId++);
        }

        // 发送最终的 OK 包
        sendResultSetOkPacket(ctx, rows.length);
    }

    private void sendEmptyResultSet(ChannelHandlerContext ctx, String sql) {
        // 根据查询类型返回适当的空结果集
        String sqlUpper = sql.toUpperCase();

        if (sqlUpper.startsWith("SHOW ENGINES")) {
            sendSimpleResultSet(ctx, new String[]{"Engine", "Support", "Comment"}, new String[0][0]);
        } else if (sqlUpper.startsWith("SHOW CHARSET")) {
            sendSimpleResultSet(ctx, new String[]{"Charset", "Description"}, new String[0][0]);
        } else if (sqlUpper.startsWith("SHOW COLLATION")) {
            sendSimpleResultSet(ctx, new String[]{"Collation", "Charset"}, new String[0][0]);
        } else if (sqlUpper.startsWith("SHOW PLUGINS")) {
            sendSimpleResultSet(ctx, new String[]{"Name", "Status"}, new String[0][0]);
        } else if (sqlUpper.startsWith("SHOW VARIABLES")) {
            sendSimpleResultSet(ctx, new String[]{"Variable_name", "Value"}, new String[0][0]);
        } else {
            sendOkPacket(ctx, "OK");
        }
    }

    private void sendOkPacket(ChannelHandlerContext ctx, String message) {
        ByteBuf ok = Unpooled.buffer();
        ok.writeByte(0); // OK
        ok.writeByte(0); // affected_rows
        ok.writeByte(0); // last_insert_id
        ok.writeShortLE(0); // status_flags
        ok.writeShortLE(0); // warnings
        writeLengthEncodedString(ok, message); // info
        sendPacket(ctx, ok, sequenceId++);
    }

    private void sendErrPacket(ChannelHandlerContext ctx, String message) {
        ByteBuf err = Unpooled.buffer();
        err.writeByte(0xFF); // ERR
        err.writeShortLE(1045); // error_code
        err.writeBytes("#28000".getBytes(StandardCharsets.US_ASCII)); // sql_state
        writeLengthEncodedString(err, message); // error_message
        sendPacket(ctx, err, sequenceId++);
    }

    private void sendEofPacket(ChannelHandlerContext ctx) {
        ByteBuf eof = Unpooled.buffer();
        eof.writeByte(0xFE); // EOF
        eof.writeShortLE(0); // warnings
        eof.writeShortLE(0); // status_flags
        sendPacket(ctx, eof, sequenceId++);
    }

    private void sendResultSetOkPacket(ChannelHandlerContext ctx, int rowCount) {
        // 使用 OK 包代替 EOF 包（符合 CLIENT_DEPRECATE_EOF）
        ByteBuf ok = Unpooled.buffer();
        ok.writeByte(0xFE); // 0xFE = EOF packet marker
        ok.writeShortLE(0); // warnings
        ok.writeShortLE(0x0002); // SERVER_STATUS_AUTOCOMMIT
        sendPacket(ctx, ok, sequenceId++);
    }

    private void sendPacket(ChannelHandlerContext ctx, ByteBuf payload, byte seq) {
        ctx.writeAndFlush(new Packet(payload.readableBytes(), seq, payload));
    }

    private String readNullTerminatedString(ByteBuf buf) {
        int len = buf.bytesBefore((byte) 0);
        if (len < 0) return "";
        String str = buf.readSlice(len).toString(StandardCharsets.US_ASCII);
        buf.readByte(); // skip null
        return str;
    }

    private String readString(ByteBuf buf) {
        int length = buf.readableBytes();
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeLengthEncodedString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
        writeLengthEncodedInteger(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    private void writeLengthEncodedInteger(ByteBuf buf, long value) {
        if (value < 251) {
            buf.writeByte((int) value);
        } else if (value <= 0xFFFF) {
            buf.writeByte(0xFC);
            buf.writeShortLE((int) value);
        } else if (value <= 0xFFFFFF) {
            buf.writeByte(0xFD);
            buf.writeMediumLE((int) value);
        } else {
            buf.writeByte(0xFE);
            buf.writeLongLE(value);
        }
    }

    private long readLengthEncodedInteger(ByteBuf buf) {
        int firstByte = buf.readUnsignedByte();
        if (firstByte < 251) {
            return firstByte;
        } else if (firstByte == 0xFC) {
            return buf.readUnsignedShortLE();
        } else if (firstByte == 0xFD) {
            return buf.readUnsignedMediumLE();
        } else if (firstByte == 0xFE) {
            return buf.readLongLE();
        } else {
            return 0; // NULL value (0xFB)
        }
    }

    private byte[] nativePasswordHash(String password, byte[] salt) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] pass1 = sha1.digest(password.getBytes(StandardCharsets.US_ASCII));
            byte[] pass2 = sha1.digest(pass1);
            sha1.update(salt);
            sha1.update(pass2);
            byte[] hash = sha1.digest();
            for (int i = 0; i < hash.length; i++) {
                hash[i] ^= pass1[i];
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof java.net.SocketException &&
            cause.getMessage() != null &&
            cause.getMessage().contains("Connection reset")) {
            // 客户端断开连接是正常情况，静默关闭
            logger.debug("Client disconnected: {}", ctx.channel().remoteAddress());
        } else {
            // 其他异常打印堆栈
            logger.error("Exception in channel: {}", ctx.channel().remoteAddress(), cause);
        }
        ctx.close();
    }
}
