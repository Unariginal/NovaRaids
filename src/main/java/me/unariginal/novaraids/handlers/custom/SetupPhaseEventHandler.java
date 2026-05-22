package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class SetupPhaseEventHandler {
    public static void register() {
        RaidEvents.SETUP_PHASE_EVENT_PRE.register(SetupPhaseEventHandler::setupPhasePre);
        RaidEvents.SETUP_PHASE_EVENT_POST.register(SetupPhaseEventHandler::setupPhasePost);
    }

    private static ActionResult setupPhasePre(Raid raid) {
        Event event = Event.getEvent("setup_phase_pre", raid.boss.raidDetails.events.setupPhase.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult setupPhasePost(Raid raid) {
        Event event = Event.getEvent("setup_phase_post", raid.boss.raidDetails.events.setupPhase.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, false);
        return ActionResult.PASS;
    }
}
