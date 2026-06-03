package com.yourmod.xianhuanbian;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;

public class ToggleBuffCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("togglebuff")
            .then(argument("id", IntegerArgumentType.integer(1, 12))
            .executes(ctx -> {
                int id = IntegerArgumentType.getInteger(ctx, "id");
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                PlayerBuffData data = PlayerBuffData.get(p);
                if (!data.isUnlocked(id)) {
                    ctx.getSource().sendFeedback(() -> Text.literal("尚未领悟" + BuffNames.NAME[id]), false);
                    return 0;
                }
                data.setActive(id, !data.isActive(id));
                data.save(p);
                ctx.getSource().sendFeedback(() -> Text.literal(BuffNames.NAME[id] + "已" + (data.isActive(id) ? "开启" : "关闭")), false);
                return 1;
            }))
        );
    }
}
