package com.yourmod.xianhuanbian;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import java.util.*;

public class BuffEventHandler {
    private static final UUID[] ATTACK_UUIDS = new UUID[12];
    static { for(int i=0;i<12;i++) ATTACK_UUIDS[i]=UUID.randomUUID(); }

    private static final int ENERGY_COST_PER_TICK = 1;

    private static double getAttackBonus(int level) {
        return 0.5 * Math.pow(2, level - 1);
    }

    public static void applyActiveBuffs(ServerPlayerEntity p, PlayerBuffData d) {
        for (int i=1;i<=12;i++) {
            if (d.isActive(i)) {
                if (d.getEnergy() >= ENERGY_COST_PER_TICK) {
                    d.addEnergy(-ENERGY_COST_PER_TICK);
                } else {
                    d.setActive(i, false);
                    continue;
                }
                if (!d.isPermanent(i) && d.getAbilityTimer(i) > 0) {
                    d.setAbilityTimer(i, d.getAbilityTimer(i) - 1);
                    if (d.getAbilityTimer(i) <= 0) {
                        d.setActive(i, false);
                        continue;
                    }
                }
                applySingleBuff(p, i, d);
            }
        }
        if (d.isActive(11)) {
            d.setPlayTicks(d.getPlayTicks() + 1);
            if (d.getPlayTicks() % (20 * 60 * 5) == 0) {
                for (int i = 1; i <= 10; i++) {
                    if (d.isActive(i) && d.getLevel(i) < 10) {
                        d.setLevel(i, d.getLevel(i) + 1);
                    }
                }
                p.sendMessage(net.minecraft.text.Text.literal("仙环之力随修行增长……"), false);
            }
        }
    }
    private static void applySingleBuff(ServerPlayerEntity p, int id, PlayerBuffData d) {
    int lv = d.getLevel(id);
    if (id <= 9) {
        var attr = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            double attack = getAttackBonus(lv);
            var mod = new EntityAttributeModifier(ATTACK_UUIDS[id-1], "xh_attack_" + id, attack, EntityAttributeModifier.Operation.ADDITION);
            if (!attr.hasModifier(mod)) attr.addTemporaryModifier(mod);
        }
    }
    switch (id) {
        case 1: if (p.age % 20 == 0) p.heal(1); break;
        case 3: p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, false, false)); break;
        case 5:
            p.getAbilities().allowFlying = true;
            p.sendAbilitiesUpdate();
            break;
        case 6:
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, false, false));
            break;
        case 8:
            p.getServerWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(16), e -> e != p)
                    .forEach(e -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false)));
            break;
    }
}

public static void processActivity(ServerPlayerEntity p, PlayerBuffData d, float probInc, long cultivationInc) {
    d.addCultivation(cultivationInc);
    boolean all10 = true;
    for (int i = 0; i < 10; i++) {
        if (!d.isUnlocked(i + 1)) {
            all10 = false;
            d.increaseChance(i, probInc);
            if (p.getRandom().nextFloat() < d.getChance(i)) {
                d.setUnlocked(i + 1, true);
                d.setActive(i + 1, true);
                d.setLevel(i + 1, 1);
                if (i + 1 == 6) d.setAbilityTimer(i + 1, 600);
                p.sendMessage(net.minecraft.text.Text.literal("领悟新环：" + BuffNames.NAME[i + 1]), false);
            }
        }
    }
    // 自动修为升级
    for (int i = 1; i <= 10; i++) {
        if (d.isUnlocked(i) && d.getLevel(i) < 10 && d.getCultivation() >= d.getUpgradeCost(i)) {
            d.setCultivation(d.getCultivation() - d.getUpgradeCost(i));
            d.setLevel(i, d.getLevel(i) + 1);
            d.setUpgradeCost(i, d.getUpgradeCost(i) * 2);
            p.sendMessage(net.minecraft.text.Text.literal(BuffNames.NAME[i] + " 升阶！"), false);
        }
    }
    if (all10 && !d.isUnlocked(11)) {
        d.setUnlocked(11, true);
        p.sendMessage(net.minecraft.text.Text.literal("仙环归一，拾壹·仙变已解锁！"), false);
    }
    if (all10 && d.isUnlocked(11) && !d.isUnlocked(12)) {
        boolean allActive = true;
        for (int i = 1; i <= 11; i++) if (!d.isActive(i)) allActive = false;
        if (allActive) {
            d.setUnlocked(12, true);
            p.sendMessage(net.minecraft.text.Text.literal("不灭之环已降下！"), false);
        }
    }
}   
    public static void handleKillExperience(ServerPlayerEntity p, PlayerBuffData d, LivingEntity t) {
        if (!d.isActive(10)) return;
        float gained = t instanceof net.minecraft.entity.player.PlayerEntity ? 10f : 1f + p.getRandom().nextFloat() * 2f;
        d.setExpPool(d.getExpPool() + gained);
        while (d.getExpPool() >= d.getNextThreshold()) {
            d.setExpPool(d.getExpPool() - d.getNextThreshold());
            d.setBonusAttack(d.getBonusAttack() + 2);
            d.setBonusHealth(d.getBonusHealth() + 2);
            d.setNextThreshold(d.getNextThreshold() * 5);
            var hp = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hp != null) hp.setBaseValue(hp.getBaseValue() + 2.0);
        }
    }

    public static void toggleAllBuffs(ServerPlayerEntity p, PlayerBuffData d) {
        boolean any = false;
        for (int i = 1; i <= 12; i++) if (d.isUnlocked(i) && d.isActive(i)) any = true;
        boolean ns = !any;
        for (int i = 1; i <= 12; i++) if (d.isUnlocked(i)) d.setActive(i, ns);
        p.sendMessage(net.minecraft.text.Text.literal(ns ? "所有仙环已开启" : "所有仙环已关闭"), false);
    }
}
