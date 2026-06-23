package com.yourmod.xianhuanbian;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import static net.minecraft.server.command.CommandManager.*;

public class BuffEventHandler {
    private static final UUID HEALTH_UUID = UUID.fromString("a1b2c3d4-1234-5678-9abc-def012345678");
    private static final UUID ATTACK_UUID = UUID.fromString("b2c3d4e5-2345-6789-abcd-ef0123456789");
    private static final UUID SPEED_UUID = UUID.fromString("c3d4e5f6-3456-789a-bcde-f01234567890");
    private static final UUID ABSORPTION_UUID = UUID.fromString("d4e5f6a7-4567-89ab-cdef-012345678901");
    private static final String WEAPON_TAG = "XianHuanWeapon";
    private static final Random RANDOM = new Random();

    public static void applyActiveBuffs(ServerPlayerEntity p, PlayerBuffData d) {
        float cost = d.getEnergyCostPerTick();
        PlayerEntity player = (PlayerEntity) p;

        if (d.isUnlocked(1)) {
            EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr != null) {
                double bonus = d.getMaxHealthBonus();
                EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_UUID, "xh_health", bonus, EntityAttributeModifier.Operation.ADDITION);
                attr.removeModifier(mod);
                attr.addPersistentModifier(mod);
                p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
            }
            if (d.isActive(1) && p.age % 20 == 0) {
                int regen = d.getRegenLevel();
                if (regen > 0) p.heal(regen);
            }
        }

        applyAbsorption(p, d);

        if (d.isMeditating()) {
            d.setMeditateTimer(d.getMeditateTimer() + 1);
            // 每秒增加1点进度
            if (p.age % 20 == 0) {
                d.addProgress(1);
            }
            if (d.getMeditateTimer() >= 6000) {
                d.setMeditateTimer(0);
                d.addAvailablePoints(1);
            }
            // 进度满后随机解锁气环
            if (d.getProgress() >= d.getMaxProgress()) {
                d.setProgress(0);
                int unlockedCount = 0;
                for (int i = 1; i <= 10; i++) if (d.isUnlocked(i)) unlockedCount++;
                if (unlockedCount >= 10) {
                    p.sendMessage(Text.literal("你已领悟所有气环，缘分圆满！"), false);
                } else {
                    List<Integer> candidates = new ArrayList<>();
                    for (int i = 1; i <= 10; i++) if (!d.isUnlocked(i)) candidates.add(i);
                    int chosen = candidates.get(RANDOM.nextInt(candidates.size()));
                    unlockBuff(p, d, chosen);
                    p.sendMessage(Text.literal("通过缘分领悟了" + BuffNames.NAME[chosen] + "！"), false);
                }
                d.updateMaxProgress();
            }
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false));
            p.setPose(net.minecraft.entity.EntityPose.SITTING);
        }
        applySpeedAttribute(p, d);

        for (int i = 1; i <= 12; i++) {
            if (!d.isActive(i)) continue;
            if (i == 2 || i == 3 || i == 4 || i == 5 || i == 6 || i == 7 || i == 9) {
                int dur = d.getDuration(i);
                if (dur > 0) { d.setDuration(i, dur - 1); if (dur - 1 <= 0) { d.setActive(i, false); continue; } }
            }
            if (d.getEnergy() >= cost) d.addEnergy(-(int) cost);
            else { d.setActive(i, false); continue; }
            applySingleBuff(p, i, d);
        }

        restoreDefaultAbilities(p, d);

        if (d.isActive(8)) {
            int cd = d.getDuration(8);
            if (cd <= 0) { giveRandomWeapon(p, d); d.setDuration(8, 600); }
            else d.setDuration(8, cd - 1);
        }

        if (d.isActive(11)) {
            d.setPlayTicks(d.getPlayTicks() + 1);
            if (d.getPlayTicks() % (20 * 60 * 5) == 0) {
                for (int i = 1; i <= 10; i++) if (d.isActive(i) && d.getLevel(i) < 99) upgradeBuff(p, d, i);
                p.sendMessage(Text.literal("仙环之力随修行增长……"), false);
            }
        }
    }

    private static void restoreDefaultAbilities(ServerPlayerEntity p, PlayerBuffData d) {
        if (p.isCreative() || p.isSpectator()) return;
        if (!d.isActive(5) && !d.isActive(7)) {
            exitObserverMode(p);
        }
    }
    private static void applySpeedAttribute(ServerPlayerEntity p, PlayerBuffData d) {
    var moveAttr = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    if (moveAttr != null) {
        double speedBonus = d.getSpeed() * 0.02;
        EntityAttributeModifier mod = new EntityAttributeModifier(SPEED_UUID, "xh_speed", speedBonus, EntityAttributeModifier.Operation.ADDITION);
        moveAttr.removeModifier(mod);
        moveAttr.addPersistentModifier(mod);
    }
}

