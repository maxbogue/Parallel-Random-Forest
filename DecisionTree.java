import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DecisionTree {
    public static DecisionTree growDecisionTree(
            Map<String,List<String>> attrs, List<Sample> samples)
        throws IllegalArgumentException
    {
        // Short circuit on empty list of samples.
        if (samples.isEmpty()) {
            raise IllegalArgumentException("Need samples to grow a DecisionTree.");
        }
        // See how many unique decisions are left in the set of samples.
        Set<String> decisions = new HashSet<String>();
        for (Sample sample : samples) {
            decisions.add(sample.decision);
        }
        // If only one, then decide on it.
        if (decisions.size() == 1) {
            return Decision(decisions.toArray()[0]);
        }
        // Otherwise we have more than one; find the best to split on.
        
    }
    public abstract String decide(Map<String,String> choices);

    private static double entropy(List<Sample> samples) {
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
    public Decision(String value) {
        this.value = value;
    }
    public String decide(Map<String,String> choices) {
        return value;
    }
}

class Tree extends DecisionTree {
    public String attr;
    public Map<String,DecisionTree> children;
    public Tree(String attr, Map<String,DecisionTree> children) {
        this.attr = attr;
        this.children = children;
    }
    public String decide(Map<String,String> choices) {
        String value = choices.get(attr);
        return children.get(value).decide(choices);
    }
}
