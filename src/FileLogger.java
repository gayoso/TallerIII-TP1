import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;

public class FileLogger extends GracefulRunnable {

    private LinkedBlockingQueue inputQueue;

    private String filename;
    private PrintWriter fileWriter;

    public FileLogger(String name, LinkedBlockingQueue queue) {
        super("FileLogger " + name);
        this.inputQueue = queue;
        this.filename = name;
    }

    @Override
    protected void initWork() {

        try {
            if (!Files.exists(Paths.get(filename))) {
                Files.createDirectory(Paths.get(filename));
            }
            fileWriter = new PrintWriter(new FileWriter(filename + "/" + filename, true));
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create file: " + filename + "/" + filename, Logger.logLevel.ERROR);
            fileWriter = null;
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    protected void doWork() {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        try {
            String line = inputQueue.take().toString();
            Logger.log(logName, "Recibi de la cola: " + line, Logger.logLevel.INFO);

            fileWriter.println(line);

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }

    @Override
    protected void endWork(){
        Logger.log(logName, "Ending RUN", Logger.logLevel.INFO);

        fileWriter.close();
    }
}
