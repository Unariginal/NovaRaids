package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class FightPhaseEventHandler {
    public static void register() {
        RaidEvents.FIGHT_PHASE_EVENT_PRE.register(FightPhaseEventHandler::fightPhasePre);
        RaidEvents.FIGHT_PHASE_EVENT_POST.register(FightPhaseEventHandler::fightPhasePost);
    }

    private static ActionResult fightPhasePre(Raid raid) {
        Event event = Event.getEvent("fight_phase_pre", raid.boss.raidDetails.events.fightPhase.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult fightPhasePost(Raid raid) {
        Event event = Event.getEvent("fight_phase_post", raid.boss.raidDetails.events.fightPhase.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, false);
        return ActionResult.PASS;
    }
}
