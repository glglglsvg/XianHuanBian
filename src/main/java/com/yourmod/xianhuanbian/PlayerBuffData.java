package com.yourmod.xianhuanbian;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;

public class PlayerBuffData {
    private boolean[] unlocked = new boolean[12];
    private boolean[] active = new boolean[12];
    private float[] chance = new float[10];
    private int[] upgradeLevels = new int[12];
    private long playTicks = 0;
    private float expPool = 0;
    private float nextThreshold = 1.0f;
    private float bonusAttack = 0;
    private float bonusHealth = 0;

    public PlayerBuffData() {
        Arrays.fill(chance, 0.01f);
    }

    public boolean isUnlocked(int id) { return unlocked[id]; }
    public void setUnlocked(int id, boolean v) { unlocked[id] = v; }
    public boolean isActive(int id) { return active[id]; }
    public void setActive(int id, boolean v) { active[id] = v; }
    public float getChance(int id) { return chance[id]; }
    public void setChance(int id, float v) { chance[id] = v; }
    public void increaseChance(int id, float inc) { chance[id] += inc; }
    public int getUpgradeLevel(int id) { return upgradeLevels[id]; }
    public void setUpgradeLevel(int id, int lv) { upgradeLevels[id] = lv; }
    public long getPlayTicks() { return playTicks; }
    public void setPlayTicks(long t) { playTicks = t; }
    public float getExpPool() { return expPool; }
    public void setExpPool(float v) { expPool = v; }
    public float getNextThreshold() { return nextThreshold; }
    public void setNextThreshold(float v) { nextThreshold = v; }
    public float getBonusAttack() { return bonusAttack; }
    public void setBonusAttack(float v) { bonusAttack = v; }
    public float getBonusHealth() { return bonusHealth; }
    public void setBonusHealth(float v) { bonusHealth = v; }

    // 从玩家持久 NBT 读取（服务端调用）
    public static PlayerBuffData get(PlayerEntity player) {
        NbtCompound root = player.getPersistentData();
        NbtCompound tag = root.getCompound("xianhuanbian");
        return fromNbt(tag);
    }

    // 保存到玩家持久 NBT
    public void save(ServerPlayerEntity player) {
        NbtCompound root = player.getPersistentData();
        root.put("xianhuanbian", toNbt());
    }

    // 客户端缓存（渲染用）
    private static PlayerBuffData clientCache = new PlayerBuffData();

    public static PlayerBuffData getClient() {
        return clientCache;
    }

    public static void updateClientFromNbt(NbtCompound tag) {
        clientCache = fromNbt(tag);
    }

    // NBT 序列化
    public static PlayerBuffData fromNbt(NbtCompound tag) {
        PlayerBuffData data = new PlayerBuffData();
        for (int i = 0; i < 12; i++) {
            data.unlocked[i] = tag.getBoolean("unlocked" + i);
            data.active[i] = tag.getBoolean("active" + i);
            data.upgradeLevels[i] = tag.getInt("upgrade" + i);
        }
        for (int i = 0; i < 10; i++) data.chance[i] = tag.getFloat("chance" + i);
        data.playTicks = tag.getLong("playTicks");
        data.expPool = tag.getFloat("expPool");
        data.nextThreshold = tag.getFloat("nextThreshold");
        data.bonusAttack = tag.getFloat("bonusAttack");
        data.bonusHealth = tag.getFloat("bonusHealth");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 0; i < 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]);
            tag.putBoolean("active" + i, active[i]);
            tag.putInt("upgrade" + i, upgradeLevels[i]);
        }
        for (int i = 0; i < 10; i++) tag.putFloat("chance" + i, chance[i]);
        tag.putLong("playTicks", playTicks);
        tag.putFloat("expPool", expPool);
        tag.putFloat("nextThreshold", nextThreshold);
        tag.putFloat("bonusAttack", bonusAttack);
        tag.putFloat("bonusHealth", bonusHealth);
        return tag;
    }
}
