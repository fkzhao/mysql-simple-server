package cc.fastsoft.jdbc;

import cc.fastsoft.jdbc.hander.AuthHandler;
import cc.fastsoft.jdbc.hander.CommandHandler;
import cc.fastsoft.jdbc.hander.HandshakeHandler;
import cc.fastsoft.jdbc.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerHandler extends SimpleChannelInboundHandler<Packet> {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    private boolean authenticated = false;
    private int clientCapabilities = 0;

    private final HandshakeHandler handshakeHandler;
    private final AuthHandler authHandler;
    private final CommandHandler commandHandler;

    public ServerHandler() {
        ConnectContext ctx = new ConnectContext();
        int currentConnections = activeConnections.get() + 1;
        logger.info("Creating new connection handler. Active connections: {}", currentConnections);
        ctx.setConnectionId(currentConnections);
        this.handshakeHandler = new HandshakeHandler(ctx);
        this.authHandler = new AuthHandler("123456", ctx.getScramble());
        this.commandHandler = new CommandHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        int count = activeConnections.incrementAndGet();
        logger.info("Client connected: {}. Total active connections: {}",
                ctx.channel().remoteAddress(), count);
        handshakeHandler.sendHandshake(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        byte sequenceId = (byte) (packet.getSequenceId() + 1); // Response seq = request seq + 1

        if (!authenticated) {
            AuthHandler.AuthResult result = authHandler.handleAuth(ctx, packet.getPayload(), sequenceId);
            authenticated = result.isAuthenticated();
            clientCapabilities = result.getClientCapabilities();
        } else {
            commandHandler.handleCommand(ctx, packet.getPayload(), sequenceId, clientCapabilities);
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
