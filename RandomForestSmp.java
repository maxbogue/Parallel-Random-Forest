import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.rit.pj.IntegerForLoop;
import edu.rit.pj.IntegerSchedule;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelTeam;
import edu.rit.pj.reduction.SharedObjectArray;
import edu.rit.pj.reduction.SharedInteger;

/**
 * A RandomForest is simply a collection of DecisionTrees.
 * These trees are grown in a certain random way, and they vote on decisions.
 */
public class RandomForestSmp<D> {
    
    /**
     * Grows a random forest from a list of samples.
     *
     * @param attrs     The attributes and their possible values.
     * @param samples   The sample data to train on.
     * @param size      The number of trees in the forest.
     * @param n         The number of sample records to choose with replacement
     *                  for each tree.
     * @param m         The best attribute at each node in a tree will be chosen
     *                  from m attributes selected at random without replacement.
     * @return          A new RandomForest.
     */
    public static <D> RandomForestSmp<D> growRandomForest(
            Map<String,List<String>> attrs,
            List<Sample<D>> samples,
            int size,
            int n,
            int m) throws Exception
    {
        final Map<String, List<String>> currAttrs = attrs;
        final List<Sample<D>> currSamples = samples;
        final int currSize = size;
        final int currN = n;
        final int currM = m;

        final SharedObjectArray<DecisionTree<D>> trees
            = new SharedObjectArray<DecisionTree<D>>(size);
        
        new ParallelTeam().execute (new ParallelRegion()
        {
            public void run() throws Exception
            {
                execute(0, currSize - 1, new IntegerForLoop()
                {
                    ArrayList<DecisionTree<D>> decisions;// = new LinkedList<DecisionTree<D>>();
                    int first;
                    public IntegerSchedule schedule()
                    {
                        return IntegerSchedule.runtime();
                    }

                    public void run(int first, int last)
                    {
                        decisions = new ArrayList<DecisionTree<D>>(last - first);

                        this.first = first;
                        for(int i = first; i <= last; i++)
                        {
                            List<Sample<D>> sampleChoices =
                                ListUtils.choices(currSamples, currN);
                            decisions.add(DecisionTree.growDecisionTree
                                          (currAttrs, sampleChoices, currM));
                        }
                        
                    }

                    public void finish()
                    {
                        int i = first;
                        if(decisions != null)
                        {
                            for(DecisionTree<D> decision : decisions)
                            {
                                trees.set(i, decision);
                                i++;
                            }
                        }
                        //reduce trees
                    }
                });
            }
        });
        
        System.out.println(trees.length());

        List<DecisionTree<D>> treesList = new ArrayList<DecisionTree<D>>(size);

        for(int i = 0; i < trees.length() - 1; i++)
        {
            treesList.add(trees.get(i));
        }
        return new RandomForestSmp<D>(treesList);
    }

    /** The trees in this forest. */
    private List<DecisionTree<D>> trees;

    /**
     * Private constructor, for some reason.
     *
     * @param trees     The trees in this forest.
     */
    private RandomForestSmp(List<DecisionTree<D>> trees) {
        this.trees = trees;
    }

    
    /** 
     * get the number of correct decisions in a list of tests performed 
     * against the forest
     *
     * @param tests     The test samples that will be used to 
     *                  evaluate the forest.
     */
    
    public int numberOfCorrectDecisions(List<Sample<D>> tests) throws Exception
    {
        final SharedInteger correct = new SharedInteger(0);
        final List<Sample<D>> currTests = tests;

        new ParallelTeam().execute (new ParallelRegion()
        {
            public void run() throws Exception
            {
                execute(0, currTests.size() - 1, new IntegerForLoop()
                {
                    int currCorrect = 0;
                    public IntegerSchedule schedule()
                    {
                        return IntegerSchedule.runtime();
                    }

                    public void run(int first, int last)
                    {
                        for(int i = first; i <= last; i++)
                        {
                            if(currTests.get(i).decision
                               .equals(decide(currTests.get(i))))
                            {
                                currCorrect++;
                            }
                        }
                    }

                    public void finish()
                    {
                        correct.addAndGet(currCorrect);
                    }
                });
            }
        });

        return correct.get();

    }

    /** 
     * gets the decision that the forest will make given a Sample's attributes
     *
     * @param sample    The sample who's attributes will be used to get
     *                  a decision from the forest
     *
     */
    public D decide(Sample<D> sample)
    {
        Counter<D> decisions = new Counter<D>();
        for(DecisionTree<D> tree : trees)
        {
            decisions.add(tree.decide(sample.choices));
        }  
        return decisions.mode();
    }

}
