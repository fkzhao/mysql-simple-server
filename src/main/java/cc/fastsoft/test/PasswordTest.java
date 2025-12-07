package cc.fastsoft.test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordTest {
    public static void main(String[] args) throws Exception {
        String password = "123456";
        String scrambleHex = "86cc478975e7a57fe880e54ffa117007d58fa871";
        String clientResponseHex = "7d10fa5a5be30389954f5fab4028061c4c38f7fa";

        byte[] scramble = hexToBytes(scrambleHex);
        byte[] clientResponse = hexToBytes(clientResponseHex);

        System.out.println("Password: " + password);
        System.out.println("Scramble: " + scrambleHex);
        System.out.println("Client Response: " + clientResponseHex);
        System.out.println();

        // Server calculation
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] step1 = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
        System.out.println("Step1 (SHA1(password)): " + bytesToHex(step1));

        byte[] step2 = sha1.digest(step1);
        System.out.println("Step2 (SHA1(SHA1(password))): " + bytesToHex(step2));

        sha1.update(scramble);
        sha1.update(step2);
        byte[] step3 = sha1.digest();
        System.out.println("Step3 (SHA1(scramble + step2)): " + bytesToHex(step3));

        byte[] expected = new byte[20];
        for (int i = 0; i < 20; i++) {
            expected[i] = (byte)(step3[i] ^ step1[i]);
        }
        System.out.println("Expected (step3 XOR step1): " + bytesToHex(expected));
        System.out.println();

        // Client should calculate: SHA1(password) XOR SHA1(scramble + SHA1(SHA1(password)))
        // Let's verify this is what client sent
        System.out.println("Match: " + MessageDigest.isEqual(expected, clientResponse));
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

