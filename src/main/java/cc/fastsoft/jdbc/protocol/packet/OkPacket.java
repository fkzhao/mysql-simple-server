package cc.fastsoft.jdbc.protocol.packet;

import cc.fastsoft.jdbc.protocol.Constants;
import cc.fastsoft.jdbc.protocol.PacketHelper;
import io.netty.buffer.ByteBuf;

/**
 * MySQL OK Packet
 *
 * Packet Format:
 * - 1 byte: 0x00 (OK packet header)
 * - length-encoded int: affected rows
 * - length-encoded int: last insert id
 * - 2 bytes: server status flags
 * - 2 bytes: warning count
 * - string[EOF]: info message (if CLIENT_SESSION_TRACK capability)
 */
public class OkPacket extends MysqlPacket {

    private static final byte OK_HEADER = 0x00;

    private long affectedRows;
    private long lastInsertId;
    private int serverStatus;
    private int warningCount;
    private String info;

    public OkPacket() {
        this((byte) 0);
    }

    public OkPacket(byte sequenceId) {
        super(sequenceId);
        this.affectedRows = 0;
        this.lastInsertId = 0;
        this.serverStatus = (int) Constants.SERVER_STATUS_AUTOCOMMIT;
        this.warningCount = 0;
        this.info = "";
    }

    public OkPacket(long affectedRows, long lastInsertId, int serverStatus, int warningCount, String info) {
        this.affectedRows = affectedRows;
        this.lastInsertId = lastInsertId;
        this.serverStatus = serverStatus;
        this.warningCount = warningCount;
        this.info = info != null ? info : "";
    }

    @Override
    public void write(ByteBuf buffer) {
        // OK header
        buffer.writeByte(OK_HEADER);

        // Affected rows (length-encoded integer)
        PacketHelper.writeLengthEncodedInteger(buffer, affectedRows);

        // Last insert id (length-encoded integer)
        PacketHelper.writeLengthEncodedInteger(buffer, lastInsertId);

        // Server status flags
        buffer.writeShortLE(serverStatus);

        // Warning count
        buffer.writeShortLE(warningCount);

        // Info message (if any)
        if (info != null && !info.isEmpty()) {
            buffer.writeBytes(info.getBytes());
        }
    }

    @Override
    public void read(ByteBuf buffer) {
        // Read and validate OK header
        byte header = buffer.readByte();
        if (header != OK_HEADER) {
            throw new IllegalStateException("Invalid OK packet header: " + header);
        }

        // Read affected rows
        affectedRows = PacketHelper.readLengthEncodedInteger(buffer);

        // Read last insert id
        lastInsertId = PacketHelper.readLengthEncodedInteger(buffer);

        // Read server status
        serverStatus = buffer.readUnsignedShortLE();

        // Read warning count
        warningCount = buffer.readUnsignedShortLE();

        // Read info message (rest of the packet)
        if (buffer.isReadable()) {
            byte[] infoBytes = new byte[buffer.readableBytes()];
            buffer.readBytes(infoBytes);
            info = new String(infoBytes);
        }
    }

    @Override
    public int getPayloadLength() {
        int length = 1; // OK header
        length += PacketHelper.getLengthEncodedIntegerLength(affectedRows);
        length += PacketHelper.getLengthEncodedIntegerLength(lastInsertId);
        length += 2; // server status
        length += 2; // warning count
        if (info != null && !info.isEmpty()) {
            length += info.getBytes().length;
        }
        return length;
    }

    // Getters and Setters

    public long getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(long affectedRows) {
        this.affectedRows = affectedRows;
    }

    public long getLastInsertId() {
        return lastInsertId;
    }

    public void setLastInsertId(long lastInsertId) {
        this.lastInsertId = lastInsertId;
    }

    public int getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(int serverStatus) {
        this.serverStatus = serverStatus;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "OkPacket{" +
                "sequenceId=" + sequenceId +
                ", affectedRows=" + affectedRows +
                ", lastInsertId=" + lastInsertId +
                ", serverStatus=" + serverStatus +
                ", warningCount=" + warningCount +
                ", info='" + info + '\'' +
                '}';
    }
}
