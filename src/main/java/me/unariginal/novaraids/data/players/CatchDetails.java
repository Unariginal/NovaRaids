package me.unariginal.novaraids.data.players;

import com.cobblemon.mod.common.pokemon.EVs;
import com.cobblemon.mod.common.pokemon.IVs;

public class CatchDetails {
    public boolean caught;
    public String species;
    public String formId;
    public IVs ivs;
    public EVs evs;
    public boolean shiny;

    public CatchDetails(
            boolean caught,
            String species,
            String formId,
            IVs ivs,
            EVs evs,
            boolean shiny
    ) {
        this.caught = caught;
        this.species = species;
        this.formId = formId;
        this.ivs = ivs;
        this.evs = evs;
        this.shiny = shiny;
    }
}
