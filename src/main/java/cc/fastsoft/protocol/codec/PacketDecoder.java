package cc.fastsoft.protocol.codec;

import cc.fastsoft.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    Logger logger = LoggerFactory.getLogger(PacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {

        // Ensure there are at least 4 bytes to read the header
        if (in.readableBytes() < 4) {
            throw new Exception("packet length less than 4 bytes");
        }

        // Mark the current reader index
        in.markReaderIndex();

        // MySQL protocol uses little-endian for packet length (3 bytes)
        int payloadLength = in.readByte() & 0xFF |
                           (in.readByte() & 0xFF) << 8 |
                           (in.readByte() & 0xFF) << 16;
        byte sequenceId = in.readByte();

        // Check if the full payload is available
        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex(); // if not enough data, reset reader index
            return;
        }

        // Read the payload
        ByteBuf payload = in.readSlice(payloadLength);
        out.add(new Packet(payloadLength, sequenceId, payload.retainedDuplicate()));

        logger.info("[IN]Decoded packet: length={} seq={}", payloadLength, sequenceId);
    }
}
