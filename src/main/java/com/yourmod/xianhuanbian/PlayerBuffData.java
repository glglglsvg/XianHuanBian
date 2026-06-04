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
    private int[] levels = new int[13];
    private int[] abilityTimer = new int[13];
    private boolean[] permanent = new boolean[13];
    private long[] upgradeCost = new long[13];
    private long cultivation = 0;
    private int energy = 100;
    private boolean phaseEnabled = false;
    private long playTicks = 0;
    private float expPool = 0;
    private float nextThreshold = 1.0f;
    private float bonusAttack = 0;
    private float bonusHealth = 0;

    public PlayerBuffData() {
        Arrays.fill(chance, 0.0001f);
        for (int i = 1; i <= 12; i++) {
            levels[i] = 1;
            upgradeCost[i] = 1000 * i * i;
        }
    }// getters/setters ...
public boolean isUnlocked(int id) { return unlocked[id]; }
public void setUnlocked(int id, boolean v) { unlocked[id] = v; }
public boolean isActive(int id) { return active[id]; }
public void setActive(int id, boolean v) { active[id] = v; }
public float getChance(int id) { return chance[id]; }
public void setChance(int id, float v) { chance[id] = v; }
public void increaseChance(int id, float inc) { chance[id] += inc; }
public int getLevel(int id) { return levels[id]; }
public void setLevel(int id, int lv) { levels[id] = lv; }
public int getAbilityTimer(int id) { return abilityTimer[id]; }
public void setAbilityTimer(int id, int t) { abilityTimer[id] = t; }
public boolean isPermanent(int id) { return permanent[id]; }
public void setPermanent(int id, boolean v) { permanent[id] = v; }
public long getUpgradeCost(int id) { return upgradeCost[id]; }
public void setUpgradeCost(int id, long cost) { upgradeCost[id] = cost; }
public long getCultivation() { return cultivation; }
public void addCultivation(long amt) { cultivation += amt; }
public void setCultivation(long v) { cultivation = v; }
public int getEnergy() { return energy; }
public void setEnergy(int v) { energy = Math.max(0, Math.min(100, v)); }
public void addEnergy(int v) { setEnergy(energy + v); }
public boolean isPhaseEnabled() { return phaseEnabled; }
public void setPhaseEnabled(boolean v) { phaseEnabled = v; }
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
            data.levels[i] = tag.getInt("level" + i);
            data.abilityTimer[i] = tag.getInt("timer" + i);
            data.permanent[i] = tag.getBoolean("perm" + i);
            data.upgradeCost[i] = tag.getLong("cost" + i);
        }
        for (int i = 1; i <= 10; i++) data.chance[i] = tag.getFloat("chance" + i);
        data.cultivation = tag.getLong("cultivation");
        data.energy = tag.getInt("energy");
        data.phaseEnabled = tag.getBoolean("phaseEnabled");
        data.playTicks = tag.getLong("playTicks");
        data.expPool = tag.getFloat("expPool");
        data.nextThreshold = tag.getFloat("nextThreshold");
        data.bonusAttack = tag.getFloat("bonusAttack");
        data.bonusHealth = tag.getFloat("bonusHealth");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 1; i <= 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]);
            tag.putBoolean("active" + i, active[i]);
            tag.putInt("level" + i, levels[i]);
            tag.putInt("timer" + i, abilityTimer[i]);
            tag.putBoolean("perm" + i, permanent[i]);
            tag.putLong("cost" + i, upgradeCost[i]);
        }
        for (int i = 1; i <= 10; i++) tag.putFloat("chance" + i, chance[i]);
        tag.putLong("cultivation", cultivation);
        tag.putInt("energy", energy);
        tag.putBoolean("phaseEnabled", phaseEnabled);
        tag.putLong("playTicks", playTicks);
        tag.putFloat("expPool", expPool);
        tag.putFloat("nextThreshold", nextThreshold);
        tag.putFloat("bonusAttack", bonusAttack);
        tag.putFloat("bonusHealth", bonusHealth);
        return tag;
    }
}
