package cc.fastsoft.test;

import java.io.*;
import java.net.Socket;

public class HandshakeTest {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 2883)) {
            InputStream in = socket.getInputStream();

            // Read handshake packet
            // MySQL packet format: 3 bytes length + 1 byte seq + payload
            byte[] header = new byte[4];
            in.read(header);

            int length = (header[0] & 0xFF) | ((header[1] & 0xFF) << 8) | ((header[2] & 0xFF) << 16);
            int seq = header[3] & 0xFF;

            System.out.println("Packet length: " + length);
            System.out.println("Sequence: " + seq);

            byte[] payload = new byte[length];
            int read = 0;
            while (read < length) {
                int n = in.read(payload, read, length - read);
                if (n < 0) break;
                read += n;
            }

            System.out.println("\n=== Handshake Packet ===");

            int pos = 0;

            // Protocol version
            int protocol = payload[pos++] & 0xFF;
            System.out.println("Protocol version: " + protocol);

            // Server version
            StringBuilder version = new StringBuilder();
            while (payload[pos] != 0) {
                version.append((char)payload[pos++]);
            }
            pos++; // skip null
            System.out.println("Server version: " + version);

            // Connection ID
            int connId = (payload[pos++] & 0xFF) |
                        ((payload[pos++] & 0xFF) << 8) |
                        ((payload[pos++] & 0xFF) << 16) |
                        ((payload[pos++] & 0xFF) << 24);
            System.out.println("Connection ID: " + connId);

            // Auth plugin data part 1 (8 bytes)
            byte[] scramblePart1 = new byte[8];
            System.arraycopy(payload, pos, scramblePart1, 0, 8);
            pos += 8;
            System.out.println("Scramble part 1 (8 bytes): " + bytesToHex(scramblePart1));

            // Filler
            pos++; // skip 0x00

            // Capability flags
            pos += 2; // lower 2 bytes

            // Character set
            pos++;

            // Status flags
            pos += 2;

            // Capability flags upper
            pos += 2;

            // Auth plugin data length
            int authDataLen = payload[pos++] & 0xFF;
            System.out.println("Auth plugin data length: " + authDataLen);

            // Reserved (10 bytes)
            pos += 10;

            // Auth plugin data part 2
            int part2Len = Math.max(13, authDataLen - 8);
            byte[] scramblePart2 = new byte[part2Len - 1]; // -1 for terminating null
            System.arraycopy(payload, pos, scramblePart2, 0, part2Len - 1);
            pos += part2Len;
            System.out.println("Scramble part 2 (" + (part2Len - 1) + " bytes): " + bytesToHex(scramblePart2));

            // Combine scramble
            byte[] fullScramble = new byte[20];
            System.arraycopy(scramblePart1, 0, fullScramble, 0, 8);
            System.arraycopy(scramblePart2, 0, fullScramble, 8, 12);
            System.out.println("\nFull scramble (20 bytes): " + bytesToHex(fullScramble));

            // Auth plugin name
            StringBuilder plugin = new StringBuilder();
            while (pos < payload.length && payload[pos] != 0) {
                plugin.append((char)payload[pos++]);
            }
            System.out.println("Auth plugin: " + plugin);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

