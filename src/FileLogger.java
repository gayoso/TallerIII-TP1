import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;

public class FileLogger extends GracefulRunnableThread {

    private LinkedBlockingQueue inputQueue;

    private String fileName;
    private String folderName;
    private PrintWriter fileWriter;

    public FileLogger(String name, LinkedBlockingQueue queue) {
        super("FileLogger " + name);
        this.inputQueue = queue;
        this.folderName = name;
        this.fileName = this.folderName + "/" + name;
    }

    @Override
    protected void initWork() {

        try {
            if (!Files.exists(Paths.get(folderName))) {
                Files.createDirectory(Paths.get(folderName));
            }
            fileWriter = new PrintWriter(new FileWriter(fileName,
                    true));
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create file: " + fileName,
                    Logger.logLevel.ERROR);
            fileWriter = null;
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    protected void doWork() throws InterruptedException {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        String line = inputQueue.take().toString();
        Logger.log(logName, "Recibi de la cola: " + line,
                Logger.logLevel.INFO);

        fileWriter.println(line);
    }

    @Override
    protected void endWork(){
        Logger.log(logName, "Ending RUN", Logger.logLevel.INFO);

        fileWriter.close();
    }
}
