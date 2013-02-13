import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import edu.rit.mp.IntegerBuf;
import edu.rit.mp.ObjectBuf;
import edu.rit.mp.buf.IntegerItemBuf;
import edu.rit.pj.Comm;
import edu.rit.util.Range;

/**
 * This class contains the main function that reads
 * the sample from the file and generate a random forest in cluster environment.
 * @author Roshan
 *
 */
public class RandomForestClusterMain {
    static Comm world;
    static int rank, size;
    static Map<Integer, List<String>> decisionbyvote=null;
    static List<String> results;
    static Map<Integer, List<String>> result;
    static List<RandomForest< String >> Forest;
    static Range[] testrange;
    static IntegerItemBuf counter;
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
        if(args.length!=4)usage();
        Comm.init(args);
        // Getting the Comm object containing the cluster.
        world = Comm.world();
        // The rank of respective cluster system.
        rank = world.rank();
        // Contains the total number of cluster systems available in Comm
        size = world.size();
        //Random forest cluster object
        @SuppressWarnings("rawtypes")
        RandomForest clusterobject;

        // IntegerBuf to get the number of correct decision
        counter= IntegerBuf.buffer();
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
        testrange = new Range(0, 99).subranges(size);
        // The total forest got from all the cluster systems.
        Forest = new ArrayList<RandomForest< String >>();
        // holds the decision of each choice during learning
        String decision = null;
        //holds the list of sample used for learning.
        List< Sample< String >> samples = new ArrayList< Sample< String > >();
        //holds the decision got from all the cluster systems.
        decisionbyvote = new HashMap<Integer, List<String>>();
        //holds the decision of individual cluster system.
        results = new ArrayList<String>();
        //stores the result of test sample from each cluster
        result = new HashMap<Integer, List<String>>();
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

    private static void usage()
    {
        System.err.println ("Usage: java  -Dpj.np=<p> <size> <n_sample_records> <m_attributes> <data_file>");
        System.err.println ("<p> = #number of processors");
        System.err.println ("<size> = #number of trees in the forest");
        System.err.println ("<n_sample_records> = # of sample records");
        System.err.println ("<m_attributes> = # of attributes");
        System.err.println ("<File> = Input file name");
        System.exit (1);
    }

}
