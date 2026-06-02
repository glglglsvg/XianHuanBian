package com.yourmod.xianhuanbian;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class XianHuanBianMod implements ModInitializer {
    public static final String MODID = "xianhuanbian";
    public static final Identifier UNLOCK_FIRST = new Identifier(MODID, "unlock_first");
    public static final Identifier TOGGLE_ALL = new Identifier(MODID, "toggle_all");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ToggleBuffCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(player);
                if (data != null) {
                    BuffEventHandler.applyActiveBuffs(player, data);
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(sp);
                if (data != null) {
                    BuffEventHandler.processActivity(sp, data, 0.002f);
                    ModComponents.BUFF_DATA.sync(sp);
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(sp);
                if (data != null) {
                    BuffEventHandler.processActivity(sp, data, 0.001f);
                    ModComponents.BUFF_DATA.sync(sp);
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity sp) {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(sp);
                if (data != null) {
                    BuffEventHandler.processActivity(sp, data, 0.003f);
                    BuffEventHandler.handleKillExperience(sp, data, entity);
                    ModComponents.BUFF_DATA.sync(sp);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerBuffComponent data = ModComponents.BUFF_DATA.get(handler.player);
            if (data != null) ModComponents.BUFF_DATA.sync(handler.player);
        });

        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_FIRST, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(player);
                if (data != null) {
                    boolean hasAny = false;
                    for (int i = 1; i <= 10; i++) {
                        if (data.isUnlocked(i)) { hasAny = true; break; }
                    }
                    if (!hasAny) {
                        int rand = player.getRandom().nextInt(10) + 1;
                        data.setUnlocked(rand, true);
                        data.setActive(rand, true);
                        player.sendMessage(net.minecraft.text.Text.literal("你顿悟了" + BuffNames.NAME[rand] + "之力！"), false);
                        ModComponents.BUFF_DATA.sync(player);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ALL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                PlayerBuffComponent data = ModComponents.BUFF_DATA.get(player);
                if (data != null) {
                    BuffEventHandler.toggleAllBuffs(player, data);
                    ModComponents.BUFF_DATA.sync(player);
                }
            });
        });
    }
}