private static void applyAbsorption(ServerPlayerEntity p, PlayerBuffData d) {
    int vitality = d.getVitality();
    if (vitality <= 0) return;
    float maxAbsorption = vitality * 2.0f;
    float current = p.getAbsorptionAmount();
    if (current < maxAbsorption) {
        if (p.age % 20 == 0) {
            p.setAbsorptionAmount(Math.min(maxAbsorption, current + 1.0f));
        }
    }
}

private static void applySingleBuff(ServerPlayerEntity p, int id, PlayerBuffData d) {
    int lv = d.getLevel(id);
    double strBonus = d.getStrength() * 0.5;
    if (d.getGlobalAttack() + strBonus > 0) {
        var attr = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attr != null) {
            double finalAttack = d.getGlobalAttack() + strBonus;
            if (d.isActive(10)) finalAttack *= d.getKillMultiplier();
            var mod = new EntityAttributeModifier(ATTACK_UUID, "xh_attack", finalAttack, EntityAttributeModifier.Operation.ADDITION);
            if (!attr.hasModifier(mod)) attr.addTemporaryModifier(mod);
        }
    }
    switch (id) {
        case 2: p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, d.getDuration(id) > 0 ? d.getDuration(id) : 100, lv - 1, false, false)); break;
        case 4: p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, d.getDuration(id) > 0 ? d.getDuration(id) : 100, lv - 1, false, false)); break;
        case 5:
            p.getAbilities().allowFlying = true;
            p.getAbilities().flying = true;
            p.sendAbilitiesUpdate();
            break;
        case 6: p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, d.getDuration(id) > 0 ? d.getDuration(id) : 100, 0, false, false)); break;
        case 7:
            enterObserverMode(p);
            break;
        case 8: p.getServerWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(16), e -> e != p)
                .forEach(e -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false))); break;
        case 12: p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate(); break;
    }
}
 public static double attackEntity(ServerPlayerEntity p, PlayerBuffData d, LivingEntity target) {
    double totalDamage = 0;

    if (d.isActive(5)) {
        int lv = d.getLevel(5);
        float power = (float) lv;
        World world = p.getWorld();
        world.createExplosion(p, target.getX(), target.getY(), target.getZ(), power, true, World.ExplosionSourceType.MOB);
    }

    if (d.isActive(3)) {
        int lv = d.getLevel(3);
        double critMultiplier = 2.0 + (lv - 1) * 0.5;
        if (!p.isOnGround()) critMultiplier += 0.5;
        totalDamage = critMultiplier * (d.getGlobalAttack() + d.getStrength() * 0.5);
        target.damage(p.getDamageSources().mobAttack(p), (float) totalDamage);
    }
    if (d.isActive(6)) {
        float mul = 2.0f + (d.getLevel(6) - 1) * 0.8f;
        float extraDmg = mul * (float)(d.getGlobalAttack() + d.getStrength() * 0.5);
        totalDamage += extraDmg;
        target.damage(p.getDamageSources().mobAttack(p), extraDmg);
    }
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
    if (d.isActive(10) && totalDamage > 0) p.heal((float) (totalDamage * 0.2));
    return totalDamage;
}

