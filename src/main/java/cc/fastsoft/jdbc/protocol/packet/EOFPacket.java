package cc.fastsoft.jdbc.protocol.packet;

import cc.fastsoft.jdbc.protocol.Constants;
import io.netty.buffer.ByteBuf;

/**
 * MySQL EOF Packet
 *
 * Packet Format:
 * - 1 byte: 0xFE (EOF packet header)
 * - 2 bytes: warning count
 * - 2 bytes: server status flags
 *
 * Note: In MySQL 5.7.5+, EOF packet is deprecated in favor of OK packet
 */
public class EOFPacket extends MysqlPacket {

    private static final byte EOF_HEADER = (byte) 0xFE;

    private int warningCount;
    private int serverStatus;

    public EOFPacket() {
        this((byte) 0);
    }

    public EOFPacket(byte sequenceId) {
        super(sequenceId);
        this.warningCount = 0;
        this.serverStatus = (int) Constants.SERVER_STATUS_AUTOCOMMIT;
    }

    public EOFPacket(int warningCount, int serverStatus) {
        this.warningCount = warningCount;
        this.serverStatus = serverStatus;
    }

    @Override
    public void write(ByteBuf buffer) {
        // EOF header
        buffer.writeByte(EOF_HEADER);

        // Warning count
        buffer.writeShortLE(warningCount);

        // Server status flags
        buffer.writeShortLE(serverStatus);
    }

    @Override
    public void read(ByteBuf buffer) {
        // Read and validate EOF header
        byte header = buffer.readByte();
        if (header != EOF_HEADER) {
            throw new IllegalStateException("Invalid EOF packet header: " + header);
        }

        // Read warning count
        warningCount = buffer.readUnsignedShortLE();

        // Read server status
        serverStatus = buffer.readUnsignedShortLE();
    }

    @Override
    public int getPayloadLength() {
        return 5; // 1 (header) + 2 (warning count) + 2 (server status)
    }

    // Getters and Setters

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(int serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public String toString() {
        return "EOFPacket{" +
                "sequenceId=" + sequenceId +
                ", warningCount=" + warningCount +
                ", serverStatus=" + serverStatus +
                '}';
    }
}

