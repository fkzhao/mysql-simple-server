package cc.fastsoft.jdbc.protocol;

import cc.fastsoft.jdbc.ConnectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class MysqlProto {
    private static final Logger LOG = LoggerFactory.getLogger(MysqlProto.class);
    public static final boolean SERVER_USE_SSL = false;


    private static String parseUser(ConnectContext context, byte[] scramble, String user) {
        String usePasswd = scramble.length == 0 ? "NO" : "YES";
        String tmpUser = user;
        if (tmpUser == null || tmpUser.isEmpty()) {
            return null;
        }

        // check workload group level. user name may contains workload group level.
        // eg:
        // ...@user_name#HIGH
        // set workload group if it is valid, or just ignore it
        String[] strList = tmpUser.split("#", 2);
        if (strList.length > 1) {
            tmpUser = strList[0];
        }

        return tmpUser;
    }

    public static byte readByte(ByteBuffer buffer) {
        return buffer.get();
    }

    public static byte readByteAt(ByteBuffer buffer, int index) {
        return buffer.get(index);
    }

    public static int readInt1(ByteBuffer buffer) {
        return readByte(buffer) & 0XFF;
    }

    public static int readInt2(ByteBuffer buffer) {
        return (readByte(buffer) & 0xFF) | ((readByte(buffer) & 0xFF) << 8);
    }

    public static int readInt3(ByteBuffer buffer) {
        return (readByte(buffer) & 0xFF) | ((readByte(buffer) & 0xFF) << 8) | ((readByte(
                buffer) & 0xFF) << 16);
    }

    public static int readLowestInt4(ByteBuffer buffer) {
        return (readByteAt(buffer, 0) & 0xFF) | ((readByteAt(buffer, 1) & 0xFF) << 8) | ((readByteAt(
                buffer, 2) & 0xFF) << 16) | ((readByteAt(buffer, 3) & 0XFF) << 24);
    }

    public static int readInt4(ByteBuffer buffer) {
        return (readByte(buffer) & 0xFF) | ((readByte(buffer) & 0xFF) << 8) | ((readByte(
                buffer) & 0xFF) << 16) | ((readByte(buffer) & 0XFF) << 24);
    }

    public static long readInt6(ByteBuffer buffer) {
        return (readInt4(buffer) & 0XFFFFFFFFL) | (((long) readInt2(buffer)) << 32);
    }

    public static long readInt8(ByteBuffer buffer) {
        return (readInt4(buffer) & 0XFFFFFFFFL) | (((long) readInt4(buffer)) << 32);
    }

    public static long readVInt(ByteBuffer buffer) {
        int b = readInt1(buffer);

        if (b < 251) {
            return b;
        }
        if (b == 252) {
            return readInt2(buffer);
        }
        if (b == 253) {
            return readInt3(buffer);
        }
        if (b == 254) {
            return readInt8(buffer);
        }
        if (b == 251) {
            throw new NullPointerException();
        }
        return 0;
    }

    public static byte[] readFixedString(ByteBuffer buffer, int len) {
        byte[] buf = new byte[len];
        buffer.get(buf);
        return buf;
    }

    public static byte[] readEofString(ByteBuffer buffer) {
        byte[] buf = new byte[buffer.remaining()];
        buffer.get(buf);
        return buf;
    }

    public static byte[] readLenEncodedString(ByteBuffer buffer) {
        long length = readVInt(buffer);
        byte[] buf = new byte[(int) length];
        buffer.get(buf);
        return buf;
    }

    public static byte[] readNulTerminateString(ByteBuffer buffer) {
        int oldPos = buffer.position();
        int nullPos = oldPos;
        for (nullPos = oldPos; nullPos < buffer.limit(); ++nullPos) {
            if (buffer.get(nullPos) == 0) {
                break;
            }
        }
        byte[] buf = new byte[nullPos - oldPos];
        buffer.get(buf);
        // skip null byte.
        buffer.get();
        return buf;
    }
}
