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
                BuffEventHandler.processActivity(sp, data, 0.002f);
                data.save(sp);
                syncToClient(sp, data);
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                BuffEventHandler.processActivity(sp, data, 0.001f);
                data.save(sp);
                syncToClient(sp, data);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity sp) {
                PlayerBuffData data = PlayerBuffData.get(sp);
                BuffEventHandler.processActivity(sp, data, 0.003f);
                BuffEventHandler.handleKillExperience(sp, data, entity);
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

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ALL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                BuffEventHandler.toggleAllBuffs(player, data);
                data.save(player);
                syncToClient(player, data);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PHASE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffData data = PlayerBuffData.get(player);
                data.setPhaseEnabled(!data.isPhaseEnabled());
                player.sendMessage(net.minecraft.text.Tex
