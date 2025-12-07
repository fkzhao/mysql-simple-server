package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * MySQL Command Packet (Client to Server)
 *
 * Packet Format:
 * - 1 byte: command type
 * - n bytes: command arguments (varies by command type)
 *
 * Common command types:
 * - COM_QUIT (0x01)
 * - COM_INIT_DB (0x02)
 * - COM_QUERY (0x03)
 * - COM_FIELD_LIST (0x04)
 * - COM_PING (0x0E)
 */
public class CommandPacket extends MysqlPacket {

    private byte command;
    private byte[] arguments;

    public CommandPacket() {
        this((byte) 0);
    }

    public CommandPacket(byte sequenceId) {
        super(sequenceId);
        this.command = (byte) Constants.COM_QUERY;
        this.arguments = new byte[0];
    }

    public CommandPacket(byte command, String argument) {
        this();
        this.command = command;
        if (argument != null) {
            this.arguments = argument.getBytes(StandardCharsets.UTF_8);
        }
    }

    public CommandPacket(byte command, byte[] arguments) {
        this();
        this.command = command;
        this.arguments = arguments != null ? arguments : new byte[0];
    }

    @Override
    public void write(ByteBuf buffer) {
        // Command type
        buffer.writeByte(command);

        // Command arguments
        if (arguments != null && arguments.length > 0) {
            buffer.writeBytes(arguments);
        }
    }

    @Override
    public void read(ByteBuf buffer) {
        // Command type
        command = buffer.readByte();

        // Command arguments (rest of packet)
        if (buffer.isReadable()) {
            arguments = new byte[buffer.readableBytes()];
            buffer.readBytes(arguments);
        } else {
            arguments = new byte[0];
        }
    }

    @Override
    public int getPayloadLength() {
        return 1 + (arguments != null ? arguments.length : 0);
    }

    // Helper methods

    public String getArgumentAsString() {
        if (arguments != null && arguments.length > 0) {
            return new String(arguments, StandardCharsets.UTF_8);
        }
        return "";
    }

    public boolean isQuery() {
        return command == Constants.COM_QUERY;
    }

    public boolean isQuit() {
        return command == Constants.COM_QUIT;
    }

    public boolean isPing() {
        return command == Constants.COM_PING;
    }

    public boolean isInitDb() {
        return command == Constants.COM_INIT_DB;
    }

    // Getters and Setters

    public byte getCommand() {
        return command;
    }

    public void setCommand(byte command) {
        this.command = command;
    }

    public byte[] getArguments() {
        return arguments;
    }

    public void setArguments(byte[] arguments) {
        this.arguments = arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments != null ? arguments.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    @Override
    public String toString() {
        return "CommandPacket{" +
                "sequenceId=" + sequenceId +
                ", command=" + String.format("0x%02X", command) +
                ", argumentLength=" + (arguments != null ? arguments.length : 0) +
                '}';
    }
}

