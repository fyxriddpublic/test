package com.fyxridd.util;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collection;

/**
 * Long列表的优化
 */
public class LongsFix {
    //每个byte的8位分成4组(每组2位)来用,从低位到高位,代表longs列表中的顺序4个的字节类型
    private byte[] lengths;
    //每个byte[]表示指定位置的long
    //可能为:
    //  byte 1字节
    //  short 2字节
    //  int 4字节
    //  long 8字节
    private byte[][] longs;

    private LongsFix(byte[] lengths, byte[][] longs) {
        this.lengths = lengths;
        this.longs = longs;
    }

    /**
     * 获取列表的大小(即Long的数量)
     * @return >=0
     */
    public int size() {
        return longs.length;
    }

    /**
     * 获取指定位置的long值
     * @param index 位置,[0,size())
     */
    public long get(int index) {
        Preconditions.checkArgument(index >= 0 && index < size());

        return getLong(longs[index]);
    }

    /**
     * 需要计算,略耗时
     * 生成的byte[]可以用fromBytes读取
     * @param longsFix 如果长度为0则返回长度为0的byte[]
     */
    public static byte[] toBytes(LongsFix longsFix) {
        if (longsFix.size() == 0) return new byte[0];

        ByteBuf buf = Unpooled.buffer();
        byte[] result;
        try {
            //size
            buf.writeInt(longsFix.size());
            //lengths
            if (longsFix.size() > 0) buf.writeBytes(longsFix.lengths);
            //longs
            for (byte[] value : longsFix.longs) buf.writeBytes(value);
            //返回
            result = new byte[buf.readableBytes()];
            buf.readBytes(result);
        } finally {
            buf.release();
        }
        return result;
    }

    /**
     * 读取的byte[]是toBytes方法返回的
     * @param bytes 如果长度为0则返回长度为0的LongsFix
     */
    public static LongsFix fromBytes(byte[] bytes) {
        if (bytes.length == 0) return new LongsFix(new byte[0], new byte[0][]);

        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        byte[] lengths;
        byte[][] longs;
        try {
            //size
            int size = buf.readInt();
            //lengths
            lengths = new byte[size == 0?0:(getLengthsIndex(size)+1)];
            if (size > 0) buf.readBytes(lengths);
            //longs
            longs = new byte[size][];
            int bytesLength;
            for (int index=0;index<size;index++) {
                bytesLength = 2^((lengths[getLengthsIndex(index)] << (8-(index%4+1)*2)) >>> 6);//字节数: 1,2,4,8
                longs[index] = new byte[bytesLength];
                buf.readBytes(longs[index]);
            }
        } finally {
            buf.release();
        }
        return new LongsFix(lengths, longs);
    }

    /**
     * @param c 可为空集合
     */
    public static LongsFix fromCollection(Collection<Long> c) {
        int size = c.size();
        byte[] lengths = new byte[size == 0?0:(getLengthsIndex(size)+1)];
        byte[][] longs = new byte[size][];
        int lengthsIndex;
        int index=0;
        for (Long value:c) {
            longs[index] = getBytes(value);
            lengthsIndex = getLengthsIndex(index);
            lengths[lengthsIndex] = (byte) (lengths[lengthsIndex] | (index%4 << (longs[index].length-1)*2));
            index++;
        }
        return new LongsFix(lengths, longs);
    }

    /**
     * 获取优化后的字节数
     * @param value 值
     * @return 优化后的字节数组
     */
    public static byte[] getBytes(Long value) {
        value = Math.abs(value);
        if (value <= Byte.MAX_VALUE) return CodecUtil.byteToBytes(value.byteValue());
        else if (value <= Short.MAX_VALUE) return CodecUtil.shortToBytes(value.shortValue());
        else if (value <= Integer.MAX_VALUE) return CodecUtil.intToBytes(value.intValue());
        else return CodecUtil.longToBytes(value);
    }

    /**
     * 根据bytes的字节数获取Long
     */
    public static long getLong(byte[] bytes) {
        switch (bytes.length) {
            case 1:
                return CodecUtil.bytesToByte(bytes);
            case 2:
                return CodecUtil.bytesToShort(bytes);
            case 3:
                return CodecUtil.bytesToInt(bytes);
            default:
                return CodecUtil.bytesToLong(bytes);
        }
    }

    /**
     * 获取指定的index在lengths中的位置
     * @param index >=0
     * @return >=0
     */
    public static int getLengthsIndex(int index) {
        return index/4+(index%4 == 0?0:1);
    }
}
