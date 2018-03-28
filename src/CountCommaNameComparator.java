import java.util.Comparator;


// es un comparador para string de la forma "numero,string" por ejemplo "3,hola"
public class CountCommaNameComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {

        int o1_int = Integer.parseInt(o1.split(",")[0]);
        String o1_str = o1.split(",")[1];
        int o2_int = Integer.parseInt(o2.split(",")[0]);
        String o2_str = o2.split(",")[1];

        if (o2_int < o1_int) {
            return -1;
        }
        if (o2_int > o1_int) {
            return 1;
        }

        return o2.compareTo(o1);
    }
}