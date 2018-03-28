import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser extends GracefulRunnable {

    private LinkedBlockingQueue inputQueue;

    private List<Pattern> patterns;
    private List<LinkedBlockingQueue> queues;

    public Parser(LinkedBlockingQueue queue, List<Pattern> patterns, List<LinkedBlockingQueue> queues) {
        super("Parser");

        this.inputQueue = queue;

        this.patterns = patterns;
        this.queues = queues;

        Logger.log("Parser", "Creating object", Logger.logLevel.INFO);
    }

    @Override
    protected void doWork() {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        try {
            String line = inputQueue.take().toString();
            Logger.log(logName, "Recibi de la cola: " + line, Logger.logLevel.INFO);

            // pass to queues
            for (int i = 0; i < patterns.size(); ++i) {
                Matcher matchResult = patterns.get(i).matcher(line);
                if (matchResult.matches()) {
                    String resultString = matchResult.group(1);
                    for (int  j = 2; j <= matchResult.groupCount(); ++j) {
                        resultString += " " + matchResult.group(j);
                    }
                    queues.get(i).put(resultString);
                }
            }

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }
}
