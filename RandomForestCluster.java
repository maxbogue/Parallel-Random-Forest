import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rit.mp.IntegerBuf;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.buf.IntegerItemBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
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
     * @param file      The file that contains the dataset, samples to learn and test.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void main(String args[]) throws Exception {
        Comm.init(args);
        if (args.length < 4 || args.length > 5) usage();
        // Getting the Comm object containing the cluster.
        Comm world = Comm.world();
        // The rank of respective cluster system.
        int rank = world.rank();
        // Contains the total number of cluster systems available in Comm
        int size = world.size();
        //Random forest cluster object
        @SuppressWarnings("rawtypes")
        RandomForest clusterobject;

        // IntegerBuf to get the number of correct decision
        IntegerItemBuf counter = IntegerBuf.buffer();
        //To encapsulate the tree subset transfered to the rank 0 System.
        ObjectBuf< RandomForest< String > > gatherforest;
        //To encapsulate the randomforest transfered from rank 0 System.
        ObjectBuf< List<RandomForest< String >> > gatherrandomforest;

        // The number of trees in the random forest.
        int forestSize = Integer.parseInt(args[0]);
        // The number of samples to choose with replacement for each tree.
        int n = Integer.parseInt(args[1]);
        // The best attribute at each node in a tree will be chosen
        // from m attributes selected at random without replacement.
        int m = Integer.parseInt(args[2]);
        String dataFile = args[3];
        // The split of training vs test samples. Defaults to 75% training.
        double split;
        if (args.length > 4) {
            split = Double.parseDouble(args[4]) / 100.0;
        } else {
            split = 0.75;
        }

        //this will split the c value to hold b values form all its column process
        Range[] rowrange = new Range(0, forestSize - 1).subranges(size);
        //this will split the c value to hold b values form all its column process
        Range[] testrange = new Range(0, 99).subranges(size);
        // The total forest got from all the cluster systems.
        List<RandomForest<String>> Forest = new ArrayList<RandomForest< String >>();
        // holds the decision of each choice during learning
        String decision = null;
        //holds the list of sample used for learning.
        List< Sample< String >> samples = new ArrayList< Sample< String > >();
        //holds the decision got from all the cluster systems.
        Map<Integer,List<String>> decisionbyvote = new HashMap<Integer, List<String>>();
        //holds the decision of individual cluster system.
        List<String> results = new ArrayList<String>();
        //stores the result of test sample from each cluster
        Map<Integer,List<String>> result = new HashMap<Integer, List<String>>();
        gatherforest=ObjectBuf.buffer();
        gatherrandomforest = ObjectBuf.buffer();

        // Read samples and attrs from the file.
        Map<String,List<String>> attrs = new HashMap<String,List<String>>();
        List<Sample<String>> data = RandomForestInput.readData(dataFile, attrs);
        data = ListUtils.shuffle(data);

        // Split into training and testing data.
        int numTraining = (int)(data.size() * split);
        List<Sample<String>> trainingData = data.subList(0, numTraining);
        List<Sample<String>> testData = data.subList(numTraining, data.size());

        clusterobject = RandomForest.growRandomForest(attrs,samples, ((rowrange[rank].ub()-
                rowrange[rank].lb())+1), n, m);
        long treestart = System.currentTimeMillis();
        //gathers the small forest from all cluster systems.
        testData=testData.subList(testrange[rank].lb(), testrange[rank].ub()+1);
        if(rank!=0)
        {
            gatherforest.fill(clusterobject);
            world.send(0, gatherforest);
            System.out.println("forest subset sent from "+rank+" to process 0");
        }
        else
        {
            Forest.add(clusterobject);
            for(int i = 1;i < size; i++)
            {
                world.receive(i, gatherforest);
                Forest.add((RandomForest<String>) gatherforest.get(0));
            }
        }
        //gathers the small forest from all cluster systems.
        if(rank == 0)
        {
            for(int i=1; i< size; i++)
            {
                gatherrandomforest.fill(Forest);
                world.send(i, gatherrandomforest);
            }
        }
        else
        {
            world.receive(0, gatherrandomforest);
            Forest=gatherrandomforest.get(0);

        }
        long treeend = System.currentTimeMillis();
        long teststart = System.currentTimeMillis();
        for(int i = 0;i < Forest.size(); i++){
            if(i!=rank)
            clusterobject.trees.addAll(Forest.get(i).trees);
        }
        System.out.println("Number of trees in each system "+clusterobject.trees.size());
        int correct_results = clusterobject.test(testData);
        long testend = System.currentTimeMillis();
        //System.out.println("Decision by "+rank+" which is "+ result);
        if(rank!=0)
        {
            counter.item=correct_results;
            world.send(0, counter);
            System.out.println("Decision sent from "+rank+" to process 0");
        }
        else
        {

            for(int i = 1;i < size; i++)
            {
                world.receive(i, counter);
                correct_results += counter.item;
            }
            //System.out.println(decisionbyvote);
        }
        System.out.println("Tree construction time of rank "+rank+" "+(treeend-treestart));
        System.out.println("Test time of rank "+rank+" "+(testend-teststart));
        System.out.println("Correct Results "+rank+" "+correct_results);

    }

}
