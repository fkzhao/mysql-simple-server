package cc.fastsoft.utils;

import cc.fastsoft.memory.MemoryUtils;

public class UnsafeUtils {


    public static final long BYTE_ARRAY_BASE_OFFSET =
            MemoryUtils.UNSAFE.arrayBaseOffset(byte[].class);
    private static final int ADDRESS_BITS_PER_WORD = 3;
    private static final int BIT_BYTE_INDEX_MASK = 7;

    public static void putBoolean(byte[] target, long offset, boolean value) {
        MemoryUtils.UNSAFE.putBoolean(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putByte(byte[] target, long offset, byte value) {
        MemoryUtils.UNSAFE.putByte(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putShort(byte[] target, long offset, short value) {
        MemoryUtils.UNSAFE.putShort(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putInt(byte[] target, long offset, int value) {
        MemoryUtils.UNSAFE.putInt(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putLong(byte[] target, long offset, long value) {
        MemoryUtils.UNSAFE.putLong(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putFloat(byte[] target, long offset, float value) {
        MemoryUtils.UNSAFE.putFloat(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void putDouble(byte[] target, long offset, double value) {
        MemoryUtils.UNSAFE.putDouble(target, BYTE_ARRAY_BASE_OFFSET + offset, value);
    }

    public static void bitSet(byte[] bytes, int baseOffset, int index) {
        int offset = baseOffset + byteIndex(index);
        byte current = getByte(bytes, offset);
        current |= (1 << (index & BIT_BYTE_INDEX_MASK));
        putByte(bytes, offset, current);
    }

    public static byte getByte(byte[] target, long offset) {
        return MemoryUtils.UNSAFE.getByte(target, BYTE_ARRAY_BASE_OFFSET + offset);
    }

    public static int getInt(byte[] target, long offset) {
        return MemoryUtils.UNSAFE.getInt(target, BYTE_ARRAY_BASE_OFFSET + offset);
    }

    public static short getShort(byte[] target, long offset) {
        return MemoryUtils.UNSAFE.getShort(target, BYTE_ARRAY_BASE_OFFSET + offset);
    }

    public static long getLong(byte[] target, long offset) {
        return MemoryUtils.UNSAFE.getLong(target, BYTE_ARRAY_BASE_OFFSET + offset);
    }

    private static int byteIndex(int bitIndex) {
        return bitIndex >>> ADDRESS_BITS_PER_WORD;
    }
}
