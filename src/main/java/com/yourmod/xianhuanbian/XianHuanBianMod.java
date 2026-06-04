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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class XianHuanBianMod implements ModInitializer {
    public static final String MODID = "xianhuanbian";
    public static final Identifier UNLOCK_FIRST = new Identifier(MODID, "unlock_first");
    public static final Identifier TOGGLE_ALL = new Identifier(MODID, "toggle_all");
    public static final Identifier SYNC_BUFFS = new Identifier(MODID, "sync_buffs");
    public static final Identifier TOGGLE_PHASE = new Identifier(MODID, "toggle_phase");
    public static final Identifier REFILL_ENERGY = new Identifier(MODID, "refill_energy");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ToggleBuffCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerBuffData data = PlayerBuffData.get(player);
                BuffEventHandler.applyActiveBuffs(player, data);
                data.save(player);
                if (player.age % 100 == 0) syncToClient(player, data);
                processPhaseMovement(player, data);
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                BuffEventHandler.processActivity(sp, data, 0.0001f, 10);
                data.save(sp);
                syncToClient(sp, data);
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                BuffEventHandler.processActivity(sp, data, 0.00005f, 5);
                data.save(sp);
                syncToClient(sp, data);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity sp) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                BuffEventHandler.processActivity(sp, data, 0.0002f, 20);
                BuffEventHandler.handleKillExperience(sp, data, entity);
                data.save(sp);
                syncToClient(sp, data);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerBuffData data = PlayerBuffData.get(handler.player);
            syncToClient(handler.player, data);
        });

        // 全局开关
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ALL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                BuffEventHandler.toggleAllBuffs(player, data);
                data.save(player);
                syncToClient(player, data);
            });
        });

        // 穿透开关
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PHASE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                data.setPhaseEnabled(!data.isPhaseEnabled());
                player.sendMessage(net.minecraft.text.Text.literal("穿透模式：" + (data.isPhaseEnabled() ? "开启" : "关闭")), false);
                data.save(player);
            });
        });

        // 回气
        ServerPlayNetworking.registerGlobalReceiver(REFILL_ENERGY, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                data.addEnergy(30);
                player.sendMessage(net.minecraft.text.Text.literal("你凝神聚气，恢复了30点能量"), true);
                data.save(player);
            });
        });

        // 初次觉醒
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_FIRST, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                boolean hasAny = false;
                for (int i = 1; i <= 10; i++) if (data.isUnlocked(i)) hasAny = true;
                if (!hasAny) {
                    int rand = player.getRandom().nextInt(10) + 1;
                    data.setUnlocked(rand, true);
                    data.setActive(rand, true);
                    player.sendMessage(net.minecraft.text.Text.literal("你顿悟了" + BuffNames.NAME[rand] + "之力！"), false);
                    data.save(player);
                    syncToClient(player, data);
                }
            });
        });

        // 十个独立开关
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
                    if (newActive) {
                        if (data.getEnergy() < 10) {
                            player.sendMessage(net.minecraft.text.Text.literal("气不足，无法开启"), false);
                            return;
                        }
                        data.addEnergy(-10);
                    }
                    data.setActive(id, newActive);
                    data.save(player);
                    syncToClient(player, data);
                    player.sendMessage(net.minecraft.text.Text.literal(BuffNames.NAME[id] + "已" + (newActive ? "开启" : "关闭")), false);
                });
            });
        }
    }    private static void syncToClient(ServerPlayerEntity player, PlayerBuffData data) {
        var buf = PacketByteBufs.create();
        buf.writeNbt(data.toNbt());
        ServerPlayNetworking.send(player, SYNC_BUFFS, buf);
    }

    private static void processPhaseMovement(ServerPlayerEntity player, PlayerBuffData data) {
        if (!data.isPhaseEnabled() || data.getEnergy() < 5) return;
        Vec3d look = player.getRotationVector().normalize();
        double distance = 3.0;
        if (!data.isActive(5) || !player.getAbilities().flying) {
            look = new Vec3d(look.x, 0, look.z).normalize();
        }
        Vec3d targetPos = player.getPos().add(look.multiply(distance));
        BlockPos targetBlock = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);
        World world = player.getWorld();
        if (world.isAir(targetBlock) && world.isAir(targetBlock.up())) {
            player.teleport(targetPos.x, targetPos.y, targetPos.z);
            data.addEnergy(-5);
        }
    }
}
