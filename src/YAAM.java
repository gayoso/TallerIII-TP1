
import java.io.*;
import java.util.*;

public class YAAM {

    public static void main(String[] args) throws InterruptedException {

        // ----------- INIT INPUT AND VARIABLES FROM CONFIG -----------

        Logger.init("YAAM_log.txt");

        Scanner sc = new Scanner(System.in);

        // ----------- MONITOR -----------
        Monitor monitor = new Monitor();
        if (!monitor.loadConfig("config")) {
            return;
        }

        // ----------- STATISTICS -----------
        monitor.createStatistics();

        // ----------- LOGGERS -----------
        monitor.createLoggers();

        // ----------- RANKINGS -----------
        monitor.createRankings();

        // ----------- PARSERS -----------
        monitor.createParsers();


        // ----------- SHUTDOWN HOOK -----------

        Runtime.getRuntime ().addShutdownHook ( new Thread () {
            @Override
            public void run () {
                Logger.output ( "Shutdown hook" );
                Logger.log("main", "Closing everything",
                        Logger.logLevel.INFO);
                try {
                    monitor.stopAll();
                    Logger.log("main", "All done",
                            Logger.logLevel.INFO);
                } catch (InterruptedException e) {
                    Logger.log("main", "Shutdown hook interrupted",
                            Logger.logLevel.INFO);
                }
                Logger.close();
            }
        } );

        // ----------- MAIN LOOP -----------

        monitor.startAll();

        while(sc.hasNextLine()) {
            String logLine = sc.nextLine();
            Logger.log("main", "Recibi de la cola: " + logLine,
                    Logger.logLevel.INFO);
            monitor.processLog(logLine);
        }
    }

}
