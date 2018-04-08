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

public class RankingLogger extends GracefulRunnableThread {

    private LinkedBlockingQueue inputQueue;

    private Object finishedLogfilesLock;
    private Map<String, Integer> lines = new HashMap<>();
    private int maxLines;
    private int maxOcurrances;
    private int currentOcurrances;

    private String folderName;
    private String folderNameTemp;
    private String finishedLogfilesList;

    public RankingLogger(String name, LinkedBlockingQueue queue, Object
            finishedLogfilesLock, int maxLines, int maxOcurrances) {

        super("RankingLogger " + name);
        this.inputQueue = queue;
        this.maxLines = maxLines;
        this.maxOcurrances = maxOcurrances;
        this.currentOcurrances = 0;

        this.folderName = name;
        this.folderNameTemp = this.folderName + "/temp/";
        this.finishedLogfilesList = folderNameTemp + "_finished_logfilenames";
        this.finishedLogfilesLock = finishedLogfilesLock;
    }

    // se acumulan en memoria hasta cierto numero de errores con su cantidad
    // de apariciones si se pasa ese numero maximo, se hace un dump de los
    // errores ordenados a un archivo
    private void saveLinesToFile() {

        Logger.log(logName, "Dumping temp rank file",
                Logger.logLevel.INFO);

        try {

            List<String> stringLines = sortedLines();

            // write dump file
            String filename = logDumpFilename();
            PrintWriter fileWriter = new PrintWriter(new FileWriter(filename,
                    true));
            for (String line : stringLines) {
                fileWriter.println(line);
            }
            fileWriter.close();
            lines.clear();
            currentOcurrances = 0;

            // add dump file name to list for processing
            synchronized (finishedLogfilesLock) {
                PrintWriter finishedLogfilesWriter = new PrintWriter(new
                        FileWriter
                        (finishedLogfilesList, true));
                finishedLogfilesWriter.println(filename);
                finishedLogfilesWriter.close();
            }
        } catch (IOException e) {
            Logger.log(logName, e.getMessage(), Logger.logLevel.ERROR);
        }
    }

    private List<String> sortedLines() {
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
        return stringLines;
    }

    private String logDumpFilename() {
        String timestampPattern = "yyyy_MM_dd_HH_mm_ss_SSS";
        String timestamp = new SimpleDateFormat(timestampPattern)
                .format(new Date());
        String filename = folderNameTemp + Thread.currentThread().getName()
                + "_" + timestamp + "-";
        int sufix = 0;
        File f = new File(filename + sufix);
        while (f.exists()) {
            sufix++;
            f = new File(filename + sufix);
        }
        filename += sufix;
        return filename;
    }

    @Override
    protected void initWork() {
        try {
            if (!Files.exists(Paths.get(folderName))) {
                Files.createDirectory(Paths.get(folderName));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    folderName, Logger.logLevel.WARNING);
        }

        try {
            if (!Files.exists(Paths.get(folderNameTemp))) {
                Files.createDirectory(Paths.get(folderNameTemp));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    folderNameTemp, Logger.logLevel.WARNING);
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    public void doWork() throws InterruptedException {

        Logger.log(logName, "Waiting for input", Logger.logLevel.INFO);

        String line = inputQueue.take().toString();
        Logger.log(logName, "Recibi de la cola: " + line,
                Logger.logLevel.INFO);

        if ((lines.size() == maxLines && !lines.containsKey(line)) ||
                currentOcurrances == maxOcurrances) {
            Logger.log(logName, "Dumping lines to file",
                    Logger.logLevel.INFO);
            saveLinesToFile();
        }
        lines.merge(line, 1, Integer::sum);
        currentOcurrances++;
    }
}
