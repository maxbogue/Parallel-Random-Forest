import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of utility methods for lists.
 */
public class ListUtils {

    /** Private constructor to enforce static class... */
    private ListUtils() {}

    /**
     * Find the element that occurs most in a list.
     * If there is a tie, the element that reached the maximum first is
     * returned.
     *
     * @param list  The list of elements.
     * @return      The mode of the list; null if the list is empty.
     */
    public static <T> T mode(List<T> list) {
        T currentMode = null;
        Integer max = 0;
        Map<T,Integer> counts = new HashMap<T,Integer>();
        for (T e : list) {
            Integer n = counts.get(e);
            if (n == null) {
                n = 1;
            } else {
                n += 1;
            }
            counts.put(e, n);
            if (n > max) {
                currentMode = e;
                max = n;
            }
        }
        return currentMode;
    }

}
