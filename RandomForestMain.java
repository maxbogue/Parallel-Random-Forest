import java.io.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RandomForestMain {

    public static void main(String[] args) throws Exception {

        // Parse arguments.
        int size = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        String dataFile = args[3];

        // Read samples and attrs from the file.
        Map<String,List<String>> attrs = new HashMap<String,List<String>>();
        List<Sample<String>> samples = RandomForestInput.readData(dataFile, attrs);

        // Start timing.
        long startTime = System.currentTimeMillis();

        // Grow the forest.
        RandomForest<String> forest = RandomForest
            .<String>growRandomForest(attrs, samples, size, n, m);

        // Stop timing.
        long endTime = System.currentTimeMillis();

        // Print results.
        System.out.println((endTime - startTime) + " millis");
                                                                           
    }

}
