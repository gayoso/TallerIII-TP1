import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static logLevel currentLogLevel = logLevel.INFO;
    private static PrintWriter logWriter = null;

    public enum logLevel {
        ERROR,
        WARNING,
        INFO
    }

    public static logLevel intToLogLevel(int i) {
        switch (i) {
            case 0:
                return logLevel.ERROR;
            case 1:
                return logLevel.WARNING;
            case 2:
                return logLevel.INFO;
            default:
                return logLevel.INFO;
        }
    }

    private static String logLevelToString(logLevel level) {
        switch(level) {
            case ERROR:
                return "[ERROR]";
            case WARNING:
                return "[WARNING]";
            case INFO:
                return "[INFO]";
            default:
                return "[INVALID LOGLEVEL]";
        }
    }

    public static void init(String filename) {
        try {
            logWriter = new PrintWriter(new FileWriter(filename));

            String timeStamp = new SimpleDateFormat("yyyy/MM/dd/ HH:mm:ss").format(new Date());
            logWriter.println("***************" + timeStamp + "***************");
        } catch (IOException e) {
            log("Logger", "Couldn't open logfile for writing", logLevel.ERROR);
        }
    }

    public static void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }

    public static void log(String name, String message, logLevel level) {

        String logLine = Thread.currentThread().getName() + "\t" +
                logLevelToString(level) + "\t" + name + ": " + message;

        // output to screen
        if (currentLogLevel.ordinal() >= level.ordinal()) {
            System.out.println(logLine);
        }
        // output to logfile
        if (logWriter != null) {
            logWriter.println(logLine);
        }
    }

    public static void output(String outString) {
        System.out.println(outString);
        logWriter.println(outString);
    }

}
