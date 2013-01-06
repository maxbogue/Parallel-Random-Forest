import java.util.Map;

public abstract class DecisionTree {
    public abstract String decide(Map<String,String> choices);
}

class Decision extends DecisionTree {
    public String value;
    public String decide(Map<String,String> choices) {
        return value;
    }
}

class Tree extends DecisionTree {
    public String attr;
    public Map<String,DecisionTree> children;
    public String decide(Map<String,String> choices) {
        String value = choices.get(attr);
        return children.get(value).decide(choices);
    }
}
