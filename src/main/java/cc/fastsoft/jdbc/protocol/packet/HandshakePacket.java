package cc.fastsoft.jdbc.protocol.packet;

import cc.fastsoft.jdbc.protocol.Constants;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * MySQL Initial Handshake Packet (Server to Client)
 *
 * Packet Format:
 * - 1 byte: protocol version
 * - string[NUL]: server version
 * - 4 bytes: connection id
 * - 8 bytes: auth-plugin-data-part-1 (first part of scramble)
 * - 1 byte: filler (0x00)
 * - 2 bytes: capability flags (lower 2 bytes)
 * - 1 byte: character set
 * - 2 bytes: status flags
 * - 2 bytes: capability flags (upper 2 bytes)
 * - 1 byte: auth plugin data length
 * - 10 bytes: reserved (all 0x00)
 * - string[12/13]: auth-plugin-data-part-2 (rest of scramble)
 * - string[NUL]: auth plugin name
 */
public class HandshakePacket extends MysqlPacket {

    private static final byte PROTOCOL_VERSION = 10; // MySQL 4.1+
    private byte protocolVersion;
    private String serverVersion;
    private int connectionId;
    private byte[] scramble; // 20 bytes auth challenge
    private MysqlCapability capability;
    private byte characterSet;
    private int statusFlags;
    private String authPluginName;

    public HandshakePacket() {
        this((byte) 0);
    }

    public HandshakePacket(byte sequenceId) {
        super(sequenceId);
        this.protocolVersion = PROTOCOL_VERSION;
        this.serverVersion = "5.7.0-mock";
        this.connectionId = 1;
        this.scramble = new byte[20];
        this.capability = MysqlCapability.DEFAULT_CAPABILITY;
        this.characterSet = (byte) 33; // utf8_general_ci
        this.statusFlags = (int) Constants.SERVER_STATUS_AUTOCOMMIT;
        this.authPluginName = Constants.MYSQL_NATIVE_PASSWORD;
    }

    public HandshakePacket(int connectionId, byte[] scramble, String serverVersion) {
        this();
        this.connectionId = connectionId;
        if (scramble != null && scramble.length == 20) {
            this.scramble = scramble;
        }
        if (serverVersion != null) {
            this.serverVersion = serverVersion;
        }
    }

    @Override
    public void write(ByteBuf buffer) {
        // Protocol version
        buffer.writeByte(protocolVersion);

        // Server version (null-terminated)
        buffer.writeBytes((serverVersion + "\0").getBytes(StandardCharsets.UTF_8));

        // Connection id
        buffer.writeIntLE(connectionId);

        // Auth-plugin-data-part-1 (first 8 bytes of scramble)
        buffer.writeBytes(scramble, 0, 8);

        // Filler
        buffer.writeByte(0x00);

        // Capability flags (lower 2 bytes)
        buffer.writeShortLE(capability.getFlags() & 0xFFFF);

        // Character set
        buffer.writeByte(characterSet);

        // Status flags
        buffer.writeShortLE(statusFlags);

        // Capability flags (upper 2 bytes)
        buffer.writeShortLE(capability.getFlags() >> 16);

        // Auth plugin data length (scramble length + 1)
        if (capability.isPluginAuth()) {
            buffer.writeByte(scramble.length + 1); // 1 byte is '\0'
        } else {
            buffer.writeByte(0x00);
        }

        // Reserved (10 bytes of 0x00)
        buffer.writeBytes(new byte[10]);

        // Auth-plugin-data-part-2 (remaining 12 bytes of scramble + terminating 0x00)
        if (capability.isSecureConnection()) {
            buffer.writeBytes(scramble, 8, 12);
            buffer.writeByte(0x00);
        }

        // Auth plugin name (null-terminated) - MUST always write for MySQL protocol
        if (capability.isPluginAuth()) {
            buffer.writeBytes(authPluginName.getBytes(StandardCharsets.UTF_8));
            buffer.writeByte(0x00);
        }
    }

    @Override
    public void read(ByteBuf buffer) {

    }

    @Override
    public int getPayloadLength() {
        int length = 1; // protocol version
        length += serverVersion.getBytes(StandardCharsets.US_ASCII).length + 1; // server version + null
        length += 4; // connection id
        length += 8; // auth-plugin-data-part-1
        length += 1; // filler
        length += 2; // capability flags lower
        length += 1; // character set
        length += 2; // status flags
        length += 2; // capability flags upper
        length += 1; // auth plugin data length
        length += 10; // reserved
        length += 13; // auth-plugin-data-part-2 (12 bytes + terminating 0x00)
        length += authPluginName.getBytes(StandardCharsets.US_ASCII).length + 1; // auth plugin name + null
        return length;
    }

    // Getters and Setters

    public byte getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(byte protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public byte[] getScramble() {
        return scramble;
    }

    public void setScramble(byte[] scramble) {
        this.scramble = scramble;
    }
    public MysqlCapability getCapability() {
        return capability;
    }
    public void setCapability(MysqlCapability capability) {
        this.capability = capability;
    }

    public byte getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(byte characterSet) {
        this.characterSet = characterSet;
    }

    public int getStatusFlags() {
        return statusFlags;
    }

    public void setStatusFlags(int statusFlags) {
        this.statusFlags = statusFlags;
    }

    public String getAuthPluginName() {
        return authPluginName;
    }

    public void setAuthPluginName(String authPluginName) {
        this.authPluginName = authPluginName;
    }

    @Override
    public String toString() {
        return "HandshakePacket{" +
                "sequenceId=" + sequenceId +
                ", protocolVersion=" + protocolVersion +
                ", serverVersion='" + serverVersion + '\'' +
                ", connectionId=" + connectionId +
                ", capabilityFlags=" + capability.getFlags() +
                ", characterSet=" + characterSet +
                ", statusFlags=" + statusFlags +
                ", authPluginName='" + authPluginName + '\'' +
                '}';
    }
}
