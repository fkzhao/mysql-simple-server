package cc.fastsoft.protocol;

import io.netty.buffer.ByteBuf;

public class Packet {
    public int payloadLength;
    public byte sequenceId;
    public ByteBuf payload;

    public Packet(int payloadLength, byte sequenceId, ByteBuf payload) {
        this.payloadLength = payloadLength;
        this.sequenceId = sequenceId;
        this.payload = payload;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(byte sequenceId) {
        this.sequenceId = sequenceId;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }
}
