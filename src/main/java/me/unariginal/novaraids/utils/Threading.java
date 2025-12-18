package me.unariginal.novaraids.utils;

import me.unariginal.novaraids.NovaRaids;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Threading {

    private Threading() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final ScheduledExecutorService ASYNC_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    /**
     * Run a task asynchronously
     * @param runnable {@link Runnable}
     */
    public static void runTaskAsync(Runnable runnable) {
        ASYNC_EXECUTOR.execute(wrapRunnable(runnable));
    }

    /**
     * Schedule a delayed asynchronous task
     * @param runnable {@link Runnable}
     * @param delay Delay (in millis)
     */
    public static ScheduledFuture<?> runDelayedTaskAsync(Runnable runnable, long delay) {
        return ASYNC_EXECUTOR.schedule(wrapRunnable(runnable), delay, TimeUnit.SECONDS);
    }

    /**
     * Schedule a delayed repeating asynchronous task
     * @param runnable {@link Runnable}
     */
    public static ScheduledFuture<?> runDelayedTaskAsyncTimer(Runnable runnable, long initDelay, long timedDelay) {
        return ASYNC_EXECUTOR.scheduleWithFixedDelay(wrapRunnable(runnable), initDelay, timedDelay, TimeUnit.SECONDS);
    }

    private static Runnable wrapRunnable(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                NovaRaids.INSTANCE.logger().error(throwable.getMessage(), throwable);
            }
        };
    }


}
