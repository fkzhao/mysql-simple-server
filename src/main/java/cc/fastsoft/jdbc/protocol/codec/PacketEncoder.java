package cc.fastsoft.jdbc.protocol.codec;

import cc.fastsoft.jdbc.protocol.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    Logger logger = LoggerFactory.getLogger(PacketEncoder.class);

    @Override
    protected void encode(io.netty.channel.ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        int payloadLength = msg.getPayload().readableBytes();

        // MySQL protocol uses little-endian for packet length (3 bytes)
        out.writeByte(payloadLength & 0xFF);
        out.writeByte((payloadLength >> 8) & 0xFF);
        out.writeByte((payloadLength >> 16) & 0xFF);

        out.writeByte(msg.getSequenceId());
        out.writeBytes(msg.getPayload());

        logger.info("[OUT]Encoded packet: length={} seq={}", payloadLength, msg.getSequenceId());

    }
}
