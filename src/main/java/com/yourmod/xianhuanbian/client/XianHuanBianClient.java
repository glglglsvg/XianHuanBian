package com.yourmod.xianhuanbian.client;

import com.yourmod.xianhuanbian.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;
public class XianHuanBianClient implements ClientModInitializer {
    private static final Vector3f[] COLORS = new Vector3f[]{
        null, new Vector3f(1,0.2f,0.2f), new Vector3f(1,0.6f,0), new Vector3f(1,1,0),
        new Vector3f(0.2f,1,0.2f), new Vector3f(0.2f,1,1), new Vector3f(0.2f,0.4f,1),
        new Vector3f(0.6f,0.2f,1), new Vector3f(1,0.5f,0.8f), new Vector3f(0.8f,0.1f,0.1f),
        new Vector3f(1,0.8f,0), new Vector3f(1,1,1), new Vector3f(0,0,0)
    };
    private static final int PARTICLE_COUNT = 8;
    private static final float PARTICLE_SIZE = 0.15f;
    private static KeyBinding refillKey, infoKey, meditateKey;
    private static final Map<Integer, KeyBinding> singleKeys = new HashMap<>();
    private static final int[] KEY_CODES = {
        GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O,
        GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
        GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M,
        GLFW.GLFW_KEY_P
    };
    @Override
public void onInitializeClient() {
    refillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("回气", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "仙环变"));
    infoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("属性", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "仙环变"));
    meditateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("修炼", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "仙环变"));
    String[] names = {"壹","贰","叁","肆","伍","陆","柒","捌","玖","拾"};
    for (int i = 1; i <= 10; i++) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding("气环"+names[i-1], InputUtil.Type.KEYSYM, KEY_CODES[i-1], "仙环变"));
        singleKeys.put(i, kb);
    }

    AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
        if (world.isClient && entity instanceof LivingEntity target) {
            spawnAttackRing(MinecraftClient.getInstance(), target);
            spawnHorizontalRing(MinecraftClient.getInstance(), target);
        }
        return ActionResult.PASS;
    });

    ClientPlayNetworking.registerGlobalReceiver(XianHuanBianMod.SYNC_BUFFS, (client, handler, buf, responseSender) -> {
        var tag = buf.readNbt(); if (tag != null) PlayerBuffData.updateClientFromNbt(tag);
    });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
        if (client.player == null || client.world == null) return;
        PlayerBuffData d = PlayerBuffData.getClient();
        while (refillKey.wasPressed()) ClientPlayNetworking.send(XianHuanBianMod.REFILL_ENERGY, PacketByteBufs.empty());
        while (infoKey.wasPressed()) { client.setScreen(new AttributeScreen(d)); }
        while (meditateKey.wasPressed()) {
            if (d.isMeditating()) ClientPlayNetworking.send(XianHuanBianMod.MEDITATE_STOP, PacketByteBufs.empty());
            else ClientPlayNetworking.send(XianHuanBianMod.MEDITATE_START, PacketByteBufs.empty());
        }
        for (int i = 1; i <= 10; i++) {
            KeyBinding kb = singleKeys.get(i);
            if (kb != null && kb.wasPressed()) ClientPlayNetworking.send(new Identifier("xianhuanbian", "toggle_" + i), PacketByteBufs.empty());
        }
        for (int i = 1; i <= 12; i++) if (d.isActive(i)) spawnPlayerRing(client, client.player, i, COLORS[i]);

        if (client.options.attackKey.wasPressed()) {
            ClientPlayNetworking.send(XianHuanBianMod.LEFT_CLICK_COUNT, PacketByteBufs.empty());
        }

        StringBuilder hud = new StringBuilder("气 [");
        int barLen = 20, filled = (int) (d.getEnergy() / 100.0 * barLen);
        for (int i = 0; i < barLen; i++) hud.append(i < filled ? "|" : " ");
        hud.append("] ").append(d.getEnergy()).append("% 修:").append(d.getCultivation()).append(" 环:");
        for (int i = 1; i <= 10; i++) {
            if (d.isUnlocked(i)) hud.append(d.isActive(i) ? "(" : "[").append(BuffNames.NAME[i].charAt(0)).append(d.isActive(i) ? ")" : "]");
        }
        client.player.sendMessage(Text.literal(hud.toString()), true);
    });
}
    private void spawnPlayerRing(MinecraftClient cl, net.minecraft.entity.player.PlayerEntity pl, int id, Vector3f col) {
    double y = pl.getY() + 1.0, rad = 0.5 + (id - 1) * 0.08;
    var effect = new DustParticleEffect(col, 0.2f);
    for (int j = 0; j < 6; j++) { double a = (2 * Math.PI / 6) * j + (pl.age * 0.1); cl.world.addParticle(effect, pl.getX() + rad * Math.cos(a), y, pl.getZ() + rad * Math.sin(a), 0, 0, 0); }
    var white = new DustParticleEffect(new Vector3f(1, 1, 1), 0.1f);
    for (int j = 0; j < 6; j++) { double a = (2 * Math.PI / 6) * j + (pl.age * 0.1); double x = pl.getX() + rad * Math.cos(a), z = pl.getZ() + rad * Math.sin(a); cl.world.addParticle(white, x, y, z, 0, 0, 0); }
    }
    private void spawnAttackRing(MinecraftClient cl, LivingEntity target) {
    double y = target.getY() + target.getHeight() / 2.0, rad = 0.3;
    var color = new Vector3f(1, 0.5f, 0); var effect = new DustParticleEffect(color, PARTICLE_SIZE);
    for (int j = 0; j < PARTICLE_COUNT; j++) { double a = (2 * Math.PI / PARTICLE_COUNT) * j + (target.age * 0.1); cl.world.addParticle(effect, target.getX() + rad * Math.cos(a), y, target.getZ() + rad * Math.sin(a), 0, 0, 0); }
    var verticalColor = new Vector3f(1, 0.8f, 0); var verticalEffect = new DustParticleEffect(verticalColor, PARTICLE_SIZE * 0.8f);
    for (int j = 0; j < PARTICLE_COUNT; j++) { double angle = (2 * Math.PI / PARTICLE_COUNT) * j; double offsetY = rad * Math.sin(angle), offsetXZ = rad * Math.cos(angle); cl.world.addParticle(verticalEffect, target.getX() + offsetXZ * 0.5, target.getY() + offsetY * 0.5 + target.getHeight()/2.0, target.getZ() + offsetXZ * 0.5, 0, 0, 0); }
}

