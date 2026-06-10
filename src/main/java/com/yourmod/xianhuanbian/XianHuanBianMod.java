package com.yourmod.xianhuanbian;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import java.util.*;

public class XianHuanBianMod implements ModInitializer {
    public static final String MODID = "xianhuanbian";
    public static final Identifier SYNC_BUFFS = new Identifier(MODID, "sync_buffs");
    public static final Identifier REFILL_ENERGY = new Identifier(MODID, "refill_energy");
    public static final Identifier MEDITATE_START = new Identifier(MODID, "meditate_start");
    public static final Identifier MEDITATE_STOP = new Identifier(MODID, "meditate_stop");
    public static final Identifier REQUEST_INFO = new Identifier(MODID, "request_info");
    public static final Identifier LEFT_CLICK_COUNT = new Identifier(MODID, "left_click");

    private final Map<UUID, Vec3d> lastPositions = new HashMap<>();

    private static final Set<Block> ORE_BLOCKS = new HashSet<>(Arrays.asList(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.ANCIENT_DEBRIS
    ));
    @Override
public void onInitialize() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        ToggleBuffCommand.register(dispatcher);
        BuffEventHandler.registerCommands(dispatcher);
    });

    ServerTickEvents.END_SERVER_TICK.register(server -> {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerBuffData data = PlayerBuffData.get(player);
            BuffEventHandler.applyActiveBuffs(player, data);
            UUID id = player.getUuid();
            Vec3d cur = player.getPos();
            Vec3d last = lastPositions.get(id);
            if (last != null) {
                double dist = cur.distanceTo(last);
                data.addWalkDist(dist);
            }
            lastPositions.put(id, cur);
            data.checkExp(player.experienceLevel);
            if (!data.hasAnyRing()) {
                BuffEventHandler.tryUnlockFirstRing(player, data);
            }
            data.save(player);
            if (player.age % 100 == 0) syncToClient(player, data);
        }
    });

    ServerPlayNetworking.registerGlobalReceiver(LEFT_CLICK_COUNT, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            data.addLeftClick();
        });
    });

    // 吃东西
    UseItemCallback.EVENT.register((player, world, hand) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isFood()) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                data.addEat();
            }
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    });

    ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
        if (source.getAttacker() instanceof ServerPlayerEntity sp) {
            PlayerBuffData data = PlayerBuffData.get(sp);
            if (!sp.getMainHandStack().isEmpty()) data.addItemKill();
            data.checkExp(sp.experienceLevel);
            BuffEventHandler.onKillEntity(sp, data, entity);
            BuffEventHandler.processActivity(sp, data, 0.00004f, 30, data.isMeditating());
            data.save(sp); syncToClient(sp, data);
        }
    });

    PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            PlayerBuffData data = PlayerBuffData.get(sp);
            data.addBreak();
            if (data.hasAnyRing() && ORE_BLOCKS.contains(state.getBlock())) {
                BuffEventHandler.processActivity(sp, data, 0.00001f, 10, data.isMeditating());
            }
            data.save(sp); syncToClient(sp, data);
        }
    });

    // 种植（使用 UseBlockCallback）
    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof AliasedBlockItem) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                data.addPlant();
                data.save(sp); syncToClient(sp, data);
            }
        }
        return ActionResult.PASS;
    });

    // 放置方块
    PlayerBlockPlaceEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            PlayerBuffData data = PlayerBuffData.get(sp);
            data.addPlace();
            data.save(sp); syncToClient(sp, data);
        }
    });

    // 点火/放水/喷溅药水
    UseItemCallback.EVENT.register((player, world, hand) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof BucketItem || stack.getItem() instanceof SplashPotionItem) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                data.addFireWater();
                data.save(sp); syncToClient(sp, data);
            }
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    });
        ServerPlayNetworking.registerGlobalReceiver(MEDITATE_START, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            data.setMeditating(true);
            data.setMeditateTimer(0);
            player.sendMessage(net.minecraft.text.Text.literal("开始修炼..."), true);
        });
    });
    ServerPlayNetworking.registerGlobalReceiver(MEDITATE_STOP, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            data.setMeditating(false);
            data.setMeditateTimer(0);
            player.setPose(net.minecraft.entity.EntityPose.STANDING);
            player.sendMessage(net.minecraft.text.Text.literal("结束修炼"), true);
        });
    });
    ServerPlayNetworking.registerGlobalReceiver(REQUEST_INFO, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            StringBuilder sb = new StringBuilder("========== 仙环属性 ==========\n");
            int count = 0;
            for (int i = 1; i <= 10; i++) if (data.isUnlocked(i)) count++;
            sb.append("气环数量: ").append(count).append("/10\n");
            for (int i = 1; i <= 10; i++) if (data.isUnlocked(i))
                sb.append(BuffNames.NAME[i]).append(": Lv").append(data.getLevel(i)).append("\n");
            sb.append("可用点数: ").append(data.getAvailablePoints()).append("\n");
            sb.append("力量: ").append(data.getStrength()).append(" | 速度: ").append(data.getSpeed()).append(" | 生命力: ").append(data.getVitality()).append("\n");
            sb.append("缘分:\n");
            for (int i = 1; i <= 10; i++) if (!data.isUnlocked(i))
                sb.append(BuffNames.NAME[i].charAt(0)).append(": ").append(String.format("%.4f%%", data.getChance(i)*100)).append("\n");
            player.sendMessage(net.minecraft.text.Text.literal(sb.toString()), false);
        });
    });
    ServerPlayNetworking.registerGlobalReceiver(REFILL_ENERGY, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            data.addEnergy(30);
            data.save(player);
            player.sendMessage(net.minecraft.text.Text.literal("你凝神聚气，恢复了30点能量"), true);
        });
    });

    for (int i = 1; i <= 10; i++) {
        final int id = i;
        Identifier toggleId = new Identifier(MODID, "toggle_" + id);
        ServerPlayNetworking.registerGlobalReceiver(toggleId, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                if (!data.isUnlocked(id)) {
                    player.sendMessage(net.minecraft.text.Text.literal("尚未领悟" + BuffNames.NAME[id]), false);
                    return;
                }
                boolean newActive = !data.isActive(id);
                if (newActive && data.getEnergy() < 10) {
                    player.sendMessage(net.minecraft.text.Text.literal("气不足，无法开启"), false);
                    return;
                }
                if (newActive) {
                    data.addEnergy(-10);
                    if (id == 8) BuffEventHandler.giveWeaponOnActivate(player, data);
                }
                data.setActive(id, newActive);
                data.save(player);
                syncToClient(player, data);
                player.sendMessage(net.minecraft.text.Text.literal(BuffNames.NAME[id] + "已" + (newActive ? "开启" : "关闭")), false);
            });
        });
    }
}
        private void syncToClient(ServerPlayerEntity player, PlayerBuffData data) {
        var buf = PacketByteBufs.create();
        buf.writeNbt(data.toNbt());
        ServerPlayNetworking.send(player, SYNC_BUFFS, buf);
    }
}
