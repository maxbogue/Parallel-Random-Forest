import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.IntegerSchedule;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.reduction.SharedObjectArray;
import edu.rit.pj.reduction.SharedInteger;

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

}
