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
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class XianHuanBianClient implements ClientModInitializer {
    private static final Vector3f[] C = new Vector3f[]{
        null, new Vector3f(1,0.2f,0.2f), new Vector3f(1,0.6f,0), new Vector3f(1,1,0),
        new Vector3f(0.2f,1,0.2f), new Vector3f(0.2f,1,1), new Vector3f(0.2f,0.4f,1),
        new Vector3f(0.6f,0.2f,1), new Vector3f(1,0.5f,0.8f), new Vector3f(0.8f,0.1f,0.1f),
        new Vector3f(1,0.8f,0), new Vector3f(1,1,1), new Vector3f(0,0,0)
    };
    private static final int P = 16;
    private static final float R = 0.8f, S = 0.12f;
    private static int lc = 0;
    private static long lt = 0;
    private static KeyBinding key;

    @Override
    public void onInitializeClient() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.xianhuanbian.toggle_all", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.xianhuanbian"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.player==null||client.world==null) return;
            PlayerBuffComponent d = ModComponents.BUFF_DATA.get(client.player);
            if(d == null) return;
            while(key.wasPressed()) ClientPlayNetworking.send(XianHuanBianMod.TOGGLE_ALL, PacketByteBufs.empty());
            for(int i=1;i<=12;i++) if(d.isActive(i)) ring(client, client.player, i, C[i]);
            if(client.options.attackKey.wasPressed()) {
                long now = System.currentTimeMillis();
                if(now-lt>2000) lc=0;
                lc++; lt=now;
                if(lc>=5) {
                    boolean has=false;
                    for(int i=1;i<=10;i++) if(d.isUnlocked(i)) has=true;
                    if(!has) {
                        ClientPlayNetworking.send(XianHuanBianMod.UNLOCK_FIRST, PacketByteBufs.empty());
                        lc=0;
                    }
                }
            }
        });
    }

    private void ring(MinecraftClient cl, net.minecraft.entity.player.PlayerEntity pl, int id, Vector3f col) {
        double y = pl.getY()+1.0, rad = R+(id-1)*S;
        var eff = new DustParticleEffect(col, 1.0f);
        for(int j=0;j<P;j++) {
            double a = (2*Math.PI/P)*j + (pl.age*0.1);
            cl.world.addParticle(eff, pl.getX()+rad*Math.cos(a), y, pl.getZ()+rad*Math.sin(a),0,0,0);
        }
    }
}
