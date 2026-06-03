package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class PlayerBuffData {
    private static final Map<UUID, PlayerBuffData> SERVER_DATA = new HashMap<>();
    private static PlayerBuffData CLIENT_CACHE = new PlayerBuffData();

    private boolean[] unlocked = new boolean[13];
    private boolean[] active = new boolean[13];
    private float[] chance = new float[11];
    private int[] upgradeLevels = new int[13];
    private boolean[] behaviorUpgraded = new boolean[13];
    private long playTicks = 0;
    private float expPool = 0;
    private float nextThreshold = 1.0f;
    private float bonusAttack = 0;
    private float bonusHealth = 0;
    private boolean phaseEnabled = false;

    public PlayerBuffData() { Arrays.fill(chance, 0.01f); }

    public boolean isUnlocked(int id) { return unlocked[id]; }
    public void setUnlocked(int id, boolean v) { unlocked[id] = v; }
    public boolean isActive(int id) { return active[id]; }
    public void setActive(int id, boolean v) { active[id] = v; }
    public float getChance(int id) { return chance[id]; }
    public void setChance(int id, float v) { chance[id] = v; }
    public void increaseChance(int id, float inc) { chance[id] += inc; }
    public int getUpgradeLevel(int id) { return upgradeLevels[id]; }
    public void setUpgradeLevel(int id, int lv) { upgradeLevels[id] = lv; }
    public boolean hasBehaviorUpgraded(int id) { return behaviorUpgraded[id]; }
    public void setBehaviorUpgraded(int id) { behaviorUpgraded[id] = true; }
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
    public boolean isPhaseEnabled() { return phaseEnabled; }
    public void setPhaseEnabled(boolean v) { phaseEnabled = v; }

    public static PlayerBuffData get(ServerPlayerEntity player) {
        return SERVER_DATA.computeIfAbsent(player.getUuid(), uuid -> new PlayerBuffData());
    }

    public void save(ServerPlayerEntity player) { SERVER_DATA.put(player.getUuid(), this); }

    public static PlayerBuffData getClient() { return CLIENT_CACHE; }
    public static void updateClientFromNbt(NbtCompound tag) { CLIENT_CACHE = fromNbt(tag); }

    public static PlayerBuffData fromNbt(NbtCompound tag) {
        PlayerBuffData data = new PlayerBuffData();
        for (int i = 1; i <= 12; i++) {
            data.unlocked[i] = tag.getBoolean("unlocked" + i);
            data.active[i] = tag.getBoolean("active" + i);
            data.upgradeLevels[i] = tag.getInt("upgrade" + i);
            data.behaviorUpgraded[i] = tag.getBoolean("behaviorUpgraded" + i);
        }
        for (int i = 1; i <= 10; i++) data.chance[i] = tag.getFloat("chance" + i);
        data.playTicks = tag.getLong("playTicks");
        data.expPool = tag.getFloat("expPool");
        data.nextThreshold = tag.getFloat("nextThreshold");
        data.bonusAttack = tag.getFloat("bonusAttack");
        data.bonusHealth = tag.getFloat("bonusHealth");
        data.phaseEnabled = tag.getBoolean("phaseEnabled");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 1; i <= 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]);
            tag.putBoolean("active" + i, active[i]);
            tag.putInt("upgrade" + i, upgradeLevels[i]);
            tag.putBoolean("behaviorUpgraded" + i, behaviorUpgraded[i]);
        }
        for (int i = 1; i <= 10; i++) tag.putFloat("chance" + i, chance[i]);
        tag.putLong("playTicks", playTicks);
        tag.putFloat("expPool", expPool);
        tag.putFloat("nextThreshold", nextThreshold);
        tag.putFloat("bonusAttack", bonusAttack);
        tag.putFloat("bonusHealth", bonusHealth);
        tag.putBoolean("phaseEnabled", phaseEnabled);
        return tag;
    }
}
