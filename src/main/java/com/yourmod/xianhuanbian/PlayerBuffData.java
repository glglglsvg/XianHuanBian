package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.*;

public class PlayerBuffData {
    public static final Map<UUID, PlayerBuffData> SERVER_DATA = new HashMap<>();
    private static PlayerBuffData CLIENT_CACHE = new PlayerBuffData();
    private static final Random RANDOM = new Random();

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
    private int eatCount = 0, sneakJumpCount = 0, itemKillCount = 0;
    private double walkDist = 0;
    private int breakCount = 0, plantCount = 0, placeCount = 0, craftToolCount = 0, fireWaterCount = 0;
    private int[] behaviorNeeded = new int[11];   // 初次行为需求次数

    // 缘分进度
    private int progress = 0;
    private int maxProgress = 100;

    public PlayerBuffData() {
        Arrays.fill(chance, 0.0f);
        Arrays.fill(behaviorNeeded, 0);          // 0 表示待随机初始化
        for (int i = 1; i <= 12; i++) {
            levels[i] = 1;
            upgradeCost[i] = 200;
        }
    }

    public static PlayerBuffData get(ServerPlayerEntity player) {
        return getOrCreate(player);
    }

    public static PlayerBuffData getOrCreate(ServerPlayerEntity player) {
        return SERVER_DATA.computeIfAbsent(player.getUuid(), uuid -> {
            ServerWorld world = player.getServerWorld();
            XianHuanState state = XianHuanState.get(world);
            NbtCompound tag = state.getPlayerData(uuid);
            if (tag.isEmpty()) {
                return new PlayerBuffData();
            }
            return fromNbt(tag);
        });
    }

    public void save(ServerPlayerEntity player) {
        SERVER_DATA.put(player.getUuid(), this);
        ServerWorld world = player.getServerWorld();
        XianHuanState state = XianHuanState.get(world);
        state.setPlayerData(player.getUuid(), toNbt());
    }

    public static void reset(ServerPlayerEntity player) {
        SERVER_DATA.remove(player.getUuid());
        ServerWorld world = player.getServerWorld();
        XianHuanState.get(world).removePlayer(player.getUuid());
    }
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
    public boolean isBehaviorDone(int id) { return behaviorDone[id]; }
    public void setBehaviorDone(int id) { behaviorDone[id] = true; }
    public int getBehaviorNeeded(int id) { return behaviorNeeded[id]; }
    public void setBehaviorNeeded(int id, int val) { behaviorNeeded[id] = val; }

    public int getProgress() { return progress; }
    public void setProgress(int v) { progress = Math.max(0, Math.min(v, maxProgress)); }
    public void addProgress(int v) { setProgress(progress + v); }
    public int getMaxProgress() { return maxProgress; }
    public void updateMaxProgress() {
        int count = 0;
        for (int i = 1; i <= 10; i++) if (unlocked[i]) count++;
        maxProgress = 100 + (count - 1) * 50;
    }
        public void addEat() {
        if (!behaviorDone[1]) {
            if (behaviorNeeded[1] == 0) behaviorNeeded[1] = 30 + RANDOM.nextInt(16);
            eatCount++;
            if (eatCount >= behaviorNeeded[1]) { setBehaviorDone(1); chance[1] = 0.2f; }
        }
    }
    public void addSneakJump() {
        if (!behaviorDone[2]) {
            if (behaviorNeeded[2] == 0) behaviorNeeded[2] = 30 + RANDOM.nextInt(16);
            sneakJumpCount++;
            if (sneakJumpCount >= behaviorNeeded[2]) { setBehaviorDone(2); chance[2] = 0.2f; }
        }
    }
    public void addItemKill() {
        if (!behaviorDone[3]) {
            if (behaviorNeeded[3] == 0) behaviorNeeded[3] = 30 + RANDOM.nextInt(16);
            itemKillCount++;
            if (itemKillCount >= behaviorNeeded[3]) { setBehaviorDone(3); chance[3] = 0.2f; }
        }
    }
    public void addWalkDist(double d) {
        if (!behaviorDone[4]) {
            if (behaviorNeeded[4] == 0) behaviorNeeded[4] = 1000;   // 第四环固定 1000 格
            walkDist += d;
            if (walkDist >= behaviorNeeded[4]) { setBehaviorDone(4); chance[4] = 0.2f; }
        }
    }
    public void addBreak() {
        if (!behaviorDone[5]) {
            if (behaviorNeeded[5] == 0) behaviorNeeded[5] = 30 + RANDOM.nextInt(16);
            breakCount++;
            if (breakCount >= behaviorNeeded[5]) { setBehaviorDone(5); chance[5] = 0.2f; }
        }
    }
    public void addPlant() {
        if (!behaviorDone[6]) {
            if (behaviorNeeded[6] == 0) behaviorNeeded[6] = 30 + RANDOM.nextInt(16);
            plantCount++;
            if (plantCount >= behaviorNeeded[6]) { setBehaviorDone(6); chance[6] = 0.2f; }
        }
    }
    public void addPlace() {
        if (!behaviorDone[7]) {
            if (behaviorNeeded[7] == 0) behaviorNeeded[7] = 30 + RANDOM.nextInt(16);
            placeCount++;
            if (placeCount >= behaviorNeeded[7]) { setBehaviorDone(7); chance[7] = 0.2f; }
        }
    }
    // 第八环：保留原有计数方式，背包检测在 XianHuanBianMod 中补充概率
    public void addCraftTool() {
        int needed = hasAnyRing() ? 150 : 15;
        if (!behaviorDone[8]) { craftToolCount++; if (craftToolCount >= needed) { setBehaviorDone(8); chance[8] = 0.2f; } }
    }
    public void addFireWater() {
        if (!behaviorDone[9]) {
            if (behaviorNeeded[9] == 0) behaviorNeeded[9] = 30 + RANDOM.nextInt(16);
            fireWaterCount++;
            if (fireWaterCount >= behaviorNeeded[9]) { setBehaviorDone(9); chance[9] = 0.2f; }
        }
    }
    public void checkExp(float level) {
        if (!behaviorDone[10] && level >= 2.0f) { setBehaviorDone(10); chance[10] = 0.2f; }
    }
    public boolean hasAnyRing() { for (int i = 1; i <= 10; i++) if (unlocked[i]) return true; return false; }

