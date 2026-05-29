package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class RaidEndEventHandler {
    public static void register() {
        RaidEvents.RAID_END_EVENT_PRE.register(RaidEndEventHandler::raidEndPre);
        RaidEvents.RAID_END_EVENT_POST.register(RaidEndEventHandler::raidEndPost);
    }

    private static ActionResult raidEndPre(Raid raid) {
        Event event = Event.getEvent("raid_end_pre", raid.boss.raidDetails.events.raidEnd.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }

    private static ActionResult raidEndPost(Raid raid) {
        Event event = Event.getEvent("raid_end_post", raid.boss.raidDetails.events.raidEnd.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }
}
