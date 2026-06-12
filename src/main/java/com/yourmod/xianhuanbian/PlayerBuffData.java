package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class PlayerBuffData {
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
    private int regenLevel = 0;

    private int availablePoints = 0;
    private int strength = 0;
    private int speed = 0;
    private int vitality = 0;
    private int killCounter = 0;
    private boolean isMeditating = false;
    private int meditateTimer = 0;

    private boolean[] behaviorDone = new boolean[11];
    private int eatCount = 0, leftClickCount = 0, itemKillCount = 0;
    private double walkDist = 0;
    private int breakCount = 0, plantCount = 0, placeCount = 0, craftToolCount = 0, fireWaterCount = 0;

    public PlayerBuffData() {
        Arrays.fill(chance, 0.000001f);
        for (int i = 1; i <= 12; i++) {
            levels[i] = 1;
            upgradeCost[i] = (i == 1) ? 200 : (long) Math.pow(200, i);
        }
    }

    public static PlayerBuffData get(ServerPlayerEntity player) {
        NbtCompound root = player.getPersistentData();
        NbtCompound tag = root.getCompound("xianhuanbian");
        return fromNbt(tag);
    }

    public void save(ServerPlayerEntity player) {
        NbtCompound root = player.getPersistentData();
        root.put("xianhuanbian", toNbt());
    }
    // --- getter/setter ---
public boolean isUnlocked(int id) { return unlocked[id]; }
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
public int getRegenLevel() { return regenLevel; }
public void setRegenLevel(int v) { regenLevel = v; }
public int getAvailablePoints() { return availablePoints; }
public void setAvailablePoints(int v) { availablePoints = v; }
public void addAvailablePoints(int v) { availablePoints += v; }
public int getStrength() { return strength; }
public void setStrength(int v) { strength = v; }
public int getSpeed() { return speed; }
public void setSpeed(int v) { speed = v; }
public int getVitality() { return vitality; }
public void setVitality(int v) { vitality = v; }
public int getKillCounter() { return killCounter; }
public void setKillCounter(int v) { killCounter = v; }
public void addKill() { killCounter++; if (killCounter >= 20) { killCounter = 0; availablePoints++; } }
public boolean isMeditating() { return isMeditating; }
public void setMeditating(boolean v) { isMeditating = v; }
public int getMeditateTimer() { return meditateTimer; }
public void setMeditateTimer(int v) { meditateTimer = v; }

// --- 行为计数 ---
public boolean isBehaviorDone(int id) { return behaviorDone[id]; }
public void setBehaviorDone(int id) { behaviorDone[id] = true; }
public void addEat() { if (!hasAnyRing() && !behaviorDone[1]) { eatCount++; if (eatCount >= 28) setBehaviorDone(1); } }
public void addLeftClick() { if (!hasAnyRing() && !behaviorDone[2]) { leftClickCount++; if (leftClickCount >= 100) setBehaviorDone(2); } }
public void addItemKill() { if (!hasAnyRing() && !behaviorDone[3]) { itemKillCount++; if (itemKillCount >= 15) setBehaviorDone(3); } }
public void addWalkDist(double d) { if (!hasAnyRing() && !behaviorDone[4]) { walkDist += d; if (walkDist >= 150) setBehaviorDone(4); } }
public void addBreak() { if (!hasAnyRing() && !behaviorDone[5]) { breakCount++; if (breakCount >= 49) setBehaviorDone(5); } }
public void addPlant() { if (!hasAnyRing() && !behaviorDone[6]) { plantCount++; if (plantCount >= 15) setBehaviorDone(6); } }
public void addPlace() { if (!hasAnyRing() && !behaviorDone[7]) { placeCount++; if (placeCount >= 72) setBehaviorDone(7); } }
public void addCraftTool() { if (!hasAnyRing() && !behaviorDone[8]) { craftToolCount++; if (craftToolCount >= 15) setBehaviorDone(8); } }
public void addFireWater() { if (!hasAnyRing() && !behaviorDone[9]) { fireWaterCount++; if (fireWaterCount >= 28) setBehaviorDone(9); } }
public void checkExp(float level) { if (!hasAnyRing() && !behaviorDone[10] && level >= 2.0f) setBehaviorDone(10); }
public boolean hasAnyRing() { for (int i = 1; i <= 10; i++) if (unlocked[i]) return true; return false; }
    // --- 概率调整 ---
public void applyBehaviorChances() {
    for (int i = 1; i <= 10; i++) {
        if (behaviorDone[i]) chance[i] = 0.1f + 0.1f;
        else chance[i] = 0.1f;
    }
}
public void resetChancesForHardMode() { Arrays.fill(chance, 0.000001f); adjustChances(); }
public float getEffectiveChance(int id, boolean isMeditating) {
    float base = chance[id];
    if (isMeditating) base *= 3.0f;
    int unlockedCount = 0;
    for (int i = 1; i <= 10; i++) if (unlocked[i]) unlockedCount++;
    if (unlockedCount >= 2) base *= 0.1f;
    return Math.min(base, 1.0f);
}
public void adjustChances() {
    int unlockedCount = 0;
    for (int i = 1; i <= 10; i++) if (unlocked[i]) unlockedCount++;
    if (unlockedCount >= 2) for (int i = 1; i <= 10; i++) if (!unlocked[i]) chance[i] = 0.000001f / unlockedCount;
}
public void upgradeEnergy() { maxEnergy += 5; energyCostPerTick = Math.max(0.2f, energyCostPerTick - 0.05f); energy = Math.min(energy, maxEnergy); }
private int calcDuration(int level, int baseMin) { if (level >= 10) return 0; return baseMin * 20 + (level - 1) * (baseMin / 9) * 20; }
public void activate(int id, int baseMin) { setActive(id, true); if (baseMin == 0) setDuration(id, 0); else setDuration(id, calcDuration(getLevel(id), baseMin)); }
public void onLevelUp(int id) { upgradeEnergy(); if (maxDurations[id] != 0) { int lv = getLevel(id); if (lv >= 10) setDuration(id, 0); else setDuration(id, calcDuration(lv, 60)); } }

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
        for (int i = 1; i <= 10; i++) {
            data.chance[i] = tag.getFloat("chance" + i);
            data.behaviorDone[i] = tag.getBoolean("bdone" + i);
        }
        data.eatCount = tag.getInt("eat"); data.leftClickCount = tag.getInt("lclick"); data.itemKillCount = tag.getInt("ikill");
        data.walkDist = tag.getDouble("walk"); data.breakCount = tag.getInt("break"); data.plantCount = tag.getInt("plant");
        data.placeCount = tag.getInt("place"); data.craftToolCount = tag.getInt("craft"); data.fireWaterCount = tag.getInt("fire");
        data.cultivation = tag.getLong("cultivation"); data.energy = tag.getInt("energy"); data.maxEnergy = tag.getInt("maxEnergy");
        data.energyCostPerTick = tag.getFloat("energyCost"); data.playTicks = tag.getLong("playTicks");
        data.globalAttack = tag.getDouble("globalAttack"); data.maxHealthBonus = tag.getInt("maxHealth");
        data.killHealAmount = tag.getInt("killHeal"); data.killMultiplier = tag.getDouble("killMult"); data.regenLevel = tag.getInt("regen");
        data.availablePoints = tag.getInt("points"); data.strength = tag.getInt("str"); data.speed = tag.getInt("spd");
        data.vitality = tag.getInt("vit"); data.killCounter = tag.getInt("kills"); data.isMeditating = tag.getBoolean("med");
        data.meditateTimer = tag.getInt("medTimer");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 1; i <= 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]); tag.putBoolean("active" + i, active[i]);
            tag.putInt("level" + i, levels[i]); tag.putInt("dur" + i, durations[i]);
            tag.putInt("maxDur" + i, maxDurations[i]); tag.putLong("cost" + i, upgradeCost[i]);
        }
        for (int i = 1; i <= 10; i++) { tag.putFloat("chance" + i, chance[i]); tag.putBoolean("bdone" + i, behaviorDone[i]); }
        tag.putInt("eat", eatCount); tag.putInt("lclick", leftClickCount); tag.putInt("ikill", itemKillCount);
        tag.putDouble("walk", walkDist); tag.putInt("break", breakCount); tag.putInt("plant", plantCount);
        tag.putInt("place", placeCount); tag.putInt("craft", craftToolCount); tag.putInt("fire", fireWaterCount);
        tag.putLong("cultivation", cultivation); tag.putInt("energy", energy); tag.putInt("maxEnergy", maxEnergy);
        tag.putFloat("energyCost", energyCostPerTick); tag.putLong("playTicks", playTicks);
        tag.putDouble("globalAttack", globalAttack); tag.putInt("maxHealth", maxHealthBonus);
        tag.putInt("killHeal", killHealAmount); tag.putDouble("killMult", killMultiplier); tag.putInt("regen", regenLevel);
        tag.putInt("points", availablePoints); tag.putInt("str", strength); tag.putInt("spd", speed);
        tag.putInt("vit", vitality); tag.putInt("kills", killCounter); tag.putBoolean("med", isMeditating);
        tag.putInt("medTimer", meditateTimer);
        return tag;
    }
}
