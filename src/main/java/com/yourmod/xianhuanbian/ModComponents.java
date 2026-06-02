package com.yourmod.xianhuanbian;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;

public class ModComponents implements EntityComponentInitializer {
    public static ComponentKey<PlayerBuffComponent> BUFF_DATA;

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        BUFF_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(
            new Identifier("xianhuanbian", "buff_data"),
            PlayerBuffComponent.class
        );
        registry.registerForPlayers(BUFF_DATA, player -> new PlayerBuffComponentImpl(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}
