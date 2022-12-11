import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class BNetwork {
    // static utils
    private static Document parseXML(String filepath) throws ParserConfigurationException, IOException, SAXException {
        File file = new File(filepath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(file);
    }

    // members
    /**
     * array of variables.
     * used for converting to keys and for variables lengths.
     */
    public Variable[] variables = null;

    /**
     * array for mapping variable to his parents.
     * parents[variable key][Pi] when Pi is the parent i of the variable
     */
    public int[][] parents = null;

    /**
     * array for mapping variable to his CPT.
     * CPTs[variable key][Pi] when Pi is the probability of i.
     * when 'i' is the index of the values
     */
    public double[][] CPTs = null;

    public BNetwork(String filepath) throws ParserConfigurationException, IOException, SAXException {

        // load the network from XML
        Document document;
        try {
            document = BNetwork.parseXML(filepath);
        } catch (Exception ex) {
            throw ex;
        }

        Element network = (Element)document.getElementsByTagName("NETWORK").item(0);

        // init
        initVariables(network);
        initCPTs(network);
    }

    /**
     * initialize the variables from the XML
     *
     * @param network network xml node
     */
    private void initVariables(Element network) {
        List<Variable> variables = new LinkedList<Variable>();

        NodeList variablesElements = network.getElementsByTagName("VARIABLE");

        for (int i = 0; i < variablesElements.getLength(); i++) {
            Element variableElement = (Element) variablesElements.item(i);
            String name = variableElement.getElementsByTagName("NAME").item(0).getTextContent();

            // load values (outcomes)
            List<String> values = new LinkedList<>();
            NodeList outcomesElements = variableElement.getElementsByTagName("OUTCOME");

            for (int j = 0; j < outcomesElements.getLength(); j++) {
                String value = outcomesElements.item(j).getTextContent();
                values.add(value);
            }

            String[] valuesAsArray = new String[values.size()];
            values.toArray(valuesAsArray);

            Variable variable = new Variable(name, valuesAsArray);
            variables.add(variable);
        }

        this.variables = new Variable[variables.size()];
        variables.toArray(this.variables);
    }

    /**
     * initialize the parents and the CPTs from the XML
     *
     * @param network network xml node
     */
    private void initCPTs(Element network) {
        int variablesLen = this.variables.length;

        this.parents = new int[variablesLen][];
        this.CPTs = new double[variablesLen][];

        NodeList definitionsElements = network.getElementsByTagName("DEFINITION");

        for (int definitionIndex = 0; definitionIndex < definitionsElements.getLength(); definitionIndex++) {
            // load data from XML
            Element definitionElement = (Element) definitionsElements.item(definitionIndex);
            String name = definitionElement.getElementsByTagName("FOR").item(0).getTextContent();
            NodeList parentsNodes = definitionElement.getElementsByTagName("GIVEN");
            String[] tableValues = definitionElement.getElementsByTagName("TABLE").item(0).getTextContent().split("\\s+");

            initSingleCPT(name, parentsNodes, tableValues);
        }
    }

    /**
     * initialize single CPT with its parents
     *
     * @param name CPT variable name
     * @param parentsNodes parents xml nodes
     * @param tableValues cpt table
     */
    private void initSingleCPT(String name, NodeList parentsNodes, String[] tableValues) {
        int variableKey = this.getVariableKey(name);
        List<Integer> parentsList = new LinkedList<>();

        for (int i = 0; i < parentsNodes.getLength(); i++) {
            String parentName = parentsNodes.item(i).getTextContent();
            int parent = this.getVariableKey(parentName);

            // not itself
            if (variableKey != parent) {
                parentsList.add(parent);
            }
        }

        Collections.reverse(parentsList);

        this.parents[variableKey] = parentsList.stream().mapToInt(i->i).toArray();

        this.CPTs[variableKey] = new double[tableValues.length];
        for (int i = 0; i < tableValues.length; i++) {
            this.CPTs[variableKey][i] = Double.parseDouble(tableValues[i]);
        }
    }

    // getters
    /**
     * get key of variable by name
     *
     * @param name variable name
     * @return key of the variable and -1 if this variable not exists in the network
     */
    public int getVariableKey(String name) {
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getName().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * get variable by key
     *
     * @param key
     * @return
     */
    public Variable getVariable(int key) {
        return this.variables[key];
    }

    // call queries utils

    /**
     * get the hidden variables of query
     *
     * @param query
     * @return array of variables keys
     */
    private int[] getHidden(Query query) {
        int[] hidden = new int[this.variables.length - 1 - query.evidencesVariables.length];

        boolean[] isHidden = new boolean[this.variables.length];
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
     * @param query
     * @param probabilities normalized probabilities array
     */
    private void calcProbability(Query query, double[] probabilities) {
        double a = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            a += probabilities[i];
            query.results.additions++;
        }

        query.results.probability = probabilities[query.queryValue] / a;
    }

    /**
     * print result of query
     *
     * @param query
     */
    private void printResult(Query query) {
        // printing the results
        System.out.println(String.format("%.07f,%d,%d", query.results.probability, query.results.additions, query.results.multiplies));
    }

    // call query

    /**
     * call (run) a single query
     *
     * @param query
     */
    public void callQuery(Query query) {
        if (query.evidencesVariables.length == 0) {
            callQueryWithoutEvidences(query);
            return;
        }

        switch (query.type) {
            case 1:
                callQuery1(query);
                break;
            case 2:
                callQuery2(query);
                break;
            case 3:
                callQuery3(query);
                break;
        }
    }

    // callQueryWithoutEvidences
    private void callQueryWithoutEvidences(Query query) {
        double[] cpt = this.CPTs[query.queryVariable];

        int i = query.queryValue;
        double probability = cpt[i];

        if (this.parents[query.queryVariable].length > 0) {
            int jump = cpt.length / this.variables[query.queryVariable].getLength();

            i += jump;

            while (i < cpt.length) {
                probability += cpt[i];

                query.results.additions++;

                i += jump;
            }
        }

        // result
        query.results.probability = probability;

        this.printResult(query);
    }

    // callQuery1
    private void callQuery1(Query query) {
        // init
        double[] probabilities = new double[this.variables[query.queryVariable].getLength()];
        Arrays.fill(probabilities, 0.0f);

        int[] hidden = getHidden(query);

        // calc
        for (int probabilityIndex = 0; probabilityIndex < probabilities.length; probabilityIndex++) {
            int[] values = new int[this.variables.length];
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
                for (int i = 0; i < this.variables.length; i++) {
                    int cptIndex = values[i];
                    int jump = this.variables[i].getLength();

                    for (int j = 0; j < this.parents[i].length; j++) {
                        int parentKey = this.parents[i][j];
                        cptIndex += values[parentKey] * jump;
                        jump *= this.variables[parentKey].getLength();
                    }

                    query.results.multiplies += (i == 0) ? 0 : 1; // the first one is 1
                    subProbability *= this.CPTs[i][cptIndex];
                }

                query.results.additions += (probabilities[probabilityIndex] == 0) ? 0 : 1; // the first one is 0
                probabilities[probabilityIndex] += subProbability;

                // move to the next values
                k--;
                while (k >= 0 && values[hidden[k]] == this.variables[hidden[k]].getLength() - 1) {
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
        this.calcProbability(query, probabilities);

        this.printResult(query);
    }

    // callQuery2
    private void variableEliminationRemoveLeavesFactors(List<Integer> net, List<Integer> hiddenVariables) {
        int hiddenIndex = 0;
        while (hiddenIndex < hiddenVariables.size()) {
            Integer hidden = hiddenVariables.get(hiddenIndex);

            boolean isLeave = true;

            for (int i = 0; i < net.size() && isLeave; i++) {
                int node = net.get(i);
                if (node != hidden) {
                    for (int j = 0; j < this.parents[node].length && isLeave; j++) {
                        if (this.parents[node][j] == hidden) {
                            isLeave = false;
                        }
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

    private List<Factor> variableEliminationCreateFactors(Query query, List<Integer> net) {
        // create factors
        List<Factor> factors = new LinkedList<>();

        for (int variableIndex = 0; variableIndex < net.size(); variableIndex++) {
            // init
            int factorVariable = net.get(variableIndex);
            int[] parents = this.parents[factorVariable];

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
                    probabilitiesLength *= this.variables[factorVariables[i]].getLength();
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

                        jump *= this.variables[originVariable].getLength();

                        cptIndex += values[j] * jump;
                    }

                    factorProbabilities[probabilityIndex] = this.CPTs[factorVariable][cptIndex];

                    // move to next values
                    int k = changeableVariablesIndexes.size() - 1;
                    boolean hasNext = true;
                    while (k >= 0 && hasNext) {
                        int variableKey = originVariables.get(changeableVariablesIndexes.get(k));
                        hasNext = values[changeableVariablesIndexes.get(k)] == this.variables[variableKey].getLength() - 1;
                        if (hasNext) {
                            values[changeableVariablesIndexes.get(k)] = 0;
                            k--;
                        }
                    }

                    if (k != -1) {
                        values[changeableVariablesIndexes.get(k)]++;
                        k = changeableVariablesIndexes.size();
                    }
                }

                // add the factor
                Factor factor = new Factor(factorVariables, factorProbabilities);
                factors.add(factor);
            }
        }

        return factors;
    }

    private void callBasedVariableElimination(Query query, Comparator<Integer> hiddenSortingComparator) {
        // init
        List<Integer> hiddenVariables = Arrays.stream(getHidden(query)).boxed().collect(Collectors.toList());

        List<Integer> net = new ArrayList<>(hiddenVariables); // the variables that need for the query
        net.add(query.queryVariable);
        net.addAll(Arrays.stream(query.evidencesVariables).boxed().collect(Collectors.toList()));

        // remove unuseful hidden variables
        variableEliminationRemoveLeavesFactors(net, hiddenVariables);

        // create factors
        List<Factor> factors = variableEliminationCreateFactors(query, net);

        // ordering the hidden variables
        hiddenVariables.sort(hiddenSortingComparator);

        // Variable Elimination
        while (!hiddenVariables.isEmpty()) {
            int hidden = hiddenVariables.get(0);
            hiddenVariables.remove(0);

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

                    return variables[factorA.variables[0]].getName().compareTo(variables[factorB.variables[0]].getName());
                });

                // join the Factors
                Factor joinedFactor = factorsToJoin.get(0);
                for (int i = 1; i < factorsToJoin.size(); i++) {
                    joinedFactor = Factor.join(this, query, joinedFactor, factorsToJoin.get(i));
                }

                // eliminate factor
                joinedFactor = Factor.eliminate(this, query, joinedFactor, hidden);

                // add the factor if it has more than probability
                if (joinedFactor.probabilities.length > 1) {
                    factors.add(firstFactorIndex, joinedFactor);
                }
            }
        }

        // join all the last factors
        Factor lastFactor = factors.get(0);
        for (int i = 1; i < factors.size(); i++) {
            lastFactor = Factor.join(this, query, lastFactor, factors.get(i));
        }

        // result
        this.calcProbability(query, lastFactor.probabilities);

        this.printResult(query);
    }

    private void callQuery2(Query query){
        callBasedVariableElimination(query, (variableAKey, variableBKey) -> {
            Variable variableA = getVariable(variableAKey);
            Variable variableB = getVariable(variableBKey);

            return variableA.getName().compareTo(variableB.getName());
        });
    }

    /**
     * does query using Variable Elimination
     *
     * Ordering the hidden variables rules:
     * If A has less dependent variables than B, choose A
     * If B has less dependent variables than A, choose B
     * If B has more values than A, choose B
     * Else, choose A
     *
     * @param query
     */
    private void callQuery3(Query query) {
        callBasedVariableElimination(query, (variableAKey, variableBKey) -> {
            Variable variableA = getVariable(variableAKey);
            Variable variableB = getVariable(variableBKey);

            int diff = this.parents[variableAKey].length - this.parents[variableBKey].length;

            if (diff != 0) {
                return (diff > 0) ? 1 : -1;
            }

            return (variableA.getLength() > variableB.getLength()) ? 1 : -1;
        });
    }
}