    public void applyBehaviorChances() {
        for (int i = 1; i <= 10; i++) {
            if (behaviorDone[i]) chance[i] = 0.2f;
            else chance[i] = 0.0f;
        }
    }

    public void resetChancesForHardMode() { Arrays.fill(chance, 0.0f); adjustChances(); }
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

    public float getCurrentChance(int id) {
        float base = 0.00001f;
        if (behaviorDone[id]) base += 0.1f;
        return base;
    }
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
            data.behaviorNeeded[i] = tag.getInt("need" + i);
        }
        data.eatCount = tag.getInt("eat");
        data.sneakJumpCount = tag.getInt("sneakjump");
        data.itemKillCount = tag.getInt("ikill");
        data.walkDist = tag.getDouble("walk"); data.breakCount = tag.getInt("break"); data.plantCount = tag.getInt("plant");
        data.placeCount = tag.getInt("place"); data.craftToolCount = tag.getInt("craft"); data.fireWaterCount = tag.getInt("fire");
        data.cultivation = tag.getLong("cultivation"); data.energy = tag.getInt("energy"); data.maxEnergy = tag.getInt("maxEnergy");
        data.energyCostPerTick = tag.getFloat("energyCost"); data.playTicks = tag.getLong("playTicks");
        data.globalAttack = tag.getDouble("globalAttack"); data.maxHealthBonus = tag.getInt("maxHealth");
        data.killHealAmount = tag.getInt("killHeal"); data.killMultiplier = tag.getDouble("killMult"); data.regenLevel = tag.getInt("regen");
        data.availablePoints = tag.getInt("points"); data.strength = tag.getInt("str"); data.speed = tag.getInt("spd");
        data.vitality = tag.getInt("vit"); data.killCounter = tag.getInt("kills"); data.isMeditating = tag.getBoolean("med");
        data.meditateTimer = tag.getInt("medTimer");
        data.progress = tag.getInt("progress");
        data.maxProgress = tag.getInt("maxProgress");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (int i = 1; i <= 12; i++) {
            tag.putBoolean("unlocked" + i, unlocked[i]); tag.putBoolean("active" + i, active[i]);
            tag.putInt("level" + i, levels[i]); tag.putInt("dur" + i, durations[i]);
            tag.putInt("maxDur" + i, maxDurations[i]); tag.putLong("cost" + i, upgradeCost[i]);
        }
        for (int i = 1; i <= 10; i++) {
            tag.putFloat("chance" + i, chance[i]);
            tag.putBoolean("bdone" + i, behaviorDone[i]);
            tag.putInt("need" + i, behaviorNeeded[i]);
        }
        tag.putInt("eat", eatCount); tag.putInt("sneakjump", sneakJumpCount); tag.putInt("ikill", itemKillCount);
        tag.putDouble("walk", walkDist); tag.putInt("break", breakCount); tag.putInt("plant", plantCount);
        tag.putInt("place", placeCount); tag.putInt("craft", craftToolCount); tag.putInt("fire", fireWaterCount);
        tag.putLong("cultivation", cultivation); tag.putInt("energy", energy); tag.putInt("maxEnergy", maxEnergy);
        tag.putFloat("energyCost", energyCostPerTick); tag.putLong("playTicks", playTicks);
        tag.putDouble("globalAttack", globalAttack); tag.putInt("maxHealth", maxHealthBonus);
        tag.putInt("killHeal", killHealAmount); tag.putDouble("killMult", killMultiplier); tag.putInt("regen", regenLevel);
        tag.putInt("points", availablePoints); tag.putInt("str", strength); tag.putInt("spd", speed);
        tag.putInt("vit", vitality); tag.putInt("kills", killCounter); tag.putBoolean("med", isMeditating);
        tag.putInt("medTimer", meditateTimer);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        return tag;
    }
}
