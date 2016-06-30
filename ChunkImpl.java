package com.fyxridd.rpgcraft.craftserver.chunk;

import com.fyxridd.rpgcraft.craftserver.env.CraftEnv;
import com.fyxridd.rpgcraft.craftserver.obj.CraftObj;
import com.fyxridd.rpgcraft.craftserver.space.CraftEnvSlot;
import com.fyxridd.rpgcraft.craftserver.space.CraftObjSlot;
import com.fyxridd.rpgcraft.server.RPG;
import com.fyxridd.rpgcraft.server.chunk.Chunk;
import com.fyxridd.rpgcraft.server.env.Env;
import com.fyxridd.rpgcraft.server.obj.Obj;
import com.fyxridd.rpgcraft.server.scene.Scene;
import com.fyxridd.rpgcraft.server.space.Slot;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Collection;

public class ChunkImpl implements Chunk{
    private Scene scene;
    private int x;
    private int y;

    //物体层(CraftObjSlot)
    //对于空的位置,一开始不会添加上,但在运行过程中可能会添加上
    private Table<Integer, Integer, Slot> objSlots;
    //环境层(CraftEnvSlot)
    //对于空的位置,一开始不会添加上,但在运行过程中可能会添加上
    private Table<Integer, Integer, Slot> envSlots;

    //未生成时为null
    private ChunkBag chunkBag;

    public ChunkImpl(Scene scene, int x, int y) {
        this.scene = scene;
        this.x = x;
        this.y = y;
        this.objSlots = HashBasedTable.create();
        this.envSlots = HashBasedTable.create();
    }

    public ChunkImpl(Scene scene, int x, int y, Table<Integer, Integer, Slot> objSlots, Table<Integer, Integer, Slot> envSlots) {
        this.scene = scene;
        this.x = x;
        this.y = y;
        this.objSlots = objSlots;
        this.envSlots = envSlots;
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public boolean isEmpty() {
        for (Slot slot:objSlots.values()) {
            if (!RPG.getSlotUtil().isSlotEmpty(slot)) return false;
        }
        for (Slot slot:envSlots.values()) {
            if (!RPG.getSlotUtil().isSlotEmpty(slot)) return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty(int x, int y, boolean env) {
        Preconditions.checkArgument(x >= 0 && x <= 7 && y >= 0 && y <= 7);

        return !(env ? envSlots.contains(x, y) : objSlots.contains(x, y)) || RPG.getSlotUtil().isSlotEmpty(env ? envSlots.get(x, y) : objSlots.get(x, y));
    }

    @Override
    public Slot getSlot(int x, int y, boolean env) {
        Preconditions.checkArgument(x >= 0 && x <= 7 && y >= 0 && y <= 7);

        if (env) {
            Slot slot = envSlots.get(x, y);
            if (slot == null) {
                slot = new CraftEnvSlot(x, y, null);
                envSlots.put(x, y, slot);
            }
            return slot;
        }else {
            Slot slot = objSlots.get(x, y);
            if (slot == null) {
                slot = new CraftObjSlot(x, y, null);
                objSlots.put(x, y, slot);
            }
            return slot;
        }
    }

    @Override
    public Collection<Slot> getSlots(boolean env) {
        return env?envSlots.values():objSlots.values();
    }

    @Override
    public Obj getObj(int x, int y) {
        Preconditions.checkArgument(x >= 0 && x <= 7 && y >= 0 && y <= 7);

        CraftObjSlot slot = (CraftObjSlot) objSlots.get(x, y);
        if (slot != null) return slot.getObj();
        return null;
    }

    @Override
    public Env getEnv(int x, int y) {
        Preconditions.checkArgument(x >= 0 && x <= 7 && y >= 0 && y <= 7);

        CraftEnvSlot slot = (CraftEnvSlot) envSlots.get(x, y);
        if (slot != null) return slot.getEnv();
        return null;
    }

    @Override
    public Obj createObj(String type, Slot slot) {
        slot = new CraftObjSlot(slot.getX(), slot.getY(), null);
        Obj obj = new CraftObj(type, slot);
        ((CraftObjSlot)slot).setObj(obj);
        objSlots.put(slot.getX(), slot.getY(), slot);//原来可能没有,也可能有
        return obj;
    }

    @Override
    public Env createEnv(String type, Slot slot) {
        slot = new CraftEnvSlot(slot.getX(), slot.getY(), null);
        Env env = new CraftEnv(type, slot);
        ((CraftEnvSlot)slot).setEnv(env);
        envSlots.put(slot.getX(), slot.getY(), slot);//原来可能没有,也可能有
        return env;
    }

    /**
     * 检测更新区块包
     * @param reset true时会重置
     */
    public void updateChunkBag(boolean reset) {
        if (chunkBag == null || reset) chunkBag = ChunkBag.fromChunk(this);
    }

    public ChunkBag getChunkBag() {
        return chunkBag;
    }
}
