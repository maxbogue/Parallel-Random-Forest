import java.util.Map;

/**
 * A wrapper for the (choices, decision) pair that constitutes a sample.
 */
public class Sample<D> {

    public final Map<String,String> choices;
    public final D decision;

    /**
     * @param choices   The attr->value choices for this sample.
     * @param decision  The decision for those choices.
     */
    public Sample(Map<String,String> choices, D decision) {
        this.choices = choices;
        this.decision = decision;
    }

}
