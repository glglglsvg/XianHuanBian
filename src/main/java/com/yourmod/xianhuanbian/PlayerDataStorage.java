package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PlayerDataStorage {
    private static final Path SAVE_DIR = FabricLoader.getInstance().getGameDir().resolve("xianhuanbian/playerdata");

    public static void savePlayerData(UUID uuid, PlayerBuffData data) {
        try {
            Files.createDirectories(SAVE_DIR);
            Path file = SAVE_DIR.resolve(uuid.toString() + ".dat");
            NbtIo.writeCompressed(data.toNbt(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerBuffData loadPlayerData(UUID uuid) {
        Path file = SAVE_DIR.resolve(uuid.toString() + ".dat");
        if (Files.exists(file)) {
            try {
                NbtCompound tag = NbtIo.readCompressed(file, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                return PlayerBuffData.fromNbt(tag);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new PlayerBuffData();
    }

    public static void remove(UUID uuid) {
        try {
            Files.deleteIfExists(SAVE_DIR.resolve(uuid.toString() + ".dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
