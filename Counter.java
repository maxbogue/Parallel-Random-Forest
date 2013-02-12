import java.util.*;

@SuppressWarnings("serial")
class Counter<K> extends HashMap<K,Integer> {

    public Integer get(Object key) {
        Integer count = super.get(key);
        return count == null ? 0 : count;
    }

    public Integer add(K key) {
        Integer count = get(key);
        if (count == null) {
            count = 0;
        }
        put(key, count + 1);
        return count + 1;
    }

    public void addAll(Iterable<? extends K> items) {
        for (K k : items) {
            add(k);
        }
    }

    public K mode() {
        K mode = null;
        Integer maxCount = 0;
        for (K k : keySet()) {
            Integer count = get(k);
            if (count > maxCount) {
                mode = k;
                maxCount = count;
            }
        }
        return mode;
    }

}
