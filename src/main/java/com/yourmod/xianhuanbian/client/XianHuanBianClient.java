package com.yourmod.xianhuanbian.client;

import com.yourmod.xianhuanbian.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class XianHuanBianClient implements ClientModInitializer {
    private static final Vector3f[] C = new Vector3f[]{
        null, new Vector3f(1,0.2f,0.2f), new Vector3f(1,0.6f,0), new Vector3f(1,1,0),
        new Vector3f(0.2f,1,0.2f), new Vector3f(0.2f,1,1), new Vector3f(0.2f,0.4f,1),
        new Vector3f(0.6f,0.2f,1), new Vector3f(1,0.5f,0.8f), new Vector3f(0.8f,0.1f,0.1f),
        new Vector3f(1,0.8f,0), new Vector3f(1,1,1), new Vector3f(0,0,0)
    };
    private static final int P = 6;
    private static final float R = 0.5f, S = 0.08f;
    private static int lc = 0;
    private static long lt = 0;
    private static KeyBinding toggleAllKey, phaseKey, refillKey;
    private static final Map<Integer, KeyBinding> singleKeys = new HashMap<>();

    private static final int[] KEY_CODES = {
        GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O,
        GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
        GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M,
        GLFW.GLFW_KEY_P
    };

    @Override
    public void onInitializeClient() {
        toggleAllKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.xianhuanbian.toggle_all", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.xianhuanbian"));
        phaseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.xianhuanbian.toggle_phase", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.xianhuanbian"));
        refillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.xianhuanbian.refill_energy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.xianhuanbian"));

        for (int i = 1; i <= 10; i++) {
            KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xianhuanbian.toggle_" + i, InputUtil.Type.KEYSYM, KEY_CODES[i-1], "category.xianhuanbian"));
            singleKeys.put(i, kb);
        }

        ClientPlayNetworking.registerGlobalReceiver(XianHuanBianMod.SYNC_BUFFS, (client, handler, buf, responseSender) -> {
            var tag = buf.readNbt();
            if (tag != null) PlayerBuffData.updateClientFromNbt(tag);
        });    ClientTickEvents.END_CLIENT_TICK.register(client -> {
        if (client.player == null || client.world == null) return;
        PlayerBuffData d = PlayerBuffData.getClient();

        while (toggleAllKey.wasPressed()) ClientPlayNetworking.send(XianHuanBianMod.TOGGLE_ALL, PacketByteBufs.empty());
        while (phaseKey.wasPressed()) ClientPlayNetworking.send(XianHuanBianMod.TOGGLE_PHASE, PacketByteBufs.empty());
        while (refillKey.wasPressed()) ClientPlayNetworking.send(XianHuanBianMod.REFILL_ENERGY, PacketByteBufs.empty());

        for (int i = 1; i <= 10; i++) {
            KeyBinding kb = singleKeys.get(i);
            if (kb != null && kb.wasPressed()) {
                ClientPlayNetworking.send(new Identifier("xianhuanbian", "toggle_" + i), PacketByteBufs.empty());
            }
        }

        for (int i = 1; i <= 12; i++) if (d.isActive(i)) spawnRing(client, client.player, i, C[i]);

        if (client.options.attackKey.wasPressed()) {
            long now = System.currentTimeMillis();
            if (now - lt > 2000) lc = 0;
            lc++; lt = now;
            if (lc >= 5) {
                boolean has = false;
                for (int i = 1; i <= 10; i++) if (d.isUnlocked(i)) has = true;
                if (!has) {
                    ClientPlayNetworking.send(XianHuanBianMod.UNLOCK_FIRST, PacketByteBufs.empty());
                    lc = 0;
                }
            }
        }

        StringBuilder hud = new StringBuilder();
        hud.append("气 [");
        int barLen = 20;
        int filled = (int) (d.getEnergy() / 100.0 * barLen);
        for (int i = 0; i < barLen; i++) hud.append(i < filled ? "|" : " ");
        hud.append("] ").append(d.getEnergy()).append("%");
        hud.append(" 修:").append(d.getCultivation());
        hud.append(" 环:");
        for (int i = 1; i <= 10; i++) {
            if (d.isUnlocked(i)) {
                hud.append(d.isActive(i) ? "(" : "[").append(BuffNames.NAME[i].charAt(0)).append(d.isActive(i) ? ")" : "]");
            }
        }
        client.player.sendMessage(Text.literal(hud.toString()), true);
    });
    }    private void spawnRing(MinecraftClient cl, net.minecraft.entity.player.PlayerEntity pl, int id, Vector3f col) {
        double y = pl.getY() + 1.0, rad = R + (id - 1) * S;
        var effect = new DustParticleEffect(col, 0.2f);
        for (int j = 0; j < P; j++) {
            double a = (2 * Math.PI / P) * j + (pl.age * 0.1);
            cl.world.addParticle(effect, pl.getX() + rad * Math.cos(a), y, pl.getZ() + rad * Math.sin(a), 0, 0, 0);
        }
        var white = new DustParticleEffect(new Vector3f(1, 1, 1), 0.1f);
        for (int j = 0; j < P; j++) {
            double a = (2 * Math.PI / P) * j + (pl.age * 0.1);
            double x = pl.getX() + rad * Math.cos(a), z = pl.getZ() + rad * Math.sin(a);
            cl.world.addParticle(white, x, y, z, 0, 0, 0);
        }
    }
}
