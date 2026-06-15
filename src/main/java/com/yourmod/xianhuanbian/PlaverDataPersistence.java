
package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerDataPersistence {

    private static final String MOD_DATA_KEY = "xianhuanbian_data";

    public static NbtCompound getModData(ServerPlayerEntity player) {
        NbtCompound persistentData = player.getPersistentData();
        if (!persistentData.contains(MOD_DATA_KEY)) {
            persistentData.put(MOD_DATA_KEY, new NbtCompound());
        }
        return persistentData.getCompound(MOD_DATA_KEY);
    }

    public static void savePlayerData(ServerPlayerEntity player, PlayerBuffData data) {
        NbtCompound modData = getModData(player);
        modData.copyFrom(data.toNbt());
    }

    public static PlayerBuffData loadPlayerData(ServerPlayerEntity player) {
        NbtCompound modData = getModData(player);
        if (modData.isEmpty()) {
            return new PlayerBuffData();
        }
        return PlayerBuffData.fromNbt(modData);
    }
}
