import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class GracefulRunnableTask extends GracefulRunnable {

    private int initialDelay;
    private int period;
    private static ScheduledExecutorService executor = null;

    public GracefulRunnableTask(String name, int initialDelay, int period) {
        super(name);
        this.initialDelay = initialDelay;
        this.period = period;
    }

    public static void setPoolSize(int poolSize) {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(poolSize);
        } else {
            Logger.log("GracefulRunnableTask", "Task pool size already " +
                    "set", Logger.logLevel.WARNING);
        }
    }

    @Override
    public void run() {

        initWork();
        try {
            // if doWork is time consuming, call shouldStop periodically
            doWork();
        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
        endWork();
    }

    @Override
    public void start() {

        if (executor == null) {
            executor = Executors.newScheduledThreadPool(1);
            Logger.log("GracefulRunnableTask", "Task started before pool " +
                    "size was set. Size 1 used", Logger.logLevel.WARNING);
        }

        executor.scheduleAtFixedRate(this, initialDelay, period,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        stopKeepAlive();
        executor.shutdown();
    }

    @Override
    public void join() throws InterruptedException {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            Logger.log(logName, "ScheduledExecutor did not " +
                    "shut down in time", Logger.logLevel.WARNING);
        }
    }
}
