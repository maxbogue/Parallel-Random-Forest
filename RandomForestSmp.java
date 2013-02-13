import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rit.pj.Comm;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.IntegerSchedule;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.reduction.SharedObjectArray;
import edu.rit.pj.reduction.SharedInteger;
import edu.rit.util.Random;

/**
 * A RandomForest is simply a collection of DecisionTrees.
 * These trees are grown in a certain random way, and they vote on decisions.
 * This class grows and runs the trees in multiple threads.
 */
public class RandomForestSmp<D> extends RandomForest<D> {
    
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
    public static <D> RandomForestSmp<D> growRandomForest(
            final Map<String,List<String>> attrs,
            final List<Sample<D>> samples,
            final int size,
            final int n,
            final int m) throws Exception
    {
        // The trees for this random forest.
        final List<DecisionTree<D>> trees = new ArrayList<DecisionTree<D>>(size);
        for (int i = 0; i < size; i++) {
            trees.add(null);
        }

        // Construct the trees in parallel.
        new ParallelTeam().execute (new ParallelRegion() {
            public void run() throws Exception {
                execute(0, size - 1, new IntegerForLoop() {
                    public void run(int first, int last) {
                        for (int i = first; i <= last; i++) {
                            // Get the subset of samples to train this tree on.
                            List<Sample<D>> sampleChoices = ListUtils.choices(samples, n);
                            // Train and save a DecisionTree.
                            trees.set(i, DecisionTree.growDecisionTree(
                                    attrs, sampleChoices, m));
                        }
                    }
                });
            }
        });

        // Construct and return the actual RandomForest object.
        return new RandomForestSmp<D>(trees);
    }

    /**
     * @param trees     The trees in this forest.
     */
    protected RandomForestSmp(List<DecisionTree<D>> trees) {
        super(trees);
    }

    /** 
     * Run a list of samples against this forest.
     *
     * @param samples   The test samples to evaluate the forest with.
     * @return          The number of correct decisions by this forest.
     */
    public int test(final List<Sample<D>> samples) throws Exception {

        // Aggregate variable for the number of correct cases.
        final SharedInteger correct = new SharedInteger(0);

        // Test the samples in parallel and count the number that were correct.
        new ParallelTeam().execute(new ParallelRegion() {
            public void run() throws Exception {
                execute(0, samples.size() - 1, new IntegerForLoop() {

                    int tCorrect = 0;

                    public void run(int first, int last) {
                        for (Sample<D> sample : samples.subList(first, last + 1)) {
                            if (sample.decision.equals(decide(sample))) {
                                tCorrect++;
                            }
                        }
                    }

                    public void finish() {
                        correct.addAndGet(tCorrect);
                    }

                });
            }
        });

        // Return the number of correctly decided samples.
        return correct.get();
    }

    /**
     * Parse input data, and time the construction of a random forest.
     */
    public static void main(String[] args) throws Exception {

        // Parse arguments.
        Comm.init(args);
        int size = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        String dataFile = args[3];
        long seed = Long.parseLong(args[4]);
        double split;
        if (args.length > 5) {
            split = Double.parseDouble(args[5]) / 100.0;
        } else {
            split = 0.75;
        }

        Random rand = Random.getInstance(seed);

        // Read samples and attrs from the file.
        Map<String,List<String>> attrs = new HashMap<String,List<String>>();
        List<Sample<String>> data = RandomForestInput.readData(dataFile, attrs);
        data = ListUtils.shuffle(data);

        // Split into training and testing data.
        int numTraining = (int)(data.size() * split);
        List<Sample<String>> trainingData = data.subList(0, numTraining);
        List<Sample<String>> testData = data.subList(numTraining, data.size());

        // Start timing.
        long t1 = System.currentTimeMillis();

        // Grow the forest.
        RandomForestSmp<String> forest = RandomForestSmp
            .<String>growRandomForest(attrs, trainingData, size, n, m);

        // Stop timing training, start timing testing.
        long t2 = System.currentTimeMillis();

        // Test the forest.
        int correct = forest.test(testData);

        // Stop timing.
        long t3 = System.currentTimeMillis();

        // Print results.
        System.out.println(trainingData.size() + " samples used to train the forest.");
        System.out.println(testData.size() + " samples used to test the forest.");
        double percent = 100.0 * correct / testData.size();
        System.out.printf("%.2f%% (%d/%d) tests passed.\n",
                percent, correct, testData.size());
        System.out.println("Forest construction time: " + (t2 - t1) + " ms");
        System.out.println("Forest testing time: " + (t3 - t2) + " ms");

    }

}
