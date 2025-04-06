package me.unariginal.novaraids.data.schedule;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CronSchedule extends Schedule {
    String expression;

    public CronSchedule(String type, List<ScheduleBoss> bosses, String expression) {
        super(type, bosses);
        this.expression = expression;
    }

    public ZonedDateTime nextExecution(ZonedDateTime date) throws NoSuchElementException, IllegalArgumentException {
        CronDefinition cronDefinition = CronDefinitionBuilder.defineCron()
                .withSeconds().and()
                .withMinutes().and()
                .withHours().and()
                .withDayOfMonth()
                    .supportsHash().supportsL().supportsW().and()
                .withMonth().and()
                .withDayOfWeek()
                    .withIntMapping(7, 0)
                    .supportsHash().supportsL().supportsW().and()
                .withSupportedNicknameDaily()
                .withSupportedNicknameHourly()
                .withSupportedNicknameMidnight()
                .withSupportedNicknameMonthly()
                .withSupportedNicknameWeekly()
                .instance();
        CronParser cronParser = new CronParser(cronDefinition);
        Cron cron = cronParser.parse(expression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(date);
        return nextExecution.orElseThrow();
    }
}
