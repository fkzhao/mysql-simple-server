package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL Result Set Row Packet
 *
 * Contains actual data values for one row in result set
 *
 * Packet Format:
 * - series of length-encoded strings (one for each column)
 * - NULL values are encoded as 0xFB
 */
public class ResultSetRowPacket extends MysqlPacket {

    private List<String> values;

    public ResultSetRowPacket() {
        this((byte) 0);
    }

    public ResultSetRowPacket(byte sequenceId) {
        super(sequenceId);
        this.values = new ArrayList<>();
    }

    public ResultSetRowPacket(List<String> values) {
        this();
        this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
    }

    @Override
    public void write(ByteBuf buffer) {
        for (String value : values) {
            if (value == null) {
                // NULL value
                buffer.writeByte(0xFB);
            } else {
                // Length-encoded string
                PacketHelper.writeLengthEncodedString(buffer, value);
            }
        }
    }

    @Override
    public void read(ByteBuf buffer) {
        values.clear();

        while (buffer.isReadable()) {
            // Check for NULL value
            buffer.markReaderIndex();
            byte first = buffer.readByte();

            if (first == (byte) 0xFB) {
                // NULL value
                values.add(null);
            } else {
                // Reset and read length-encoded string
                buffer.resetReaderIndex();
                String value = PacketHelper.readLengthEncodedString(buffer);
                values.add(value);
            }
        }
    }

    @Override
    public int getPayloadLength() {
        int length = 0;
        for (String value : values) {
            if (value == null) {
                length += 1; // NULL marker (0xFB)
            } else {
                length += PacketHelper.getLengthEncodedStringLength(value);
            }
        }
        return length;
    }

    // Helper methods

    public void addValue(String value) {
        values.add(value);
    }

    public void addNull() {
        values.add(null);
    }

    public String getValue(int index) {
        if (index >= 0 && index < values.size()) {
            return values.get(index);
        }
        return null;
    }

    public int getColumnCount() {
        return values.size();
    }

    public List<String> getValues() {
        return new ArrayList<>(values);
    }

    public void setValues(List<String> values) {
        this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ResultSetRowPacket{" +
                "sequenceId=" + sequenceId +
                ", values=" + values +
                '}';
    }
}

