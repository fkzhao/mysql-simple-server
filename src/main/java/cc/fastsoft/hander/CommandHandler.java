package cc.fastsoft.hander;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MySQL commands
 */
public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    private final QueryHandler queryHandler;

    public CommandHandler() {
        this.queryHandler = new QueryHandler();
    }

    /**
     * Handle MySQL command packet
     */
    public void handleCommand(ChannelHandlerContext ctx, ByteBuf payload, byte sequenceId) {
        byte command = payload.readByte();

        switch (command) {
            case 0x01: // COM_QUIT
                logger.debug("Received COM_QUIT from {}", ctx.channel().remoteAddress());
                ctx.close();
                break;

            case 0x03: // COM_QUERY
                String sql = PacketHelper.readString(payload);
                // Clean SQL: remove comments, extract real SQL
                sql = cleanSql(sql);
                logger.info("Executing SQL from {}: {}", ctx.channel().remoteAddress(), sql);
                queryHandler.handleQuery(ctx, sql, sequenceId);
                break;

            case 0x0E: // COM_PING
                logger.debug("Received COM_PING from {}", ctx.channel().remoteAddress());
                PacketHelper.sendOkPacket(ctx, "PONG", sequenceId);
                break;

            default:
                logger.warn("Unsupported command: {} from {}", command, ctx.channel().remoteAddress());
                PacketHelper.sendErrPacket(ctx, "Unsupported command: " + command, sequenceId);
        }
    }

    /**
     * Clean SQL by removing comments
     */
    private String cleanSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        // Remove /* ... */ comments
        sql = sql.replaceAll("/\\*.*?\\*/", " ");

        // Remove -- single line comments
        sql = sql.replaceAll("--[^\n]*", " ");

        // Remove # single line comments
        sql = sql.replaceAll("#[^\n]*", " ");

        // Remove extra whitespace
        sql = sql.trim().replaceAll("\\s+", " ");

        return sql;
    }
}

