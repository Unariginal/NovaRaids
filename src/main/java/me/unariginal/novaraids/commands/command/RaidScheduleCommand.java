package me.unariginal.novaraids.commands.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.novaraids.data.schedules.CronSchedule;
import me.unariginal.novaraids.data.schedules.RandomSchedule;
import me.unariginal.novaraids.data.schedules.Schedule;
import me.unariginal.novaraids.data.schedules.SpecificSchedule;
import net.minecraft.server.command.ServerCommandSource;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import static me.unariginal.novaraids.config.ConfigManager.SCHEDULES;
import static me.unariginal.novaraids.utils.TextUtils.deserialize;
import static net.minecraft.server.command.CommandManager.literal;

public class RaidScheduleCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return literal("schedule")
                .requires(Permissions.require("novaraids.schedule", 4))
                .executes(RaidScheduleCommand::execute);
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        for (Schedule schedule : SCHEDULES.schedules) {
            if (schedule instanceof SpecificSchedule specificSchedule) {
                List<LocalTime> closestTimes = new ArrayList<>();

                for (int i = 0; i < specificSchedule.localTimes.size(); i++) {
                    LocalTime now = LocalTime.now(SCHEDULES.getTimezone());
                    LocalTime closestTime = null;
                    for (LocalTime time : specificSchedule.localTimes) {
                        if (time.isAfter(now) || time.equals(now)) {
                            if ((closestTime == null || time.isBefore(closestTime)) && !closestTimes.contains(time)) {
                                closestTime = time;
                            }
                        }
                    }

                    if (closestTime == null) {
                        now = LocalTime.of(0, 0);
                        for (LocalTime time : specificSchedule.localTimes) {
                            if (time.isAfter(now) || time.equals(now)) {
                                if ((closestTime == null || time.isBefore(closestTime)) && !closestTimes.contains(time)) {
                                    closestTime = time;
                                }
                            }
                        }
                    }

                    closestTimes.add(closestTime);
                }

                ctx.getSource().sendMessage(deserialize("<red>Specific Schedule Nearest Times:"));
                for (LocalTime time : closestTimes) {
                    ctx.getSource().sendMessage(deserialize("<gray><i> - " + time.toString()));
                }
            } else if (schedule instanceof RandomSchedule randomSchedule) {
                ctx.getSource().sendMessage(deserialize("<red>Random Schedule (<i>" + randomSchedule.minSeconds + "s - " + randomSchedule.maxSeconds + "s<!i>) Next Raid:"));
                ctx.getSource().sendMessage(deserialize("<gray><i> - " + randomSchedule.nextRandom.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone()))));
            } else if (schedule instanceof CronSchedule cronSchedule) {
                ctx.getSource().sendMessage(deserialize("<red>Cron Schedule (<i>" + cronSchedule.expression + "<!i>) Next Raid:"));
                ctx.getSource().sendMessage(deserialize("<gray><i> - " + cronSchedule.nextExecution.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(SCHEDULES.getTimezone()))));
            }
            ctx.getSource().sendMessage(deserialize(""));
        }
        return Command.SINGLE_SUCCESS;
    }
}
