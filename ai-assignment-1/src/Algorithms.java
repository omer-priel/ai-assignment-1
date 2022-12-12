import java.util.*;
import java.util.stream.Collectors;

public class Algorithms {

    // call queries utils
    /**
     * get the hidden variables of query
     *
     * @param query the query
     * @return array of variables keys
     */
    static private int[] getHidden(Query query, BNetwork network) {
        int[] hidden = new int[network.variables.length - 1 - query.evidencesVariables.length];

        boolean[] isHidden = new boolean[network.variables.length];
        Arrays.fill(isHidden, true);

        isHidden[query.queryVariable] = false;
        for (int key: query.evidencesVariables) {
            isHidden[key] = false;
        }

        int k = 0;
        for (int i = 0; i < isHidden.length; i++) {
            if (isHidden[i]) {
                hidden[k] = i;
                k++;
            }
        }

        return hidden;
    }

    /**
     * calculate query probability by list of normalized probabilities
     *
     * @param query the query
     * @param probabilities normalized probabilities array
     */
    static private void calcProbability(Query query, double[] probabilities) {
        double a = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            a += probabilities[i];
            query.results.additions++;
        }

        query.results.probability = probabilities[query.queryValue] / a;
    }

    /**
     * print results of query
     *
     * @param results results of query
     */
    static private void printResult(QueryResults results) {
        // printing the results
        System.out.printf("%.05f,%d,%d%n", results.probability, results.additions, results.multiplies);
    }

    // call query
    /**
     * call (run) a single query
     *
     * @param query the query
     */
    static public void callQuery(Query query, BNetwork network) {
        // if dos not exists evidences
        if (query.evidencesVariables.length == 0) {
            callQueryWithoutEvidences(query, network);
            printResult(query.results);
        }

        // check if the probability in the CPT
        int[] parents = network.parents[query.queryVariable];
        boolean inCPT = parents.length == query.evidencesVariables.length;
        for (int i = 0; i < query.evidencesVariables.length && inCPT; i++) {
            inCPT = false;
            for (int j = 0; !inCPT && j < parents.length; j++) {
                inCPT = query.evidencesVariables[i] == parents[j];
            }
        }

        if (inCPT) {
            int index = query.queryValue;
            int jump = 1;

            for (int parent : parents) {
                jump *= network.variablesLengths[parent];
                int j = 0;
                while (parent != query.evidencesVariables[j]) {
                    j++;
                }

                index += query.evidencesValues[j] * jump;
            }

            query.results.probability = network.CPTs[query.queryVariable][index];
            printResult(query.results);

            return;
        }

        // call query type
        switch (query.type) {
            case 1:
                callQuery1(query, network);
                break;
            case 2:
                callQuery2(query, network);
                break;
            case 3:
                callQuery3(query, network);
                break;
        }

        printResult(query.results);
    }

    // callQueryWithoutEvidences
    /**
     * if dos not exists evidences
     *
     * @param query the query
     */
    static private void callQueryWithoutEvidences(Query query, BNetwork network) {
        double[] cpt = network.CPTs[query.queryVariable];

        int i = query.queryValue;
        double probability = cpt[i];

        if (network.parents[query.queryVariable].length > 0) {
            int jump = cpt.length / network.variablesLengths[query.queryVariable];

            i += jump;

            while (i < cpt.length) {
                probability += cpt[i];

                query.results.additions++;

                i += jump;
            }
        }

        // result
        query.results.probability = probability;
    }

    static private void callQuery1(Query query, BNetwork network) {
        // init
        double[] probabilities = new double[network.variablesLengths[query.queryVariable]];
        Arrays.fill(probabilities, 0.0f);

        int[] hidden = getHidden(query, network);

        // calc
        for (int probabilityIndex = 0; probabilityIndex < probabilities.length; probabilityIndex++) {
            int[] values = new int[network.variables.length];
            Arrays.fill(values, 0);

            values[query.queryVariable] = probabilityIndex;
            for (int i = 0; i < query.evidencesVariables.length; i++) {
                values[query.evidencesVariables[i]] = query.evidencesValues[i];
            }

            // get single probability
            int k = hidden.length;
            while (k != -1) {
                // get probability of one value of hidden variables
                double subProbability = 1;
                for (int i = 0; i < network.variables.length; i++) {
                    int cptIndex = values[i];
                    int jump = network.variablesLengths[i];

                    for (int j = 0; j < network.parents[i].length; j++) {
                        int parentKey = network.parents[i][j];
                        cptIndex += values[parentKey] * jump;
                        jump *= network.variablesLengths[parentKey];
                    }

                    query.results.multiplies += (i == 0) ? 0 : 1; // the first one is 1
                    subProbability *= network.CPTs[i][cptIndex];
                }

                query.results.additions += (probabilities[probabilityIndex] == 0) ? 0 : 1; // the first one is 0
                probabilities[probabilityIndex] += subProbability;

                // move to the next values
                k--;
                while (k >= 0 && values[hidden[k]] == network.variablesLengths[hidden[k]] - 1) {
                    values[hidden[k]] = 0;
                    k--;
                }

                if (k != -1) {
                    values[hidden[k]]++;
                    k = hidden.length;
                }
            }
        }

        // result
        calcProbability(query, probabilities);
    }

    /**
     * This query using Variable Elimination
     *
     * @param query the query
     */
    static private void callQuery2(Query query, BNetwork network){
        callBasedVariableElimination(query, network, (hiddenVariables, factors) -> 0);
    }

    /**
     * This query using Variable Elimination
     * Min-fill: Choose vertices to minimize the size of the factor that will be added to the graph.
     *
     * @param query the query
     */
    static private void callQuery3(Query query, BNetwork network) {
        callBasedVariableElimination(query, network, (hiddenVariables, factors) -> {

            int minFactorCreatedLength = -1;
            int chooseIndex = -1;

            boolean[] factorVariables = new boolean[network.variables.length];

            for (int i = 0; i < hiddenVariables.size(); i++) {
                int variable = hiddenVariables.get(i);

                Arrays.fill(factorVariables, false);

                for (Factor factor : factors) {
                    if (factor.variableExists(variable)) {
                        int[] variables = factor.variables;
                        for (int j : variables) {
                            factorVariables[j] = true;
                        }
                    }
                }

                factorVariables[variable] = false;

                int factorCreatedLength = 1;

                for (int j = 0; j < factorVariables.length; j++) {
                    if (factorVariables[i]) {
                        factorCreatedLength *= network.variablesLengths[j];
                    }
                }

                if (minFactorCreatedLength == -1 || minFactorCreatedLength > factorCreatedLength) {
                    chooseIndex = i;
                    minFactorCreatedLength = factorCreatedLength;
                }
            }

            return chooseIndex;
        });
    }

    // factor operations
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

    // Variable Elimination
    static private void variableEliminationRemoveLeavesFactors(BNetwork network, List<Integer> net, List<Integer> hiddenVariables) {
        int hiddenIndex = 0;
        while (hiddenIndex < hiddenVariables.size()) {
            Integer hidden = hiddenVariables.get(hiddenIndex);

            boolean isLeave = true;

            for (int i = 0; i < net.size() && isLeave; i++) {
                int node = net.get(i);
                if (node != hidden) {
                    for (int j = 0; j < network.parents[node].length && isLeave; j++) {
                        isLeave = (network.parents[node][j] != hidden);
                    }
                }
            }

            if (isLeave) {
                hiddenVariables.remove(hidden);
                net.remove(hidden);

                hiddenIndex = 0;
            } else {
                hiddenIndex++;
            }
        }
    }

    static private List<Factor> variableEliminationCreateFactors(Query query, BNetwork network, List<Integer> net) {
        // create factors
        List<Factor> factors = new LinkedList<>();

        for (int factorVariable : net) {
            // init
            int[] parents = network.parents[factorVariable];

            // load the factor variables
            List<Integer> factorVariablesList = new ArrayList<>(1 + parents.length);

            List<Integer> originVariables = new ArrayList<>(1 + parents.length);
            originVariables.add(factorVariable);
            originVariables.addAll(Arrays.stream(parents).boxed().collect(Collectors.toList()));

            int[] values = new int[originVariables.size()];
            Arrays.fill(values, 0);
            List<Integer> changeableVariablesIndexes = new LinkedList<>();

            for (int i = 0; i < originVariables.size(); i++) {
                Integer variable = originVariables.get(i);
                boolean isEvidence = false;

                for (int j = 0; j < query.evidencesVariables.length && !isEvidence; j++) {
                    int evidencesVariable = query.evidencesVariables[j];

                    if (variable.equals(evidencesVariable)) {
                        isEvidence = true;
                        values[i] = query.evidencesValues[j];
                    }
                }

                if (!isEvidence) {
                    factorVariablesList.add(variable);
                    changeableVariablesIndexes.add(i);
                }
            }

            // only factors with variables (factors with more than one probability).
            if (factorVariablesList.size() > 0) {
                // factorVariablesList to factorVariables and get probabilities length
                int[] factorVariables = new int[factorVariablesList.size()];
                int probabilitiesLength = 1;

                for (int i = 0; i < factorVariables.length; i++) {
                    factorVariables[i] = factorVariablesList.get(i);
                    probabilitiesLength *= network.variablesLengths[factorVariables[i]];
                }

                // load the factor probabilities
                double[] factorProbabilities = new double[probabilitiesLength];

                Collections.reverse(changeableVariablesIndexes);

                for (int probabilityIndex = 0; probabilityIndex < probabilitiesLength; probabilityIndex++) {
                    // add new factor probability
                    int cptIndex = values[0];
                    int jump = 1;

                    for (int j = 1; j < originVariables.size(); j++) {
                        int originVariable = originVariables.get(j - 1);

                        jump *= network.variablesLengths[originVariable];

                        cptIndex += values[j] * jump;
                    }

                    factorProbabilities[probabilityIndex] = network.CPTs[factorVariable][cptIndex];

                    // move to next values
                    int k = changeableVariablesIndexes.size() - 1;
                    boolean hasNext = true;
                    while (k >= 0 && hasNext) {
                        int variableKey = originVariables.get(changeableVariablesIndexes.get(k));
                        hasNext = values[changeableVariablesIndexes.get(k)] == network.variablesLengths[variableKey] - 1;
                        if (hasNext) {
                            values[changeableVariablesIndexes.get(k)] = 0;
                            k--;
                        }
                    }

                    if (k != -1) {
                        values[changeableVariablesIndexes.get(k)]++;
                    }
                }

                // add the factor
                Factor factor = new Factor(factorVariables, factorProbabilities);
                factors.add(factor);
            }
        }

        return factors;
    }

    static private void callBasedVariableElimination(Query query, BNetwork network, HiddenChooser hiddenChooser) {
        // init
        List<Integer> hiddenVariables = Arrays.stream(getHidden(query, network)).boxed().collect(Collectors.toList());

        List<Integer> net = new ArrayList<>(hiddenVariables); // the variables that need for the query
        net.add(query.queryVariable);
        net.addAll(Arrays.stream(query.evidencesVariables).boxed().collect(Collectors.toList()));

        // remove unuseful hidden variables
        variableEliminationRemoveLeavesFactors(network, net, hiddenVariables);

        // create factors
        List<Factor> factors = variableEliminationCreateFactors(query, network, net);

        // ordering the hidden variables
        hiddenVariables.sort((variableAKey, variableBKey) -> {
            Variable variableA = network.variables[variableAKey];
            Variable variableB = network.variables[variableBKey];

            return variableA.getName().compareTo(variableB.getName());
        });

        // Variable Elimination
        while (!hiddenVariables.isEmpty()) {
            int hiddenIndex = hiddenChooser.choose(hiddenVariables, factors);
            int hidden = hiddenVariables.get(hiddenIndex);
            hiddenVariables.remove(hiddenIndex);

            // collect factors with the hidden to single factor
            List<Factor> factorsToJoin = new ArrayList<>(factors.size());
            int firstFactorIndex = -1;
            for (int i = 0; i < factors.size(); i++) {
                Factor factor = factors.get(i);
                if (factor.variableExists(hidden)) {
                    // get the factor index, to return the new factor to the same place
                    if (firstFactorIndex == -1) {
                        firstFactorIndex = i;
                    }

                    factorsToJoin.add(factor);
                    factors.remove(i);
                    i--;
                }
            }

            if (factorsToJoin.size() > 0) {
                // ordering the factors to join
                factorsToJoin.sort((factorA, factorB) -> {
                    if (factorA.probabilities.length != factorB.probabilities.length) {
                        return (factorA.probabilities.length > factorB.probabilities.length) ? 1 : -1;
                    }

                    return network.variables[factorA.variables[0]].getName().compareTo(network.variables[factorB.variables[0]].getName());
                });

                // join the Factors
                Factor joinedFactor = factorsToJoin.get(0);
                for (int i = 1; i < factorsToJoin.size(); i++) {
                    joinedFactor = join(network, query, joinedFactor, factorsToJoin.get(i));
                }

                // eliminate factor
                joinedFactor = eliminate(network, query, joinedFactor, hidden);

                // add the factor if it has more than probability
                if (joinedFactor.probabilities.length > 1) {
                    factors.add(firstFactorIndex, joinedFactor);
                }
            }
        }

        // join all the last factors
        Factor lastFactor = factors.get(0);
        for (int i = 1; i < factors.size(); i++) {
            lastFactor = join(network, query, lastFactor, factors.get(i));
        }

        // result
        calcProbability(query, lastFactor.probabilities);
    }
}
