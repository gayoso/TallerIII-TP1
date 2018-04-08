import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser extends GracefulRunnableThread {

    private LinkedBlockingQueue inputQueue;

    private List<Pattern> patterns;
    private List<LinkedBlockingQueue> queues;

    public Parser(LinkedBlockingQueue queue, List<Pattern> patterns,
                  List<LinkedBlockingQueue> queues) {
        super("Parser");

        this.inputQueue = queue;

        this.patterns = patterns;
        this.queues = queues;
    }

    @Override
    protected void doWork() throws InterruptedException {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        String line = inputQueue.take().toString();
        Logger.log(logName, "Recibi de la cola: " + line,
                Logger.logLevel.INFO);

        // por cada patron registrado se matcha la linea de log
        for (int i = 0; i < patterns.size(); ++i) {
            Matcher matchResult = patterns.get(i).matcher(line);
            if (matchResult.matches()) {
                String resultString = matchResult.group(1);
                // en caso de match j = 1 es la linea completa, asi que
                // se ignora. el resto de los campos capturados se concatenan
                // y se envian a traves de la cola correspondiente
                for (int  j = 2; j <= matchResult.groupCount(); ++j) {
                    resultString += " " + matchResult.group(j);
                }
                // se envian los campos capturados a la cola correspondiente
                queues.get(i).put(resultString);
            }
        }
    }
}
