import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class Monitor {

    private Config config = null;

    private List<Pattern> patterns = new LinkedList<>();
    private List<LinkedBlockingQueue> queues = new LinkedList<>();
    private LinkedBlockingQueue parsersQueue;

    private List<GracefulRunnable> runnables = new LinkedList<>();

    public boolean loadConfig(String configFilename) {
        try {
            // read json config
            BufferedReader br = new BufferedReader(
                    new FileReader( configFilename));
            String jsonString = "";
            String s;
            while ((s = br.readLine()) != null) {
                jsonString += s;
            }

            // load to object
            config = new Gson().fromJson(jsonString, Config.class);

            // general configs
            Logger.currentLogLevel = Logger.intToLogLevel(config.logLevel);
            GracefulRunnableTask.setPoolSize(config.maxTaskPoolSize);

            return true;
        }
        catch (FileNotFoundException e) {
            Logger.log("Monitor", "config file: " + configFilename +
                    " not found", Logger.logLevel.ERROR);
        } catch (IOException e) {
            Logger.log("Monitor", "config file: " + configFilename +
                    " could not be read", Logger.logLevel.ERROR);
        }

        return false;
    }

    public void createStatistics() {

        // statistic updater threads
        List<Statistic> statistics = new LinkedList<>();
        for (ConfigNameRegex conf : config.config_statistics) {

            String statisticName = conf.name;
            String pattern = conf.regex;

            Statistic statistic = new Statistic(statisticName);
            statistics.add(statistic);
            LinkedBlockingQueue statisticQueue = new LinkedBlockingQueue();
            StatisticUpdater statisticUpdater =
                    new StatisticUpdater(statisticQueue, statistic);

            patterns.add(Pattern.compile(pattern));
            queues.add(statisticQueue);
            runnables.add(statisticUpdater);
        }

        // statistics viewer
        int delay = config.config_statistics_viewer.millisecondsWindow;
        StatisticViewer viewer = new StatisticViewer(statistics, delay);

        runnables.add(viewer);
    }

    public void createLoggers() {

        for (ConfigNameRegex conf : config.config_loggers) {

            String name = conf.name;
            String pattern = conf.regex;

            LinkedBlockingQueue loggerQueue = new LinkedBlockingQueue();
            FileLogger fileLogger = new FileLogger(name, loggerQueue);

            patterns.add(Pattern.compile(pattern));
            queues.add(loggerQueue);
            runnables.add(fileLogger);
        }
    }

    public void createRankings() {

        for (ConfigNameRegex conf : config.config_rankings.rankings) {

            String name = conf.name;
            String pattern = conf.regex;

            // temp threads
            Object finishedLogFilesLock = new Object();
            LinkedBlockingQueue rankingLoggerQueue = new LinkedBlockingQueue();
            for (int j = 0; j < config.config_rankings.numthreadsPerRankingDump;
                 ++j) {
                RankingLogger rankingLogger = new RankingLogger(name,
                        rankingLoggerQueue, finishedLogFilesLock,
                        config.config_rankings.linesPerTempFile,
                        config.config_rankings.ocurrancesPerTempFile);

                runnables.add(rankingLogger);
            }
            patterns.add(Pattern.compile(pattern));
            queues.add(rankingLoggerQueue);

            // merge thread
            RankingLoggerMerge rankingMerger = new RankingLoggerMerge(name,
                    finishedLogFilesLock,
                    config.config_rankings.rankingMergeSleepMilliseconds,
                    config.config_rankings.rankingDisplayNum);

            runnables.add(rankingMerger);
        }
    }

    // el orden de llamadas es importante, este tiene que llamarse despues de
    // los demas porque necesita sus colas
    public void createParsers() {

        parsersQueue = new LinkedBlockingQueue();

        for (int i = 0; i < config.parserNumThreads; ++i) {
            Parser parser = new Parser(parsersQueue, patterns, queues);

            runnables.add(parser);
        }
    }

    public void startAll() {
        for (GracefulRunnable runnable : runnables) {
            runnable.start();
        }
    }

    public void stopAll() throws InterruptedException {
        for (GracefulRunnable runnable : runnables) {
            runnable.stop();
        }
        for (GracefulRunnable runnable : runnables) {
            runnable.join();
        }
    }

    public void processLog(String log) throws InterruptedException {
        parsersQueue.put(log);
    }
}
