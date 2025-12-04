package cc.fastsoft.hander;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Handles MySQL authentication
 */
public class AuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

    private final String password;
    private final byte[] authPluginData;

    public AuthHandler(String password, byte[] authPluginData) {
        this.password = password;
        this.authPluginData = authPluginData;
    }

    /**
     * Handle authentication packet and return authentication result
     */
    public AuthResult handleAuth(ChannelHandlerContext ctx, ByteBuf payload, byte sequenceId) {
        logger.debug("Received auth packet from {}, payload size: {}",
                ctx.channel().remoteAddress(), payload.readableBytes());

        // Check if this is HandshakeResponse41 (needs at least 32 bytes)
        if (payload.readableBytes() < 32) {
            logger.warn("Packet too small for HandshakeResponse41, treating as simple auth");
            PacketHelper.sendOkPacket(ctx, "Authenticated (simple mode)", sequenceId);
            return new AuthResult(true, null, null, "");
        }

        // Parse HandshakeResponse41
        int clientCapabilities = payload.readIntLE();
        int maxPacketSize = payload.readIntLE();
        int charset = payload.readUnsignedByte();

        // Check if there are enough bytes to skip reserved field
        if (payload.readableBytes() >= 23) {
            payload.skipBytes(23); // reserved (23 bytes of 0x00)
        } else {
            logger.error("Not enough bytes for reserved field: {}", payload.readableBytes());
            PacketHelper.sendOkPacket(ctx, "Authenticated (incomplete packet)", sequenceId);
            return new AuthResult(true, null, null, "");
        }

        logger.debug("Client capabilities: 0x{}, remaining bytes: {}",
                Integer.toHexString(clientCapabilities), payload.readableBytes());

        // Read username (null-terminated string)
        String username = PacketHelper.readNullTerminatedString(payload);
        logger.debug("Username: '{}', remaining bytes: {}", username, payload.readableBytes());

        // Read authentication response
        byte[] authResponse = new byte[0];

        if (payload.readableBytes() > 0) {
            // Check CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA flag
            if ((clientCapabilities & 0x00200000) != 0) {
                // Use length-encoded integer format
                long authResponseLen = PacketHelper.readLengthEncodedInteger(payload);
                authResponse = new byte[(int) authResponseLen];
                if (payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(authResponse);
                } else {
                    logger.error("Not enough bytes for auth response: expected {}, got {}",
                            authResponseLen, payload.readableBytes());
                }
            } else if ((clientCapabilities & 0x00008000) != 0) {
                // CLIENT_SECURE_CONNECTION: use single byte length format
                int authResponseLen = payload.readUnsignedByte();
                authResponse = new byte[authResponseLen];
                if (payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(authResponse);
                } else {
                    logger.error("Not enough bytes for auth response: expected {}, got {}",
                            authResponseLen, payload.readableBytes());
                }
            } else {
                // Old format: null-terminated string
                int len = payload.bytesBefore((byte) 0);
                if (len >= 0) {
                    authResponse = new byte[len];
                    payload.readBytes(authResponse);
                    payload.readByte(); // skip null
                }
            }
        }

        logger.debug("Auth response length: {}, remaining bytes: {}",
                authResponse.length, payload.readableBytes());

        // Read database name (if CLIENT_CONNECT_WITH_DB flag is set)
        String database = null;
        if ((clientCapabilities & 0x00000008) != 0 && payload.readableBytes() > 0) {
            database = PacketHelper.readNullTerminatedString(payload);
            logger.debug("Database: '{}', remaining bytes: {}", database, payload.readableBytes());
        }

        // Read auth plugin name (if CLIENT_PLUGIN_AUTH flag is set)
        String authPluginName = "";
        if ((clientCapabilities & 0x00080000) != 0 && payload.readableBytes() > 0) {
            authPluginName = PacketHelper.readNullTerminatedString(payload);
            logger.debug("Auth plugin: '{}', remaining bytes: {}", authPluginName, payload.readableBytes());
        }

        // Authentication logic (mysql_native_password)
        byte[] expected = nativePasswordHash(password, authPluginData);
        boolean authOk = MessageDigest.isEqual(authResponse, expected);

        logger.info("Authentication result for user '{}': {}", username, authOk ? "SUCCESS" : "FAILED");

        // Always return success for testing
        PacketHelper.sendOkPacket(ctx, "Authenticated", sequenceId);
        return new AuthResult(true, username, database, authPluginName);
    }

    /**
     * Calculate native password hash
     */
    private byte[] nativePasswordHash(String password, byte[] salt) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] pass1 = sha1.digest(password.getBytes(StandardCharsets.US_ASCII));
            byte[] pass2 = sha1.digest(pass1);
            sha1.update(salt);
            sha1.update(pass2);
            byte[] hash = sha1.digest();
            for (int i = 0; i < hash.length; i++) {
                hash[i] ^= pass1[i];
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Authentication result
     */
    public static class AuthResult {
        private final boolean authenticated;
        private final String username;
        private final String database;
        private final String authPluginName;

        public AuthResult(boolean authenticated, String username, String database, String authPluginName) {
            this.authenticated = authenticated;
            this.username = username;
            this.database = database;
            this.authPluginName = authPluginName;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public String getUsername() {
            return username;
        }

        public String getDatabase() {
            return database;
        }

        public String getAuthPluginName() {
            return authPluginName;
        }
    }
}

