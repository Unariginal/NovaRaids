package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface RaidLostEvent {
    interface Pre {
        ActionResult onRaidLostPre(Raid raid);
    }
    interface Post {
        ActionResult onRaidLostPost(Raid raid);
    }
}
