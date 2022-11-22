import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.IOException;

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
    Variable[] variables = null;
    int[][] parents = null;
    float[][] CPTs = null;

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

    // call queries
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

    public void callQuery1(Query query) {
        float probability = 0;

        // init
        int[] hidden = getHidden(query);

        int[] values = new int[this.variables.length];
        Arrays.fill(values, -1);

        values[query.queryVarible] = query.queryValue;
        for (int i = 0; i < query.evidencesVaribles.length; i++) {
            values[query.evidencesVaribles[i]] = query.evidencesValues[i];
        }

        // get probability
        StringBuilder rul = new StringBuilder("");

        int k = 0;
        do {
            if (k == hidden.length) {
                // get probability of one value of hidden variables
                float subProbability = 1;
                rul.append("(1");
                for (int i = 0; i < this.variables.length; i++) {
                    int cptIndex = values[i];
                    int jump = this.variables[i].getLength();

                    for (int j = 0; j < this.parents[i].length; j++) {
                        int parentKey = this.parents[i][j];
                        cptIndex += values[parentKey] * jump;
                        jump *= this.variables[parentKey].getLength();
                    }
                    subProbability *= this.CPTs[i][cptIndex];
                    rul.append(" * ");
                    rul.append(this.CPTs[i][cptIndex]);
                    System.out.println(String.format("%.05f", this.CPTs[i][cptIndex]));
                }

                probability += subProbability;
                rul.append(") + ");
                System.out.println(String.format("%.05f", probability));

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

        // printing the results
        System.out.println(String.format("%.05f", probability));
        System.out.println(rul);
    }

    public void callQuery2(Query query) {
        // TODO
    }

    public void callQuery3(Query query) {
        // TODO
    }

    // utils
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
}
