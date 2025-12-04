package cc.fastsoft.protocol.codec;

import cc.fastsoft.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) return;

        in.markReaderIndex(); // 标记读取位置

        // MySQL 协议使用小端序读取 3 字节长度
        int payloadLength = in.readByte() & 0xFF |
                           (in.readByte() & 0xFF) << 8 |
                           (in.readByte() & 0xFF) << 16;
        byte sequenceId = in.readByte();

        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex(); // 数据不够，重置读取位置
            return;
        }

        ByteBuf payload = in.readSlice(payloadLength);
        out.add(new Packet(payloadLength, sequenceId, payload.retainedDuplicate()));
    }
}
