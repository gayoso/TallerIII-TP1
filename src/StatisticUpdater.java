import java.util.concurrent.LinkedBlockingQueue;

public class StatisticUpdater extends GracefulRunnable {

    private LinkedBlockingQueue inputQueue;
    private Statistic myStatistic;

    public StatisticUpdater(LinkedBlockingQueue queue, Statistic stat) {
        super("StatisticUpdater " + stat.name);

        this.inputQueue = queue;
        this.myStatistic = stat;
    }

    @Override
    protected void doWork() {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        try {
            String value = inputQueue.take().toString();
            Logger.log(logName, "Recibi de la cola: " + value, Logger.logLevel.INFO);

            myStatistic.updateStatistic(value);

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }
}
