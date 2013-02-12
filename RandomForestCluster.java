
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A RandomForest is simply a collection of DecisionTrees.
 * These trees are grown in a certain random way, and they vote on decisions.
 */
public class RandomForestCluster<D> {
    
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

    /** The trees in this forest. */
    public List<DecisionTree<D>> trees;

    /**
     * Private constructor, for some reason.
     *
     * @param trees     The trees in this forest.
     */
    private RandomForestCluster(List<DecisionTree<D>> trees) {
        this.trees = trees;
    }
   
    /**
     * This is used to merge all the decisions of a single
     * sample from the cluster systems.
     * @param _gather
     * @returns decisionbyvote The merged Map of testsample id and its respective decisions from
     * cluster systems 
     */
    public static Map<Integer, List<String>> merge(Map<Integer, List<String>> decisionbyvote, 
            Map<Integer, List<String>> _gather, List<String> results)
    {
        for(int i = 0;i < _gather.size(); i++)
        {
            results=decisionbyvote.get(i);
            results.addAll(_gather.get(i));
            decisionbyvote.put(i, results);
            
        }
        return decisionbyvote;
    }
    
    /**
     * return the number of correct decisions of the test samples
     * @param _testsamples  The samples that are used for testing the decision tree.
     * @param _decisions    The final decisions of each sample gathered by rank zero process
     * @return counter_value Contains the number of correct matches.
     */
    public static int numberOfCorrectDecisionsCluster( List<Sample< String >> _testsamples, 
            Map< Integer, List< String >> _decisions)
    {
        int counter_value=0;
        for(int i = 0; i < _testsamples.size(); i++)
        {
            if(_testsamples.get(i).decision.equals(ListUtils.mode(_decisions.get(i))))
            {
                counter_value++;
            }
        }
        return counter_value;
        
    }
    }
