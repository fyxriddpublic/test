package com.fyxridd.rpgcraft.craftserver.chunk;

import com.fyxridd.rpgcraft.server.chunk.ChunkUtil;

public class ChunkUtilImpl implements ChunkUtil{
    public static final byte[] INDEX = new byte[]{1, 2, 4, 8, 16, 32, 64, -128};

    /**
     * 检测指定位置是否为空
     * @param rows 行列表
     * @param x 0-7
     * @param y 0-7
     * @return 是否为空
     */
    public static boolean isEmpty(byte[] rows, int x, int y) {
        return ((rows[y] & INDEX[x])>>>x) == 0;
    }

    /**
     * 获取在列表中的位置
     * (调用此方法前请自行确认指定的位置不为空)
     * @param rows 行列表
     * @param x 0-7
     * @param y 0-7
     * @return 位置,>=0
     */
    public static int getPos(byte[] rows, int x, int y) {
        int pos = -1;

        byte row;
        for (int yIndex = 0;yIndex<=y;yIndex++) {
            row = rows[yIndex];
            for (int xIndex=0;xIndex<=x;xIndex++) {
                if ((row & INDEX[xIndex])>>>xIndex == 1) pos++;
            }
        }

        return pos;
    }

    /**
     * 获取不为空的格子数量(也即列表大小)
     * @return >=0
     */
    public static int getSize(byte[] rows) {
        int count = 0;

        byte row;
        for (int yIndex = 0;yIndex<=7;yIndex++) {
            row = rows[yIndex];
            for (int xIndex=0;xIndex<=7;xIndex++) {
                if ((row & INDEX[xIndex])>>>xIndex == 1) count++;
            }
        }

        return count;
    }

    /**
     * 获取区块的x坐标
     * @param x 实际的x坐标
     * @return 区块的x坐标
     */
    @Override
    public int getChunkX(int x) {
        return (x<0?(x-CHUNK_SIZE):x)/CHUNK_SIZE;
    }

    /**
     * 获取区块的y坐标
     * @param y 实际的y坐标
     * @return 区块的y坐标
     */
    @Override
    public int getChunkY(int y) {
        return (y<0?(y-CHUNK_SIZE):y)/CHUNK_SIZE;
    }

    /**
     * 获取区块的最小位置对应的实际x坐标
     * @param chunkX 区块x坐标
     * @return 区块内最小位置对应的实际x坐标
     */
    @Override
    public int getMinX(int chunkX) {
        return chunkX*CHUNK_SIZE;
    }

    /**
     * 获取区块的最大位置对应的实际x坐标
     * @param chunkX 区块x坐标
     * @return 区块内最大位置对应的实际x坐标
     */
    @Override
    public int getMaxX(int chunkX) {
        return chunkX*CHUNK_SIZE+CHUNK_SIZE-1;
    }

    /**
     * 获取区块的最小位置对应的实际y坐标
     * @param chunkY 区块y坐标
     * @return 区块内最小位置对应的实际y坐标
     */
    @Override
    public int getMinY(int chunkY) {
        return chunkY*CHUNK_SIZE;
    }

    /**
     * 获取区块的最大位置对应的实际y坐标
     * @param chunkY 区块y坐标
     * @return 区块内最大位置对应的实际y坐标
     */
    @Override
    public int getMaxY(int chunkY) {
        return chunkY*CHUNK_SIZE+CHUNK_SIZE-1;
    }

    /**
     * 获取x坐标在区块内的坐标
     * @param x 实际x坐标
     * @return 区块内的坐标,0-7
     */
    @Override
    public int getInChunkX(int x) {
        int result = x%CHUNK_SIZE;
        if (result < 0) result += CHUNK_SIZE;
        return result;
    }

    /**
     * 获取y坐标在区块内的坐标
     * @param y 实际y坐标
     * @return 区块内的坐标,0-7
     */
    @Override
    public int getInChunkY(int y) {
        int result = y%CHUNK_SIZE;
        if (result < 0) result += CHUNK_SIZE;
        return result;
    }
}
