package cc.fastsoft.protocol.codec;

import cc.fastsoft.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(io.netty.channel.ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        int payloadLength = msg.getPayload().readableBytes();

        // MySQL protocol uses little-endian for packet length (3 bytes)
        out.writeByte(payloadLength & 0xFF);
        out.writeByte((payloadLength >> 8) & 0xFF);
        out.writeByte((payloadLength >> 16) & 0xFF);

        out.writeByte(msg.sequenceId);
        out.writeBytes(msg.payload);

        System.out.println("Encoded packet: length=" + payloadLength + ", seq=" + msg.sequenceId);

    }
}
