package cc.fastsoft.jdbc.protocol;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MysqlPassword {
    private static final Logger logger = LoggerFactory.getLogger(MysqlPassword.class);
    public static final byte[] EMPTY_PASSWORD = new byte[0];
    public static final int SCRAMBLE_LENGTH = 20;
    public static final int SCRAMBLE_LENGTH_HEX_LENGTH = 2 * SCRAMBLE_LENGTH + 1;
    public static final byte PVERSION41_CHAR = '*';
    private static final byte[] DIG_VEC_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final Random random = new SecureRandom();
    private static final Set<Character> complexCharSet;
    public static final int MIN_PASSWORD_LEN = 8;

    static {
        complexCharSet = "~!@#$%^&*()_+|<>,.?/:;'[]{}".chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
    }

    public static byte[] createRandomString(int len) {
        byte[] bytes = new byte[len];
        random.nextBytes(bytes);
        // NOTE: MySQL challenge string can't contain 0.
        for (int i = 0; i < len; ++i) {
            if (!((bytes[i] >= 'a' && bytes[i] <= 'z')
                    || (bytes[i] >= 'A' && bytes[i] <= 'Z'))) {
                bytes[i] = (byte) ('a' + (bytes[i] % 26));
            }
        }
        return bytes;
    }

    private static byte[] xorCrypt(byte[] s1, byte[] s2) {
        if (s1.length != s2.length) {
            return null;
        }
        byte[] res = new byte[s1.length];
        for (int i = 0; i < s1.length; ++i) {
            res[i] = (byte) (s1[i] ^ s2[i]);

        }
        return res;
    }

    // Check that scrambled message corresponds to the password; the function
    // is used by server to check that received reply is authentic.
    // This function does not check lengths of given strings: message must be
    // null-terminated, reply and hash_stage2 must be at least SHA1_HASH_SIZE
    // long (if not, something fishy is going on).
    // SYNOPSIS
    //   check_scramble_sha1()
    //   scramble     clients' reply, presumably produced by scramble()
    //   message      original random string, previously sent to client
    //                (presumably second argument of scramble()), must be
    //                exactly SCRAMBLE_LENGTH long and NULL-terminated.
    //   hash_stage2  hex2octet-decoded database entry
    //   All params are IN.
    //
    // RETURN VALUE
    //   0  password is correct
    //   !0  password is invalid
    public static boolean checkScramble(byte[] scramble, byte[] message, byte[] hashStage2) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            logger.warn("No SHA-1 Algorithm when compute password.");
            return false;
        }
        // compute result1: XOR(scramble, SHA-1 (public_seed + hashStage2))
        md.update(message);
        md.update(hashStage2);
        byte[] hashStage1 = xorCrypt(md.digest(), scramble);

        // compute result2: SHA-1(result1)
        md.reset();
        md.update(hashStage1);
        byte[] candidateHash2 = md.digest();
        // compare result2 and hashStage2 using MessageDigest.isEqual()
        return MessageDigest.isEqual(candidateHash2, hashStage2);
    }

    // MySQL client use this function to form scramble password
    // password: plaintext password
    public static byte[] scramble(byte[] seed, String password) {
        byte[] scramblePassword = null;
        try {
            byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashStage1 = md.digest(passBytes);
            md.reset();
            byte[] hashStage2 = md.digest(hashStage1);
            md.reset();
            md.update(seed);
            scramblePassword = xorCrypt(hashStage1, md.digest(hashStage2));
        } catch (Exception e) {
            // no UTF-8 character set
            logger.warn("No UTF-8 character set when compute password. {}", e);
        }

        return scramblePassword;
    }

    // Convert plaintext password into the corresponding 2-staged hashed password
    // Used for users to set password
    private static byte[] twoStageHash(String password) {
        try {
            byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashStage1 = md.digest(passBytes);
            md.reset();

            return md.digest(hashStage1);
        } catch (Exception e) {
            logger.warn("Two Stage Hash has exception:{}", e);
        }

        return null;
    }

    // covert octet 'from' to hex 'to'
    // NOTE: this function assume that to buffer is enough
    private static void octetToHexSafe(byte[] to, int toOff, byte[] from) {
        int j = toOff;
        for (int i = 0; i < from.length; i++) {
            int val = from[i] & 0xff;
            to[j++] = DIG_VEC_UPPER[val >> 4];
            to[j++] = DIG_VEC_UPPER[val & 0x0f];
        }
    }

    private static int fromByte(int b) {
        return (b >= '0' && b <= '9') ? b - '0'
                : (b >= 'A' && b <= 'F') ? b - 'A' + 10 : b - 'a' + 10;
    }

    // covert hex 'from' to octet 'to'
    // fromOff: offset of 'from' to covert, there is no pointer in JAVA
    // NOTE: this function assume that to buffer is enough
    private static void hexToOctetSafe(byte[] to, byte[] from, int fromOff) {
        int j = 0;
        for (int i = fromOff; i < from.length; i++) {
            int val = fromByte(from[i++] & 0xff);
            to[j++] = ((byte) ((val << 4) + fromByte(from[i] & 0xff)));
        }
    }

    // Make password which stored in palo meta from plain text
    public static byte[] makeScrambledPassword(String plainPasswd) {
        if (StringUtils.isBlank(plainPasswd)) {
            return EMPTY_PASSWORD;
        }

        byte[] hashStage2 = twoStageHash(plainPasswd);
        byte[] passwd = new byte[SCRAMBLE_LENGTH_HEX_LENGTH];
        passwd[0] = (PVERSION41_CHAR);
        assert hashStage2 != null;
        octetToHexSafe(passwd, 1, hashStage2);
        return passwd;
    }

    // Convert scrambled password from ascii hex string to binary form.
    public static byte[] getSaltFromPassword(byte[] password) {
        if (password == null || password.length == 0) {
            return EMPTY_PASSWORD;
        }
        byte[] hashStage2 = new byte[SCRAMBLE_LENGTH];
        hexToOctetSafe(hashStage2, password, 1);
        return hashStage2;
    }

    public static boolean checkPlainPass(byte[] scrambledPass, String plainPass) {
        byte[] pass = makeScrambledPassword(plainPass);
        if (pass.length != scrambledPass.length) {
            return false;
        }
        for (int i = 0; i < pass.length; ++i) {
            if (pass[i] != scrambledPass[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] checkPassword(String passwdString) throws Exception {
        if (StringUtils.isBlank(passwdString)) {
            return EMPTY_PASSWORD;
        }

        byte[] passwd = null;
        try {
            passwdString = passwdString.toUpperCase();
            passwd = passwdString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        if (passwd.length != SCRAMBLE_LENGTH_HEX_LENGTH || passwd[0] != PVERSION41_CHAR) {
            throw new Exception("Wrong password format");
        }

        for (int i = 1; i < passwd.length; ++i) {
            if (!((passwd[i] <= '9' && passwd[i] >= '0') || passwd[i] >= 'A' && passwd[i] <= 'F')) {
               throw new Exception("Wrong password format");
            }
        }

        return passwd;
    }

    public static void validatePlainPassword(long validaPolicy, String text) throws Exception {
        if (StringUtils.isBlank(text) || text.length() < MIN_PASSWORD_LEN) {
            throw new Exception(
                    "Violate password validation policy: STRONG. The password must be at least 8 characters");
        }

        int i = 0;
        if (text.chars().anyMatch(Character::isDigit)) {
            i++;
        }
        if (text.chars().anyMatch(Character::isLowerCase)) {
            i++;
        }
        if (text.chars().anyMatch(Character::isUpperCase)) {
            i++;
        }
        if (text.chars().anyMatch(c -> complexCharSet.contains((char) c))) {
            i++;
        }
        if (i < 3) {
            throw new Exception(
                    "Violate password validation policy: STRONG. The password must contain at least 3 types of "
                            + "numbers, uppercase letters, lowercase letters and special characters.");
        }
    }

    public static boolean verifyPassword(String password, byte[] nonce, byte[] clientResponse, String authPluginName) {
        if (authPluginName.equalsIgnoreCase(Constants.MYSQL_NATIVE_PASSWORD)) {
            return nativeVerify(password, nonce, clientResponse);
        }
        throw new IllegalArgumentException("Unsupported auth plugin name: " + authPluginName);
    }

    /**
     * Verify password using mysql_native_password method
     */
    public static boolean nativeVerify(String password, byte[] nonce, byte[] clientResponse) {

        // Debug input parameters
        logger.debug("nativeVerify - password: '{}', nonce length: {} (hex):{}, clientResponse length: {} (hex): {}",
                password, nonce.length, cc.fastsoft.utils.StringUtils.bytesToHex(nonce), clientResponse.length, cc.fastsoft.utils.StringUtils.bytesToHex(clientResponse));

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
            logger.debug("Step1 (SHA1(password)): {}", cc.fastsoft.utils.StringUtils.bytesToHex(step1));

            byte[] step2 = sha1.digest(step1);
            logger.debug("Step2 (SHA1(SHA1(password))): {}", cc.fastsoft.utils.StringUtils.bytesToHex(step2));

            sha1.update(nonce);
            sha1.update(step2);
            byte[] expected = sha1.digest();
            logger.debug("Step3 (SHA1(nonce + step2)): {}", cc.fastsoft.utils.StringUtils.bytesToHex(expected));

            for (int i = 0; i < 20; i++) expected[i] ^= step1[i];
            logger.debug("Expected result (step3 XOR step1): {}", cc.fastsoft.utils.StringUtils.bytesToHex(expected));
            logger.debug("Client response: {}", cc.fastsoft.utils.StringUtils.bytesToHex(clientResponse));

            boolean result = MessageDigest.isEqual(expected, clientResponse);
            logger.debug("Verification result: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error during native password verification", e);
            return false;
        }
    }
}
