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
        return growDecisionTree(attrs, samples, 0);
    }

    public static <D> DecisionTree<D> growDecisionTree(
            Map<String,List<String>> attrs, List<Sample<D>> samples, int m)
        throws IllegalArgumentException
    {
        // Short circuit on empty list of samples.
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Need samples to grow a DecisionTree.");
        }
        // See how many unique decisions are left in the set of samples.
        Counter<D> decisions = new Counter<D>();
        for (Sample<D> sample : samples) {
            decisions.add(sample.decision);
        }
        // If only one, then decide on it.
        if (decisions.size() == 1) {
            for (D decision : decisions.keySet()) {
                return new Decision<D>(decision);
            }
        }
        // If there are no attributes left somehow, take the mode decision.
        if (attrs.isEmpty()) {
            return new Decision<D>(decisions.mode());
        }
        Map<String,List<String>> selectedAttrs;
        if (m > 0) {
            selectedAttrs = new HashMap<String,List<String>>();
            for (String attr : ListUtils.sample(attrs.keySet(), m)) {
                selectedAttrs.put(attr, attrs.get(attr));
            }
        } else {
            selectedAttrs = attrs;
        }
        String best = bestAttribute(selectedAttrs, samples);
        Map<String,DecisionTree<D>> children = new HashMap<String,DecisionTree<D>>();
        for (String v : selectedAttrs.get(best)) {
            List<Sample<D>> vSamples = ListUtils.filter(samples, new SamplePredicate<D>(best, v));
            if (vSamples.isEmpty()) {
                children.put(v, new Decision<D>(decisions.mode()));
            } else {
                Map<String,List<String>> attrsCopy = new HashMap<String,List<String>>(attrs);
                attrsCopy.remove(best);
                children.put(v, growDecisionTree(attrsCopy, vSamples));
            }
        }
        return new Tree<D>(best, children);
    }

    private static <D> String bestAttribute(Map<String,List<String>> attrs, List<Sample<D>> samples) {
        double totalH = entropy(samples);
        double maxIG = 0.0;
        String result = null;
        for (String attr : attrs.keySet()) {
            List<String> values = attrs.get(attr);
            double informationGain = totalH;
            for (String v : values) {
                List<Sample<D>> vSamples = ListUtils.filter(samples, new SamplePredicate<D>(attr, v));
                informationGain -= (double)vSamples.size() / samples.size() * entropy(vSamples);
            }
            if (informationGain > maxIG) {
                maxIG = informationGain;
                result = attr;
            }
        }
        return result;
    }

    private static <D> double entropy(List<Sample<D>> samples) {
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

    private static class SamplePredicate<D> implements Predicate<Sample<D>> {
        private String attr;
        private String value;
        public SamplePredicate(String attr, String value) {
            this.attr = attr;
            this.value = value;
        }
        public boolean test(Sample<D> sample) {
            return sample.choices.get(attr) == value;
        }
    }

    public abstract D decide(Map<String,String> choices);
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
