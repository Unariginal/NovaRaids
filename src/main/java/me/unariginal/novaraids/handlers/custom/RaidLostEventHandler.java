package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class RaidLostEventHandler {
    public static void register() {
        RaidEvents.RAID_LOST_EVENT_PRE.register(RaidLostEventHandler::raidLostPre);
        RaidEvents.RAID_LOST_EVENT_POST.register(RaidLostEventHandler::raidLostPost);
    }

    private static ActionResult raidLostPre(Raid raid) {
        Event event = Event.getEvent("raid_lost_pre", raid.boss.raidDetails.events.raidLost.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }

    private static ActionResult raidLostPost(Raid raid) {
        Event event = Event.getEvent("raid_lost_post", raid.boss.raidDetails.events.raidLost.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null);
        return ActionResult.PASS;
    }
}
