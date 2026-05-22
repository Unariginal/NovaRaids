package me.unariginal.novaraids.handlers.custom;

import me.unariginal.novaraids.data.events.Event;
import me.unariginal.novaraids.events.RaidEvents;
import me.unariginal.novaraids.raid.Raid;
import net.minecraft.util.ActionResult;

public class BossDefeatedEventHandler {
    public static void register() {
        RaidEvents.BOSS_DEFEATED_EVENT_PRE.register(BossDefeatedEventHandler::bossDefeatedPre);
        RaidEvents.BOSS_DEFEATED_EVENT_POST.register(BossDefeatedEventHandler::bossDefeatedPost);
    }

    private static ActionResult bossDefeatedPre(Raid raid) {
        Event event = Event.getEvent("boss_defeated_pre", raid.boss.raidDetails.events.bossDefeated.pre);
        if (event != null) RaidEventHandler.runEvent(event, raid, null,false);
        return ActionResult.PASS;
    }

    private static ActionResult bossDefeatedPost(Raid raid) {
        Event event = Event.getEvent("boss_defeated_post", raid.boss.raidDetails.events.bossDefeated.post);
        if (event != null) RaidEventHandler.runEvent(event, raid, null, true);
        return ActionResult.PASS;
    }
}
