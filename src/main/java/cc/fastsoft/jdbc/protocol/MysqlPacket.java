package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Base class for all MySQL protocol packets
 *
 * MySQL Packet Format:
 * - 3 bytes: payload length
 * - 1 byte: sequence id
 * - n bytes: payload
 */
public abstract class MysqlPacket {

    protected byte sequenceId;

    public MysqlPacket() {
    }

    public MysqlPacket(byte sequenceId) {
        this.sequenceId = sequenceId;
    }

    /**
     * Write packet payload to ByteBuf
     */
    public abstract void write(ByteBuf buffer);

    /**
     * Read packet payload from ByteBuf
     */
    public abstract void read(ByteBuf buffer);

    /**
     * Calculate payload length
     */
    public abstract int getPayloadLength();

    public byte getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(byte sequenceId) {
        this.sequenceId = sequenceId;
    }
}

