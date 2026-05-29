package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class BossDamagedEventHandler {
    public static void register() {
        RaidEvents.BOSS_DAMAGED_EVENT_PRE.register(BossDamagedEventHandler::bossDamagedPre);
        RaidEvents.BOSS_DAMAGED_EVENT_POST.register(BossDamagedEventHandler::bossDamagedPost);
    }

    private static ActionResult bossDamagedPre(Raid raid, int damage) {
        Event event = Event.getEvent("boss_damaged_pre", raid.boss.raidDetails.events.bossDamaged.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, damage);
        return ActionResult.PASS;
    }

    private static ActionResult bossDamagedPost(Raid raid, int damage) {
        Event event = Event.getEvent("boss_damaged_post", raid.boss.raidDetails.events.bossDamaged.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, damage);
        return ActionResult.PASS;
    }
}
