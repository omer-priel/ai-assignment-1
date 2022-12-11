import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Factor {
    public List<Integer> variables;
    public List<Double> probabilities;

    public Factor(List<Integer> variables, List<Double> probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters
    public boolean variableExists(Integer variable) {
        return this.variables.contains(variable);
    }

    // static operations
    static public List<Integer> unionGroups(List<Integer> groupA, List<Integer> groupB) {
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

    static public Factor join(BNetwork network, Query query, Factor factorA, Factor factorB) {
        // get the factor variables
        List<Integer> factorVariables = unionGroups(factorA.variables, factorB.variables);

        // get the probabilities
        List<Double> factorProbabilities = new LinkedList<>();

        int[] values = new int[factorVariables.size()];
        Arrays.fill(values, 0);

        int[] indexesA = new int[factorA.variables.size()];
        for (int i = 0; i < indexesA.length; i++) {
            indexesA[i] = factorVariables.indexOf(factorA.variables.get(i));
        }

        int[] indexesB = new int[factorB.variables.size()];
        for (int i = 0; i < indexesB.length; i++) {
            indexesB[i] = factorVariables.indexOf(factorB.variables.get(i));
        }

        int k = 0;
        while (k < values.length) {
            // add new probability to the new factor
            int cptIndexA = 0;
            int jumpA = 1;
            int cptIndexB = 0;
            int jumpB = 1;

            for (int i = 0; i < factorA.variables.size(); i++) {
                int variable = factorA.variables.get(i);

                cptIndexA += values[indexesA[i]] * jumpA;

                jumpA *= network.variables[variable].getLength();
            }

            for (int i = 0; i < factorB.variables.size(); i++) {
                int variable = factorB.variables.get(i);

                cptIndexB += values[indexesB[i]] * jumpB;

                jumpB *= network.variables[variable].getLength();
            }

            double probabilityA = factorA.probabilities.get(cptIndexA);
            double probabilityB = factorB.probabilities.get(cptIndexB);

            factorProbabilities.add(probabilityA * probabilityB);
            query.results.multiplies++;

            // move to next value
            k = 0;
            while (k < values.length && values[k] == network.variables[factorVariables.get(k)].getLength() - 1) {
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

    static public Factor eliminate(BNetwork network, Query query, Factor factor, Integer variable) {
        List<Integer> variables = new ArrayList<>(factor.variables);
        variables.remove(variable);

        // get the probabilities
        List<Double> probabilities = new ArrayList<>();

        int jumpS = 1;
        int variableLength;
        int jumpE = 1;

        int factorVariableIndex = 0;
        while (!factor.variables.get(factorVariableIndex).equals(variable)) {
            jumpS *= network.variables[factor.variables.get(factorVariableIndex)].getLength();
            factorVariableIndex++;
        }

        variableLength = network.variables[factor.variables.get(factorVariableIndex)].getLength();
        factorVariableIndex++;

        while (factorVariableIndex < factor.variables.size()) {
            jumpE *= network.variables[factor.variables.get(factorVariableIndex)].getLength();
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
