import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RandomForest<D> {
    
    public static <D> RandomForest<D> growRandomForest(
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
        return new RandomForest<D>(trees);
    }

    private List<DecisionTree<D>> trees;

    private RandomForest(List<DecisionTree<D>> trees) {
        this.trees = trees;
    }

}
