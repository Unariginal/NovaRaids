package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface CatchPhaseEvent {
    interface Pre {
        ActionResult onCatchPhasePre(Raid raid);
    }
    interface Post {
        ActionResult onCatchPhasePost(Raid raid);
    }
}
