package cc.fastsoft;

import cc.fastsoft.protocol.codec.PacketDecoder;
import cc.fastsoft.protocol.codec.PacketEncoder;
import cc.fastsoft.hander.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
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
}
