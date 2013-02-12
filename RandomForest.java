import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A RandomForest is simply a collection of DecisionTrees.
 * These trees are grown in a certain random way, and they vote on decisions.
 */
public class RandomForest<D> {
    
    /**
     * Grows a random forest from a list of samples.
     *
     * @param attrs     The attributes and their possible values.
     * @param samples   The sample data to train on.
     * @param size      The number of trees in the forest.
     * @param n         The number of sample records to choose with replacement
     *                  for each tree.
     * @param m         The best attribute at each node in a tree will be chosen
     *                  from m attributes selected at random without replacement.
     * @return          A new RandomForest.
     */
    public static <D> RandomForest<D> growRandomForest(
            Map<String,List<String>> attrs,
            List<Sample<D>> samples,
            int size,
            int n,
            int m) throws Exception
    {
        List<DecisionTree<D>> trees = new ArrayList<DecisionTree<D>>(size);
        for (int i = 0; i < size; i++) {
            List<Sample<D>> sampleChoices = ListUtils.choices(samples, n);
            trees.add(DecisionTree.growDecisionTree(attrs, sampleChoices, m));
        }
        return new RandomForest<D>(trees);
    }

    /** The trees in this forest. */
    protected List<DecisionTree<D>> trees;

    /**
     * @param trees     The trees in this forest.
     */
    protected RandomForest(List<DecisionTree<D>> trees) {
        this.trees = trees;
    }

    /** 
     * Gets the mode decision of the trees in this forest on the sample.
     *
     * @param sample    The sample whose attributes will be used to get
     *                  a decision from the forest.
     * @return          The decision of this forest on the sample.
     */
    public D decide(Sample<D> sample) {
        Counter<D> decisions = new Counter<D>();
        for (DecisionTree<D> tree : trees) {
            decisions.add(tree.decide(sample.choices));
        }
        return decisions.mode();
    }

    /**
     * Run a list of samples against this forest.
     *
     * @param samples   The test samples to evaluate the forest with.
     * @return          The number of correct decisions by this forest.
     */
    public int test(List<Sample<D>> samples) throws Exception {
        int correct = 0;
        for (Sample<D> sample : samples) {
            if (sample.decision.equals(decide(sample))) {
                correct++;
            }
        }
        return correct;
    }

    /**
     * Parse input data, and time the construction of a random forest.
     */
    public static void main(String[] args) throws Exception {

        // Parse arguments.
        int size = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        String dataFile = args[3];

        // Read samples and attrs from the file.
        Map<String,List<String>> attrs = new HashMap<String,List<String>>();
        List<Sample<String>> data = RandomForestInput.readData(dataFile, attrs);

        // Split into training and testing data.
        int numTraining = (int)(data.size() * 0.75);
        List<Sample<String>> trainingData = data.subList(0, numTraining);
        List<Sample<String>> testData = data.subList(numTraining, data.size());

        // Start timing.
        long t1 = System.currentTimeMillis();

        // Grow the forest.
        RandomForest<String> forest = RandomForest
            .<String>growRandomForest(attrs, trainingData, size, n, m);

        // Stop timing.
        long t2 = System.currentTimeMillis();

        // Print results.
        System.out.println("Forest construction took " + (t2 - t1) + " ms");

        // Start timing.
        long t3 = System.currentTimeMillis();

        // Test the forest.
        int correct = forest.test(testData);

        // Stop timing.
        long t4 = System.currentTimeMillis();

        // Print results.
        double percent = 100.0 * correct / testData.size();
        System.out.printf("%.2f%% (%d/%d) tests passed.\n",
                percent, correct, testData.size());
        System.out.println("Forest testing took " + (t4 - t3) + " ms");

    }

}
