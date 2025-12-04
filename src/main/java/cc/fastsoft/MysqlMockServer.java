package cc.fastsoft;

import cc.fastsoft.hander.AuthHandler;
import cc.fastsoft.hander.CommandHandler;
import cc.fastsoft.hander.HandshakeHandler;
import cc.fastsoft.protocol.Packet;
import cc.fastsoft.protocol.codec.PacketDecoder;
import cc.fastsoft.protocol.codec.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlMockServer {
    private static final Logger logger = LoggerFactory.getLogger(MysqlMockServer.class);

    public static void main(String[] args) {
        System.setProperty("io.netty.channel.AbstractChannel.connectionReset", "false");
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(8, NioIoHandler.newFactory());
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new PacketDecoder());
                            ch.pipeline().addLast(new PacketEncoder());
                            ch.pipeline().addLast(new ServerHandler());
                        }
                    });

            int port = 2883;
            ChannelFuture f = b.bind(port).sync();
            logger.info("MySQL Mock Server started on port {}", port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            logger.info("Shutting down MySQL Mock Server");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    //
    static class ServerHandler extends SimpleChannelInboundHandler<Packet> {
        private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
        private byte sequenceId = 0;
        private boolean authenticated = false;

        private final HandshakeHandler handshakeHandler;
        private final AuthHandler authHandler;
        private final CommandHandler commandHandler;

        public ServerHandler() {
            this.handshakeHandler = new HandshakeHandler();
            this.authHandler = new AuthHandler("123456", handshakeHandler.getAuthPluginData());
            this.commandHandler = new CommandHandler();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshakeHandler.sendHandshake(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            sequenceId = (byte) (packet.getSequenceId() + 1); // Response seq = request seq + 1

            if (!authenticated) {
                AuthHandler.AuthResult result = authHandler.handleAuth(ctx, packet.getPayload(), sequenceId);
                authenticated = result.isAuthenticated();
            } else {
                commandHandler.handleCommand(ctx, packet.getPayload(), sequenceId);
            }
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
    }
}
