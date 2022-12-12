import java.util.Arrays;
public class Factor {
    public int[] variables;
    public double[] probabilities;

    public Factor(int[] variables, double[] probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters
    public boolean variableExists(int variable) {
        for (int i = 0; i < this.variables.length; i++) {
            if (this.variables[i] == variable) {
                return true;
            }
        }
        return false;
    }

    // static operations
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

        // get the probabilities length
        int factorProbabilitiesLength = 1;
        for (int i = 0; i < factorVariables.length; i++) {
            factorProbabilitiesLength *= network.variablesLengths[factorVariables[i]];
        }

        // get the probabilities
        double[] factorProbabilities = new double[factorProbabilitiesLength];

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

        for (int probabilityIndex = 0; probabilityIndex < factorProbabilitiesLength; probabilityIndex++) {
            // add new probability to the new factor
            int cptIndexA = 0;
            int jumpA = 1;
            int cptIndexB = 0;
            int jumpB = 1;

            for (int i = 0; i < factorA.variables.length; i++) {
                int variable = factorA.variables[i];

                cptIndexA += values[indexesA[i]] * jumpA;

                jumpA *= network.variablesLengths[variable];
            }

            for (int i = 0; i < factorB.variables.length; i++) {
                int variable = factorB.variables[i];

                cptIndexB += values[indexesB[i]] * jumpB;

                jumpB *= network.variablesLengths[variable];
            }

            double probabilityA = factorA.probabilities[cptIndexA];
            double probabilityB = factorB.probabilities[cptIndexB];

            factorProbabilities[probabilityIndex] = probabilityA * probabilityB;
            query.results.multiplies++;

            // move to next value
            int k = 0;
            while (k < values.length && values[k] == network.variablesLengths[factorVariables[k]] - 1) {
                values[k] = 0;
                k++;
            }

            if (k < values.length) {
                values[k]++;
            }
        }

        // create the factor
        return new Factor(factorVariables, factorProbabilities);
    }

    static public Factor eliminate(BNetwork network, Query query, Factor factor, int variable) {
        int[] variables = new int[factor.variables.length - 1];

        // get the new factor variables and the length of the new factor
        int variablesBeforeLength = 1;
        int variableLength;
        int variablesAfterLength = 1;

        int factorVariableIndex = 0;
        while (factor.variables[factorVariableIndex] != variable) {
            variables[factorVariableIndex] = factor.variables[factorVariableIndex];
            variablesBeforeLength *= network.variablesLengths[factor.variables[factorVariableIndex]];
            factorVariableIndex++;
        }

        variableLength = network.variablesLengths[factor.variables[factorVariableIndex]];
        factorVariableIndex++;

        while (factorVariableIndex < factor.variables.length) {
            variables[factorVariableIndex - 1] = factor.variables[factorVariableIndex];

            variablesAfterLength *= network.variablesLengths[factor.variables[factorVariableIndex]];
            factorVariableIndex++;
        }

        // get the probabilities
        double[] probabilities = new double[variablesBeforeLength * variablesAfterLength];

        int probabilityNewIndex = 0;
        for (int k = 0; k < variablesAfterLength; k++) {
            for (int j = 0; j < variablesBeforeLength; j++) {
                int probabilityIndex = k * variablesBeforeLength * variableLength + j;

                double probability = factor.probabilities[probabilityIndex];
                for (int i = 1; i < variableLength; i++) {
                    probability += factor.probabilities[probabilityIndex + i * variablesBeforeLength];
                    query.results.additions++;
                }

                probabilities[probabilityNewIndex] = probability;
                probabilityNewIndex++;
            }
        }

        return new Factor(variables, probabilities);
    }
}
