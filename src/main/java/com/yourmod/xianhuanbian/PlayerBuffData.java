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
    private int[] durations = new int[13];
    private int[] maxDurations = new int[13];
    private long[] upgradeCost = new long[13];
    private long cultivation = 0;
    private int energy = 100;
    private int maxEnergy = 100;
    private float energyCostPerTick = 1.0f;
    private long playTicks = 0;
    private double globalAttack = 0;
    private int maxHealthBonus = 0;
    private int killHealAmount = 0;
    private double killMultiplier = 1.0;

    public PlayerBuffData() {
        Arrays.fill(chance, 0.000001f);
        for (int i = 1; i <= 12; i++) {
            levels[i] = 1;
            upgradeCost[i] = (i == 1) ? 200 : (long) Math.pow(200, i);
            maxDurations[i] = 0;
            durations[i] = 0;
        }
    }public boolean isUnlocked(int id) { return unlocked[id]; }
public void setUnlocked(int id, boolean v) { unlocked[id] = v; }
public boolean isActive(int id) { return active[id]; }
public void setActive(int id, boolean v) { active[id] = v; }
public float getChance(int id) { return chance[id]; }
public void setChance(int id, float v) { chance[id] = v; }
public void increaseChance(int id, float inc) { chance[id] += inc; }
public int getLevel(int id) { return levels[id]; }
public void setLevel(int id, int lv) { levels[id] = lv; }
public int getDuration(int id) { return durations[id]; }
public void setDuration(int id, int v) { durations[id] = v; }
public int getMaxDuration(int id) { return maxDurations[id]; }
public void setMaxDuration(int id, int v) { maxDurations[id] = v; }
public long getUpgradeCost(int id) { return upgradeCost[id]; }
public void setUpgradeCost(int id, long cost) { upgradeCost[id] = cost; }
public long getCultivation() { return cultivation; }
public void addCultivation(long amt) { cultivation += amt; }
public void setCultivation(long v) { cultivation = v; }
public int getEnergy() { return energy; }
public void setEnergy(int v) { energy = Math.max(0, Math.min(maxEnergy, v)); }
public void addEnergy(int v) { setEnergy(energy + v); }
public int getMaxEnergy() { return maxEnergy; }
public void setMaxEnergy(int v) { maxEnergy = v; }
public float getEnergyCostPerTick() { return energyCostPerTick; }
public void setEnergyCostPerTick(float v) { energyCostPerTick = v; }
public long getPlayTicks() { return playTicks; }
public void setPlayTicks(long t) { playTicks = t; }
public double getGlobalAttack() { return globalAttack; }
public void setGlobalAttack(double v) { globalAttack = v; }
public int getMaxHealthBonus() { return maxHealthBonus; }
public void setMaxHealthBonus(int v) { maxHealthBonus = v; }
public int getKillHealAmount() { return killHealAmount; }
public void setKillHealAmount(int v) { killHealAmount = v; }
public double getKillMultiplier() { return killMultiplier; }
public void setKillMultiplier(double v) { killMultiplier = v; }

public void adjustChances() {
    int unlockedCount = 0;
    for (int i = 1; i <= 10; i++) if (unlocked[i]) unlockedCount++;
    if (unlockedCount >= 2) {
        for (int i = 1; i <= 10; i++) {
            if (!unlocked[i]) chance[i] = 0.000001f / unlockedCount;
        }
    }
}

public void upgradeEnergy() {
    maxEnergy += 5;
    energyCostPerTick = Math.max(0.2f, energyCostPerTick - 0.05f);
    energy = Math.min(energy, maxEnergy);
}

private int calcDuration(int level, int baseMin) {
    if (level >= 10) return 0;
    return baseMin * 20 + (level - 1) * (baseMin / 9) * 20;
}

public void activate(int id, int baseMin) {
    setActive(id, true);
    if (baseMin == 0) setDuration(id, 0);
    else setDuration(id, calcDuration(getLevel(id), baseMin));
}

public void onLevelUp(int id) {
    upgradeEnergy();
    if (maxDurations[id] != 0) {
        int lv = getLevel(id);
        if (lv >= 10) setDuration(id, 0);
        else setDuration(id, calcDuration(lv, 60));
    }
}public static PlayerBuffData get(ServerPlayerEntity player) {
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
        data.durations[i] = tag.getInt("dur" + i);
        data.maxDurations[i] = tag.getInt("maxDur" + i);
        data.upgradeCost[i] = tag.getLong("cost" + i);
    }
    for (int i = 1; i <= 10; i++) data.chance[i] = tag.getFloat("chance" + i);
    data.cultivation = tag.getLong("cultivation");
    data.energy = tag.getInt("energy");
    data.maxEnergy = tag.getInt("maxEnergy");
    data.energyCostPerTick = tag.getFloat("energyCost");
    data.playTicks = tag.getLong("playTicks");
    data.globalAttack = tag.getDouble("globalAttack");
    data.maxHealthBonus = tag.getInt("maxHealth");
    data.killHealAmount = tag.getInt("killHeal");
    data.killMultiplier = tag.getDouble("killMult");
    return data;
}    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 1; i <= 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]);
            tag.putBoolean("active" + i, active[i]);
            tag.putInt("level" + i, levels[i]);
            tag.putInt("dur" + i, durations[i]);
            tag.putInt("maxDur" + i, maxDurations[i]);
            tag.putLong("cost" + i, upgradeCost[i]);
        }
        for (int i = 1; i <= 10; i++) tag.putFloat("chance" + i, chance[i]);
        tag.putLong("cultivation", cultivation);
        tag.putInt("energy", energy);
        tag.putInt("maxEnergy", maxEnergy);
        tag.putFloat("energyCost", energyCostPerTick);
        tag.putLong("playTicks", playTicks);
        tag.putDouble("globalAttack", globalAttack);
        tag.putInt("maxHealth", maxHealthBonus);
        tag.putInt("killHeal", killHealAmount);
        tag.putDouble("killMult", killMultiplier);
        return tag;
    }
}
