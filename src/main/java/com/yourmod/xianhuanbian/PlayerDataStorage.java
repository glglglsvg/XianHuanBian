package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PlayerDataStorage {
    private static Path SAVE_DIR = FabricLoader.getInstance().getGameDir().resolve("xianhuanbian/playerdata");

    public static void setWorldPath(Path worldPath) {
        SAVE_DIR = worldPath.resolve("xianhuanbian/playerdata");
    }

    public static void savePlayerData(UUID uuid, PlayerBuffData data) {
        try {
            Files.createDirectories(SAVE_DIR);
            File file = SAVE_DIR.resolve(uuid.toString() + ".dat").toFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                NbtIo.writeCompressed(data.toNbt(), fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerBuffData loadPlayerData(UUID uuid) {
        File file = SAVE_DIR.resolve(uuid.toString() + ".dat").toFile();
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                NbtCompound tag = NbtIo.readCompressed(fis);
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
