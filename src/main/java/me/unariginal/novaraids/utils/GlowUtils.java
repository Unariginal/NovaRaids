package me.unariginal.novaraids.utils;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import me.unariginal.novaraids.NovaRaids;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

public class GlowUtils {
    public static void applyGlowing(String color, Pokemon pokemon) {
        PokemonEntity pokemonEntity = pokemon.getEntity();
        if (pokemonEntity == null) return;
        ServerScoreboard scoreboard = NovaRaids.INSTANCE.server.getScoreboard();

        String teamName = "glow_" + pokemon.getUuid().toString();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.addTeam(teamName);

        Formatting teamColor = Formatting.byName(color);
        if (teamColor == null) teamColor = Formatting.WHITE;
        team.setColor(teamColor);

        scoreboard.addScoreHolderToTeam(pokemonEntity.getNameForScoreboard(), team);

        pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 999999, 1, false, false));
    }

    public static void removeGlowing(Pokemon pokemon) {
        ServerScoreboard scoreboard = NovaRaids.INSTANCE.server.getScoreboard();

        String teamName = "glow_" + pokemon.getUuid().toString();
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            scoreboard.removeTeam(team);
        }

        PokemonEntity pokemonEntity = pokemon.getEntity();
        if (pokemonEntity != null) pokemonEntity.removeStatusEffect(StatusEffects.GLOWING);
    }
}
