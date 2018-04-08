import java.util.List;

public class Config {

    public int logLevel;
    public int parserNumThreads;
    public int maxTaskPoolSize;

    public List<ConfigNameRegex> config_statistics;
    public ConfigStatisticsViewer config_statistics_viewer;
    public List<ConfigNameRegex> config_loggers;
    public ConfigRankings config_rankings;
}
