package com.yourmod.xianhuanbian;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

public class BuffEventHandler {
    private static final UUID HEALTH_UUID = UUID.fromString("a1b2c3d4-1234-5678-9abc-def012345678");
    private static final UUID ATTACK_UUID = UUID.randomUUID();
    private static final String WEAPON_TAG = "XianHuanWeapon";
    private static final Random RANDOM = new Random();

    public static void applyActiveBuffs(ServerPlayerEntity p, PlayerBuffData d) {
        float cost = d.getEnergyCostPerTick();

        // 每tick重置能力状态，避免残留（修复右键问题）
        PlayerEntity player = (PlayerEntity) p;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.getAbilities().invulnerable = false;
        player.noClip = false;
        player.sendAbilitiesUpdate();

        // 第一环常驻生命上限
        if (d.isUnlocked(1)) {
            EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr != null) {
                double bonus = d.getMaxHealthBonus();
                EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_UUID, "xh_health", bonus, EntityAttributeModifier.Operation.ADDITION);
                attr.removeModifier(mod);
                attr.addPersistentModifier(mod);
                p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
            }
        }

        for (int i = 1; i <= 12; i++) {
            if (!d.isActive(i)) continue;
            // 时间限制处理
            if (i == 2 || i == 3 || i == 4 || i == 5 || i == 6 || i == 7 || i == 9) {
                int dur = d.getDuration(i);
                if (dur > 0) {
                    d.setDuration(i, dur - 1);
                    if (dur - 1 <= 0) {
                        d.setActive(i, false);
                        continue;
                    }
                }
            }
            if (d.getEnergy() >= cost) {
                d.addEnergy(-(int) cost);
            } else {
                d.setActive(i, false);
                continue;
            }
            applySingleBuff(p, i, d);
        }

        // 第八环武器定时给予（30秒）
        if (d.isActive(8)) {
            int cd = d.getDuration(8);
            if (cd <= 0) {
                giveRandomWeapon(p, d);
                d.setDuration(8, 600);
            } else {
                d.setDuration(8, cd - 1);
            }
        }

        // 第十一环自动升级
        if (d.isActive(11)) {
            d.setPlayTicks(d.getPlayTicks() + 1);
            if (d.getPlayTicks() % (20 * 60 * 5) == 0) {
                for (int i = 1; i <= 10; i++) {
                    if (d.isActive(i) && d.getLevel(i) < 99) {
                        upgradeBuff(p, d, i);
                    }
                }
                p.sendMessage(Text.literal("仙环之力随修行增长……"), false);
            }
        }
    }
    private static void applySingleBuff(ServerPlayerEntity p, int id, PlayerBuffData d) {
    int lv = d.getLevel(id);
    if (d.getGlobalAttack() > 0) {
        var attr = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            double finalAttack = d.getGlobalAttack();
            if (d.isActive(10)) finalAttack *= d.getKillMultiplier();
            var mod = new EntityAttributeModifier(ATTACK_UUID, "xh_attack", finalAttack, EntityAttributeModifier.Operation.ADDITION);
            if (!attr.hasModifier(mod)) attr.addTemporaryModifier(mod);
        }
    }
    switch (id) {
        case 2: p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, d.getDuration(id) > 0 ? d.getDuration(id) : 100, lv - 1, false, false)); break;
        case 4: p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, d.getDuration(id) > 0 ? d.getDuration(id) : 100, lv - 1, false, false)); break;
        case 5: p.getAbilities().allowFlying = true; p.sendAbilitiesUpdate(); break;
        case 6: p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, d.getDuration(id) > 0 ? d.getDuration(id) : 100, 0, false, false)); break;
        case 7: p.getAbilities().allowFlying = true; p.getAbilities().invulnerable = true; p.setOnGround(false); p.noClip = true; p.sendAbilitiesUpdate(); break;
        case 8: p.getServerWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(16), e -> e != p)
                .forEach(e -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false))); break;
        case 12: p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate(); break;
    }
}

