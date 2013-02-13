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
        // # of trees in the random forest.
        int numoftrees=Integer.parseInt(args[0]);
        //The number of sample records to choose with replacement for each tree
        int n= Integer.parseInt(args[1]);
        //The best attribute at each node in a tree will be chosen
        //from m attributes selected at random without replacement.
        int m= Integer.parseInt(args[2]);
        // Random 
        Scanner scan;
        //this will split the c value to hold b values form all its column process
        Range[] rowrange=new Range(0,numoftrees-1).subranges(size);
        //this will split the c value to hold b values form all its column process
        testrange = new Range(0, 99).subranges(size);
        //To hold the attribute -> value mapping of each sample needed for learning
        Map< String,String > choices = new HashMap< String,String >();
        // The total forest got from all the cluster systems.
        Forest = new ArrayList<RandomForest< String >>();
        //To hold the set of attributes and its values. 
        Map< String,List< String > > attrs = new HashMap< String,List< String > >();
        // holds the decision of each choice during learning
        String decision = null;
        //holds the list of sample used for learning.
        List< Sample< String >> samples = new ArrayList< Sample< String > >();
        // File to read the samples from.
        File sample = new File(args[3]);
        // a temporary list to store all the attribute values.
        List<String> type = new ArrayList<String>();
        //holds the decision got from all the cluster systems.
        decisionbyvote = new HashMap<Integer, List<String>>();
        //holds the decision of individual cluster system.
        results = new ArrayList<String>();
        //Stores the samples to test the decision.
        List<Sample<String>> testsamples = new ArrayList<Sample<String>>();
        //stores the result of test sample from each cluster
        result = new HashMap<Integer, List<String>>();
        gatherforest=ObjectBuf.buffer();
        gatherrandomforest = ObjectBuf.buffer();
        int testcount=0;
        try {
            scan = new Scanner(sample);
            int number_of_attributes = scan.nextInt();
            String[] attributes = new String[number_of_attributes];
            // Gets the attributes from the file.
            for(int i = 0;i < number_of_attributes; i++)
            {
                attributes[i] = scan.next();
            }

            // Gets the different values present for each attributes.
            for(int i = 0;i < number_of_attributes; i++) 
            {
                int num = scan.nextInt();
                for(int j = 0;j < num; j++) 
                {
                    type.add(scan.next());
                }
                attrs.put(attributes[i], type);
                type = new ArrayList<String>();
            }
            // Reads the choices and the decisions 
            while(scan.hasNext()) 
            {
                testcount++;
                decision = scan.next();
                // Reads the attribute -> value choices.
                for(int i = 0;i < number_of_attributes; i++) 
                {
                    choices.put(attributes[i], scan.next());
                }
                // Each choice along with its respective decision is added to the samples list.
                if(testcount > 100){
                    samples.add(new Sample< String >(choices, decision));
                }
                else{
                    testsamples.add(new Sample< String >(choices, decision));
                } 
                choices = new HashMap< String, String >();
            }
        } catch (FileNotFoundException e) 
        {
            System.err.print("File Not found");
            System.exit(1);
        }

        clusterobject = RandomForest.growRandomForest(attrs,samples, ((rowrange[rank].ub()-
                rowrange[rank].lb())+1), n, m);
        long treestart = System.currentTimeMillis();
        //gathers the small forest from all cluster systems.
        testsamples=testsamples.subList(testrange[rank].lb(), testrange[rank].ub()+1);
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
        testcount = 0;
        long treeend = System.currentTimeMillis();
        long teststart = System.currentTimeMillis();
        for(int i = 0;i < Forest.size(); i++){
            if(i!=rank)
            clusterobject.getTrees().addAll(Forest.get(i).getTrees());
        }
        System.out.println("Number of trees in each system "+clusterobject.getTrees().size());
        int correct_results = clusterobject.numberOfCorrectDecisions(testsamples);
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
        System.err.println ("Usage: java  -Dpj.np=<p> <size> <n_sample_records> <m_attributes> <File>");
        System.err.println ("<p> = #number of processors");
        System.err.println ("<size> = #number of trees in the forest");
        System.err.println ("<n_sample_records> = # of sample records");
        System.err.println ("<m_attributes> = # of attributes");
        System.err.println ("<File> = Input file name");
        System.exit (1);
    }

}
