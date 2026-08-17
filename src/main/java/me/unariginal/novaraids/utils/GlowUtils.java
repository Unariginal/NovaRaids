package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public class GlowUtils {
    public static void applyGlowing(String color, Pokemon pokemon, @Nullable PokemonEntity pokemonEntity) {
        if (pokemonEntity == null) return;
        ServerScoreboard scoreboard = NovaRaids.INSTANCE.server.getScoreboard();

        String teamName = "glow_" + pokemon.getUuid().toString();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.addTeam(teamName);

        Formatting teamColor = Formatting.byName(color);
        if (teamColor == null) teamColor = Formatting.WHITE;
        if (team.getColor() != teamColor) team.setColor(teamColor);

        String scoreHolder = pokemonEntity.getNameForScoreboard();
        Team holderTeam = scoreboard.getScoreHolderTeam(scoreHolder);
        if (holderTeam == null || !holderTeam.equals(team)) scoreboard.addScoreHolderToTeam(scoreHolder, team);

        pokemonEntity.setGlowing(false);
        pokemonEntity.setGlowing(true);
    }

    public static void removeGlowing(Pokemon pokemon, @Nullable PokemonEntity pokemonEntity) {
        ServerScoreboard scoreboard = NovaRaids.INSTANCE.server.getScoreboard();

        String teamName = "glow_" + pokemon.getUuid().toString();
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            scoreboard.removeTeam(team);
        }

        if (pokemonEntity != null) {
            pokemonEntity.setGlowing(false);
        }
    }
}
