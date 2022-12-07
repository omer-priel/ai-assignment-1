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
        Document document = builder.parse(file);

        return document;
    }

    // members
    public Variable[] variables = null;
    public int[][] parents = null;
    public double[][] CPTs = null;

    public BNetwork(String filepath) throws ParserConfigurationException, IOException, SAXException {
        // load XML
        Document document = BNetwork.parseXML(filepath);

        Element network = (Element)document.getElementsByTagName("NETWORK").item(0);

        // init
        initVariables(network);
        initCPTs(network);
    }

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

            loadCPT(name, parentsNodes, tableValues);
        }
    }

    private void loadCPT(String name, NodeList parentsNodes, String[] tableValues) {
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
    public int getVariableKey(String variableName) {
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getName().equals(variableName)) {
                return i;
            }
        }

        return -1;
    }

    public Variable getVariable(int key) {
        return this.variables[key];
    }

    // call queries utils
    private int[] getHidden(Query query) {
        int[] hidden = new int[this.variables.length - 1 - query.evidencesVaribles.length];

        boolean[] isHidden = new boolean[this.variables.length];
        Arrays.fill(isHidden, true);

        isHidden[query.queryVarible] = false;
        for (int key: query.evidencesVaribles) {
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

    private void callQueryResult(Query query, double[] probabilities, int additions, int multiplies) {
        double a = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            a += probabilities[i];
            additions++;
        }

        double probability = probabilities[query.queryValue] / a;

        // printing the results
        System.out.println(String.format("%.05f,%d,%d", probability, additions, multiplies));
    }

    // call query
    public void callQuery(Query query) {
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

    // callQuery1
    public void callQuery1(Query query) {
        // init
        int additions = 0;
        int multiplies = 0;

        double[] probabilities = new double[this.variables[query.queryVarible].getLength()];
        Arrays.fill(probabilities, 0.0f);

        int[] hidden = getHidden(query);

        // calc
        for (int probabilityIndex = 0; probabilityIndex < probabilities.length; probabilityIndex++) {
            int[] values = new int[this.variables.length];
            Arrays.fill(values, -1);

            values[query.queryVarible] = probabilityIndex;
            for (int i = 0; i < query.evidencesVaribles.length; i++) {
                values[query.evidencesVaribles[i]] = query.evidencesValues[i];
            }

            // get probability
            int k = 0;
            do {
                if (k == hidden.length) {
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

                        multiplies += ( i == 0) ? 0 : 1; // the first one is 1
                        subProbability *= this.CPTs[i][cptIndex];
                    }

                    additions += (probabilities[probabilityIndex] == 0) ? 0 : 1; // the first one is 0
                    probabilities[probabilityIndex] += subProbability;

                    k--;
                } else {
                    if (values[hidden[k]] == this.variables[hidden[k]].getLength() - 1) {
                        values[hidden[k]] = -1;
                        k--;
                    } else {
                        values[hidden[k]]++;
                        k++;
                    }
                }
            } while (k >= 0);
        }

        // result
        this.callQueryResult(query, probabilities, additions, multiplies);
    }

    // callQuery2
    private void callQuery2RemoveLeavesFactors(List<Integer> net, List<Integer> hiddenVariables) {
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

    private List<Factor> callQuery2CreateFactors(Query query, List<Integer> netAsList, List<Integer> hiddenVariables) {
        // net to
        Integer[] net = new Integer[netAsList.size()];
        netAsList.toArray(net);

        // create factors
        List<Factor> factors = new LinkedList<>();

        for (int variableIndex = 0; variableIndex < net.length; variableIndex++) {
            // init
            Integer factorVariable = net[variableIndex];
            int[] parents = this.parents[factorVariable];

            // load the factor variables
            List<Integer> factorVariables = new LinkedList<>();

            List<Integer> originVariables = new LinkedList<>();
            originVariables.add(factorVariable);
            originVariables.addAll(Arrays.stream(parents).boxed().collect(Collectors.toList()));

            int[] values = new int[originVariables.size()];
            Arrays.fill(values, -1);
            List<Integer> changeableVariablesIndexes = new LinkedList<>();

            for (int i = 0; i < originVariables.size(); i++) {
                Integer variable = originVariables.get(i);
                boolean isEvidence = false;

                for (int j = 0; j < query.evidencesVaribles.length && !isEvidence; j++) {
                    int evidencesVariable = query.evidencesVaribles[j];

                    if (variable.equals(evidencesVariable)) {
                        isEvidence = true;
                        values[i] = query.evidencesValues[j];
                    }
                }

                if (!isEvidence) {
                    factorVariables.add(variable);
                    changeableVariablesIndexes.add(i);
                }
            }

            // only factors with variables (factors with more then one probability).
            if (factorVariables.size() > 0) {
                // load the factor probabilities
                List<Double> factorProbabilities = new LinkedList<>();

                int k = 0;
                do {
                    if (k == changeableVariablesIndexes.size()) {
                        // add new factor probability
                        int cptIndex = values[0];
                        int jump = 1;

                        for (int j = 1; j < originVariables.size(); j++) {
                            int originVariable = originVariables.get(j - 1);

                            jump *= this.variables[originVariable].getLength();

                            cptIndex += values[j] * jump;
                        }

                        factorProbabilities.add(this.CPTs[factorVariable][cptIndex]);

                        k--;
                    } else {
                        int variableKey = originVariables.get(changeableVariablesIndexes.get(k));
                        if (values[changeableVariablesIndexes.get(k)] == this.variables[variableKey].getLength() - 1) {
                            values[changeableVariablesIndexes.get(k)] = -1;
                            k--;
                        } else {
                            values[changeableVariablesIndexes.get(k)]++;
                            k++;
                        }
                    }
                } while (k >= 0);

                // add the factor
                Factor factor = new Factor(this, factorVariables, factorProbabilities);
                factors.add(factor);
            }
        }

        return factors;
    }

    public void callQuery2(Query query){
        // init
        int additions = 0;
        int multiplies = 0;

        double[] probabilities = new double[this.variables[query.queryVarible].getLength()];
        Arrays.fill(probabilities, 0.0f);

        List<Integer> hiddenVariables = Arrays.stream(getHidden(query)).boxed().collect(Collectors.toList());

        // calc
        List<Integer> net = new LinkedList<>(hiddenVariables);
        net.add(query.queryVarible);
        net.addAll(Arrays.stream(query.evidencesVaribles).boxed().collect(Collectors.toList()));

        // remove unuseful hidden variables
        callQuery2RemoveLeavesFactors(net, hiddenVariables);

        // create factors
        List<Factor> factors = callQuery2CreateFactors(query, net, hiddenVariables);

        // remove hidden variables
        while (!hiddenVariables.isEmpty()) {
            // choose hidden variable
            int hidden = hiddenVariables.get(0);
            hiddenVariables.remove(0);

            // collect factors with the hidden to single factor
            List<Factor> factorsToJoin = new LinkedList<>();
            for (int i = 0; i < factors.size(); i++) {
                Factor factor = factors.get(i);
                if (factor.variableExists(hidden)) {
                    factorsToJoin.add(factor);
                    factors.remove(i);
                    i--;
                }
            }

            if (factorsToJoin.size() > 0) {
                // join the Factors
                Factor joinedFactor = factorsToJoin.get(0);
                factorsToJoin.remove(0);
                while (!factorsToJoin.isEmpty()) {
                    joinedFactor = Factor.join(joinedFactor, factorsToJoin.get(0));
                    factorsToJoin.remove(0);
                }

                // eliminate factor
                joinedFactor = Factor.eliminate(joinedFactor, hidden);

                // add the factor if it has more than probability
                if (joinedFactor.probabilities.size() > 1) {
                    factors.add(joinedFactor);
                }
            }
        }

        // join all the last factors
        Factor lastFactor = factors.get(0);
        factors.remove(0);
        while (!factors.isEmpty()) {
            lastFactor = Factor.join(lastFactor, factors.get(0));
            factors.remove(0);
        }

        probabilities = lastFactor.probabilities.stream().mapToDouble(i->i).toArray();

        // result
        this.callQueryResult(query, probabilities, additions, multiplies);
    }

    public void callQuery3(Query query) {
        // TODO
    }
}
