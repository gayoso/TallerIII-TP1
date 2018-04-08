import java.util.concurrent.LinkedBlockingQueue;

public class StatisticUpdater extends GracefulRunnableThread {

    private LinkedBlockingQueue inputQueue;
    private Statistic myStatistic;

    public StatisticUpdater(LinkedBlockingQueue queue, Statistic stat) {
        super("StatisticUpdater " + stat.name);

        this.inputQueue = queue;
        this.myStatistic = stat;
    }

    @Override
    protected void doWork() throws InterruptedException {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        String value = inputQueue.take().toString();
        Logger.log(logName, "Recibi de la cola: " + value,
                Logger.logLevel.INFO);

        myStatistic.updateStatistic(value);
    }
}
