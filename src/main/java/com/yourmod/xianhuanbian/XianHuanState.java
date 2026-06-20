package com.yourmod.xianhuanbian;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XianHuanState extends PersistentState {
    private static final String KEY = "xianhuanbian_data";
    private final Map<UUID, NbtCompound> playerData = new HashMap<>();

    public XianHuanState() { }

    public static XianHuanState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            nbt -> fromNbt(world, nbt),
            () -> new XianHuanState(),
            KEY
        );
    }

    public NbtCompound getPlayerData(UUID uuid) {
        return playerData.getOrDefault(uuid, new NbtCompound());
    }

    public void setPlayerData(UUID uuid, NbtCompound data) {
        playerData.put(uuid, data);
        markDirty();                     // 标记需要保存
    }

    public void removePlayer(UUID uuid) {
        playerData.remove(uuid);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        for (Map.Entry<UUID, NbtCompound> entry : playerData.entrySet()) {
            players.put(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("players", players);
        return nbt;
    }

    private static XianHuanState fromNbt(ServerWorld world, NbtCompound nbt) {
        XianHuanState state = new XianHuanState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) {
            UUID uuid = UUID.fromString(key);
            state.playerData.put(uuid, players.getCompound(key));
        }
        return state;
    }
}
