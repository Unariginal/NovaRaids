package me.unariginal.novaraids.events;

import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.ActionResult;

public interface SetupPhaseEvent {
    interface Pre {
        ActionResult onSetupPhasePre(Raid raid);
    }
    interface Post {
        ActionResult onSetupPhasePost(Raid raid);
    }
}
