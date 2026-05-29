package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class RaidStartEventHandler {
    public static void register() {
        RaidEvents.RAID_START_EVENT_PRE.register(RaidStartEventHandler::raidStartPre);
        RaidEvents.RAID_START_EVENT_POST.register(RaidStartEventHandler::raidStartPost);
    }

    private static ActionResult raidStartPre(Raid raid) {
        Event event = Event.getEvent("raid_start_pre", raid.boss.raidDetails.events.raidStart.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }

    private static ActionResult raidStartPost(Raid raid) {
        Event event = Event.getEvent("raid_start_post", raid.boss.raidDetails.events.raidStart.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }
}
