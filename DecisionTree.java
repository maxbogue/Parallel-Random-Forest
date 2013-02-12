import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Represents a decision tree with a decision of type D.
 *
 * Can either be a Decision or a Tree.
 */
@SuppressWarnings("serial")
public abstract class DecisionTree<D> implements java.io.Serializable {

    /**
     * Grow a normal decision tree.
     *
     * @param attrs     The attributes to use in this tree.
     * @param samples   The sample data to train from; non-empty.
     * @return          A decision tree.
     * @throws IllegalArgumentException If the list of samples is empty.
     */
    public static <D> DecisionTree<D> growDecisionTree(
            Map<String,List<String>> attrs, List<Sample<D>> samples)
        throws IllegalArgumentException
    {
        return growDecisionTree(attrs, samples, 0);
    }

    /**
     * Grow a normal or random forest decicion tree.
     *
     * @param attrs     The attributes to use in this tree.
     * @param samples   The sample data to train from; non-empty.
     * @param m         The number of attributes to choose from at each node.
     * @return          A decision tree.
     * @throws IllegalArgumentException If the list of samples is empty.
     */
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
        // Determine the attributes to select the best from.
        Map<String,List<String>> selectedAttrs;
        // If this is a random forest decision tree...
        if (m > 0) {
            // choose m attributes at random.
            selectedAttrs = new HashMap<String,List<String>>();
            for (String attr : ListUtils.sample(attrs.keySet(), m)) {
                selectedAttrs.put(attr, attrs.get(attr));
            }
        } else {
            // otherwise just use all the attrs.
            selectedAttrs = attrs;
        }
        // Find the best attribute from the selected ones.
        String best = bestAttribute(selectedAttrs, samples);
        // Construct the children map for this Tree.
        Map<String,DecisionTree<D>> children = new HashMap<String,DecisionTree<D>>();
        // For each value of the selected "best" attribute
        for (String v : selectedAttrs.get(best)) {
            // Pick out the samples with a matching value for that attribute.
        	
      List<Sample<D>> vSamples = ListUtils.filter(samples, new SamplePredicate<D>(best, v));
            // If there aren't any..,
            if (vSamples.isEmpty()) {
                // we have to make due with the mode of the samples we had.
                children.put(v, new Decision<D>(decisions.mode()));
            } else {
                // otherwise, make an attrs map without the best attr...
                Map<String,List<String>> attrsCopy = new HashMap<String,List<String>>(attrs);
                attrsCopy.remove(best);
                // and recursively call this function.
                children.put(v, growDecisionTree(attrsCopy, vSamples));
            }
        }
        // Return a Tree object that splits on our "best" attribute.
        return new Tree<D>(best, children);
    }

    /**
     * Select the best attribute from a set attribute for the given set of samples.
     * Best is defined as the one with the most information gain.
     *
     * @param attrs     A map of the attributes (and their values) to choose from.
     * @param samples   The samples to find the information gain on.
     * @return          An attribute whose values best divide the data up by decision.
     */
    private static <D> String bestAttribute(Map<String,List<String>> attrs, List<Sample<D>> samples) {
        // The total entropy of the samples.
        double totalH = entropy(samples);
        // The highest information gain found so far.
        double maxIG = 0.0;
        // The attr with the highest information gain so far.
        String result = null;
        for (String attr : attrs.keySet()) {
            List<String> values = attrs.get(attr);
            double informationGain = totalH;
            // For each value of this attribute...
            for (String v : values) {
                // find the samples that match it...
                List<Sample<D>> vSamples = ListUtils.filter(samples, new SamplePredicate<D>(attr, v));
                // and subtract IG equal to the entropy of vSamples multiplied by
                // the percentage of total samples it represents.
                informationGain -= (double)vSamples.size() / samples.size() * entropy(vSamples);
            }
            if (informationGain > maxIG) {
                maxIG = informationGain;
                result = attr;
            }
        }
        return result;
    }

    /**
     * Calculates the entropy of a list of samples.
     *
     * @param samples   The sample list to calculate the entropy of.
     * @return          The entropy of the sample list.
     */
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

    /**
     * A predicate to be used with ListUtils.filter() that matches only samples
     * with a certain value for a specified attribute.
     */
    private static class SamplePredicate<D> implements Predicate<Sample<D>> {

        private String attr;
        private String value;

        /**
         * @param attr  The attribute to match.
         * @param value The value of attr to match.
         */
        public SamplePredicate(String attr, String value) {
            this.attr = attr;
            this.value = value;
        }

        /**
         * Implement the Predicate interface.
         *
         * @param sample    A sample to test.
         * @return          Whether the sample has value for attr.
         */
       

		public String getAttr() {
			// TODO Auto-generated method stub
			return this.attr;
		}

		public String getVal() {
			// TODO Auto-generated method stub
			return this.value;
		}

		public boolean test(Sample<D> sample) {
			return sample.choices.get(getAttr()).equals(getVal());
		}
		}

    /**
     * The key function for subclasses to implement to be decision trees.
     *
     * @param choices   The choices (values) for each attribute.
     * @return          The decision for the given choices.
     */
    public abstract D decide(Map<String,String> choices);

}

/**
 * Represents a decision (leaf) node of a DecisionTree.
 */
@SuppressWarnings("serial")
class Decision<D> extends DecisionTree<D> {

    public D decision;

    /**
     * @param decision  The decision this node represents.
     */
    public Decision(D decision) {
        this.decision = decision;
    }

    /**
     * Simply return the decision of this node.
     *
     * @param choices   Ignored.
     */
    public D decide(Map<String,String> choices) {
        return decision;
    }

}

/**
 * Represents a non-decision node of a decision tree.
 * Has an attribute, with a child subtree for each of the attribute's values.
 */
@SuppressWarnings("serial")
class Tree<D> extends DecisionTree<D> {

    public String attr;
    public Map<String,DecisionTree<D>> children;

    /**
     * @param attr      The attribute that this node splits on.
     * @param children  The subtrees for each of the values for attr.
     */
    public Tree(String attr, Map<String,DecisionTree<D>> children) {
        this.attr = attr;
        this.children = children;
    }

    /**
     * Recurse on the subtree for the value of attr in choices.
     *
     * @param choices   The values for each attribute.
     * @return          A decision.
     */
    public D decide(Map<String,String> choices) {
        String value = choices.get(attr);
        return children.get(value).decide(choices);
    }

}
