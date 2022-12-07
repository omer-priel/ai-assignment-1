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
    private Variable[] variables = null;
    private int[][] parents = null;
    private float[][] CPTs = null;

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
        this.CPTs = new float[variablesLen][];

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

        this.CPTs[variableKey] = new float[tableValues.length];
        for (int i = 0; i < tableValues.length; i++) {
            this.CPTs[variableKey][i] = Float.parseFloat(tableValues[i]);
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

    private void callQueryResult(Query query, float[] probabilities, int additions, int multiplies) {
        float a = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            a += probabilities[i];
            additions++;
        }

        float probability = probabilities[query.queryVarible] / a;

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

        float[] probabilities = new float[this.variables[query.queryVarible].getLength()];
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
                    float subProbability = 1;
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
    private List<Factor> callQuery2CreateFactors(Query query, List<Integer> hiddenVariables) {
        // create factors
        List<Factor> factors = new LinkedList<>();
        for (int variableIndex = 0; variableIndex < this.variables.length; variableIndex++) {
            Variable variable = this.getVariable(variableIndex);
            Factor factor = new Factor(this);

            // set variables
            factor.variables = new LinkedList<>();
            factor.variables.add(variableIndex);
            for (int j = 0; j < this.parents[variableIndex].length; j++) {
                factor.variables.add(this.parents[variableIndex][j]);
            }

            // set values and probabilities
            factor.values = new LinkedList<>();
            factor.probabilities = new LinkedList<>();

            int[] values = new int[this.variables.length];
            Arrays.fill(values, -1);

            for (int i = 0; i < query.evidencesVaribles.length; i++) {
                values[query.evidencesVaribles[i]] = query.evidencesValues[i];
            }

            List<Integer> possibleVariables = new LinkedList<>();

            possibleVariables.add(query.queryVarible);
            possibleVariables.addAll(hiddenVariables);

            int k = 0;
            do {
                if (k == possibleVariables.size()) {
                    // add probabilityValues and probability
                    List<Integer> probabilityValues = new LinkedList<>();
                    float probability = 0;

                    // get probabilityValues
                    for (int i = 0; i < factor.variables.size(); i++) {
                        probabilityValues.add(values[factor.variables.get(i)]);
                    }

                    // get probability from CPT
                    int cptIndex = values[variableIndex];
                    int jump = this.variables[variableIndex].getLength();

                    for (int i = 0; i < this.parents[variableIndex].length; i++) {
                        int parentKey = this.parents[variableIndex][i];
                        cptIndex += values[parentKey] * jump;
                        jump *= this.variables[parentKey].getLength();
                    }

                    probability = this.CPTs[variableIndex][cptIndex];

                    // create new line in the factor
                    factor.values.add(probabilityValues);
                    factor.probabilities.add(probability);

                    // next
                    k--;
                } else {
                    if (values[possibleVariables.get(k)] == this.variables[possibleVariables.get(k)].getLength() - 1) {
                        values[possibleVariables.get(k)] = -1;
                        k--;
                    } else {
                        values[possibleVariables.get(k)]++;
                        k++;
                    }
                }
            } while (k >= 0);

            // add the new factor
            factors.add(factor);
        }

        return factors;
    }

    private void callQuery2RemoveLeavesFactors(List<Factor> factors, List<Integer> hiddenVariables) {
        int hiddenIndex = 0;
        while (hiddenIndex < hiddenVariables.size()) {
            int hidden = hiddenVariables.get(hiddenIndex);
            int factorIndex = -1;

            for (int i = 0; i < factors.size() && factorIndex == -1; i++) {
                if (factors.get(i).variables.get(0) == hidden) {
                    factorIndex = i;
                }
            }

            if (factorIndex == -1) {
                hiddenVariables.remove(hiddenIndex);
                hiddenIndex = 0;
            } else {
                boolean isLeave = true;
                for (int i = 0; i < factors.size() && isLeave; i++) {
                    if (factorIndex != i) {
                        if (factors.get(i).variableExists(hidden)) {
                            isLeave = false;
                        }
                    }
                }

                if (isLeave) {
                    hiddenVariables.remove(hiddenIndex);
                    factors.remove(factorIndex);
                    hiddenIndex = 0;
                } else {
                    hiddenIndex++;
                }
            }
        }
    }


    public void callQuery2(Query query){
        // init
        int additions = 0;
        int multiplies = 0;

        float[] probabilities = new float[this.variables[query.queryVarible].getLength()];
        Arrays.fill(probabilities, 0.0f);

        List<Integer> hiddenVariables = Arrays.stream(getHidden(query)).boxed().collect(Collectors.toList());

        // calc
        // create factors
        List<Factor> factors = callQuery2CreateFactors(query, hiddenVariables);

        // remove unuseful hidden variables
        callQuery2RemoveLeavesFactors(factors, hiddenVariables);

        // remove hidden variables
        while (!hiddenVariables.isEmpty()) {
            // choose hidden variable
            int hidden = hiddenVariables.get(0);
            hiddenVariables.remove(0);

            // collect factors with the hidden to single factor
            List<Factor> factorsToJoin = new LinkedList<>();
            List<Integer> variables = new LinkedList<>();
            for (int i = 0; i < factors.size(); i++) {
                Factor factor = factors.get(i);
                if (factor.variableExists(hidden)) {
                    factorsToJoin.add(factor);
                    factors.remove(i);

                    variables.addAll(factor.variables);
                }
            }

            if (factorsToJoin.size() > 0) {
                // join the Factors
                variables = new LinkedList<>(new HashSet<>(variables));
                variables.remove(Integer.valueOf(hidden));

                Factor joinedFactor = factorsToJoin.get(0);
                factorsToJoin.remove(0);
                while (!factorsToJoin.isEmpty()) {
                    joinedFactor.join(factorsToJoin.get(0));
                    factorsToJoin.remove(0);
                }


                // eliminate factor
                // TODO

                // remove the factor if is single valued
                // TODO
            }
        }

        // last eliminate factor
        // TODO

        // result
        this.callQueryResult(query, probabilities, additions, multiplies);
    }

    public void callQuery3(Query query) {
        // TODO
    }
}
