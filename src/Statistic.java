import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Statistic {

    //  ---------------------- CLASS VARIABLES ----------------------

    public String name;
    private Map<String, Integer> statisticValues = new HashMap<>();
    private Object statisticLock = new Object();

    public Statistic(String name) {
        this.name = name;
    }


    //  ---------------------- CLASS METHODS ----------------------

    public void updateStatistic(String key) {

        synchronized (statisticLock) {
            statisticValues.merge(key, 1, Integer::sum);
        }

    }

    public Map<String, Integer> getStatistic() {

        synchronized (statisticLock) {
            Map<String, Integer> temp = new HashMap<>(statisticValues);
            statisticValues.clear();
            return temp;
        }

    }
}