// 攻击实体附加效果（第三环必定暴击）
public static void onAttackEntity(ServerPlayerEntity p, PlayerBuffData d, LivingEntity target) {
    // 第三环必定暴击
    if (d.isActive(3)) {
        int lv = d.getLevel(3);
        double critMultiplier = 2.0 + (lv - 1) * 0.5; // 1级2倍，每级+0.5倍
        if (!p.isOnGround()) {
            critMultiplier += 0.5; // 跳跃额外加0.5倍
        }
        // 必定暴击伤害
        target.damage(p.getDamageSources().mobAttack(p), (float) (critMultiplier * d.getGlobalAttack()));
    }

    // 第五环空气屏障
    if (d.isActive(5)) {
        BlockPos pos = target.getBlockPos();
        World world = p.getWorld();
        world.setBlockState(pos, net.minecraft.block.Blocks.BARRIER.getDefaultState());
        target.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        p.getServer().execute(() -> world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState()));
    }

    // 第六环伤害倍率
    if (d.isActive(6)) {
        float mul = 2.0f + (d.getLevel(6) - 1) * 0.8f;
        target.damage(p.getDamageSources().mobAttack(p), mul * (float) d.getGlobalAttack());
    }

    // 第九环负面效果
    if (d.isActive(9)) {
        int dur = d.getDuration(9) == 0 ? (d.getLevel(9) < 10 ? 10 * 20 : 600 * 20) : d.getDuration(9);
        List<StatusEffectInstance> effects = Arrays.asList(
            new StatusEffectInstance(StatusEffects.WITHER, dur, 1),
            new StatusEffectInstance(StatusEffects.POISON, dur, 1),
            new StatusEffectInstance(StatusEffects.SLOWNESS, dur, 2),
            new StatusEffectInstance(StatusEffects.WEAKNESS, dur, 1)
        );
        target.addStatusEffect(effects.get(RANDOM.nextInt(effects.size())));
        if (Math.random() < 0.1) {
            net.minecraft.entity.LightningEntity lightning = new net.minecraft.entity.LightningEntity(
                net.minecraft.entity.EntityType.LIGHTNING_BOLT, target.getWorld());
            lightning.setPosition(target.getPos());
            target.getWorld().spawnEntity(lightning);
        }
    }
}

public static void onKillEntity(ServerPlayerEntity p, PlayerBuffData d, LivingEntity target) {
    if (d.isActive(10)) {
        p.heal(d.getKillHealAmount());
        double mul = d.getKillMultiplier();
        d.addCultivation((long) (20 * mul));
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
                unlockBuff(p, d, i + 1);
                d.adjustChances();
            }
        }
    }
    for (int i = 1; i <= 10; i++) {
        if (d.isUnlocked(i) && d.getLevel(i) < 99 && d.getCultivation() >= d.getUpgradeCost(i)) {
            upgradeBuff(p, d, i);
        }
    }
    if (all10 && d.isUnlocked(11) && !d.isUnlocked(12)) {
        boolean allActive = true;
        for (int i = 1; i <= 11; i++) if (!d.isActive(i)) allActive = false;
        if (allActive) {
            d.setUnlocked(12, true);
            p.sendMessage(Text.literal("修行圆满"), false);
        }
    }
}

private static void unlockBuff(ServerPlayerEntity p, PlayerBuffData d, int id) {
    d.setUnlocked(id, true);
    d.setActive(id, true);
    d.setLevel(id, 1);
    if (id == 1) { d.setMaxHealthBonus(5); applyHealth(p, d); }
    if (id == 10) { d.setKillHealAmount(5); d.setKillMultiplier(0.5); }
    if (id == 5) d.setDuration(id, 60 * 20);
    double newAttack = Math.pow(d.getGlobalAttack() + 2, 1.5);
    d.setGlobalAttack(newAttack);
    d.onLevelUp(id);
    p.sendMessage(Text.literal("领悟新环：" + BuffNames.NAME[id]), false);
}

