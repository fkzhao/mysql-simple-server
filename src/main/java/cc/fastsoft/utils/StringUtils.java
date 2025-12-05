package cc.fastsoft.utils;

public class StringUtils {

    /**
     * Convert byte array to hex string for debugging
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
