import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to read in data.
 */
public class RandomForestInput {

    /**
     * Utility function to read a data file.
     *
     * @param dataFile  The file to read data from.
     * @param attrs     An empty map to put the attribute data into.
     * @return A list of Sample<String> objects.
     */
    public static List<Sample<String>> readData(
            String dataFile,
            Map<String,List<String>> attrs)
        throws Exception
    {

        // File reader.
        BufferedReader in = new BufferedReader(new FileReader(dataFile));

        // List to hold all the samples.
        List<Sample<String>> samples = new LinkedList<Sample<String>>();

        // Read the file.
        String line;
        while ((line = in.readLine()) != null) {

            // Split the line and initialize the sample data structures.
            String[] lineSplit = line.split(",");
            String decision = lineSplit[0];
            Map<String,String> choices = new HashMap<String,String>();

            // Process the attrs.
            for (int i = 1; i < lineSplit.length; i++) {

                // We'll use the string of i as our attribute name.
                String attr = String.valueOf(i);
                String value = lineSplit[i];
                choices.put(attr, value);

                // Make sure this attr/value pair is in the attrs map.
                if (!attrs.containsKey(attr)) {
                    attrs.put(attr, new ArrayList<String>());
                }
                if (!attrs.get(attr).contains(value)) {
                    attrs.get(attr).add(value);
                }

            }

            // Add our data to the list of samples.
            samples.add(new Sample<String>(choices, decision));

        }

        // Return the list of samples.
        return samples;

    }

}
