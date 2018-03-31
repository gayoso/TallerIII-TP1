import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class RankingLogger extends GracefulRunnable {

    private LinkedBlockingQueue inputQueue;

    private Object finishedLogfilesLock;
    private Map<String, Integer> lines = new HashMap<>();
    private int maxLines;
    private String folderName;
    private String finishedLogfiles;

    public RankingLogger(String name, LinkedBlockingQueue queue, Object
            finishedLogfilesLock, int maxLines) {

        super("RankingLogger " + name);

        this.folderName = name;
        this.inputQueue = queue;
        this.maxLines = maxLines;
        this.finishedLogfiles = folderName + "/temp/" +
                "_finished_logfilenames";
        this.finishedLogfilesLock = finishedLogfilesLock;
    }

    // se acumulan en memoria hasta cierto numero de errores con su cantidad de apariciones
    // si se pasa ese numero maximo, se hace un dump de los errores ordenados a un archivo
    private void saveLinesToFile() {

        Logger.log(logName, "Dumping temp rank file", Logger.logLevel.INFO);

        try {

            List<String> stringLines = new LinkedList<>();
            for (String key : lines.keySet()) {
                String line = key + "," + lines.get(key);
                stringLines.add(line);
            }
            stringLines.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return Collator.getInstance().compare(o1, o2);
                }
            });

            String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(new Date());
            String filename = logName.split(" ")[1] + "/temp/" + Thread.currentThread().getName() +
                    "_" + timestamp + "-";
            int sufix = 0;
            File f = new File(filename + sufix);
            while (f.exists()) {
                sufix++;
                f = new File(filename + sufix);
            }
            filename += sufix;

            PrintWriter fileWriter = new PrintWriter(new FileWriter(filename, true));
            for (String line : stringLines) {
                fileWriter.println(line);
            }
            fileWriter.close();
            lines.clear();

            synchronized (finishedLogfilesLock) {
                PrintWriter finishedLogfilesWriter = new PrintWriter(new
                        FileWriter
                        (finishedLogfiles, true));
                finishedLogfilesWriter.println(filename);
                finishedLogfilesWriter.close();
            }
        } catch (IOException e) {
            Logger.log(logName, e.getMessage(), Logger.logLevel.ERROR);
        }
    }

    @Override
    protected void initWork() {
        try {
            if (!Files.exists(Paths.get(folderName))) {
                Files.createDirectory(Paths.get(folderName));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " + folderName, Logger.logLevel.ERROR);
        }

        try {
            if (!Files.exists(Paths.get(folderName + "/temp"))) {
                Files.createDirectory(Paths.get(folderName + "/temp"));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " + folderName + "/temp", Logger.logLevel.ERROR);
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    public void doWork() {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        try {
            String line = inputQueue.take().toString();
            Logger.log(logName, "Recibi de la cola: " + line, Logger.logLevel.INFO);

            if (lines.size() == maxLines && !lines.containsKey(line)) {
                Logger.log(logName, "Dumping lines to file", Logger.logLevel.INFO);
                saveLinesToFile();
            }
            lines.merge(line, 1, Integer::sum);

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }
}
