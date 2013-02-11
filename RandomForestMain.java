import java.io.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RandomForestMain
{
    static int size;
    static int n;
    static int m;

    static String dataFile;
    public static void main(String[] args) throws Exception
    {
        size = Integer.parseInt(args[0]);
        n = Integer.parseInt(args[1]);
        m = Integer.parseInt(args[2]);

        dataFile = args[3];
        BufferedReader in = new BufferedReader(new FileReader(dataFile));

        List<Sample<String>> samples = new ArrayList<Sample<String>>();
        Map<String, List<String>> attributes =
            new HashMap<String, List<String>>();

        String line;
        String[] lineSplit;
        String decision;
        Map<String, String> currChoices;
        while((line = in.readLine()) != null)
        {
            lineSplit = line.split(",");
            decision = lineSplit[0];
            currChoices = new HashMap<String, String>();

            for(int i = 1; i < lineSplit.length; i++)
            {
                currChoices.put(String.valueOf(i), lineSplit[i]);
                samples.add(new Sample<String>(currChoices, decision)); 
                
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
        }
        

        /*RandomForest<String> forest = */

        long startTime = System.currentTimeMillis();
        RandomForest<String> forest = RandomForest
            .<String>growRandomForest(attributes, samples, size, n, m);
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime) + " millis");
                                                                           
    }
}