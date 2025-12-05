package cc.fastsoft.jdbc.hander;

import cc.fastsoft.jdbc.ConnectContext;
import cc.fastsoft.jdbc.protocol.Constants;
import cc.fastsoft.jdbc.protocol.PacketHelper;
import cc.fastsoft.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Handles MySQL handshake protocol
 */
public class HandshakeHandler {
    private static final Logger logger = LoggerFactory.getLogger(HandshakeHandler.class);

    private final ConnectContext connectCtx;

    public HandshakeHandler(ConnectContext connectCtx) {
        if (connectCtx == null) {
            throw new IllegalArgumentException("ConnectContext cannot be null");
        }
        this.connectCtx = connectCtx;
    }

    /**
     * Send MySQL HandshakeV10 packet to client
     */
    public void sendHandshake(ChannelHandlerContext ctx) {
        ByteBuf buf = Unpooled.buffer();

        byte[] scramble = connectCtx.getScramble();
        logger.info("Handshake scramble: {}", StringUtils.bytesToHex(scramble));

        // 1. protocol version (0x0a for MySQL 4.1+)
        buf.writeByte(10);

        // 2. server version (NULL-terminated)
        buf.writeBytes("5.7.0-fastsoft\0".getBytes(StandardCharsets.US_ASCII));

        // 3. connection id (should be unique per connection in real implementation)
        buf.writeIntLE(connectCtx.getConnectionId());

        // 4. auth-plugin-data-part-1 (first 8 bytes of scramble)
        // Make sure 'scramble' has at least 20 random bytes
        buf.writeBytes(scramble, 0, 8);

        // 5. filler (always 0x00)
        buf.writeByte(0x00);

        // 6. capability flags lower 2 bytes
        int capabilityFlags =
                Constants.CLIENT_LONG_PASSWORD |
                        Constants.CLIENT_LONG_FLAG |
                        Constants.CLIENT_PROTOCOL_41 |
                        Constants.CLIENT_SECURE_CONNECTION |
                        Constants.CLIENT_PLUGIN_AUTH;
        // Add CLIENT_CONNECT_WITH_DB if you really support specifying DB at connect time
        // | Constants.CLIENT_CONNECT_WITH_DB;

        int capabilityFlagsLower = capabilityFlags & 0xFFFF;
        int capabilityFlagsUpper = (capabilityFlags >>> 16) & 0xFFFF;

        buf.writeShortLE(capabilityFlagsLower);

        // 7. character set (33 = utf8_general_ci)
        buf.writeByte(33);

        // 8. status flags (e.g. SERVER_STATUS_AUTOCOMMIT = 2)
        buf.writeShortLE(2);

        // 9. capability flags upper 2 bytes
        buf.writeShortLE(capabilityFlagsUpper);

        // 10. auth-plugin-data length
        // We use 20 bytes scramble + 1 terminating byte = 21
        buf.writeByte(21);

        // 11. reserved (10 bytes), all 0
        buf.writeBytes(new byte[10]);

        // 12. auth-plugin-data-part-2:
        // According to protocol: length is max(13, auth_plugin_data_len - 8)
        // With auth_plugin_data_len = 21 => 21 - 8 = 13
        // So we send 12 bytes from scramble[8..20) + one 0x00 as the 13th byte
        buf.writeBytes(scramble, 8, 12); // 12 bytes
        buf.writeByte(0x00);             // 13th byte (terminating byte)

        // 13. auth plugin name (NULL-terminated)
        buf.writeBytes("mysql_native_password\0".getBytes(StandardCharsets.US_ASCII));

        logger.debug("Sending handshake to {}, payload size: {}",
                ctx.channel().remoteAddress(), buf.readableBytes());

        PacketHelper.sendPacket(ctx, buf, (byte) 0);

        logger.info("Handshake sent to {}", ctx.channel().remoteAddress());
    }
}


