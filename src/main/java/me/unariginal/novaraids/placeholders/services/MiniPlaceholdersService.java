package me.unariginal.novaraids.placeholders.services;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import me.unariginal.novaraids.NovaRaids;
import me.unariginal.novaraids.config.RaidHistory;
import me.unariginal.novaraids.placeholders.interfaces.PlayerPlaceholder;
import me.unariginal.novaraids.placeholders.interfaces.RaidHistoryPlaceholder;
import me.unariginal.novaraids.placeholders.interfaces.ServerPlaceholder;
import me.unariginal.novaraids.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MiniPlaceholdersService {
    private final Expansion.Builder builder = Expansion.builder(NovaRaids.MOD_ID);

    public void registerPlayer(PlayerPlaceholder placeholder) {
        placeholder.id().forEach(id -> builder.filter(ServerPlayerEntity.class)
                .audiencePlaceholder(id, (audience, queue, ctx) -> {
                    ServerPlayerEntity player = (ServerPlayerEntity) audience;
                    List<String> arguments = new ArrayList<>();
                    while (queue.peek() != null) {
                        arguments.add(queue.pop().toString());
                    }
                    return Tag.preProcessParsed(placeholder.handle(player, arguments).string);
                }));
    }

    public void registerRaidHistory(RaidHistoryPlaceholder placeholder) {
        placeholder.id().forEach(id -> builder.globalPlaceholder(id, (queue, ctx) -> {
            List<String> args = new ArrayList<>();
            while (queue.peek() != null) {
                args.add(queue.pop().toString());
            }
            return Tag.preProcessParsed(placeholder.handle(null, args).string);
        }).audiencePlaceholder(id, ((audience, queue, ctx) -> {
            if (!(audience instanceof ServerPlayerEntity)) return Tag.inserting(Component.empty());
            List<String> arguments = new ArrayList<>();
            while (queue.peek() != null) {
                arguments.add(queue.pop().toString());
            }
            return Tag.preProcessParsed(placeholder.handle(null, arguments).string);
        })));
    }

    public void registerServer(ServerPlaceholder placeholder) {
        placeholder.id().forEach(id -> builder.globalPlaceholder(id, (queue, ctx) -> {
            List<String> args = new ArrayList<>();
            while (queue.peek() != null) {
                args.add(queue.pop().toString());
            }
            return Tag.preProcessParsed(placeholder.handle(args).string);
        }).audiencePlaceholder(id, ((audience, queue, ctx) -> {
            if (!(audience instanceof ServerPlayerEntity)) return Tag.inserting(Component.empty());
            List<String> arguments = new ArrayList<>();
            while (queue.peek() != null) {
                arguments.add(queue.pop().toString());
            }
            return Tag.preProcessParsed(placeholder.handle(arguments).string);
        })));
    }

    public void registerBuilder() {
        builder.build().register();
    }

    // Todo: raid history support
    public String parse(String input, @Nullable ServerPlayerEntity player, @Nullable RaidHistory raidHistory) {
        List<TagResolver> resolvers = new ArrayList<>(List.of(MiniPlaceholders.getGlobalPlaceholders()));
        if (player != null) resolvers.add(MiniPlaceholders.getAudiencePlaceholders(player));
        TagResolver resolver = TagResolver.resolver(resolvers);

        return NovaRaids.INSTANCE.audience.toNative(TextUtils.miniMessage.deserialize(input, resolver)).getString();
    }
}
