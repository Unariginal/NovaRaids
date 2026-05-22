package me.unariginal.novaraids.events;

import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public interface RaidEndEvent {
    interface Pre {
        ActionResult onRaidEndPre(Raid raid);
    }
    interface Post {
        ActionResult onRaidEndPost(Raid raid);
    }
}
