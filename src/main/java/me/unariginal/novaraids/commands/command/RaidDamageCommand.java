package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import me.unariginal.novaraids.raid.RaidManager;
import me.unariginal.novaraids.placeholders.ParseContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidDamageCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("damage")
                .requires(Permissions.require("novaraids.damage", 4))
                .then(argument("id", IntegerArgumentType.integer(1))
                        .then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(RaidDamageCommand::execute)
                        )
                );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Raid raid = RaidManager.getRaid(id - 1);
        if (raid == null) return 0;
        if (ctx.getSource().isExecutedByPlayer()) {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player != null) {
                int damage = IntegerArgumentType.getInteger(ctx, "amount");
                if (damage > raid.currentHealth) damage = raid.currentHealth;

                RaidEvents.BOSS_DAMAGED_EVENT_PRE.invoker().onBossDamagedPre(raid, damage);
                raid.applyDamage(damage);
                raid.updatePlayerDamage(player.getUuid(), damage);
                RaidEvents.BOSS_DAMAGED_EVENT_POST.invoker().onBossDamagedPost(raid, damage);

                player.sendMessage(deserialize("<green>The damage has been applied.", ParseContext.builder().player(player).raid(raid).build()));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
