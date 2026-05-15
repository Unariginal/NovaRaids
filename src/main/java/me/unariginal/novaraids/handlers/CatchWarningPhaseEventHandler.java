package me.unariginal.novaraids.handlers;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.ActionResult;

public class CatchWarningPhaseEventHandler {
    public static void register() {
        RaidEvents.CATCH_WARNING_PHASE_EVENT_PRE.register(CatchWarningPhaseEventHandler::catchWarningPhasePre);
        RaidEvents.CATCH_WARNING_PHASE_EVENT_POST.register(CatchWarningPhaseEventHandler::catchWarningPhasePost);
    }

    private static ActionResult catchWarningPhasePre(Raid raid) {
        Event event = Event.getEvent("catch_warning_phase_pre", raid.bossInfo.raidDetails.events.catchWarningPhase.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult catchWarningPhasePost(Raid raid) {
        Event event = Event.getEvent("catch_warning_phase_post", raid.bossInfo.raidDetails.events.catchWarningPhase.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, false);
        return ActionResult.PASS;
    }
}
