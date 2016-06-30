package com.fyxridd.rpgcraft.craftserver.chunk;

import com.fyxridd.netty.common.util.NettyUtil;
import com.fyxridd.rpgcraft.craftserver.scene.CraftScene;
import com.fyxridd.rpgcraft.craftserver.space.CraftEnvSlot;
import com.fyxridd.rpgcraft.craftserver.space.CraftObjSlot;
import com.fyxridd.rpgcraft.craftserver.world.CraftWorld;
import com.fyxridd.rpgcraft.server.RPG;
import com.fyxridd.rpgcraft.server.chunk.Chunk;
import com.fyxridd.rpgcraft.server.space.Grid;
import com.fyxridd.rpgcraft.server.space.Slot;
import com.fyxridd.util.LongsFix;
import com.google.common.collect.BiMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 区块包
 * 用来网络通信
 */
public class ChunkBag {
    //区块的x与y坐标
    private int x;
    private int y;

    //index: 1
    //长度为8,表示0-7行
    //每个字节的8位用来表示对应行的对应位置是否为空(无物体)
    private byte[] objRows;

    //obj的id列表
    private LongsFix objIds;
    //obj类型引用
    //服务端事先将BiMap<Short,String>传给客户端(即ref与实际obj类型字符串的映射)
    private short[] objTypeRefs;

    //index: 2
    //长度为8,表示0-7行
    //每个字节的8位用来表示对应行的对应位置是否为空(无环境)
    private byte[] envRows;
    //env类型引用
    //服务端事先将BiMap<Short,String>传给客户端(即ref与实际env类型字符串的映射)
    private short[] envTypeRefs;

    public ChunkBag(int x, int y, byte[] objRows, LongsFix objIds, short[] objTypeRefs, byte[] envRows, short[] envTypeRefs) {
        this.x = x;
        this.y = y;
        this.objRows = objRows;
        this.objIds = objIds;
        this.objTypeRefs = objTypeRefs;
        this.envRows = envRows;
        this.envTypeRefs = envTypeRefs;
    }

    /**
     * @see ChunkUtilImpl#isEmpty(byte[], int, int)
     */
    public boolean isEmpty(int x, int y, boolean env) {
        return ChunkUtilImpl.isEmpty(env?envRows:objRows, x, y);
    }

    /**
     * @see ChunkUtilImpl#getPos(byte[], int, int)
     */
    public int getPos(int x, int y, boolean env) {
        return ChunkUtilImpl.getPos(env?envRows:objRows, x, y);
    }

    /**
     * @see ChunkUtilImpl#getSize(byte[])
     */
    public int getSize(boolean env) {
        return ChunkUtilImpl.getSize(env?envRows:objRows);
    }

    public static void main(String... args) {
        ChunkBag chunkBag = new ChunkBag(0, 0, new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, LongsFix.fromCollection(new ArrayList<Long>()), new short[0], new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, new short[0]);
        byte[] bytes = toBytes(chunkBag);
        chunkBag = fromBytes(bytes);
    }

    public static byte[] toBytes(ChunkBag chunkBag) {
        ByteBuf buf = Unpooled.buffer();
        //x,y
        buf.writeInt(chunkBag.x);
        buf.writeInt(chunkBag.y);
        //objRows
        NettyUtil.writeBytes(buf, chunkBag.objRows);
        //objIds
        NettyUtil.writeBytes(buf, LongsFix.toBytes(chunkBag.objIds));
        //objTypeRefsLength
        buf.writeInt(chunkBag.objTypeRefs.length);
        //objTypeRefs
        for (short objTypeRef: chunkBag.objTypeRefs) buf.writeShort(objTypeRef);
        //envRows
        NettyUtil.writeBytes(buf, chunkBag.envRows);
        //envTypeRefsLength
        buf.writeInt(chunkBag.envTypeRefs.length);
        //envTypeRefs
        for (short envTypeRef: chunkBag.envTypeRefs) buf.writeShort(envTypeRef);
        //读取
        byte[] result = new byte[buf.readableBytes()];
        buf.readBytes(result);
        //释放
        buf.release();
        //返回
        return result;
    }

    public static ChunkBag fromBytes(byte[] bytes) {
        ByteBuf buf = Unpooled.buffer();
        //写入
        buf.writeBytes(bytes);
        //x,y
        int x = buf.readInt();
        int y = buf.readInt();
        //objRows
        byte[] objRows = NettyUtil.readBytes(buf);
        //objIds
        LongsFix objIds = LongsFix.fromBytes(NettyUtil.readBytes(buf));
        //objTypeRefsLength
        int objTypeRefsLength = buf.readInt();
        //objTypeRefs
        short[] objTypeRefs = new short[objTypeRefsLength];
        for (int index=0;index<objTypeRefs.length;index++) objTypeRefs[index] = buf.readShort();
        //envRows
        byte[] envRows = NettyUtil.readBytes(buf);
        //envTypeRefsLength
        int envTypeRefsLength = buf.readInt();
        //envTypeRefs
        short[] envTypeRefs = new short[envTypeRefsLength];
        for (int index=0;index<envTypeRefs.length;index++) envTypeRefs[index] = buf.readShort();
        //返回
        return new ChunkBag(x, y, objRows, objIds, objTypeRefs, envRows, envTypeRefs);
    }

    public static ChunkBag fromChunk(Chunk chunk) {
        //obj
        byte[] objRows;
        LongsFix objIds;
        short[] objTypeRefs;
        {
            Collection<Slot> objSlots = chunk.getSlots(false);
            objRows = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            int y;
            for (Slot slot:objSlots) {
                y = slot.getY()%8;
                objRows[y] = (byte) (objRows[y] | ChunkUtilImpl.INDEX[slot.getX()%8]);
            }
            List<Long> c = new ArrayList<>();
            for (Slot slot:objSlots) c.add(((CraftObjSlot)slot).getObj().getId().getValue());
            objIds = LongsFix.fromCollection(c);
            objTypeRefs = new short[objSlots.size()];
            BiMap<String, Short> objRefsInverse = RPG.getObjRefConfig().getObjRefs().inverse();
            int index=0;
            for (Slot slot:objSlots) objTypeRefs[index++] = objRefsInverse.get(((CraftObjSlot)slot).getObj().getType());
        }

        //env
        byte[] envRows;
        short[] envTypeRefs;
        {
            Collection<Slot> envSlots = chunk.getSlots(true);
            envRows = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            int y;
            for (Slot slot:envSlots) {
                y = slot.getY()%8;
                envRows[y] = (byte) (envRows[y] | ChunkUtilImpl.INDEX[slot.getX()%8]);
            }
            envTypeRefs = new short[envSlots.size()];
            BiMap<String, Short> envRefsInverse = RPG.getEnvRefConfig().getEnvRefs().inverse();
            int index=0;
            for (Slot slot:envSlots) envTypeRefs[index++] = envRefsInverse.get(((CraftEnvSlot)slot).getEnv().getType());
        }
        return new ChunkBag(chunk.getX(), chunk.getY(), objRows, objIds, objTypeRefs, envRows, envTypeRefs);
    }
}


