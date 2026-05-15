package me.unariginal.novaraids.handlers;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.managers.Raid;
import net.minecraft.util.ActionResult;

public class RaidLostEventHandler {
    public static void register() {
        RaidEvents.RAID_LOST_EVENT_PRE.register(RaidLostEventHandler::raidLostPre);
        RaidEvents.RAID_LOST_EVENT_POST.register(RaidLostEventHandler::raidLostPost);
    }

    private static ActionResult raidLostPre(Raid raid) {
        Event event = Event.getEvent("raid_lost_pre", raid.bossInfo.raidDetails.events.raidLost.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult raidLostPost(Raid raid) {
        Event event = Event.getEvent("raid_lost_post", raid.bossInfo.raidDetails.events.raidLost.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, true);
        return ActionResult.PASS;
    }
}
