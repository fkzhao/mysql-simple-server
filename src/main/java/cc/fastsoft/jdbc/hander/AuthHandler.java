package cc.fastsoft.jdbc.hander;

import cc.fastsoft.jdbc.protocol.Constants;
import cc.fastsoft.jdbc.protocol.PacketHelper;
import cc.fastsoft.utils.StringUtils;
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
    private final byte[] scramble;

    public AuthHandler(String password, byte[] scramble) {
        this.password = password;
        this.scramble = scramble;
        logger.info("scramble: {}", StringUtils.bytesToHex(scramble));
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
            return new AuthResult(true, 0, null, null, "");
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
            return new AuthResult(true, clientCapabilities, null, null, "");
        }

        logger.debug("Client capabilities: 0x{}, remaining bytes: {}",
                Integer.toHexString(clientCapabilities), payload.readableBytes());

        // Read username (null-terminated string)
        String username = PacketHelper.readNullTerminatedString(payload);
        logger.debug("Username: '{}', remaining bytes: {}", username, payload.readableBytes());

        // Read authentication response
        byte[] clientAuthData = new byte[0];

        if (payload.readableBytes() > 0) {
            // Check CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA flag
            if ((clientCapabilities & Constants.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0) {
                // Use length-encoded integer format
                long authResponseLen = PacketHelper.readLengthEncodedInteger(payload);
                clientAuthData = new byte[(int) authResponseLen];
                if (authResponseLen > 0 && payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(clientAuthData);
                } else if (authResponseLen == 0){
                    clientAuthData = new byte[0];
                } else {
                    logger.error("Not enough bytes for auth request: expected {}, got {}",
                            authResponseLen, payload.readableBytes());
                }
            } else if ((clientCapabilities & Constants.CLIENT_SECURE_CONNECTION) != 0) {
                // CLIENT_SECURE_CONNECTION: use single byte length format
                int authResponseLen = payload.readUnsignedByte();
                clientAuthData = new byte[authResponseLen];
                if (authResponseLen > 0 && payload.readableBytes() >= authResponseLen) {
                    payload.readBytes(clientAuthData);
                } else if (authResponseLen == 0){
                    clientAuthData = new byte[0];
                } else {
                    logger.error("Not enough bytes for auth response: expected {}, got {}",
                            authResponseLen, payload.readableBytes());
                }
            } else {
                // Old format: null-terminated string
                int len = payload.bytesBefore((byte) 0);
                if (len >= 0 && payload.readableBytes() >= len + 1) {
                    clientAuthData = new byte[len];
                    payload.readBytes(clientAuthData);
                    payload.readByte(); // skip null
                } else if (len == -1) {
                    clientAuthData = new byte[payload.readableBytes()];
                    payload.readBytes(clientAuthData);
                }
            }
        }

        logger.debug("Auth response length: {}, remaining bytes: {}",
                clientAuthData.length, payload.readableBytes());

        // Print clientAuthData for debugging
        if (clientAuthData.length > 0) {
            logger.info("Client auth data (hex): {}", StringUtils.bytesToHex(clientAuthData));
        } else {
            logger.info("Client auth data is empty (length=0)");
        }

        // Read database name (if CLIENT_CONNECT_WITH_DB flag is set)
        String database = null;
        if ((clientCapabilities & Constants.CLIENT_CONNECT_WITH_DB) != 0 && payload.readableBytes() > 0) {
            database = PacketHelper.readNullTerminatedString(payload);
            logger.debug("Database: '{}', remaining bytes: {}", database, payload.readableBytes());
        }

        // Read auth plugin name (if CLIENT_PLUGIN_AUTH flag is set)
        String authPluginName = Constants.MYSQL_NATIVE_PASSWORD;
        if ((clientCapabilities & Constants.CLIENT_PLUGIN_AUTH) != 0 && payload.readableBytes() > 0) {
            authPluginName = PacketHelper.readNullTerminatedString(payload);
            logger.debug("Auth plugin: '{}', remaining bytes: {}", authPluginName, payload.readableBytes());
        }

        // Authentication logic (mysql_native_password)
        boolean authOk = false;
        switch (authPluginName) {
            case Constants.MYSQL_NATIVE_PASSWORD:
                authOk = nativeVerify(password, scramble, clientAuthData);
                break;
            case Constants.CACHING_SHA2_PASSWORD:
                authOk = cachingSha2Verify(password, scramble, clientAuthData);
                break;
            default:
                logger.warn("Unsupported auth plugin '{}' for user '{}'", authPluginName, username);
                PacketHelper.sendErrPacket(ctx, "Unsupported auth plugin '" + authPluginName + " for user '"+ username +"'", sequenceId);
                return new AuthResult(false, clientCapabilities, username, database, authPluginName);
        }

        // For debugging: log authentication attempt details
        logger.info("Authentication attempt for user '{}': authOk={}, plugin={}",
                username, authOk, authPluginName);

        // Send response based on authentication result
        if (authOk) {
            // Authentication successful
            PacketHelper.sendOkPacket(ctx, "Authenticated", sequenceId);
            return new AuthResult(true, clientCapabilities, username, database, authPluginName);
        } else {
            // Authentication failed
            logger.warn("Authentication failed for user '{}'", username);
            PacketHelper.sendErrPacket(ctx, "Authentication failed", sequenceId);
            return new AuthResult(false, clientCapabilities, username, database, authPluginName);
        }
    }

    /**
     * Verify password using mysql_native_password method
     */
    private boolean nativeVerify(String password, byte[] nonce, byte[] clientResponse) {

        // Debug input parameters
        logger.debug("nativeVerify - password: '{}', nonce length: {}, clientResponse length: {}",
                password, nonce.length, clientResponse.length);

        // Handle empty password case
        if (password.isEmpty()) {
            boolean result = clientResponse.length == 0;
            logger.debug("Empty password check: {}", result);
            return result;
        }

        // Perform SHA-1 based verification
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] step1 = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
            logger.debug("Step1 (SHA1(password)): {}", StringUtils.bytesToHex(step1));

            byte[] step2 = sha1.digest(step1);
            logger.debug("Step2 (SHA1(SHA1(password))): {}", StringUtils.bytesToHex(step2));

            sha1.update(nonce);
            sha1.update(step2);
            byte[] expected = sha1.digest();
            logger.debug("Step3 (SHA1(nonce + step2)): {}", StringUtils.bytesToHex(expected));

            for (int i = 0; i < 20; i++) expected[i] ^= step1[i];
            logger.debug("Expected result (step3 XOR step1): {}", StringUtils.bytesToHex(expected));
            logger.debug("Client response: {}", StringUtils.bytesToHex(clientResponse));

            boolean result = MessageDigest.isEqual(expected, clientResponse);
            logger.debug("Verification result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error during native password verification", e);
            return false;
        }
    }

    /**
     * Calculate caching_sha2_password hash
     */
    private boolean cachingSha2Verify(String password, byte[] nonce, byte[] clientResponse) {
        if (password.isEmpty()) return clientResponse.length == 1 && clientResponse[0] == 0;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] pwdBytes = password.getBytes(StandardCharsets.UTF_8);

            byte[] stage1 = sha256.digest(pwdBytes);
            byte[] stage2 = sha256.digest(stage1);
            sha256.update(stage2);
            sha256.update(nonce);
            byte[] stage3 = sha256.digest();

            for (int i = 0; i < stage3.length; i++) {
                stage3[i] ^= clientResponse[i];
            }
            return MessageDigest.isEqual(sha256.digest(stage3), stage1);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Authentication result
     */
    public static class AuthResult {
        private final boolean authenticated;
        private final int clientCapabilities;
        private final String username;
        private final String database;
        private final String authPluginName;

        public AuthResult(boolean authenticated, int clientCapabilities, String username, String database, String authPluginName) {
            this.authenticated = authenticated;
            this.clientCapabilities = clientCapabilities;
            this.username = username;
            this.database = database;
            this.authPluginName = authPluginName;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public int getClientCapabilities() {
            return clientCapabilities;
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

