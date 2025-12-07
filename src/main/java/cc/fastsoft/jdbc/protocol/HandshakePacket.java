package cc.fastsoft.jdbc.protocol;

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

    private static final int CLIENT_LONG_PASSWORD                    = 0x00000001;
    private static final int CLIENT_LONG_FLAG                        = 0x00000004;
    private static final int CLIENT_PROTOCOL_41                      = 0x00000200;
    private static final int CLIENT_TRANSACTIONS                     = 0x00002000;
    private static final int CLIENT_SECURE_CONNECTION                = 0x00008000;
    private static final int CLIENT_PLUGIN_AUTH                      = 0x00080000;
    private static final int CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA   = 0x00200000;
    private static final int CLIENT_DEPRECATE_EOF                    = 0x01000000;

    private static final int SERVER_STATUS_AUTOCOMMIT                = 0x0002;
    private byte protocolVersion;
    private String serverVersion;
    private int connectionId;
    private byte[] scramble; // 20 bytes auth challenge
    private int capabilityFlags;
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
        this.capabilityFlags = CLIENT_LONG_PASSWORD |
                CLIENT_LONG_FLAG |
                CLIENT_PROTOCOL_41 |
                CLIENT_TRANSACTIONS |
                CLIENT_SECURE_CONNECTION |
                CLIENT_PLUGIN_AUTH |
                CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA |
                CLIENT_DEPRECATE_EOF;
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
        buffer.writeBytes((serverVersion + "\0").getBytes(StandardCharsets.US_ASCII));

        // Connection id
        buffer.writeIntLE(connectionId);

        // Auth-plugin-data-part-1 (first 8 bytes of scramble)
        buffer.writeBytes(scramble, 0, 8);

        // Filler
        buffer.writeByte(0x00);

        // Capability flags (lower 2 bytes)
        buffer.writeShortLE(capabilityFlags & 0xFFFF);

        // Character set
        buffer.writeByte(characterSet);

        // Status flags
        buffer.writeShortLE(statusFlags);

        // Capability flags (upper 2 bytes)
        buffer.writeShortLE((capabilityFlags >>> 16) & 0xFFFF);

        // Auth plugin data length (scramble length + 1)
        buffer.writeByte(21);

        // Reserved (10 bytes of 0x00)
        buffer.writeBytes(new byte[10]);

        // Auth-plugin-data-part-2 (remaining 12 bytes of scramble + terminating 0x00)
        buffer.writeBytes(scramble, 8, 12);
        buffer.writeByte(0x00);

        // Auth plugin name (null-terminated) - MUST always write for MySQL protocol
        if ((capabilityFlags & CLIENT_PLUGIN_AUTH) != 0) {
            buffer.writeBytes(authPluginName.getBytes(StandardCharsets.US_ASCII));
            buffer.writeByte(0x00);
        }
    }

    @Override
    public void read(ByteBuf buffer) {
        // Protocol version
        protocolVersion = buffer.readByte();

        // Server version (null-terminated)
        serverVersion = PacketHelper.readNullTerminatedString(buffer);

        // Connection id
        connectionId = buffer.readIntLE();

        // Auth-plugin-data-part-1 (8 bytes)
        buffer.readBytes(scramble, 0, 8);

        // Filler
        buffer.readByte();

        // Capability flags (lower 2 bytes)
        int capLower = buffer.readUnsignedShortLE();

        // Character set
        characterSet = buffer.readByte();

        // Status flags
        statusFlags = buffer.readUnsignedShortLE();

        // Capability flags (upper 2 bytes)
        int capUpper = buffer.readUnsignedShortLE();
        capabilityFlags = capLower | (capUpper << 16);

        // Auth plugin data length
        int authDataLen = buffer.readUnsignedByte();

        // Reserved (10 bytes)
        buffer.skipBytes(10);

        // Auth-plugin-data-part-2
        int part2Len = Math.max(13, authDataLen - 8);
        buffer.readBytes(scramble, 8, Math.min(12, part2Len - 1));
        buffer.skipBytes(part2Len - 12); // Skip remaining including terminating byte

        // Auth plugin name
        if (buffer.isReadable()) {
            authPluginName = PacketHelper.readNullTerminatedString(buffer);
        }
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

    public int getCapabilityFlags() {
        return capabilityFlags;
    }

    public void setCapabilityFlags(int capabilityFlags) {
        this.capabilityFlags = capabilityFlags;
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
                ", capabilityFlags=" + capabilityFlags +
                ", characterSet=" + characterSet +
                ", statusFlags=" + statusFlags +
                ", authPluginName='" + authPluginName + '\'' +
                '}';
    }
}
