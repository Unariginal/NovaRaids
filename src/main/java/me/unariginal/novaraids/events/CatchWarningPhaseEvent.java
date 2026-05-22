package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface CatchWarningPhaseEvent {
    interface Pre {
        ActionResult onCatchWarningPhasePre(Raid raid);
    }
    interface Post {
        ActionResult onCatchWarningPhasePost(Raid raid);
    }
}
