import java.io.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import edu.rit.pj.Comm;

import edu.rit.util.Random;

public class RandomForestSmpMain
{
    static int size;
    static int numSamples;
    static int n;
    static int m;
    static int seed;

    static String dataFile;

    static Random rand;

    public static void main(String[] args) throws Exception
    {
        Comm.init(args);
        size = Integer.parseInt(args[0]);
        numSamples = Integer.parseInt(args[1]);
        n = Integer.parseInt(args[2]);
        m = Integer.parseInt(args[3]);

        dataFile = args[4];
        seed = Integer.parseInt(args[5]);

        rand = Random.getInstance(seed);

        BufferedReader in = new BufferedReader(new FileReader(dataFile));


        //list of samples from the file
        List<Sample<String>> samples = new ArrayList<Sample<String>>();
        //list of samples to test the tree
        List<Sample<String>> tests = new ArrayList<Sample<String>>();
        //attributes per sample, gets reset through each loop
        Map<String, List<String>> attributes =
            new HashMap<String, List<String>>();

        //line read
        String line;
        //line split by commas
        String[] lineSplit;
        //decision of each line
        String decision;
        Map<String, String> currChoices;

        //parsing file
        int currLine = 0;
        while((line = in.readLine()) != null)
        {
            lineSplit = line.split(",");
            decision = lineSplit[0];
            currChoices = new HashMap<String, String>();

            for(int i = 1; i < lineSplit.length; i++)
            {
                currChoices.put(String.valueOf(i), lineSplit[i]);
                
                if(attributes.get(String.valueOf(i)) == null)
                {
                    attributes.put(String.valueOf(i), new ArrayList<String>());
                    attributes.get(String.valueOf(i)).add(lineSplit[i]);
                }
                else
                {
                    if(! attributes.get(String.valueOf(i))
                       .contains(lineSplit[i]))
                    {
                        attributes.get(String.valueOf(i)).add(lineSplit[i]);
                    }
                }
            }

            //add every sample to tests, will remove random samples later for 
            //generating the tree
            tests.add(new Sample<String>(currChoices, decision));

            currLine++;
        }
        
        for(int i = 0; i < numSamples; i++)
        {
            samples.add(tests.remove(rand.nextInt(tests.size())));
        }

        //calculating time for forest building
        long startCreateTime = System.currentTimeMillis();
        RandomForestSmp<String> forest = RandomForestSmp
            .<String>growRandomForest(attributes, samples, size, n, m);
        long endCreateTime = System.currentTimeMillis();
        

        //calcluating time to evaluate forest
        long startDecisionTime = System.currentTimeMillis();
        
        int correct = forest.numberOfCorrectDecisions(tests);

        long endDecisionTime = System.currentTimeMillis();


        //print stats
        System.out.println(samples.size() + " samples used to create tree");
        System.out.println(tests.size() + " tests used to test tree");

        System.out.println((endCreateTime - startCreateTime) 
                           + " millis to create tree"); 
        System.out.println((endDecisionTime - startDecisionTime) 
                           + " millis to test decisions");
        System.out.println(correct + " tested correctly");
        System.out.println((tests.size() - correct) + " tested incorrectly");
    }
}