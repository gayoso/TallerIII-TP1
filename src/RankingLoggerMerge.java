import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class RankingLoggerMerge extends GracefulRunnable {

    private String finishedLogfiles;
    private Object finishedLogfilesLock;
    private int sleepMilliseconds;
    private int numErrorsToList;
    private String folderName;

    public RankingLoggerMerge(String name, Object finishedLogfilesLock,
                              int sleepMilliseconds, int numErrorsToList) {
        super("RankingLoggerMerge " + name);

        this.folderName = name;
        this.finishedLogfiles = folderName + "/temp/" +
                "_finished_logfilenames";
        this.finishedLogfilesLock = finishedLogfilesLock;
        this.sleepMilliseconds = sleepMilliseconds;
        this.numErrorsToList = numErrorsToList;
    }

    // en vez de hacer un merge de todos los archivos generados por los
    // RankingLoggers se usa el ranking anterior (si hay) y los archivos
    // generados luego de ese
    private String getOldRankingFilename() {

        File dir = new File(folderName + "/temp/");
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

        Logger.log(logName, "Using old ranking: " + folderName +
                "/temp/" + newest, Logger.logLevel.INFO);
        return folderName + "/temp/" + newest;
    }

    @Override
    protected void initWork() {
        try {
            if (!Files.exists(Paths.get(folderName))) {
                Files.createDirectory(Paths.get(folderName));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    folderName, Logger.logLevel.ERROR);
        }

        try {
            if (!Files.exists(Paths.get(folderName + "/temp"))) {
                Files.createDirectory(Paths.get(folderName + "/temp"));
            }
        } catch (IOException e) {
            Logger.log(logName, "Couldn't create folder: " +
                    folderName + "/temp", Logger.logLevel.ERROR);
        }

        Logger.log(logName, "Starting RUN", Logger.logLevel.INFO);
    }

    @Override
    protected void doWork() {

        try {

            Logger.log(logName, "Going to sleep for " +
                            (sleepMilliseconds / 1000) + " seconds",
                            Logger.logLevel.INFO);
            Thread.sleep(sleepMilliseconds);
            Logger.log(logName, "Waking up", Logger.logLevel.INFO);

            // los archivos a mergear se sacan de la lista y despues se borra
            List<String> currentFinishedLogFiles = new LinkedList<>();
            synchronized (finishedLogfilesLock) {
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(finishedLogfiles);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        currentFinishedLogFiles.add(line);
                    }
                }
                bufferedReader.close();

                File deleteFile = new File(finishedLogfiles);
                deleteFile.delete();
                } catch (FileNotFoundException e) {
                    Logger.log(logName, "Error loading temp rank " +
                            "filenames: " + e.getMessage(), Logger.logLevel
                            .ERROR);
                    currentFinishedLogFiles.clear();
                } catch (IOException e) {
                    Logger.log(logName, "Error loading temp rank " +
                            "filenames: " + e.getMessage(), Logger.logLevel
                            .ERROR);
                    currentFinishedLogFiles.clear();
                }
            }

            if (currentFinishedLogFiles.size() > 0) {

                //Logger.log(logName, "Doing work", Logger.logLevel.INFO);
                Logger.output("[RANKING LOGGER MERGE " + folderName
                        + "] Going to work on merging files");

                // get newest old ranking file
                String oldRanking = getOldRankingFilename();
                if (oldRanking != "") {
                    currentFinishedLogFiles.add(oldRanking);
                }

                // output file
                String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS").format(new Date());
                String outFilename = folderName + "/temp/ranking_" + timestamp;
                PrintWriter fileWriter;
                try {
                    fileWriter = new PrintWriter(new FileWriter(outFilename, true));
                } catch (IOException e) {
                    Logger.log(logName, "Error opening output file: " + outFilename, Logger.logLevel.ERROR);
                    return;
                }

                // se inician los FileReaders y se lee la primera linea de cada archivo
                List<BufferedReader> fileReaders = new LinkedList<>();
                List<String> lines = new LinkedList<>();
                for (int i = 0; i < currentFinishedLogFiles.size(); ++i) {
                    String filename = currentFinishedLogFiles.get(i);
                    try {
                        fileReaders.add(new BufferedReader(new FileReader(filename)));
                        try {
                            lines.add(fileReaders.get(i).readLine());
                        } catch (IOException e) {
                            Logger.log(logName, "Error reading from file: " + filename, Logger.logLevel.ERROR);
                            fileReaders.remove(i);
                        }
                    } catch (FileNotFoundException e) {
                        Logger.log(logName, "Error opening file: " + filename, Logger.logLevel.ERROR);
                    }
                }

                LimitedSortedSet<String> mostFrequentErrors = new LimitedSortedSet<>(numErrorsToList,
                        new CountCommaNameComparator());

                while (fileReaders.size() > 0 && lines.size() > 0) {

                    if (shouldStop()) {

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
                        fileWriter.close();
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

                        return;
                    }

                    // se busca el siguiente error con mas apariciones
                    // no es un merge comun, porque hay registros de la forma "1,error1", "2,error1"
                    // en cada archivo no puede aparecer dos veces un error
                    // entonces se hace un merge acumulando las apariciones de la linea actual en cada archivo
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

                    fileWriter.println(errMsg + "," + errMsgCount);
                    mostFrequentErrors.add(errMsgCount + "," + errMsg);

                    // advance used files
                    for (int i = 0; i < smallestIndexes.size(); ++i) {
                        int smallestIndex = smallestIndexes.get(i);

                        try {
                            lines.set(smallestIndex, fileReaders.get(smallestIndex).readLine());
                        } catch (IOException e) {
                            Logger.log(logName, "Error reading from file: " + smallestIndex,
                                    Logger.logLevel.ERROR);
                            //lines.remove(smallestIndex);
                            //fileReaders.remove(smallestIndex);
                        }
                    }

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

                fileWriter.close();

                // most frequent errors output
                try {
                    File ranking = new File(folderName + "/ranking");
                    if (ranking.exists()) {
                        ranking.delete();
                    }
                    PrintWriter freqErrorsFileWriter = new PrintWriter(
                            new FileWriter(folderName + "/ranking"));
                    for (String line : mostFrequentErrors) {
                        freqErrorsFileWriter.println(line);
                    }
                    freqErrorsFileWriter.close();
                } catch (IOException e) {
                    Logger.log(logName, "Error opening output file: "
                                    + folderName + "/ranking",
                            Logger.logLevel.ERROR);
                }

            }

        } catch (InterruptedException e) {
            Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
        }
    }
}
