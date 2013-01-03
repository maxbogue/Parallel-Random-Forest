import java.util.*;

public abstract class DecisionTree {
    public abstract String decide(Map<String,String> choices);
}

class Decision extends DecisionTree {
    public String value;
    public String decide(Map<String,String> choices) {
        return value;
    }
}

class Tree extends DecisionTree {
    public String attr;
    public Map<String,DecisionTree> children;
    public String decide(Map<String,String> choices) {
        String value = choices.get(attr);
        return children.get(value).decide(choices);
    }
}

class ListUtils {
    private ListUtils() {}
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