private static void upgradeBuff(ServerPlayerEntity p, PlayerBuffData d, int id) {
    d.setCultivation(d.getCultivation() - d.getUpgradeCost(id));
    d.setLevel(id, d.getLevel(id) + 1);
    d.setUpgradeCost(id, (long) Math.pow(200, d.getLevel(id) + 1));
    double newAttack = Math.pow(d.getGlobalAttack() + 2, 1.5);
    d.setGlobalAttack(newAttack);
    if (id == 1) { d.setMaxHealthBonus(d.getMaxHealthBonus() + 5); applyHealth(p, d); }
    if (id == 10) {
        d.setKillHealAmount(d.getKillHealAmount() + 5);
        d.setKillMultiplier(Math.min(5.0, d.getKillMultiplier() + 0.5));
    }
    d.onLevelUp(id);
    p.sendMessage(Text.literal(BuffNames.NAME[id] + " 升阶！"), false);
}

public static void applyHealth(ServerPlayerEntity p, PlayerBuffData d) {
    EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
    if (attr != null) {
        double bonus = d.getMaxHealthBonus();
        EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_UUID, "xh_health", bonus, EntityAttributeModifier.Operation.ADDITION);
        attr.removeModifier(mod);
        attr.addPersistentModifier(mod);
        p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
    }
}
    private static void giveRandomWeapon(ServerPlayerEntity p, PlayerBuffData d) {
    for (ItemStack stack : p.getInventory().main) {
        if (stack.hasNbt() && stack.getNbt().contains(WEAPON_TAG)) return;
    }
    for (ItemStack stack : p.getInventory().offHand) {
        if (stack.hasNbt() && stack.getNbt().contains(WEAPON_TAG)) return;
    }

    int lv = d.getLevel(8);
    List<Item> weapons = new ArrayList<>();
    if (lv <= 1) {
        weapons = Arrays.asList(Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.STONE_SWORD, Items.STONE_AXE);
    } else if (lv <= 3) {
        weapons = Arrays.asList(Items.IRON_SWORD, Items.IRON_AXE);
    } else if (lv <= 5) {
        weapons = Arrays.asList(Items.DIAMOND_SWORD, Items.DIAMOND_AXE);
    } else {
        weapons = Arrays.asList(Items.NETHERITE_SWORD, Items.NETHERITE_AXE);
    }
    if (RANDOM.nextBoolean()) weapons.add(Items.BOW);
    if (RANDOM.nextBoolean()) weapons.add(Items.CROSSBOW);

    Item chosen = weapons.get(RANDOM.nextInt(weapons.size()));
    ItemStack weaponStack = new ItemStack(chosen);
    weaponStack.setDamage(weaponStack.getMaxDamage() - 3);
    weaponStack.getOrCreateNbt().putBoolean(WEAPON_TAG, true);

    List<Enchantment> attackEnchants = Arrays.asList(
        Enchantments.SHARPNESS, Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT,
        Enchantments.LOOTING, Enchantments.SWEEPING
    );
    Enchantment enchant = attackEnchants.get(RANDOM.nextInt(attackEnchants.size()));
    if (enchant == Enchantments.SWEEPING && !(chosen instanceof SwordItem)) {
        enchant = Enchantments.SHARPNESS;
    }
    weaponStack.addEnchantment(enchant, Math.min(lv, 10));
    if (!p.getInventory().insertStack(weaponStack)) {
        p.dropItem(weaponStack, false);
    }
    if (chosen == Items.BOW || chosen == Items.CROSSBOW) {
        ItemStack arrows = new ItemStack(Items.ARROW, 3);
        if (!p.getInventory().insertStack(arrows)) {
            p.dropItem(arrows, false);
        }
    }
}

// 主动开启第八环时调用的武器给予
public static void giveWeaponOnActivate(ServerPlayerEntity p, PlayerBuffData d) {
    giveRandomWeapon(p, d);
}
        public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("getring").then(literal("8").executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
            PlayerBuffData data = PlayerBuffData.get(player);
            if (data.isUnlocked(8)) {
                ctx.getSource().sendError(Text.literal("你已经拥有第八气环！"));
                return 0;
            }
            data.setUnlocked(8, true);
            data.setActive(8, true);
            data.setLevel(8, 1);
            double newAttack = Math.pow(data.getGlobalAttack() + 2, 1.5);
            data.setGlobalAttack(newAttack);
            data.onLevelUp(8);
            data.save(player);
            ctx.getSource().sendFeedback(() -> Text.literal("你获得了第八气环！"), false);
            return 1;
        })));
    }
}
