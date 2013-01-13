import java.util.Map;

/**
 * A wrapper for the (choices, decision) pair that constitutes a sample.
 */
public class Sample {

    public final Map<String,String> choices;
    public final String decision;

    public Sample(Map<String,String> choices, String decision) {
        this.choices = choices;
        this.decision = decision;
    }
}
