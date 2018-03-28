import java.util.*;

public class StatisticViewer extends GracefulRunnable {

    private Map<String, Statistic> statistics;
    private int sleepMilliseconds;

    public StatisticViewer(Map<String, Statistic> statistics, int sleepMilliseconds) {
        super("StatisticsViewer");

        this.statistics = statistics;
        this.sleepMilliseconds = sleepMilliseconds;
    }

    @Override
    public void doWork() {

        try {
            Thread.sleep(sleepMilliseconds);

            Logger.output("\n");

            // requests por segundo
            int totalRequests = 0;
            Map<String, Integer> requestStatistics = statistics.get("requests").getStatistic();
            for (String key : requestStatistics.keySet()) {
                totalRequests += requestStatistics.getOrDefault(key, 0);
            }
            float requestsPerSecond = (float)totalRequests / (sleepMilliseconds / 1000);
            Logger.output("[VIEWER] requests per second: " + requestsPerSecond);

            // requests por cliente
            int totalDistinctClients = statistics.get("clients").getStatistic().keySet().size();
            float requestsPerClient = totalDistinctClients > 0 ? (float)totalRequests / totalDistinctClients : 0;
            Logger.output("[VIEWER] requests per client: " + requestsPerClient);

            // cantidad de errores
            int totalErrors = statistics.get("errors").getStatistic().getOrDefault("error", 0);
            Logger.output("[VIEWER] total errors: " + totalErrors);

            // 10 recursos mas pedidos
            LimitedSortedSet<String> topResources = new LimitedSortedSet<>(10, new CountCommaNameComparator());
            Map<String, Integer> resources = statistics.get("resources").getStatistic();
            for (String key : resources.keySet()) {
                topResources.add(resources.get(key) + "," + key);
            }
            Logger.output("[VIEWER] 10 most requested resources: ");
            for (String resource : topResources) {
                Logger.output("\t" + resource);
            }

            Logger.output("\n");

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }
}