private void spawnHorizontalRing(MinecraftClient cl, LivingEntity target) {
    double y = target.getY() + target.getHeight() / 2.0;
    double rad = 0.3;
    var color = new Vector3f(0.5f, 1.0f, 0.5f);
    var effect = new DustParticleEffect(color, 0.5f);
    for (int j = 0; j < 12; j++) {
        double angle = (2 * Math.PI / 12) * j + (target.age * 0.1);
        double x = target.getX() + rad * Math.cos(angle);
        double z = target.getZ() + rad * Math.sin(angle);
        cl.world.addParticle(effect, x, y, z, Math.cos(angle)*0.05, 0, Math.sin(angle)*0.05);
    }
    rad = 0.5;
    effect = new DustParticleEffect(new Vector3f(0.3f, 0.8f, 1.0f), 0.4f);
    for (int j = 0; j < 12; j++) {
        double angle = (2 * Math.PI / 12) * j + (target.age * 0.1);
        double x = target.getX() + rad * Math.cos(angle);
        double z = target.getZ() + rad * Math.sin(angle);
        cl.world.addParticle(effect, x, y, z, Math.cos(angle)*0.02, 0, Math.sin(angle)*0.02);
    }
}
    private static class AttributeScreen extends Screen {
    private final PlayerBuffData data;
    private ButtonWidget strButton, spdButton, vitButton;

    protected AttributeScreen(PlayerBuffData data) {
        super(Text.literal("仙环属性"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.width / 2 - 100;
        int y = 60;
        strButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("力量+ [" + data.getStrength() + "]"), button -> {
            if (data.getAvailablePoints() > 0) {
                ClientPlayNetworking.send(XianHuanBianMod.ADD_STR, PacketByteBufs.empty());
                data.setAvailablePoints(data.getAvailablePoints() - 1);
                data.setStrength(data.getStrength() + 1);
                updateButtons();
            }
        }).dimensions(x, y, 200, 20).build());

        y += 25;
        spdButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("速度+ [" + data.getSpeed() + "]"), button -> {
            if (data.getAvailablePoints() > 0) {
                ClientPlayNetworking.send(XianHuanBianMod.ADD_SPD, PacketByteBufs.empty());
                data.setAvailablePoints(data.getAvailablePoints() - 1);
                data.setSpeed(data.getSpeed() + 1);
                updateButtons();
            }
        }).dimensions(x, y, 200, 20).build());

        y += 25;
        vitButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("抗性+ [" + data.getVitality() + "]"), button -> {
            if (data.getAvailablePoints() > 0) {
                ClientPlayNetworking.send(XianHuanBianMod.ADD_VIT, PacketByteBufs.empty());
                data.setAvailablePoints(data.getAvailablePoints() - 1);
                data.setVitality(data.getVitality() + 1);
                updateButtons();
            }
        }).dimensions(x, y, 200, 20).build());
    }

    private void updateButtons() {
        strButton.setMessage(Text.literal("力量+ [" + data.getStrength() + "]"));
        spdButton.setMessage(Text.literal("速度+ [" + data.getSpeed() + "]"));
        vitButton.setMessage(Text.literal("抗性+ [" + data.getVitality() + "]"));
    }
                @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("可用点数: " + data.getAvailablePoints()), this.width / 2, 20, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("力量: " + data.getStrength() + "  速度: " + data.getSpeed() + "  抗性: " + data.getVitality()), this.width / 2, 40, 0xAAAAAA);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }
}
