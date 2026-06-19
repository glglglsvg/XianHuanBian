package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PlayerDataStorage {
    private static Path SAVE_DIR = null;  // 初始为 null，等待世界路径设置

    public static void setWorldPath(Path worldPath) {
        SAVE_DIR = worldPath.resolve("xianhuanbian/playerdata");
    }

    public static void savePlayerData(UUID uuid, PlayerBuffData data) {
        if (SAVE_DIR == null) {
            System.err.println("[XianHuanBian] 存档路径未设置，无法保存数据！");
            return;
        }
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
        if (SAVE_DIR == null) {
            return new PlayerBuffData();   // 路径未就绪时返回全新数据
        }
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
        if (SAVE_DIR == null) return;
        try {
            Files.deleteIfExists(SAVE_DIR.resolve(uuid.toString() + ".dat"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
