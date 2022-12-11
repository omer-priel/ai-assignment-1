import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Factor {
    public int[] variables;
    public List<Double> probabilities;

    public Factor(int[] variables, List<Double> probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters
    public boolean variableExists(Integer variable) {
        for (int i = 0; i < this.variables.length; i++) {
            if (this.variables[i] == variable) {
                return true;
            }
        }
        return false;
    }

    // static operations
//    static public List<Integer> unionGroups(List<Integer> groupA, List<Integer> groupB) {
//        List<Integer> union = new LinkedList<>(groupA);
//
//        for (int i = 0; i < groupB.size(); i++) {
//            boolean inA = false;
//            for (int j = 0; j < groupA.size() && !inA; j++) {
//                inA = groupB.get(i).equals((groupA.get(j)));
//            }
//
//            if (!inA) {
//             union.add(groupB.get(i));
//            }
//        }
//
//        return union;
//    }

    static public int[] unionGroups(int[] groupA, int[] groupB) {
        int[] union = new int[groupA.length + groupB.length];

        int index = 0;
        while (index < groupA.length) {
            union[index] = groupA[index];
            index++;
        }

        for (int i = 0; i < groupB.length; i++) {
            boolean inA = false;
            for (int j = 0; j < groupA.length && !inA; j++) {
                inA = groupB[i] == groupA[j];
            }

            if (!inA) {
                union[index] = groupB[i];
                index++;
            }
        }

        int[] fixedUnion = new int[index];
        for (int i = 0; i < fixedUnion.length; i++) {
            fixedUnion[i] = union[i];
        }

        return fixedUnion;
    }

    static public Factor join(BNetwork network, Query query, Factor factorA, Factor factorB) {
        // get the factor variables
        int[] factorVariables = unionGroups(factorA.variables, factorB.variables);

        // get the probabilities
        List<Double> factorProbabilities = new LinkedList<>();

        int[] values = new int[factorVariables.length];
        Arrays.fill(values, 0);

        int[] indexesA = new int[factorA.variables.length];
        for (int i = 0; i < indexesA.length; i++) {
            int j = 0;
            while (factorVariables[j] != factorA.variables[i]) {
                j++;
            }

            indexesA[i] = j;
        }

        int[] indexesB = new int[factorB.variables.length];
        for (int i = 0; i < indexesB.length; i++) {
            int j = 0;
            while (factorVariables[j] != factorB.variables[i]) {
                j++;
            }

            indexesB[i] = j;
        }

        int k = 0;
        while (k < values.length) {
            // add new probability to the new factor
            int cptIndexA = 0;
            int jumpA = 1;
            int cptIndexB = 0;
            int jumpB = 1;

            for (int i = 0; i < factorA.variables.length; i++) {
                int variable = factorA.variables[i];

                cptIndexA += values[indexesA[i]] * jumpA;

                jumpA *= network.variables[variable].getLength();
            }

            for (int i = 0; i < factorB.variables.length; i++) {
                int variable = factorB.variables[i];

                cptIndexB += values[indexesB[i]] * jumpB;

                jumpB *= network.variables[variable].getLength();
            }

            double probabilityA = factorA.probabilities.get(cptIndexA);
            double probabilityB = factorB.probabilities.get(cptIndexB);

            factorProbabilities.add(probabilityA * probabilityB);
            query.results.multiplies++;

            // move to next value
            k = 0;
            while (k < values.length && values[k] == network.variables[factorVariables[k]].getLength() - 1) {
                values[k] = 0;
                k++;
            }

            if (k < values.length) {
                values[k]++;
                k = 0;
            }
        }

        // create the factor
        return new Factor(factorVariables, factorProbabilities);
    }

    static public Factor eliminate(BNetwork network, Query query, Factor factor, int variable) {
        int[] variables = new int[factor.variables.length - 1];

        int variableNewIndex = 0;
        for (int i = 0; i < factor.variables.length; i++) {
            if (factor.variables[i] != variable) {
                variables[variableNewIndex] = factor.variables[i];
                variableNewIndex++;
            }
        }

        // get the probabilities
        List<Double> probabilities = new ArrayList<>();

        int jumpS = 1;
        int variableLength;
        int jumpE = 1;

        int factorVariableIndex = 0;
        while (factor.variables[factorVariableIndex] != variable) {
            jumpS *= network.variables[factor.variables[factorVariableIndex]].getLength();
            factorVariableIndex++;
        }

        variableLength = network.variables[factor.variables[factorVariableIndex]].getLength();
        factorVariableIndex++;

        while (factorVariableIndex < factor.variables.length) {
            jumpE *= network.variables[factor.variables[factorVariableIndex]].getLength();
            factorVariableIndex++;
        }

        for (int k = 0; k < jumpE; k++) {
            for (int j = 0; j < jumpS; j++) {
                int probabilityIndex = k * jumpS * variableLength  + j;

                double probability = factor.probabilities.get(probabilityIndex);
                for (int i = 1; i < variableLength; i++) {
                    probability += factor.probabilities.get(probabilityIndex + i * jumpS);
                    query.results.additions++;
                }

                probabilities.add(probability);
            }
        }

        return new Factor(variables, probabilities);
    }
}
