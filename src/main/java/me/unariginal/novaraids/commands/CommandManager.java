package me.unariginal.novaraids.commands;

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import io.leangen.geantyref.TypeToken;
import me.unariginal.novaraids.commands.parser.BossParser;
import me.unariginal.novaraids.data.bosssettings.Boss;
import net.minecraft.server.command.ServerCommandSource;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.brigadier.parser.WrappedBrigadierParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.setting.ManagerSetting;

import java.util.concurrent.CompletableFuture;

public class CommandManager {

    public static final CommandManager INSTANCE = new CommandManager();

    private final FabricServerCommandManager<ServerCommandSource> commandManager = FabricServerCommandManager.createNative(
            ExecutionCoordinator.simpleCoordinator()
    );

    private final AnnotationParser<ServerCommandSource> annotationParser = new AnnotationParser<>(
            this.commandManager,
            ServerCommandSource.class
    );

    public CommandManager() {

        commandManager.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
        commandManager.settings().set(ManagerSetting.OVERRIDE_EXISTING_COMMANDS, true);

        this.commandManager.parserRegistry().registerParserSupplier(
                TypeToken.get(Species.class),
                params -> pokemonArgumentType()
        );

        this.commandManager.parserRegistry().registerParserSupplier(
                TypeToken.get(Boss.class),
                params -> new BossParser()
        );

    }

    private ArgumentParser.FutureArgumentParser<ServerCommandSource, Species> pokemonArgumentType() {
        return new WrappedBrigadierParser<ServerCommandSource, Species>(
                SpeciesArgumentType.Companion.species()
        ).mapSuccess((ctx, u) -> CompletableFuture.completedFuture(u));
    }

    public void parseClass(Object object) {
        this.annotationParser.parse(object);
    }

}
