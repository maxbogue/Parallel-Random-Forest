import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DecisionTree {
    public abstract String decide(Map<String,String> choices);

    private double entropy(List<Sample> samples) {
        double total = (double)samples.size();
        // We need the breakdown of decision occurences; use a counter.
        Counter<String> counter = new Counter<String>();
        for (Sample sample : samples) {
            counter.add(sample.decision);
        }
        double entropy_total = 0.0;
        for (Integer count : counter.values()) {
            // The portion of samples with the corresponding decision.
            double portion = count / total;
            // Calculate the entropy and add it to the total.
            entropy_total += -portion * Math.log(portion) / Math.log(2);
        }
        return entropy_total;
    }

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
