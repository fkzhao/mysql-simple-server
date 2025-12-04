package cc.fastsoft.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

/**
 * Helper class for MySQL packet operations
 */
public class PacketHelper {

    /**
     * Send a MySQL packet
     */
    public static void sendPacket(ChannelHandlerContext ctx, ByteBuf payload, byte seq) {
        ctx.writeAndFlush(new Packet(payload.readableBytes(), seq, payload));
    }

    /**
     * Send OK packet
     */
    public static void sendOkPacket(ChannelHandlerContext ctx, String message, byte sequenceId) {
        ByteBuf ok = Unpooled.buffer();
        ok.writeByte(0); // OK
        ok.writeByte(0); // affected_rows
        ok.writeByte(0); // last_insert_id
        ok.writeShortLE(0); // status_flags
        ok.writeShortLE(0); // warnings
        writeLengthEncodedString(ok, message); // info
        sendPacket(ctx, ok, sequenceId);
    }

    /**
     * Send ERR packet
     */
    public static void sendErrPacket(ChannelHandlerContext ctx, String message, byte sequenceId) {
        ByteBuf err = Unpooled.buffer();
        err.writeByte(0xFF); // ERR
        err.writeShortLE(1045); // error_code
        err.writeBytes("#28000".getBytes(StandardCharsets.US_ASCII)); // sql_state
        writeLengthEncodedString(err, message); // error_message
        sendPacket(ctx, err, sequenceId);
    }

    /**
     * Read null-terminated string from buffer
     */
    public static String readNullTerminatedString(ByteBuf buf) {
        int len = buf.bytesBefore((byte) 0);
        if (len < 0) return "";
        String str = buf.readSlice(len).toString(StandardCharsets.US_ASCII);
        buf.readByte(); // skip null
        return str;
    }

    /**
     * Read remaining bytes as string
     */
    public static String readString(ByteBuf buf) {
        int length = buf.readableBytes();
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Write length-encoded string
     */
    public static void writeLengthEncodedString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
        writeLengthEncodedInteger(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * Write length-encoded integer
     */
    public static void writeLengthEncodedInteger(ByteBuf buf, long value) {
        if (value < 251) {
            buf.writeByte((int) value);
        } else if (value <= 0xFFFF) {
            buf.writeByte(0xFC);
            buf.writeShortLE((int) value);
        } else if (value <= 0xFFFFFF) {
            buf.writeByte(0xFD);
            buf.writeMediumLE((int) value);
        } else {
            buf.writeByte(0xFE);
            buf.writeLongLE(value);
        }
    }

    /**
     * Read length-encoded integer
     */
    public static long readLengthEncodedInteger(ByteBuf buf) {
        int firstByte = buf.readUnsignedByte();
        if (firstByte < 251) {
            return firstByte;
        } else if (firstByte == 0xFC) {
            return buf.readUnsignedShortLE();
        } else if (firstByte == 0xFD) {
            return buf.readUnsignedMediumLE();
        } else if (firstByte == 0xFE) {
            return buf.readLongLE();
        } else {
            return 0; // NULL value (0xFB)
        }
    }
}

