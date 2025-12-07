package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Factory class for creating and parsing MySQL packets
 */
public class PacketFactory {

    /**
     * Create packet from raw ByteBuf based on packet type
     */
    public static MysqlPacket parsePacket(ByteBuf buffer, byte sequenceId) {
        if (!buffer.isReadable()) {
            return null;
        }

        // Mark the position to allow peeking
        buffer.markReaderIndex();
        byte firstByte = buffer.readByte();
        buffer.resetReaderIndex();

        MysqlPacket packet;

        switch (firstByte) {
            case 0x00:
                // OK packet
                packet = new OkPacket(sequenceId);
                break;

            case (byte) 0xFF:
                // ERR packet
                packet = new ErrPacket(sequenceId);
                break;

            case (byte) 0xFE:
                // EOF packet or OK packet (in CLIENT_DEPRECATE_EOF mode)
                if (buffer.readableBytes() < 9) {
                    // EOF packet (5 bytes)
                    packet = new EOFPacket(sequenceId);
                } else {
                    // OK packet in CLIENT_DEPRECATE_EOF mode
                    packet = new OkPacket(sequenceId);
                }
                break;

            case 0x0A:
                // Handshake packet (starts with protocol version 10)
                packet = new HandshakePacket(sequenceId);
                break;

            default:
                // Could be Command packet, Auth packet, or data packet
                // Default to generic read
                packet = null;
                break;
        }

        if (packet != null) {
            packet.read(buffer);
        }

        return packet;
    }

    /**
     * Create an OK packet
     */
    public static OkPacket createOkPacket(byte sequenceId) {
        return new OkPacket(sequenceId);
    }

    /**
     * Create an OK packet with message
     */
    public static OkPacket createOkPacket(byte sequenceId, String info) {
        OkPacket packet = new OkPacket(sequenceId);
        packet.setInfo(info);
        return packet;
    }

    /**
     * Create an ERR packet
     */
    public static ErrPacket createErrPacket(byte sequenceId, int errorCode, String message) {
        return new ErrPacket(errorCode, message);
    }

    /**
     * Create an ERR packet with SQL state
     */
    public static ErrPacket createErrPacket(byte sequenceId, int errorCode, String sqlState, String message) {
        ErrPacket packet = new ErrPacket(errorCode, sqlState, message);
        packet.setSequenceId(sequenceId);
        return packet;
    }

    /**
     * Create an EOF packet
     */
    public static EOFPacket createEofPacket(byte sequenceId) {
        return new EOFPacket(sequenceId);
    }

    /**
     * Create an EOF packet with status
     */
    public static EOFPacket createEofPacket(byte sequenceId, int warningCount, int serverStatus) {
        EOFPacket packet = new EOFPacket(warningCount, serverStatus);
        packet.setSequenceId(sequenceId);
        return packet;
    }

    /**
     * Create a Handshake packet
     */
    public static HandshakePacket createHandshakePacket(byte sequenceId, int connectionId, byte[] scramble) {
        HandshakePacket packet = new HandshakePacket(connectionId, scramble, "5.7.0-mock");
        packet.setSequenceId(sequenceId);
        return packet;
    }

    /**
     * Create a Command packet
     */
    public static CommandPacket createCommandPacket(byte sequenceId, byte command, String argument) {
        CommandPacket packet = new CommandPacket(command, argument);
        packet.setSequenceId(sequenceId);
        return packet;
    }

    /**
     * Create a Column Definition packet
     */
    public static ColumnDefinitionPacket createColumnPacket(byte sequenceId, String name, byte type, long length) {
        ColumnDefinitionPacket packet = new ColumnDefinitionPacket(name, type, length);
        packet.setSequenceId(sequenceId);
        return packet;
    }

    /**
     * Create a Result Set Row packet
     */
    public static ResultSetRowPacket createRowPacket(byte sequenceId) {
        return new ResultSetRowPacket(sequenceId);
    }
}

