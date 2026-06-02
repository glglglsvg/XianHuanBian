package com.yourmod.xianhuanbian;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;

public interface PlayerBuffComponent extends ComponentV3 {
    boolean isUnlocked(int id);
    void setUnlocked(int id, boolean v);
    boolean isActive(int id);
    void setActive(int id, boolean v);
    float getChance(int id);
    void setChance(int id, float v);
    void increaseChance(int id, float inc);
    int getUpgradeLevel(int id);
    void setUpgradeLevel(int id, int lv);
    long getPlayTicks();
    void setPlayTicks(long t);
    float getExpPool();
    void setExpPool(float v);
    float getNextThreshold();
    void setNextThreshold(float v);
    float getBonusAttack();
    void setBonusAttack(float v);
    float getBonusHealth();
    void setBonusHealth(float v);
}
