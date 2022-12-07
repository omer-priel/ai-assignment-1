import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Factor {
    private BNetwork network;
    public List<Integer> variables;
    public List<List<Integer>> values;
    public List<Float> probabilities;

    public Factor( BNetwork network) {
        this.network = network;
    }

    // getters
    public boolean variableExists(int variable) {
        for (Integer item: this.variables) {
            if (item == variable) {
                return true;
            }
        }

        return false;
    }

    // setters

    // utils
    private List<Integer> unionGroups(List<Integer> groupA, List<Integer> groupB) {
        List<Integer> union = new LinkedList<>(groupA);

        for (int i = 0; i < groupB.size(); i++) {
            boolean inA = false;
            for (int j = 0; j < groupA.size() && !inA; j++) {
                inA = groupB.get(i).equals((groupA.get(j)));
            }

            if (!inA) {
             union.add(groupB.get(i));
            }
        }

        return union;
    }

        private List<Integer> cutGroups(List<Integer> groupA, List<Integer> groupB) {
        List<Integer> both = new LinkedList<>(groupA);

        int i = 0;
        while (i < both.size()) {
            boolean inB = false;
            for (int j = 0; j < groupB.size() && !inB; j++) {
                inB = both.get(i).equals((groupB.get(j)));
            }

            if (inB) {
                i++;
            } else {
                both.remove(i);
            }
        }

        return both;
    }

    // actions
    public void join(Factor other) {
        // get the union and cut of the factors variables
        List<Integer> unionVariables = unionGroups(this.variables, other.variables);
        List<Integer> bothVariables = cutGroups(this.variables, other.variables);

        //

    }

//    public void combine(int variableKey) {
//        int index = -1;
//        for (int i = 0; i < this.variables.size() && index == -1; i++) {
//            if (this.variables.get(i) == variableKey) {
//                index = i;
//            }
//        }
//
//        // the variable is not in the factor
//        if (index == -1) {
//            return;
//        }
//
//        // combine
//        int variableLength = this.network.getVariable(index).getLength();
//        int jump = variableLength;
//        for (int i = 0; i < index; i++) {
//            jump *= this.network.getVariable(this.variables.get(i)).getLength();
//        }
//
//        List<List<Integer>> newValues = new LinkedList<>();
//        List<Integer> newProbabilities = new LinkedList<>();
//
//        for (int i = 0; i < jump; i++) {
//            List<Integer> newValue = this.values.get(i);
//            newValue.remove(index);
//
//            int newProbability = this.probabilities.get(i);
//            for (int j = 1; j < variableLength; j++) {
//                newProbability += this.probabilities.get(i + jump * j);
//            }
//
//            newValues.add(newValue);
//            newProbabilities.add(newProbability);
//        }
//
//        // clean up
//        this.variables.remove(index);
//        this.values = newValues;
//        this.probabilities = newProbabilities;
//    }
}
