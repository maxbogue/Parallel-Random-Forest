import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rit.mp.IntegerBuf;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.buf.IntegerItemBuf;
import edu.rit.mp.buf.ObjectItemBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.reduction.IntegerOp;
import edu.rit.pj.reduction.SharedInteger;
import edu.rit.util.Range;

/**
 * A RandomForest is simply a collection of DecisionTrees.
 * These trees are grown in a certain random way, and they vote on decisions.
 */
@SuppressWarnings("serial")
public class RandomForestCluster<D> extends RandomForestSmp<D>
    implements java.io.Serializable {

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
    public static <D> RandomForestCluster<D> growRandomForest(
            Map<String,List<String>> attrs,
            List<Sample<D>> samples,
            int size,
            int n,
            int m)
    {
        List<DecisionTree<D>> trees = new ArrayList<DecisionTree<D>>(size);
        for (int i = 0; i < size; i++) {
            List<Sample<D>> sampleChoices = ListUtils.choices(samples, n);
            trees.add(DecisionTree.growDecisionTree(attrs, sampleChoices, m));
        }
        return new RandomForestCluster<D>(trees);
    }

    /**
     * @param trees     The trees in this forest.
     */
    private RandomForestCluster(List<DecisionTree<D>> trees) {
        super(trees);
    }

    /**
     * The Main function for the cluster version
     * @param size      The number of trees in the forest.
     * @param n         The number of sample records to choose with replacement
     *                  for each tree.
     * @param m         The best attribute at each node in a tree will be chosen
     *                  from m attributes selected at random without replacement.
     * @param dataFile  The file that contains the dataset, samples to learn and test.
     * @param split     The split of training vs test samples. Defaults to 75% training.
     */
    public static void main(String args[]) throws Exception {
        Comm.init(args);
        if (args.length < 4 || args.length > 5) usage();

        // Initialize comm variables.
        Comm world = Comm.world();
        int rank = world.rank();
        int size = world.size();

        // Parse arguments.
        int forestSize = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        String dataFile = args[3];
        double split;
        if (args.length > 4) {
            split = Double.parseDouble(args[4]) / 100.0;
        } else {
            split = 0.75;
        }

        // Read samples and attrs from the file.
        Map<String,List<String>> attrs = new HashMap<String,List<String>>();
        List<Sample<String>> data = RandomForestInput.readData(dataFile, attrs);
        //data = ListUtils.shuffle(data);

        // Split into training and testing data.
        int numTraining = (int)(data.size() * split);
        List<Sample<String>> trainingData = data.subList(0, numTraining);
        List<Sample<String>> testData = data.subList(numTraining, data.size());

        // Ranges for tree construction.
        Range[] treeRanges = new Range(0, forestSize - 1).subranges(size);
        Range treeRange = treeRanges[rank];

        // Ranges for test data splitting.
        Range[] testRanges = new Range(0, testData.size() - 1).subranges(size);
        Range testRange = testRanges[rank];

        // Sub-list of test data that this processor will test with.
        List<Sample<String>> testDataSlice = testData.subList(
                testRange.lb(), testRange.ub() + 1);

        // Start timing.
        long t1 = System.currentTimeMillis();

        // Grow the forest for this processor.
        RandomForestCluster<String> forest = RandomForestCluster.<String>growRandomForest(
                attrs, trainingData, treeRange.length(), n, m);

        // Make an array to hold and gather all the trees.
        DecisionTree<String>[] trees = (DecisionTree<String>[])new DecisionTree[forestSize];
        // Populate this processor's section of it.
        int treeIndex = treeRange.lb();
        for (DecisionTree<String> tree : forest.trees) {
            trees[treeIndex] = tree;
            treeIndex++;
        }

        // Buffers for the tree data.
        ObjectBuf[] treeBufs = ObjectBuf.sliceBuffers(trees, treeRanges);
        ObjectBuf treeBuf = treeBufs[rank];

        // Gather the trees from each processor.
        world.gather(0, treeBuf, treeBufs);

        // Stop timing training, start timing testing.
        long t2 = System.currentTimeMillis();

        // Send the completed RandomForest to each processor.
        world.broadcast(0, ObjectBuf.buffer(trees));

        // Convert the tree array into a list and make the RandomForest.
        forest = new RandomForestCluster<String>(Arrays.asList(trees));

        // Test the forest.
        int correct = forest.test(testDataSlice);
        IntegerItemBuf correctBuf = IntegerBuf.buffer(correct);
        world.reduce(0, correctBuf, IntegerOp.SUM);
        correct = correctBuf.item;

        // Stop timing.
        long t3 = System.currentTimeMillis();

        // Print results.
        if (rank == 0) {
            System.out.println(trainingData.size() + " samples used to train the forest.");
            System.out.println(testData.size() + " samples used to test the forest.");
            double percent = 100.0 * correct / testData.size();
            System.out.printf("%.2f%% (%d/%d) tests passed.\n",
                    percent, correct, testData.size());
            System.out.println("Forest construction time: " + (t2 - t1) + " ms");
            System.out.println("Forest testing time: " + (t3 - t2) + " ms");
        }

    }

}
