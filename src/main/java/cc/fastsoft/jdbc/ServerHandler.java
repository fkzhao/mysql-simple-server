package cc.fastsoft.jdbc;

import cc.fastsoft.jdbc.hander.CommandHandler;
import cc.fastsoft.jdbc.protocol.packet.AuthPacket;
import cc.fastsoft.jdbc.protocol.packet.HandshakePacket;
import cc.fastsoft.jdbc.protocol.MysqlPassword;
import cc.fastsoft.jdbc.protocol.packet.Packet;
import cc.fastsoft.jdbc.protocol.PacketFactory;
import cc.fastsoft.jdbc.protocol.PacketHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private static final int SCRAMBLE_LENGTH = 20;
    private static final String DEFAULT_PASSWORD = "123456";
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    private final byte[] authPluginData;
    private final CommandHandler commandHandler;
    private final ConnectContext connectContext;

    public ServerHandler() {
        this.connectContext = new ConnectContext();
        this.authPluginData = MysqlPassword.createRandomString(SCRAMBLE_LENGTH);
        int currentConnections = activeConnections.get() + 1;
        logger.info("Creating new connection handler. Active connections: {}", currentConnections);
        this.connectContext.setConnectionId(currentConnections);
        this.commandHandler = new CommandHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        int connectId = activeConnections.incrementAndGet();
        logger.info("Client connected: {}. Total active connections: {}",
                ctx.channel().remoteAddress(), connectId);
        // Create HandshakePacket using factory
        HandshakePacket handshakePacket = PacketFactory.createHandshakePacket(
                (byte) 0,
                connectId,
                this.authPluginData
        );

        // Set server version
        handshakePacket.setServerVersion("5.7.0-mock");
        logger.debug("Sending handshake to {}, connection id: {}",
                ctx.channel().remoteAddress(),  connectId);

        // Send packet using PacketHelper
        PacketHelper.sendMysqlPacket(ctx, handshakePacket);
        logger.info("Handshake sent to {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        byte sequenceId = (byte) (packet.getSequenceId() + 1); // Response seq = request seq + 1

        if (this.connectContext.isAuthenticated()) {
            commandHandler.handleCommand(ctx,
                    packet.getPayload(),
                    sequenceId,
                    this.connectContext.getClientCapabilities()
            );
        } else {
            try {
                AuthPacket authPacket = PacketFactory.createAuthPacketFromBuf(packet.getPayload());
                authPacket.setSequenceId(sequenceId);

                // verify username and password
                if (MysqlPassword.verifyPassword(DEFAULT_PASSWORD, authPluginData, authPacket.getAuthResponse(), authPacket.getAuthPluginName())) {
                    this.connectContext.setUserName(authPacket.getUsername());
                    this.connectContext.setDatabase(authPacket.getDatabase());
                    this.connectContext.setClientCapabilities(authPacket.getCapabilityFlags());
                    logger.info("User '{}' authenticated successfully from {}",
                            authPacket.getUsername(), ctx.channel().remoteAddress());
                    PacketHelper.sendOkPacket(ctx, "Authentication successful", sequenceId);
                } else {
                    logger.error("Authentication failed for user '{}' from {}",
                            authPacket.getUsername(), ctx.channel().remoteAddress());
                    PacketHelper.sendErrPacket(ctx, "Authentication failed", sequenceId);
                }
            } catch (Exception e) {
                logger.error("Failed to verify user name from {}: {}",
                        ctx.channel().remoteAddress(), e.getMessage());
                PacketHelper.sendErrPacket(ctx, e.getMessage(), sequenceId);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // connection closed
        int count = activeConnections.decrementAndGet();
        logger.info("Client disconnected: {}. Remaining active connections: {}",
                ctx.channel().remoteAddress(), count);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof java.net.SocketException &&
                cause.getMessage() != null &&
                cause.getMessage().contains("Connection reset")) {
            logger.debug("Client disconnected: {}", ctx.channel().remoteAddress());
        } else {
            logger.error("Exception in channel: {}", ctx.channel().remoteAddress(), cause);
        }
        ctx.close();
    }

    public static int getActiveConnectionCount() {
        return activeConnections.get();
    }

}
