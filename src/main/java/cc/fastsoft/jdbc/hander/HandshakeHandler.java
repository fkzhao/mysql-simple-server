package cc.fastsoft.jdbc.hander;

import cc.fastsoft.jdbc.ConnectContext;
import cc.fastsoft.jdbc.protocol.*;
import cc.fastsoft.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        byte[] scramble = connectCtx.getScramble();
        logger.info("Handshake scramble: {}", StringUtils.bytesToHex(scramble));

        // Create HandshakePacket using factory
        HandshakePacket handshakePacket = PacketFactory.createHandshakePacket(
            (byte) 0,
            connectCtx.getConnectionId(),
            scramble
        );

        // Set server version
        handshakePacket.setServerVersion("5.7.0-mock");

        logger.debug("Sending handshake to {}, connection id: {}",
                ctx.channel().remoteAddress(), connectCtx.getConnectionId());

        // Send packet using PacketHelper
        PacketHelper.sendMysqlPacket(ctx, handshakePacket);


        logger.info("Handshake sent to {}", ctx.channel().remoteAddress());
    }
}


