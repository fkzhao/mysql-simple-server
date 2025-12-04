package cc.fastsoft.hander;

import cc.fastsoft.protocol.PacketHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Handles MySQL handshake protocol
 */
public class HandshakeHandler {
    private static final Logger logger = LoggerFactory.getLogger(HandshakeHandler.class);

    private final byte[] authPluginData;

    public HandshakeHandler() {
        this.authPluginData = new byte[20];
        Random random = new Random();
        random.nextBytes(authPluginData);
    }

    public byte[] getAuthPluginData() {
        return authPluginData;
    }

    /**
     * Send MySQL HandshakeV10 packet to client
     */
    public void sendHandshake(ChannelHandlerContext ctx) {
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

        logger.debug("Sending handshake to {}, payload size: {}",
                ctx.channel().remoteAddress(), handshake.readableBytes());

        PacketHelper.sendPacket(ctx, handshake, (byte) 0);

        logger.info("Handshake sent to {}", ctx.channel().remoteAddress());
    }
}


