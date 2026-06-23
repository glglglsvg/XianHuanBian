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
import java.util.Random;

public class XianHuanBianClient implements ClientModInitializer {
    private static final Vector3f[] COLORS = new Vector3f[]{
        null, new Vector3f(1,0.2f,0.2f), new Vector3f(1,0.6f,0), new Vector3f(1,1,0),
        new Vector3f(0.2f,1,0.2f), new Vector3f(0.2f,1,1), new Vector3f(0.2f,0.4f,1),
        new Vector3f(0.6f,0.2f,1), new Vector3f(1,0.5f,0.8f), new Vector3f(0.8f,0.1f,0.1f),
        new Vector3f(1,0.8f,0), new Vector3f(1,1,1), new Vector3f(0,0,0)
    };
    private static final int PARTICLE_COUNT = 8;
    private static final float PARTICLE_SIZE = 0.15f;
    private static KeyBinding refillKey, infoKey, meditateKey, fateKey;
    private static final Map<Integer, KeyBinding> singleKeys = new HashMap<>();
    private static final int[] KEY_CODES = {
        GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O,
        GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
        GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M,
        GLFW.GLFW_KEY_P
    };

    private static boolean localMeditating = false;
    private static boolean localRefilling = false;
    private static final Random RAND = new Random();
    @Override
public void onInitializeClient() {
    refillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("回气", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "仙环变"));
    infoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("属性", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "仙环变"));
    meditateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("修炼", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "仙环变"));
    fateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("缘分", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "仙环变"));
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
        var tag = buf.readNbt();
        if (tag != null) PlayerBuffData.updateClientFromNbt(tag);
        PlayerBuffData d = PlayerBuffData.getClient();
        localMeditating = d.isMeditating();
    });
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.player == null || client.world == null) return;
    PlayerBuffData d = PlayerBuffData.getClient();

    boolean moving = client.player.input.movementForward != 0
            || client.player.input.movementSideways != 0
            || client.options.jumpKey.isPressed();
    if (moving) {
        if (localMeditating) {
            localMeditating = false;
            ClientPlayNetworking.send(XianHuanBianMod.MEDITATE_STOP, PacketByteBufs.empty());
        }
        if (localRefilling) {
            localRefilling = false;
        }
    }

    if (refillKey.wasPressed()) {
        localRefilling = !localRefilling;
        if (localRefilling) {
            ClientPlayNetworking.send(XianHuanBianMod.REFILL_ENERGY, PacketByteBufs.empty());
        }
    }

    if (meditateKey.wasPressed()) {
        localMeditating = !localMeditating;
        if (localMeditating) {
            ClientPlayNetworking.send(XianHuanBianMod.MEDITATE_START, PacketByteBufs.empty());
        } else {
            ClientPlayNetworking.send(XianHuanBianMod.MEDITATE_STOP, PacketByteBufs.empty());
        }
    }

    if (infoKey.wasPressed()) {
        client.setScreen(new AttributeScreen(d));
    }

    if (fateKey.wasPressed()) {
        client.setScreen(new FateScreen(d));
    }

    for (int i = 1; i <= 10; i++) {
        KeyBinding kb = singleKeys.get(i);
        if (kb != null && kb.wasPressed()) ClientPlayNetworking.send(new Identifier("xianhuanbian", "toggle_" + i), PacketByteBufs.empty());
    }
    for (int i = 1; i <= 12; i++) if (d.isActive(i)) spawnPlayerRing(client, client.player, i, COLORS[i]);

    // 第二环行为：蹲下或跳跃
    if (client.options.sneakKey.wasPressed() || client.options.jumpKey.wasPressed()) {
        ClientPlayNetworking.send(XianHuanBianMod.SNEAK_JUMP_COUNT, PacketByteBufs.empty());
    }
               StringBuilder hud = new StringBuilder();
        if (localRefilling) {
            hud.append("【回气中】 ");
        }
        if (localMeditating) {
            hud.append("【修炼中】 ");
        }
        for (int i = 1; i <= 12; i++) {
            if (d.isActive(i) && d.getMaxDuration(i) > 0) {
                int remainingSeconds = d.getDuration(i) / 20;
                hud.append(BuffNames.NAME[i].charAt(0)).append(":").append(remainingSeconds).append("s ");
            }
        }
        hud.append("气 [");
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
        PlayerBuffData d = PlayerBuffData.getClient();
        double baseY = target.getY() + target.getHeight() / 2.0;
        for (int id = 1; id <= 12; id++) {
            if (d.isActive(id) && COLORS[id] != null) {
                Vector3f color = COLORS[id];
                int count = 8 + RAND.nextInt(9);
                for (int j = 0; j < count; j++) {
                    double angle = RAND.nextDouble() * Math.PI * 2;
                    double pitch = (RAND.nextDouble() - 0.5) * Math.PI;
                    double speed = 0.15 + RAND.nextDouble() * 0.25;
                    double dx = Math.cos(angle) * Math.cos(pitch) * speed;
                    double dy = Math.sin(pitch) * speed;
                    double dz = Math.sin(angle) * Math.cos(pitch) * speed;
                    cl.world.addParticle(new DustParticleEffect(color, 0.5f + RAND.nextFloat() * 0.5f),
                        target.getX(), baseY, target.getZ(), dx, dy, dz);
                }
            }
        }
    }

    private void spawnHorizontalRing(MinecraftClient cl, LivingEntity target) {
        PlayerBuffData d = PlayerBuffData.getClient();
        double baseY = target.getY() + target.getHeight() / 2.0;
        for (int id = 1; id <= 12; id++) {
            if (d.isActive(id) && COLORS[id] != null) {
                Vector3f color = COLORS[id];
                double rad = 0.5 + RAND.nextDouble() * 0.5;
                int count = 12 + RAND.nextInt(8);
                for (int j = 0; j < count; j++) {
                    double angle = (2 * Math.PI / count) * j + RAND.nextDouble() * 0.5;
                    double x = target.getX() + rad * Math.cos(angle);
                    double z = target.getZ() + rad * Math.sin(angle);
                    double dy = (RAND.nextDouble() - 0.5) * 0.2;
                    cl.world.addParticle(new DustParticleEffect(color, 0.4f),
                        x, baseY + dy, z, 0, 0, 0);
                }
            }
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

    // ========== 缘分界面 ==========
    private static class FateScreen extends Screen {
        private final PlayerBuffData data;
        protected FateScreen(PlayerBuffData data) {
            super(Text.literal("缘分"));
            this.data = data;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            int y = 20;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("======== 缘分 ========"), this.width / 2, y, 0xFFFFFF);
            y += 20;
            for (int i = 1; i <= 10; i++) {
                String chanceText = data.isUnlocked(i)
                    ? "已领悟"
                    : String.format("%.1f%%", data.getChance(i) * 100);
                context.drawTextWithShadow(this.textRenderer,
                    Text.literal(BuffNames.NAME[i] + ": " + chanceText),
                    20, y, data.isUnlocked(i) ? 0x00FF00 : 0xAAAAAA);
                y += 12;
            }
            y += 10;
            // 进度条
            int barWidth = 200;
            int barHeight = 12;
            int filled = (int) ((float) data.getProgress() / data.getMaxProgress() * barWidth);
            context.fill(20, y, 20 + barWidth, y + barHeight, 0xFF444444);
            context.fill(20, y, 20 + filled, y + barHeight, 0xFF00FF00);
            String progressText = data.getProgress() + "/" + data.getMaxProgress();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(progressText), this.width / 2, y - 12, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("修炼进度（满值随机领悟）"), this.width / 2, y + barHeight + 4, 0xAAAAAA);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }
}