public static void onKillEntity(ServerPlayerEntity p, PlayerBuffData d, LivingEntity target) {
    d.addCultivation(2);
    if (d.isActive(10)) {
        p.heal(d.getKillHealAmount());
    }
    d.addKill();
}
    public static void processActivity(ServerPlayerEntity p, PlayerBuffData d, float probInc, long cultivationInc, boolean isMeditating) {
    d.addCultivation(cultivationInc); boolean all10 = true;
    for (int i = 0; i < 10; i++) {
        if (!d.isUnlocked(i + 1)) { all10 = false; d.increaseChance(i, probInc); float effectiveChance = d.getEffectiveChance(i + 1, isMeditating); if (p.getRandom().nextFloat() < effectiveChance) { unlockBuff(p, d, i + 1); if (!d.hasAnyRing()) d.resetChancesForHardMode(); d.adjustChances(); } }
    }
    for (int i = 1; i <= 10; i++) { if (d.isUnlocked(i) && d.getLevel(i) < 99 && d.getCultivation() >= d.getUpgradeCost(i)) upgradeBuff(p, d, i); }
    if (all10 && d.isUnlocked(11) && !d.isUnlocked(12)) { boolean allActive = true; for (int i = 1; i <= 11; i++) if (!d.isActive(i)) allActive = false; if (allActive) { d.setUnlocked(12, true); p.sendMessage(Text.literal("修行圆满"), false); } }
}

public static boolean tryUnlockFirstRing(ServerPlayerEntity p, PlayerBuffData d) {
    if (d.hasAnyRing()) return false;
    d.applyBehaviorChances();
    for (int i = 1; i <= 10; i++) {
        if (p.getRandom().nextFloat() < d.getChance(i)) {
            d.setUnlocked(i, true); d.setActive(i, true); d.setLevel(i, 1);
            if (i == 1) { d.setMaxHealthBonus(5); d.setRegenLevel(1); applyHealth(p, d); }
            if (i == 10) { d.setKillHealAmount(5); d.setKillMultiplier(0.5); }
            if (i == 5) d.setDuration(i, 60 * 20);
            if (i == 7) { d.setMaxDuration(7, 1); d.activate(7, 1); }
            double newAttack = Math.pow(d.getGlobalAttack() + 2, 1.5);
            d.setGlobalAttack(newAttack);
            d.onLevelUp(i);
            d.resetChancesForHardMode();
            p.sendMessage(Text.literal("你顿悟了" + BuffNames.NAME[i] + "之力！"), false);
            return true;
        }
    }
    return false;
}

public static void unlockBuff(ServerPlayerEntity p, PlayerBuffData d, int id) {
    d.setUnlocked(id, true); d.setActive(id, true); d.setLevel(id, 1);
    if (id == 1) { d.setMaxHealthBonus(5); d.setRegenLevel(1); applyHealth(p, d); }
    if (id == 10) { d.setKillHealAmount(5); d.setKillMultiplier(0.5); }
    if (id == 5) d.setDuration(id, 60 * 20);
    if (id == 7) { d.setMaxDuration(7, 1); d.activate(7, 1); }
    double newAttack = Math.pow(d.getGlobalAttack() + 2, 1.5);
    d.setGlobalAttack(newAttack);
    d.onLevelUp(id);
    p.sendMessage(Text.literal("领悟新环：" + BuffNames.NAME[id]), false);
}

private static void upgradeBuff(ServerPlayerEntity p, PlayerBuffData d, int id) {
    d.setCultivation(d.getCultivation() - d.getUpgradeCost(id));
    d.setLevel(id, d.getLevel(id) + 1);
    d.setUpgradeCost(id, (long) (200 * Math.pow(2, d.getLevel(id) - 1)));
    double newAttack = Math.pow(d.getGlobalAttack() + 2, 1.5);
    d.setGlobalAttack(newAttack);
    if (id == 1) { d.setMaxHealthBonus(d.getMaxHealthBonus() + 5); d.setRegenLevel(d.getLevel(id)); applyHealth(p, d); }
    if (id == 10) { d.setKillHealAmount(d.getKillHealAmount() + 5); d.setKillMultiplier(Math.min(5.0, d.getKillMultiplier() + 0.5)); }
    d.onLevelUp(id);
    p.sendMessage(Text.literal(BuffNames.NAME[id] + " 升阶！"), false);
}

