package com.fyxridd.netty.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class NettyUtil {
    /**
     * 读取Long型(会优化存储空间)
     * 对应writeLong方法
     */
    public static long readLong(ByteBuf buf) {
        byte b = buf.readByte();//1 2 4 8
        switch (b) {
            case 1:
                return buf.readByte();
            case 2:
                return buf.readShort();
            case 4:
                return buf.readInt();
            default:
                return buf.readLong();
        }
    }

    /**
     * 写入Long型(会优化存储空间)
     * 对应readLong方法
     */
    public static void writeLong(ByteBuf buf, Long value) {
        value = Math.abs(value);
        if (value <= Byte.MAX_VALUE) {
            buf.writeByte(1);
            buf.writeByte(value.byteValue());
        }else if (value <= Short.MAX_VALUE) {
            buf.writeByte(2);
            buf.writeShort(value.shortValue());
        }else if (value <= Integer.MAX_VALUE) {
            buf.writeByte(4);
            buf.writeInt(value.intValue());
        }else {
            buf.writeByte(8);
            buf.writeLong(value);
        }
    }

    /**
     * 以UTF8的格式读取字符串
     * 对应writeString方法
     * @return 可能为""
     */
    public static String readString(ByteBuf buf) {
        byte[] bytes = readBytes(buf);
        if (bytes.length > 0) return new String(bytes, CharsetUtil.UTF_8);
        else return "";
    }

    /**
     * 以UTF8的格式写入字符串
     * 对应readString方法
     * @param s 可能为""
     */
    public static void writeString(ByteBuf buf, String s) {
        writeBytes(buf, s.getBytes(CharsetUtil.UTF_8));
    }

    /**
     * 读取字节数组
     * 对应writeBytes方法
     * @return 可能长度为0
     */
    public static byte[] readBytes(ByteBuf buf) {
        int length = buf.readInt();
        if (length > 0) {
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            return bytes;
        }else return new byte[0];
    }

    /**
     * 写入字节数组
     * 对应readBytes方法
     * @param bytes 可能长度为0
     */
    public static void writeBytes(ByteBuf buf, byte[] bytes) {
        if (bytes.length > 0) {
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }else {
            buf.writeInt(0);
        }
    }
}
