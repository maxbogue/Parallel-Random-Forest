import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DecisionTree<D> {
    public static <D> DecisionTree<D> growDecisionTree(
            Map<String,List<String>> attrs, List<Sample<D>> samples)
        throws IllegalArgumentException
    {
        // Short circuit on empty list of samples.
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Need samples to grow a DecisionTree.");
        }
        // See how many unique decisions are left in the set of samples.
        Set<D> decisions = new HashSet<D>();
        for (Sample<D> sample : samples) {
            decisions.add(sample.decision);
        }
        // If only one, then decide on it.
        if (decisions.size() == 1) {
            for (D decision : decisions) {
                return new Decision<D>(decision);
            }
        }
        // Otherwise we have more than one; find the best to split on.

        return null;
    }
    public abstract D decide(Map<String,String> choices);

    private double entropy(List<Sample<D>> samples) {
        double total = (double)samples.size();
        // We need the breakdown of decision occurences; use a counter.
        Counter<D> counter = new Counter<D>();
        for (Sample<D> sample : samples) {
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

class Decision<D> extends DecisionTree<D> {
    public D decision;
    public Decision(D decision) {
        this.decision = decision;
    }
    public D decide(Map<String,String> choices) {
        return decision;
    }
}

class Tree<D> extends DecisionTree<D> {
    public String attr;
    public Map<String,DecisionTree<D>> children;
    public Tree(String attr, Map<String,DecisionTree<D>> children) {
        this.attr = attr;
        this.children = children;
    }
    public D decide(Map<String,String> choices) {
        String value = choices.get(attr);
        return children.get(value).decide(choices);
    }
}
