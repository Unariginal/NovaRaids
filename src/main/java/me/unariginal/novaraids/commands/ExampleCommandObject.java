package me.unariginal.novaraids.commands;

import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.minecraft.server.command.ServerCommandSource;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public class ExampleCommandObject {

    @Command("raids start <boss>")
    @Permission("novaraids.start")
    public void onBoss(
            ServerCommandSource source,
            @Argument("boss") Boss boss
    ) {

        if(NovaRaids.LOADED) {
            // if source instanceOf PlayerSender....
            // your code here
        }

    }

    @Command("raids start random [category]")
    @Permission("novaraids.start")
    public void onStart(
            ServerCommandSource source,
            @Argument("category") String category
    ) {
        if(category == null) {

        }
        // same thing
    }

}
