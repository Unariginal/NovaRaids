package me.unariginal.novaraids.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public final class RaidEvents {
    public static final Event<RaidStartEvent.Pre> RAID_START_EVENT_PRE = EventFactory.createArrayBacked(
            RaidStartEvent.Pre.class,
            listeners -> raid -> {
                for (RaidStartEvent.Pre listener : listeners) {
                    ActionResult result = listener.onRaidStartPre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<RaidStartEvent.Post> RAID_START_EVENT_POST = EventFactory.createArrayBacked(
            RaidStartEvent.Post.class,
            listeners -> raid -> {
                for (RaidStartEvent.Post listener : listeners) {
                    ActionResult result = listener.onRaidStartPost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<RaidEndEvent.Pre> RAID_END_EVENT_PRE = EventFactory.createArrayBacked(
            RaidEndEvent.Pre.class,
            listeners -> raid -> {
                for (RaidEndEvent.Pre listener : listeners) {
                    ActionResult result = listener.onRaidEndPre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<RaidEndEvent.Post> RAID_END_EVENT_POST = EventFactory.createArrayBacked(
            RaidEndEvent.Post.class,
            listeners -> raid -> {
                for (RaidEndEvent.Post listener : listeners) {
                    ActionResult result = listener.onRaidEndPost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<RaidLostEvent.Pre> RAID_LOST_EVENT_PRE = EventFactory.createArrayBacked(
            RaidLostEvent.Pre.class,
            listeners -> raid -> {
                for (RaidLostEvent.Pre listener : listeners) {
                    ActionResult result = listener.onRaidLostPre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<RaidLostEvent.Post> RAID_LOST_EVENT_POST = EventFactory.createArrayBacked(
            RaidLostEvent.Post.class,
            listeners -> raid -> {
                for (RaidLostEvent.Post listener : listeners) {
                    ActionResult result = listener.onRaidLostPost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<BossDamagedEvent.Pre> BOSS_DAMAGED_EVENT_PRE = EventFactory.createArrayBacked(
            BossDamagedEvent.Pre.class,
            listeners -> (raid, damage) -> {
                for (BossDamagedEvent.Pre listener : listeners) {
                    ActionResult result = listener.onBossDamagedPre(raid, damage);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<BossDamagedEvent.Post> BOSS_DAMAGED_EVENT_POST = EventFactory.createArrayBacked(
            BossDamagedEvent.Post.class,
            listeners -> (raid, damage) -> {
                for (BossDamagedEvent.Post listener : listeners) {
                    ActionResult result = listener.onBossDamagedPost(raid, damage);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<BossDefeatedEvent.Pre> BOSS_DEFEATED_EVENT_PRE = EventFactory.createArrayBacked(
            BossDefeatedEvent.Pre.class,
            listeners -> raid -> {
                for (BossDefeatedEvent.Pre listener : listeners) {
                    ActionResult result = listener.onBossDefeatedPre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<BossDefeatedEvent.Post> BOSS_DEFEATED_EVENT_POST = EventFactory.createArrayBacked(
            BossDefeatedEvent.Post.class,
            listeners -> raid -> {
                for (BossDefeatedEvent.Post listener : listeners) {
                    ActionResult result = listener.onBossDefeatedPost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<SetupPhaseEvent.Pre> SETUP_PHASE_EVENT_PRE = EventFactory.createArrayBacked(
            SetupPhaseEvent.Pre.class,
            listeners -> raid -> {
                for (SetupPhaseEvent.Pre listener : listeners) {
                    ActionResult result = listener.onSetupPhasePre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<SetupPhaseEvent.Post> SETUP_PHASE_EVENT_POST = EventFactory.createArrayBacked(
            SetupPhaseEvent.Post.class,
            listeners -> raid -> {
                for (SetupPhaseEvent.Post listener : listeners) {
                    ActionResult result = listener.onSetupPhasePost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<FightPhaseEvent.Pre> FIGHT_PHASE_EVENT_PRE = EventFactory.createArrayBacked(
            FightPhaseEvent.Pre.class,
            listeners -> raid -> {
                for (FightPhaseEvent.Pre listener : listeners) {
                    ActionResult result = listener.onFightPhasePre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<FightPhaseEvent.Post> FIGHT_PHASE_EVENT_POST = EventFactory.createArrayBacked(
            FightPhaseEvent.Post.class,
            listeners -> raid -> {
                for (FightPhaseEvent.Post listener : listeners) {
                    ActionResult result = listener.onFightPhasePost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<CatchWarningPhaseEvent.Pre> CATCH_WARNING_PHASE_EVENT_PRE = EventFactory.createArrayBacked(
            CatchWarningPhaseEvent.Pre.class,
            listeners -> raid -> {
                for (CatchWarningPhaseEvent.Pre listener : listeners) {
                    ActionResult result = listener.onCatchWarningPhasePre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<CatchWarningPhaseEvent.Post> CATCH_WARNING_PHASE_EVENT_POST = EventFactory.createArrayBacked(
            CatchWarningPhaseEvent.Post.class,
            listeners -> raid -> {
                for (CatchWarningPhaseEvent.Post listener : listeners) {
                    ActionResult result = listener.onCatchWarningPhasePost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<CatchPhaseEvent.Pre> CATCH_PHASE_EVENT_PRE = EventFactory.createArrayBacked(
            CatchPhaseEvent.Pre.class,
            listeners -> raid -> {
                for (CatchPhaseEvent.Pre listener : listeners) {
                    ActionResult result = listener.onCatchPhasePre(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    public static final Event<CatchPhaseEvent.Post> CATCH_PHASE_EVENT_POST = EventFactory.createArrayBacked(
            CatchPhaseEvent.Post.class,
            listeners -> raid -> {
                for (CatchPhaseEvent.Post listener : listeners) {
                    ActionResult result = listener.onCatchPhasePost(raid);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });
}
