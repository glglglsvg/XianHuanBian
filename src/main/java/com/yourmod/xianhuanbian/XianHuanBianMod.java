package com.yourmod.xianhuanbian;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import java.util.*;

public class XianHuanBianMod implements ModInitializer {
    public static final String MODID = "xianhuanbian";
    public static final Identifier UNLOCK_FIRST = new Identifier(MODID, "unlock_first");
    public static final Identifier SYNC_BUFFS = new Identifier(MODID, "sync_buffs");
    public static final Identifier REFILL_ENERGY = new Identifier(MODID, "refill_energy");

    private static final Set<Block> ORE_BLOCKS = new HashSet<>(Arrays.asList(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE,
        Blocks.NETHER_GOLD_ORE,
        Blocks.ANCIENT_DEBRIS
    ));@Override
public void onInitialize() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        ToggleBuffCommand.register(dispatcher);
        BuffEventHandler.registerCommands(dispatcher);
    });

    ServerTickEvents.END_SERVER_TICK.register(server -> {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerBuffData data = PlayerBuffData.get(player);
            BuffEventHandler.applyActiveBuffs(player, data);
            data.save(player);
            if (player.age % 100 == 0) syncToClient(player, data);
        }
    });

    AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof LivingEntity target) {
            PlayerBuffData data = PlayerBuffData.get(sp);
            BuffEventHandler.onAttackEntity(sp, data, target);
            if (!hasAnyRing(data)) unlockFirstRing(sp, data);
            data.save(sp);
            syncToClient(sp, data);
        }
        return net.minecraft.util.ActionResult.PASS;
    });

    PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            PlayerBuffData data = PlayerBuffData.get(sp);
            if (!hasAnyRing(data)) {
                unlockFirstRing(sp, data);
            } else if (ORE_BLOCKS.contains(state.getBlock())) {
                BuffEventHandler.processActivity(sp, data, 0.00001f, 10);
            }
            data.save(sp);
            syncToClient(sp, data);
        }
    });

    ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
        if (source.getAttacker() instanceof ServerPlayerEntity sp) {
            LivingEntity target = entity;
            PlayerBuffData data = PlayerBuffData.get(sp);
            if (!hasAnyRing(data)) {
                unlockFirstRing(sp, data);
            } else {
                BuffEventHandler.onKillEntity(sp, data, target);
                BuffEventHandler.processActivity(sp, data, 0.00004f, 30);
            }
            data.save(sp);
            syncToClient(sp, data);
        }
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
        PlayerBuffData data = PlayerBuffData.get(handler.player);
        syncToClient(handler.player, data);
    });

    ServerPlayNetworking.registerGlobalReceiver(UNLOCK_FIRST, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            if (!hasAnyRing(data)) {
                unlockFirstRing(player, data);
                data.save(player);
                syncToClient(player, data);
            }
        });
    });

    ServerPlayNetworking.registerGlobalReceiver(REFILL_ENERGY, (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            PlayerBuffData data = PlayerBuffData.get(player);
            data.addEnergy(30);
            player.sendMessage(net.minecraft.text.Text.literal("你凝神聚气，恢复了30点能量"), true);
            data.save(player);
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
                if (newActive) data.addEnergy(-10);
                data.setActive(id, newActive);
                data.save(player);
                syncToClient(player, data);
                player.sendMessage(net.minecraft.text.Text.literal(BuffNames.NAME[id] + "已" + (newActive ? "开启" : "关闭")), false);
            });
        });
    }
}    private static boolean hasAnyRing(PlayerBuffData data) {
        for (int i = 1; i <= 10; i++) if (data.isUnlocked(i)) return true;
        return false;
    }

    private static void unlockFirstRing(ServerPlayerEntity player, PlayerBuffData data) {
        int rand = player.getRandom().nextInt(10) + 1;
        data.setUnlocked(rand, true);
        data.setActive(rand, true);
        data.setGlobalAttack(3);
        if (rand == 1) {
            data.setMaxHealthBonus(5);
            BuffEventHandler.applyHealth(player, data);
        }
        player.sendMessage(net.minecraft.text.Text.literal("你顿悟了" + BuffNames.NAME[rand] + "之力！"), false);
    }

    private static void syncToClient(ServerPlayerEntity player, PlayerBuffData data) {
        var buf = PacketByteBufs.create();
        buf.writeNbt(data.toNbt());
        ServerPlayNetworking.send(player, SYNC_BUFFS, buf);
    }
}
