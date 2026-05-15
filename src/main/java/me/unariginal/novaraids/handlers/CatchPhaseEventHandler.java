package me.unariginal.novaraids.handlers;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.ActionResult;

public class CatchPhaseEventHandler {
    public static void register() {
        RaidEvents.CATCH_PHASE_EVENT_PRE.register(CatchPhaseEventHandler::catchPhasePre);
        RaidEvents.CATCH_PHASE_EVENT_POST.register(CatchPhaseEventHandler::catchPhasePost);
    }

    private static ActionResult catchPhasePre(Raid raid) {
        Event event = Event.getEvent("catch_phase_pre", raid.bossInfo.raidDetails.events.catchPhase.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult catchPhasePost(Raid raid) {
        Event event = Event.getEvent("catch_phase_post", raid.bossInfo.raidDetails.events.catchPhase.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, false);
        return ActionResult.PASS;
    }
}
