package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * MySQL ERR Packet
 *
 * Packet Format:
 * - 1 byte: 0xFF (ERR packet header)
 * - 2 bytes: error code
 * - 1 byte: '#' (SQL state marker)
 * - 5 bytes: SQL state
 * - string[EOF]: error message
 */
public class ErrPacket extends MysqlPacket {

    private static final byte ERR_HEADER = (byte) 0xFF;
    private static final byte SQL_STATE_MARKER = '#';

    private int errorCode;
    private String sqlState;
    private String errorMessage;

    public ErrPacket() {
        this((byte) 0);
    }

    public ErrPacket(byte sequenceId) {
        super(sequenceId);
        this.errorCode = (int) 1105; // ER_UNKNOWN_ERROR
        this.sqlState = "HY000"; // General error
        this.errorMessage = "Unknown error";
    }

    public ErrPacket(int errorCode, String errorMessage) {
        this(errorCode, "HY000", errorMessage);
    }

    public ErrPacket(int errorCode, String sqlState, String errorMessage) {
        this.errorCode = errorCode;
        this.sqlState = sqlState != null ? sqlState : "HY000";
        this.errorMessage = errorMessage != null ? errorMessage : "Unknown error";
    }

    @Override
    public void write(ByteBuf buffer) {
        // ERR header
        buffer.writeByte(ERR_HEADER);

        // Error code (2 bytes, little-endian)
        buffer.writeShortLE(errorCode);

        // SQL state marker
        buffer.writeByte(SQL_STATE_MARKER);

        // SQL state (5 bytes)
        byte[] sqlStateBytes = sqlState.getBytes(StandardCharsets.UTF_8);
        if (sqlStateBytes.length >= 5) {
            buffer.writeBytes(sqlStateBytes, 0, 5);
        } else {
            buffer.writeBytes(sqlStateBytes);
            // Pad with zeros if less than 5 bytes
            for (int i = sqlStateBytes.length; i < 5; i++) {
                buffer.writeByte(0);
            }
        }

        // Error message
        buffer.writeBytes(errorMessage.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void read(ByteBuf buffer) {
        // Read and validate ERR header
        byte header = buffer.readByte();
        if (header != ERR_HEADER) {
            throw new IllegalStateException("Invalid ERR packet header: " + header);
        }

        // Read error code
        errorCode = buffer.readUnsignedShortLE();

        // Read SQL state marker
        byte marker = buffer.readByte();
        if (marker != SQL_STATE_MARKER) {
            throw new IllegalStateException("Invalid SQL state marker: " + marker);
        }

        // Read SQL state (5 bytes)
        byte[] sqlStateBytes = new byte[5];
        buffer.readBytes(sqlStateBytes);
        sqlState = new String(sqlStateBytes, StandardCharsets.UTF_8);

        // Read error message (rest of the packet)
        byte[] messageBytes = new byte[buffer.readableBytes()];
        buffer.readBytes(messageBytes);
        errorMessage = new String(messageBytes, StandardCharsets.UTF_8);
    }

    @Override
    public int getPayloadLength() {
        int length = 1; // ERR header
        length += 2; // error code
        length += 1; // SQL state marker
        length += 5; // SQL state
        length += errorMessage.getBytes(StandardCharsets.UTF_8).length;
        return length;
    }

    // Getters and Setters

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    public void setSqlState(String sqlState) {
        this.sqlState = sqlState;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ErrPacket{" +
                "sequenceId=" + sequenceId +
                ", errorCode=" + errorCode +
                ", sqlState='" + sqlState + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
