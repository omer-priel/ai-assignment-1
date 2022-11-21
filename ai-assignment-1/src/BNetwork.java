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
    float[][] CPTs = null;
    boolean[][] relations = null;

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

        this.relations = new boolean[variablesLen][variablesLen];
        this.CPTs = new float[variablesLen][];

        for (int i = 0; i < variablesLen; i++) {
            for (int j = 0; j < variablesLen; j++) {
                this.relations[i][j] = false;
            }
        }

        // load relations and CPTs from definitions
        // Note - given is parent variable
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
        System.out.println("CPT of " + name);

        // load the parents and relations
        int variableKey = this.getVariableKey(name);
        List<Integer> parentsOriginList = new LinkedList<>();

        for (int i = 0; i < parentsNodes.getLength(); i++) {
            String parentName = parentsNodes.item(i).getTextContent();
            int parent = this.getVariableKey(parentName);

            // not itself
            if (variableKey != parent) {
                parentsOriginList.add(parent);
                this.relations[variableKey][parent] = true;
            }
        }

        // load the CPT
        Integer[] parentsOrigin = new Integer[parentsOriginList.size()];
        Integer[] parentsSorted = new Integer[parentsOriginList.size()];
        parentsOriginList.toArray(parentsOrigin);
        parentsOriginList.toArray(parentsSorted);

        Arrays.sort(parentsSorted);

        // get cpt length and int the lengths of the parents variables
        int cptLength = this.variables[variableKey].getLength();
        for (int i = 0; i < parentsOrigin.length; i++) {
            int originKey = parentsOrigin[i];
            int givenSortedKey = parentsSorted[i];

            parentsOrigin[i] = this.variables[originKey].getLength();
            parentsSorted[i] = this.variables[givenSortedKey].getLength();

            cptLength *= parentsOrigin[i];
        }

        float[] cpt = new float[cptLength];

        // fill the CPT
        int[] indexesArr = new int[parentsSorted.length];
        Arrays.fill(indexesArr, 0);

        int index = indexesArr.length;

        int tableTargetIndex = 0;
        do {
            if (index < indexesArr.length && indexesArr.length > 0) {
                if (indexesArr[index] == parentsOrigin[index]) {
                    // back variable level
                    indexesArr[index] = 0;
                    index--;
                } else {
                    // next variable level
                    indexesArr[index]++;
                    index++;
                }
            } else {
                int tableSourceIndex = 0;
                int a = 1;
                for (int i = 0; i < indexesArr.length; i++) {
                    int value = indexesArr[indexesArr.length - 1 - i];

                    tableSourceIndex += value * a;
                    a *= parentsOrigin[i];
                }

                System.out.println(tableSourceIndex + ":" + tableTargetIndex);

                for (int value = 0; value < this.variables[variableKey].getLength(); value++) {
                    cpt[tableTargetIndex + value] = Float.parseFloat(tableValues[tableSourceIndex + value]);
                }

                tableTargetIndex++;
                index--;
            }
        } while (index >= 0);

        System.out.println("Add cpt: " + tableValues.length + "-" + cpt.length);

        this.CPTs[variableKey] = cpt;
    }

    // getters
    private int getVariableKey(String variableName) {
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getName().equals(variableName)) {
                return i;
            }
        }

        return -1;
    }
}