public static void applyHealth(ServerPlayerEntity p, PlayerBuffData d) {
    EntityAttributeInstance attr = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
    if (attr != null) {
        double bonus = d.getMaxHealthBonus() + d.getVitality() * 1.0;
        EntityAttributeModifier mod = new EntityAttributeModifier(HEALTH_UUID, "xh_health", bonus, EntityAttributeModifier.Operation.ADDITION);
        attr.removeModifier(mod);
        attr.addPersistentModifier(mod);
        p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
    }
}
        private static void giveRandomWeapon(ServerPlayerEntity p, PlayerBuffData d) {
        for (ItemStack stack : p.getInventory().main) if (stack.hasNbt() && stack.getNbt().contains(WEAPON_TAG)) return;
        for (ItemStack stack : p.getInventory().offHand) if (stack.hasNbt() && stack.getNbt().contains(WEAPON_TAG)) return;
        int lv = d.getLevel(8);
        List<Item> weapons = new ArrayList<>();
        if (lv <= 1) weapons = new ArrayList<>(Arrays.asList(Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.STONE_SWORD, Items.STONE_AXE));
        else if (lv <= 3) weapons = new ArrayList<>(Arrays.asList(Items.IRON_SWORD, Items.IRON_AXE));
        else if (lv <= 5) weapons = new ArrayList<>(Arrays.asList(Items.DIAMOND_SWORD, Items.DIAMOND_AXE));
        else weapons = new ArrayList<>(Arrays.asList(Items.NETHERITE_SWORD, Items.NETHERITE_AXE));
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
        if (enchant == Enchantments.SWEEPING && !(chosen instanceof SwordItem)) enchant = Enchantments.SHARPNESS;
        weaponStack.addEnchantment(enchant, Math.min(lv, 10));

        if (!p.getInventory().insertStack(weaponStack)) p.dropItem(weaponStack, false);
        if (chosen == Items.BOW || chosen == Items.CROSSBOW) {
            ItemStack arrows = new ItemStack(Items.ARROW, 3);
            if (!p.getInventory().insertStack(arrows)) p.dropItem(arrows, false);
        }
    }

    public static void giveWeaponOnActivate(ServerPlayerEntity p, PlayerBuffData d) {
        giveRandomWeapon(p, d);
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("getring")
            .then(argument("id", IntegerArgumentType.integer(1, 12))
            .executes(ctx -> {
                int id = IntegerArgumentType.getInteger(ctx, "id");
                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                PlayerBuffData data = PlayerBuffData.get(player);
                if (data.isUnlocked(id)) {
                    ctx.getSource().sendError(Text.literal("你已经拥有" + BuffNames.NAME[id] + "！"));
                    return 0;
                }
                data.setUnlocked(id, true);
                data.setActive(id, true);
                data.setLevel(id, 1);
                if (id == 1) { data.setMaxHealthBonus(5); data.setRegenLevel(1); applyHealth(player, data); }
                if (id == 10) { data.setKillHealAmount(5); data.setKillMultiplier(0.5); }
                if (id == 5) data.setDuration(id, 60 * 20);
                if (id == 7) { data.setMaxDuration(7, 1); data.activate(7, 1); }
                double newAttack = Math.pow(data.getGlobalAttack() + 2, 1.5);
                data.setGlobalAttack(newAttack);
                data.onLevelUp(id);
                data.save(player);
                ctx.getSource().sendFeedback(() -> Text.literal("你获得了" + BuffNames.NAME[id] + "！"), false);
                return 1;
            }))
        );
    }

    private static void enterObserverMode(ServerPlayerEntity player) {
        player.getAbilities().invulnerable = true;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.noClip = true;
        player.setOnGround(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().creativeMode = true;
        }
        player.sendAbilitiesUpdate();
    }

    private static void exitObserverMode(ServerPlayerEntity player) {
        player.getAbilities().invulnerable = false;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.noClip = false;
        player.setOnGround(true);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().creativeMode = false;
        }
        player.sendAbilitiesUpdate();
    }

    public static void tickObserverMode(ServerPlayerEntity player, PlayerBuffData data) {
        if (!data.isActive(7)) return;
        if (data.getDuration(7) <= 0) {
            exitObserverMode(player);
            data.setActive(7, false);
        }
    }
}
                                                
