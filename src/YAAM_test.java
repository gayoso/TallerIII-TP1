import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class YAAM_test {

    private static List<String> readConfigLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new LinkedList<>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.startsWith("#")) {
                lines.add(line);
            }
        }
        bufferedReader.close();
        return lines;
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        // ---------------------- INIT INPUT AND VARIABLES FROM CONFIG ----------------------

        Logger.init("YAAM_log.txt");

        Scanner sc = new Scanner(System.in);
        /*File apacheLogs = new File("test_log.txt");
        Scanner sc = new Scanner(apacheLogs);*/

        String config_general = readConfigLines("config_general").get(0);
        Logger.currentLogLevel = Logger.intToLogLevel(Integer.parseInt(config_general.split(",")[0]));
        int PARSER_POOL_SIZE = Integer.parseInt(config_general.split(",")[1]);

        List<String> config_statistics = readConfigLines("config_statistics");

        List<String> config_statistics_viewer = readConfigLines("config_statistics_viewer");
        String config_statistics_viewer_line1 = config_statistics_viewer.remove(0);
        int statisticsViewWaitMilliseconds = Integer.parseInt(config_statistics_viewer_line1.split(",")[0]);

        List<String> config_loggers = readConfigLines("config_loggers");

        List<String> config_rankings = readConfigLines("config_rankings");
        String config_rankings_line1 = config_rankings.remove(0);
        int rankingTempFileMaxLines = Integer.parseInt(config_rankings_line1.split(",")[0]);
        int rankingTempNumThreads = Integer.parseInt(config_rankings_line1.split(",")[1]);
        int rankingMergerSleepMilliseconds = Integer.parseInt(config_rankings_line1.split(",")[2]);
        int maxErrorsToShow = Integer.parseInt(config_rankings_line1.split(",")[3]);

        // ---------------------- LOAD STATISTICS AND START STATISTICS UPDATER THREADS ----------------------

        List<String> statisticsNames = new LinkedList<>();
        List<Pattern> statisticsRegex = new LinkedList<>();
        HashMap<String, Statistic> statistics = new HashMap<>();
        List<LinkedBlockingQueue> statisticUpdatersQueues = new LinkedList<>();
        List<StatisticUpdater> statisticUpdaters = new LinkedList<>();
        List<Thread> statisticUpdaterThreads = new LinkedList<>();

        /*String[] config_statistics = {"requests,^(.+ .+ \\[.+\\] \".+ .+ .+\" [0-9]{3} .+)$",
                "clients,^(.+) .+ \\[.+\\] \".+ .+ .+\" [0-9]{3} .+$",
                "errors,^.+ .+ \\[(error)\\] \".+ .+ .+\" [0-9]{3} .+$",
                "resources,^.+ .+ \\[.+\\] \".+ (.+) .+\" [0-9]{3} .+$"};*/


        for (int i = 0; i < config_statistics.size(); ++i) {
            String[] splitLine = config_statistics.get(i).split(",");
            statisticsNames.add(splitLine[0]);
            statisticsRegex.add(Pattern.compile(splitLine[1]));

            statistics.put(statisticsNames.get(i), new Statistic(statisticsNames.get(i)));
            statisticUpdatersQueues.add(new LinkedBlockingQueue());
            statisticUpdaters.add(new StatisticUpdater(statisticUpdatersQueues.get(i),
                    statistics.get(statisticsNames.get(i))));
            statisticUpdaterThreads.add(new Thread(statisticUpdaters.get(i)));
            statisticUpdaterThreads.get(i).start();
        }

        // ---------------------- START STATISTICS VIEWER THREAD ----------------------

        StatisticViewer statisticViewer = new StatisticViewer(statistics, statisticsViewWaitMilliseconds);
        Thread statisticsViewerThread = new Thread(statisticViewer);
        statisticsViewerThread.start();

        // ---------------------- START LOGGING THREADS ----------------------

        List<String> loggerNames = new LinkedList<>();
        List<Pattern> loggerRegex = new LinkedList<>();
        List<LinkedBlockingQueue> fileLoggersQueues = new LinkedList<>();
        List<FileLogger> fileLoggers = new LinkedList<>();
        List<Thread> fileLoggersThreads = new LinkedList<>();

        /*String[] config_loggers = {"full_log,^(.+ .+ \\[.+\\] \".+ .+ .+\" [0-9]{3} .+)$",
                                    "error_log,^.+ (.+) \\[error\\] \".+ .+ .+\" [0-9]{3} (.+)$"};*/

        for (int i = 0; i < config_loggers.size(); ++i) {
            String[] splitLine = config_loggers.get(i).split(",");

            loggerNames.add(splitLine[0]);
            loggerRegex.add(Pattern.compile(splitLine[1]));
            fileLoggersQueues.add(new LinkedBlockingQueue());
            fileLoggers.add(new FileLogger(loggerNames.get(i), fileLoggersQueues.get(i)));
            fileLoggersThreads.add(new Thread(fileLoggers.get(i)));
            fileLoggersThreads.get(i).start();
        }

        // ---------------------- START RANKING THREADS ----------------------

        List<String> rankingNames = new LinkedList<>();
        List<Pattern> rankingRegex = new LinkedList<>();
        List<LinkedBlockingQueue> rankingQueues = new LinkedList<>();
        List<List<RankingLogger>> rankingLoggers = new LinkedList<>();
        List<List<Thread>> rankingThreads = new LinkedList<>();
        Object finishedLogFilesLock = new Object();

        /*String[] config_rankings = {"errors_ranking,^.+ .+ \\[error\\] \".+ .+ .+\" [0-9]{3} (.+)$"};*/

        for (int i = 0; i < config_rankings.size(); ++i) {
            String[] splitLine = config_rankings.get(i).split(",");

            rankingNames.add(splitLine[0]);
            rankingRegex.add(Pattern.compile(splitLine[1]));
            rankingQueues.add(new LinkedBlockingQueue());
            rankingLoggers.add(new LinkedList<>());
            rankingThreads.add(new LinkedList<>());
            for (int j = 0; j < rankingTempNumThreads; ++j) {
                RankingLogger rankingLogger = new RankingLogger(rankingNames.get(i), rankingQueues.get(i),
                        finishedLogFilesLock, rankingTempFileMaxLines);
                rankingLoggers.get(i).add(rankingLogger);
                Thread rankingLoggerThread = new Thread(rankingLogger);
                rankingThreads.get(i).add(rankingLoggerThread);
                rankingLoggerThread.start();
            }
        }

        // ---------------------- START RANKING MERGE THREADS ----------------------

        List<RankingLoggerMerge> rankingMergers = new LinkedList<>();
        List<Thread> rankingMergerThreads = new LinkedList<>();

        for (int i = 0; i < config_rankings.size(); ++i) {
            String[] splitLine = config_rankings.get(i).split(",");

            rankingMergers.add(new RankingLoggerMerge(splitLine[0],
                    finishedLogFilesLock,
                    rankingMergerSleepMilliseconds, maxErrorsToShow));
            rankingMergerThreads.add(new Thread(rankingMergers.get(i)));
            rankingMergerThreads.get(i).start();
        }

        // ---------------------- START PARSER THREAD POOL ----------------------

        LinkedBlockingQueue analyzerPoolQueue = new LinkedBlockingQueue();

        Parser[] analyzers = new Parser[PARSER_POOL_SIZE];
        Thread[] analyzerThreads = new Thread[PARSER_POOL_SIZE];

        List<LinkedBlockingQueue> allQueues = new LinkedList<>();
        allQueues.addAll(statisticUpdatersQueues);
        allQueues.addAll(fileLoggersQueues);
        allQueues.addAll(rankingQueues);
        List<Pattern> allRegex = new LinkedList<>();
        allRegex.addAll(statisticsRegex);
        allRegex.addAll(loggerRegex);
        allRegex.addAll(rankingRegex);

        for (int i = 0; i < PARSER_POOL_SIZE; ++i) {
            analyzers[i] = new Parser(analyzerPoolQueue, allRegex, allQueues);
            analyzerThreads[i] = new Thread(analyzers[i]);
            analyzerThreads[i].start();
        }


        // ---------------------- SHUTDOWN HOOK ----------------------

        Runtime.getRuntime ().addShutdownHook ( new Thread () {
            @Override
            public void run () {
                System.out.println ( "Shutdown hook" );
                Logger.log("main", "Closing everything", Logger.logLevel.INFO);

                try {
                    // ---------------------- CLOSE STATISTIC UPADTER THREADS ----------------------

                    for (int i = 0; i < statisticUpdaterThreads.size(); ++i) {
                        statisticUpdaters.get(i).stopKeepAlive();
                        statisticUpdaterThreads.get(i).interrupt();
                        statisticUpdaterThreads.get(i).join();
                    }

                    // ---------------------- CLOSE STATISTIC VIEWER THREAD ----------------------

                    statisticViewer.stopKeepAlive();
                    statisticsViewerThread.interrupt();
                    statisticsViewerThread.join();

                    // ---------------------- CLOSE LOGGER THREADS ----------------------

                    for (int i = 0; i < fileLoggersThreads.size(); ++i) {
                        fileLoggers.get(i).stopKeepAlive();
                        fileLoggersThreads.get(i).interrupt();
                        fileLoggersThreads.get(i).join();
                    }

                    // ---------------------- CLOSE RANKING THREADS ----------------------

                    for (int i = 0; i < rankingThreads.size(); ++i) {
                        List<RankingLogger> oneRankLoggers = rankingLoggers.get(i);
                        List<Thread> oneRankLoggerThreads = rankingThreads.get(i);
                        for (int j = 0; j < oneRankLoggers.size(); ++j) {
                            oneRankLoggers.get(j).stopKeepAlive();
                            oneRankLoggerThreads.get(j).interrupt();
                            oneRankLoggerThreads.get(j).join();
                        }
                    }

                    // ---------------------- CLOSE RANKING MERGER THREADS ----------------------

                    for (int i = 0; i < rankingMergerThreads.size(); ++i) {
                        rankingMergers.get(i).stopKeepAlive();
                        rankingMergerThreads.get(i).interrupt();
                        rankingMergerThreads.get(i).join();
                    }

                    // ---------------------- CLOSE PARSER THREADS ----------------------

                    for (int i = 0; i < PARSER_POOL_SIZE; ++i) {
                        analyzers[i].stopKeepAlive();
                        analyzerThreads[i].interrupt();
                        analyzerThreads[i].join();
                    }

                    Logger.log("main", "All done", Logger.logLevel.INFO);
                } catch (InterruptedException e) {
                    Logger.log("main", "Shutdown hook interrupted", Logger.logLevel.INFO);
                }

                Logger.close();
            }
        } );

        // ---------------------- MAIN LOOP ----------------------

        boolean endSignal = false;
        while(!endSignal && sc.hasNextLine()) {
            String logLine = sc.nextLine();
            Logger.log("main", "Recibi de la cola: " + logLine, Logger.logLevel.INFO);

            if (logLine.equals("end")) {
                endSignal = true;
            } else {
                analyzerPoolQueue.put(logLine);
            }
        }
    }

}
