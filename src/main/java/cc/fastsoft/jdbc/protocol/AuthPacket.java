package cc.fastsoft.jdbc.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * MySQL Handshake Response Packet (Client to Server)
 * Also known as Authentication Packet
 *
 * Packet Format:
 * - 4 bytes: capability flags
 * - 4 bytes: max packet size
 * - 1 byte: character set
 * - 23 bytes: reserved (all 0x00)
 * - string[NUL]: username
 * - length-encoded string: auth response (password hash)
 * - string[NUL]: database name (if CLIENT_CONNECT_WITH_DB)
 * - string[NUL]: auth plugin name (if CLIENT_PLUGIN_AUTH)
 */
public class AuthPacket extends MysqlPacket {

    private int capabilityFlags;
    private int maxPacketSize;
    private byte characterSet;
    private String username;
    private byte[] authResponse;
    private String database;
    private String authPluginName;

    public AuthPacket() {
        this((byte) 0);
    }

    public AuthPacket(byte sequenceId) {
        super(sequenceId);
        this.capabilityFlags = 0;
        this.maxPacketSize = 0xFFFFFF; // 16MB - 1
        this.characterSet = (byte) 33; // utf8_general_ci
        this.username = "";
        this.authResponse = new byte[0];
        this.database = null;
        this.authPluginName = Constants.MYSQL_NATIVE_PASSWORD;
    }

    @Override
    public void write(ByteBuf buffer) {
        // Capability flags
        buffer.writeIntLE(capabilityFlags);

        // Max packet size
        buffer.writeIntLE(maxPacketSize);

        // Character set
        buffer.writeByte(characterSet);

        // Reserved (23 bytes of 0x00)
        buffer.writeBytes(new byte[23]);

        // Username (null-terminated)
        buffer.writeBytes((username + "\0").getBytes(StandardCharsets.UTF_8));

        // Auth response
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0) {
            // Length-encoded auth response
            PacketHelper.writeLengthEncodedInteger(buffer, authResponse.length);
            buffer.writeBytes(authResponse);
        } else if ((capabilityFlags & Constants.CLIENT_SECURE_CONNECTION) != 0) {
            // 1 byte length + auth response
            buffer.writeByte(authResponse.length);
            buffer.writeBytes(authResponse);
        } else {
            // Legacy: null-terminated auth response
            buffer.writeBytes(authResponse);
            buffer.writeByte(0x00);
        }

        // Database name (if CLIENT_CONNECT_WITH_DB)
        if ((capabilityFlags & Constants.CLIENT_CONNECT_WITH_DB) != 0 && database != null) {
            buffer.writeBytes((database + "\0").getBytes(StandardCharsets.UTF_8));
        }

        // Auth plugin name (if CLIENT_PLUGIN_AUTH)
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH) != 0 && authPluginName != null) {
            buffer.writeBytes((authPluginName + "\0").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void read(ByteBuf buffer) {
        // Capability flags
        capabilityFlags = buffer.readIntLE();

        // Max packet size
        maxPacketSize = buffer.readIntLE();

        // Character set
        characterSet = buffer.readByte();

        // Reserved (23 bytes)
        buffer.skipBytes(23);

        // Username (null-terminated)
        username = PacketHelper.readNullTerminatedString(buffer);

        // Auth response
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0) {
            // Length-encoded auth response
            long authResponseLen = PacketHelper.readLengthEncodedInteger(buffer);
            if (authResponseLen > 0 && buffer.readableBytes() >= authResponseLen) {
                authResponse = new byte[(int) authResponseLen];
                buffer.readBytes(authResponse);
            }
        } else if ((capabilityFlags & Constants.CLIENT_SECURE_CONNECTION) != 0) {
            // 1 byte length + auth response
            int authResponseLen = buffer.readUnsignedByte();
            if (authResponseLen > 0 && buffer.readableBytes() >= authResponseLen) {
                authResponse = new byte[authResponseLen];
                buffer.readBytes(authResponse);
            }
        } else {
            // Legacy: read until 0x00 or end of buffer
            int startIdx = buffer.readerIndex();
            int nullIdx = buffer.indexOf(startIdx, buffer.writerIndex(), (byte) 0x00);
            if (nullIdx >= 0) {
                authResponse = new byte[nullIdx - startIdx];
                buffer.readBytes(authResponse);
                buffer.readByte(); // skip null terminator
            } else {
                authResponse = new byte[buffer.readableBytes()];
                buffer.readBytes(authResponse);
            }
        }

        // Database name (if CLIENT_CONNECT_WITH_DB)
        if ((capabilityFlags & Constants.CLIENT_CONNECT_WITH_DB) != 0 && buffer.isReadable()) {
            database = PacketHelper.readNullTerminatedString(buffer);
        }

        // Auth plugin name (if CLIENT_PLUGIN_AUTH)
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH) != 0 && buffer.isReadable()) {
            authPluginName = PacketHelper.readNullTerminatedString(buffer);
        }
    }

    @Override
    public int getPayloadLength() {
        int length = 4; // capability flags
        length += 4; // max packet size
        length += 1; // character set
        length += 23; // reserved
        length += username.getBytes(StandardCharsets.UTF_8).length + 1; // username + null

        // Auth response
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0) {
            length += PacketHelper.getLengthEncodedIntegerLength(authResponse.length);
            length += authResponse.length;
        } else if ((capabilityFlags & Constants.CLIENT_SECURE_CONNECTION) != 0) {
            length += 1; // length byte
            length += authResponse.length;
        } else {
            length += authResponse.length + 1; // auth response + null
        }

        // Database
        if ((capabilityFlags & Constants.CLIENT_CONNECT_WITH_DB) != 0 && database != null) {
            length += database.getBytes(StandardCharsets.UTF_8).length + 1;
        }

        // Auth plugin name
        if ((capabilityFlags & Constants.CLIENT_PLUGIN_AUTH) != 0 && authPluginName != null) {
            length += authPluginName.getBytes(StandardCharsets.UTF_8).length + 1;
        }

        return length;
    }

    // Getters and Setters

    public int getCapabilityFlags() {
        return capabilityFlags;
    }

    public void setCapabilityFlags(int capabilityFlags) {
        this.capabilityFlags = capabilityFlags;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public byte getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(byte characterSet) {
        this.characterSet = characterSet;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getAuthResponse() {
        return authResponse;
    }

    public void setAuthResponse(byte[] authResponse) {
        this.authResponse = authResponse;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getAuthPluginName() {
        return authPluginName;
    }

    public void setAuthPluginName(String authPluginName) {
        this.authPluginName = authPluginName;
    }

    @Override
    public String toString() {
        return "AuthPacket{" +
                "sequenceId=" + sequenceId +
                ", capabilityFlags=" + capabilityFlags +
                ", maxPacketSize=" + maxPacketSize +
                ", characterSet=" + characterSet +
                ", username='" + username + '\'' +
                ", authResponseLength=" + (authResponse != null ? authResponse.length : 0) +
                ", database='" + database + '\'' +
                ", authPluginName='" + authPluginName + '\'' +
                '}';
    }
}

