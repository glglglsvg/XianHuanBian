package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import java.util.Arrays;

public class PlayerBuffComponentImpl implements PlayerBuffComponent {
    private boolean[] unlocked = new boolean[12];
    private boolean[] active = new boolean[12];
    private float[] chance = new float[10];
    private int[] upgradeLevels = new int[12];
    private long playTicks = 0;
    private float expPool = 0;
    private float nextThreshold = 1.0f;
    private float bonusAttack = 0;
    private float bonusHealth = 0;

    public PlayerBuffComponentImpl() {
        Arrays.fill(chance, 0.01f);
    }

    @Override public boolean isUnlocked(int id) { return unlocked[id]; }
    @Override public void setUnlocked(int id, boolean v) { unlocked[id] = v; }
    @Override public boolean isActive(int id) { return active[id]; }
    @Override public void setActive(int id, boolean v) { active[id] = v; }
    @Override public float getChance(int id) { return chance[id]; }
    @Override public void setChance(int id, float v) { chance[id] = v; }
    @Override public void increaseChance(int id, float inc) { chance[id] += inc; }
    @Override public int getUpgradeLevel(int id) { return upgradeLevels[id]; }
    @Override public void setUpgradeLevel(int id, int lv) { upgradeLevels[id] = lv; }
    @Override public long getPlayTicks() { return playTicks; }
    @Override public void setPlayTicks(long t) { playTicks = t; }
    @Override public float getExpPool() { return expPool; }
    @Override public void setExpPool(float v) { expPool = v; }
    @Override public float getNextThreshold() { return nextThreshold; }
    @Override public void setNextThreshold(float v) { nextThreshold = v; }
    @Override public float getBonusAttack() { return bonusAttack; }
    @Override public void setBonusAttack(float v) { bonusAttack = v; }
    @Override public float getBonusHealth() { return bonusHealth; }
    @Override public void setBonusHealth(float v) { bonusHealth = v; }

    @Override
    public void readFromNbt(NbtCompound tag) {
        for (int i = 0; i < 12; i++) {
            unlocked[i] = tag.getBoolean("unlocked" + i);
            active[i] = tag.getBoolean("active" + i);
            upgradeLevels[i] = tag.getInt("upgrade" + i);
        }
        for (int i = 0; i < 10; i++) chance[i] = tag.getFloat("chance" + i);
        playTicks = tag.getLong("playTicks");
        expPool = tag.getFloat("expPool");
        nextThreshold = tag.getFloat("nextThreshold");
        bonusAttack = tag.getFloat("bonusAttack");
        bonusHealth = tag.getFloat("bonusHealth");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
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
    }
}
