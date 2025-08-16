package me.unariginal.novaraids.data.schedule;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import me.unariginal.novaraids.NovaRaids;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CronSchedule extends Schedule {
    public String expression;
    public ZonedDateTime nextExecution;

    public CronSchedule(String type, List<ScheduleBoss> bosses, String expression) {
        super(type, bosses);
        this.expression = expression;
    }

    public void setNextExecution(ZonedDateTime date) throws NoSuchElementException, IllegalArgumentException {
        CronDefinition cronDefinition = CronDefinitionBuilder.defineCron()
                .withSeconds().and()
                .withMinutes().and()
                .withHours().and()
                .withDayOfMonth()
                    .supportsHash().supportsL().supportsW().supportsQuestionMark().and()
                .withMonth().and()
                .withDayOfWeek()
                    .withIntMapping(7, 0)
                    .supportsHash().supportsL().supportsW().supportsQuestionMark().and()
                .withSupportedNicknameDaily()
                .withSupportedNicknameHourly()
                .withSupportedNicknameMidnight()
                .withSupportedNicknameMonthly()
                .withSupportedNicknameWeekly()
                .withSupportedNicknameAnnually()
                .withSupportedNicknameYearly()
                .instance();
        CronParser cronParser = new CronParser(cronDefinition);
        try {
            Cron cron = cronParser.parse(expression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(date);
            try {
                this.nextExecution = nextExecution.orElseThrow();
            } catch (NullPointerException e) {
                NovaRaids.INSTANCE.logError("Cron Schedule's next execution time is null!");
            }
        } catch (IllegalArgumentException e) {
            NovaRaids.INSTANCE.logError("Cron Schedule's expression is invalid! " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                NovaRaids.INSTANCE.logError("  " + el.toString());
            }
        }
    }

    public boolean isNextTime() {
        ZonedDateTime now = ZonedDateTime.now(NovaRaids.INSTANCE.schedulesConfig().zone);
        if (nextExecution == null) {
            setNextExecution(now);
        }
        return now.isAfter(nextExecution);
    }
}
