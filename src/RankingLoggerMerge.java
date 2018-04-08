import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public class RankingLoggerMerge extends GracefulRunnableTask {

    // config variables
    private String finishedLogfiles;
    private Object finishedLogfilesLock;
    //private int timeWindow;
    private int numErrorsToList;
    private String folderName;
    private String tempFolderName;
    private String tempOutputFilenamePrefix;
    private String outputFilename;

    // loop variables
    private String outFilename;
    private PrintWriter outputFileWriter;
    private String oldRanking;
    private List<String> currentFinishedLogFiles;
    private List<BufferedReader> fileReaders;
    private List<String> lines;
    private LimitedSortedSet<String> mostFrequentErrors;

    public RankingLoggerMerge(String name, Object finishedLogfilesLock,
                              int period, int numErrorsToList) {
        super("RankingLoggerMerge " + name, period, period);

        this.folderName = name;
        this.tempFolderName = folderName + "/temp/";
        this.finishedLogfiles = tempFolderName + "_finished_logfilenames";
        this.tempOutputFilenamePrefix = folderName + "/temp/ranking_";
        this.outputFilename = folderName + "/ranking";

        this.finishedLogfilesLock = finishedLogfilesLock;
        this.numErrorsToList = numErrorsToList;
    }

    // en vez de hacer un merge de todos los archivos generados por los
    // RankingLoggers se usa el ranking anterior (si hay) y los archivos
    // generados luego de ese
    private String getOldRankingFilename() {

        File dir = new File(tempFolderName);
        File[] files = dir.listFiles((d, name) -> name.startsWith("ranking"));
        if (files.length <= 0) {
            return "";
        }

        String newest = files[0].getName();
        for (int i = 1; i < files.length; ++i) {
            if (newest.compareTo(files[i].getName()) < 0) {
                newest = files[i].getName();
            }
        }

        Logger.log(logName, "Using old ranking: " + tempFolderName
                + newest, Logger.logLevel.INFO);
        return tempFolderName + newest;
    }

    @Override
    protected void initWork() {

        // create work folder
        try {
            if (!Files.exists(Paths.get(folderName))) {
                Files.createDirectory(Paths.get(folderName));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    folderName, Logger.logLevel.WARNING);
        }

        // create temp folder inside work folder
        try {
            if (!Files.exists(Paths.get(tempFolderName))) {
                Files.createDirectory(Paths.get(tempFolderName));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    tempFolderName, Logger.logLevel.WARNING);
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    protected void doWork() throws InterruptedException {

        /*Logger.log(logName, "Going to sleep for " +
                        (timeWindow / 1000) + " seconds",
                        Logger.logLevel.INFO);
        Thread.sleep(timeWindow);*/
        Logger.log(logName, "Waking up", Logger.logLevel.INFO);

        // los archivos a mergear se sacan de la lista y despues se limpia
        currentFinishedLogFiles = finishedLogfileNames();
        if (currentFinishedLogFiles.size() <= 0) {
            return;
        }

        Logger.output("[RANKING LOGGER MERGE " + folderName
                + "] Going to work on merging files");

        // get newest old ranking file
        oldRanking = getOldRankingFilename();
        if (oldRanking != "") {
            // si hay un ranking anterior lo voy a usar en el merge
            currentFinishedLogFiles.add(oldRanking);
        }

        // output file
        String timestamp = new SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss_SSS").format(new Date());
        outFilename = tempOutputFilenamePrefix + timestamp;
        try {
            outputFileWriter =
                    new PrintWriter(new FileWriter(outFilename, true));
        } catch (IOException e) {
            Logger.log(logName, "Error opening output file: "
                    + outFilename, Logger.logLevel.ERROR);
            return;
        }

        // se inician los FileReaders y se lee
        // la primera linea de cada archivo
        fileReaders = new LinkedList<>();
        lines = new LinkedList<>();
        initFileReadersForMerge();

        // el resultado final del merge es un archivo con todos los errores
        // ordenados alfabeticamente, y otro mas reducido con los N con mas
        // apariciones, ordenado en memoria
        mostFrequentErrors = new LimitedSortedSet<>(numErrorsToList,
                        new CountCommaNameComparator());

        while (fileReaders.size() > 0 && lines.size() > 0) {

            if (shouldStop()) {
                interruptMerge();
                return;
            }

            String[] errMsgAndCount = getNextHighestCountError();
            String errMsg = errMsgAndCount[0];
            String errCount = errMsgAndCount[1];
            outputFileWriter.println(errMsg + "," + errCount);
            mostFrequentErrors.add(errCount + "," + errMsg);

            clearEmptyFiles();
        }

        // merge alfabetico completado
        outputFileWriter.close();

        // falta ordenar y guardar a archivo los N con mas apariciones
        writeReducedRankingToFile();
    }

    private List<String> finishedLogfileNames() {
        List<String> currentFinishedLogFiles = new LinkedList<>();
        synchronized (finishedLogfilesLock) {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(finishedLogfiles);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    currentFinishedLogFiles.add(line);
                }
                bufferedReader.close();

                File deleteFile = new File(finishedLogfiles);
                deleteFile.delete();
            } catch (FileNotFoundException e) {
                Logger.log(logName, "No new temp logfiles ",
                        Logger.logLevel.INFO);
                currentFinishedLogFiles.clear();
            } catch (IOException e) {
                Logger.log(logName, "Error loading temp rank " +
                        "filenames: " + e.getMessage(), Logger.logLevel
                        .ERROR);
                currentFinishedLogFiles.clear();
            }
        }
        return currentFinishedLogFiles;
    }

    private void initFileReadersForMerge() {
        for (int i = 0; i < currentFinishedLogFiles.size(); ++i) {
            String filename = currentFinishedLogFiles.get(i);
            try {
                fileReaders.add(new BufferedReader(new FileReader(filename)));
                try {
                    lines.add(fileReaders.get(i).readLine());
                } catch (IOException e) {
                    Logger.log(logName, "Error reading from file: " +
                            filename,  Logger.logLevel.ERROR);
                    fileReaders.remove(i);
                }
            } catch (FileNotFoundException e) {
                Logger.log(logName, "Error opening file: " + filename,
                        Logger.logLevel.ERROR);
            }
        }
    }

    private void interruptMerge() {
        // close file readers
        for (int i = 0; i < fileReaders.size(); ++i) {
            try {
                fileReaders.get(i).close();
            } catch (IOException e) {
                Logger.log(logName,
                        "Error closing file",
                        Logger.logLevel.ERROR);
            }
        }

        // delete temp output
        outputFileWriter.close();
        File deleteOutFile = new File(outFilename);
        deleteOutFile.delete();

        // save filenames back to file for future processing
        if (oldRanking != "") {
            currentFinishedLogFiles.remove
                    (currentFinishedLogFiles.size() - 1);
        }
        PrintWriter tempFilenamesWriter = null;
        try {
            tempFilenamesWriter = new PrintWriter(new
                    FileWriter(finishedLogfiles, true));
        } catch (IOException e) {
            Logger.log(logName,
                    "Error opening file after" +
                            "interrupt",
                    Logger.logLevel.ERROR);
        }
        for (int i = 0; i < currentFinishedLogFiles.size();
             ++i) {
            tempFilenamesWriter.println
                    (currentFinishedLogFiles.get(i));
        }
        tempFilenamesWriter.close();

        Logger.log(logName, "Finished InterruptMerge", Logger.logLevel.INFO);
    }

    private String[] getNextHighestCountError() {
        // se busca el siguiente error con mas apariciones. no es un merge
        // comun, porque hay registros de la forma "1,error1", "2,error1"
        // en cada archivo no puede aparecer dos veces un error entonces
        // se hace un merge acumulando las apariciones de la linea actual
        // en cada archivo
        List<Integer> smallestIndexes = new LinkedList<>();
        smallestIndexes.add(0);

        String errMsg = lines.get(0).split(",")[0];
        int errMsgCount = Integer.parseInt(lines.get(0).split(",")[1]);
        for (int i = 1; i < lines.size(); ++i) {
            int compVal = lines.get(i).split(",")[0].compareTo(errMsg);
            if (compVal < 0) {
                smallestIndexes.clear();
                smallestIndexes.add(i);

                String[] line = lines.get(i).split(",");
                errMsg = line[0];
                errMsgCount = Integer.parseInt(line[1]);
            } else if (compVal == 0) {
                smallestIndexes.add(i);
                errMsgCount += Integer.parseInt(lines.get(i).split(",")[1]);
            }
        }

        // advance used files
        for (int i = 0; i < smallestIndexes.size(); ++i) {
            int smallestIndex = smallestIndexes.get(i);

            try {
                lines.set(smallestIndex,
                        fileReaders.get(smallestIndex).readLine());
            } catch (IOException e) {
                Logger.log(logName, "Error reading from file: "
                        + smallestIndex, Logger.logLevel.ERROR);
            }
        }

        return new String[] {errMsg, Integer.toString(errMsgCount)};
    }

    private void clearEmptyFiles() {
        // clear empty files
        Iterator<String> itLines = lines.iterator();
        Iterator<BufferedReader> itFilesReaders = fileReaders.iterator();
        while(itLines.hasNext() && itFilesReaders.hasNext()) {
            BufferedReader fr = itFilesReaders.next();
            String str = itLines.next();
            if (str == null) {
                itLines.remove();
                try {
                    fr.close();
                } catch (IOException e) {
                    Logger.log(logName,
                            "Error closing file",
                            Logger.logLevel.ERROR);
                }
                itFilesReaders.remove();
            }
        }
    }

    private void writeReducedRankingToFile() {
        try {
            File ranking = new File(outputFilename);
            if (ranking.exists()) {
                ranking.delete();
            }
            PrintWriter freqErrorsFileWriter = new PrintWriter(
                    new FileWriter(outputFilename));
            for (String line : mostFrequentErrors) {
                freqErrorsFileWriter.println(line);
            }
            freqErrorsFileWriter.close();
        } catch (IOException e) {
            Logger.log(logName, "Error opening output file: "
                    + outputFilename, Logger.logLevel.ERROR);
        }
    }
}
