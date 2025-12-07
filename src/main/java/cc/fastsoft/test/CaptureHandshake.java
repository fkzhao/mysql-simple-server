package cc.fastsoft.test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Test tool to capture and display the handshake packet from server
 */
public class CaptureHandshake {

    public static void main(String[] args) {
        System.out.println("=== Capturing Handshake Packet ===\n");

        try (Socket socket = new Socket("127.0.0.1", 2883)) {
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Read packet header
            int payloadLength = in.readUnsignedByte() |
                               (in.readUnsignedByte() << 8) |
                               (in.readUnsignedByte() << 16);
            int sequenceId = in.readUnsignedByte();

            System.out.println("Packet length: " + payloadLength);
            System.out.println("Sequence ID: " + sequenceId);
            System.out.println();

            // Read payload
            byte[] payload = new byte[payloadLength];
            in.readFully(payload);

            // Parse handshake
            int pos = 0;

            // Protocol version
            int protocolVersion = payload[pos++] & 0xFF;
            System.out.println("Protocol version: " + protocolVersion);

            // Server version (null-terminated)
            StringBuilder serverVersion = new StringBuilder();
            while (payload[pos] != 0) {
                serverVersion.append((char) payload[pos++]);
            }
            pos++; // skip null
            System.out.println("Server version: " + serverVersion);

            // Connection ID
            int connectionId = (payload[pos++] & 0xFF) |
                              ((payload[pos++] & 0xFF) << 8) |
                              ((payload[pos++] & 0xFF) << 16) |
                              ((payload[pos++] & 0xFF) << 24);
            System.out.println("Connection ID: " + connectionId);

            // Auth-plugin-data-part-1 (8 bytes)
            byte[] scramblePart1 = new byte[8];
            System.arraycopy(payload, pos, scramblePart1, 0, 8);
            pos += 8;
            System.out.println("Scramble part 1 (8 bytes): " + bytesToHex(scramblePart1));

            // Filler
            pos++; // skip filler

            // Capability flags lower
            pos += 2;

            // Character set
            pos++;

            // Status flags
            pos += 2;

            // Capability flags upper
            pos += 2;

            // Auth plugin data length
            int authPluginDataLength = payload[pos++] & 0xFF;
            System.out.println("Auth plugin data length: " + authPluginDataLength);

            // Reserved (10 bytes)
            pos += 10;

            // Auth-plugin-data-part-2 (至少 13 bytes)
            int part2Length = Math.max(13, authPluginDataLength - 8);
            byte[] scramblePart2Raw = new byte[part2Length];
            System.arraycopy(payload, pos, scramblePart2Raw, 0, part2Length);
            pos += part2Length;

            // Remove null terminator if present
            int actualPart2Length = part2Length;
            for (int i = 0; i < part2Length; i++) {
                if (scramblePart2Raw[i] == 0) {
                    actualPart2Length = i;
                    break;
                }
            }

            byte[] scramblePart2 = new byte[actualPart2Length];
            System.arraycopy(scramblePart2Raw, 0, scramblePart2, 0, actualPart2Length);

            System.out.println("Scramble part 2 (" + actualPart2Length + " bytes): " + bytesToHex(scramblePart2));

            // Full scramble
            byte[] fullScramble = new byte[scramblePart1.length + scramblePart2.length];
            System.arraycopy(scramblePart1, 0, fullScramble, 0, scramblePart1.length);
            System.arraycopy(scramblePart2, 0, fullScramble, scramblePart1.length, scramblePart2.length);

            System.out.println("\n=== Full Scramble ===");
            System.out.println("Length: " + fullScramble.length + " bytes");
            System.out.println("Hex: " + bytesToHex(fullScramble));

            // Auth plugin name
            if (pos < payload.length) {
                StringBuilder authPlugin = new StringBuilder();
                while (pos < payload.length && payload[pos] != 0) {
                    authPlugin.append((char) payload[pos++]);
                }
                System.out.println("\nAuth plugin: " + authPlugin);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}

