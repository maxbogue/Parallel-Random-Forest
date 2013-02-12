import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of utility methods for lists.
 */
class ListUtils {

    /** Private constructor to enforce static class... */
    private ListUtils() {}

    public static <T> List<T> filter(List<T> ls, Predicate<T> p) {
    	List<T> result = new ArrayList<T>();
    	
        for (T e : ls) {
        	
            if (p.test(e)) {
            	
                result.add(e);
            }
        }
        
        return result;
    }

    /**
     * Converts a list of one type into a list of another type.
     *
     * @param xs    The list to convert.
     * @param t     How to transform each element.
     * @return      A new list of type Y.
     */
    public static <X,Y> List<Y> map(List<X> xs, Transform<X,Y> t) {
        List<Y> ys = new ArrayList<Y>();
        for (X x : xs) {
            ys.add(t.transform(x));
        }
        return ys;
    }

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

    /**
     * Pick a random element of a list.
     */
    public static <T> T random(List<T> list) {
        int i = (int)(Math.random() * list.size());
        return list.get(i);
    }

    /**
     * Sample n values from a list WITH replacement.
     */
    public static <T> List<T> choices(List<T> source, int n) {
        List<T> result = new ArrayList<T>(n);
        for (int i = 0; i < n; i++) {
            result.add(random(source));
        }
        return result;
    }

    /**
     * Sample m values from a list WITHOUT replacement.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> sample(Collection<T> source, int m) {
        int n = source.size();
        if (n <= m) {
            return new ArrayList<T>(source);
        }
        List<T> result = new ArrayList<T>(m);
        T[] a = (T[])source.toArray();
        for (int i = 0; i < m; i++) {
            int r = i + (int)(Math.random() * (n - i));
            result.add(a[r]);
            a[r] = a[i];
        }
        return result;
    }

}

/** A predicate to use with filtering. */
interface Predicate<T> { public boolean test(T e); 
public String getAttr();
public String getVal();
}

/** A transform for use in mapping a list. */
interface Transform<X,Y> { public Y transform(X x); }
