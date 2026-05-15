package me.unariginal.novaraids.events;

import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.ActionResult;

public interface FightPhaseEvent {
    interface Pre {
        ActionResult onFightPhasePre(Raid raid);
    }
    interface Post {
        ActionResult onFightPhasePost(Raid raid);
    }
}
