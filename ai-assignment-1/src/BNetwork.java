import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
    int[][] CPTs = null;
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
            Element variableElement = (Element) variablesElements.item(0);
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
        this.CPTs = new int[variablesLen][];

        for (int i = 0; i < variablesLen; i++) {
            for (int j = 0; j < variablesLen; j++) {
                this.relations[i][j] = false;
            }
        }

        // load relations and CPTs from definitions
        NodeList definitionsElements = network.getElementsByTagName("DEFINITION");

        for (int i = 0; i < definitionsElements.getLength(); i++) {
            Element variableElement = (Element) definitionsElements.item(0);
            String name = variableElement.getElementsByTagName("FOR").item(0).getTextContent();
            NodeList givens = variableElement.getElementsByTagName("GIVEN");
            String[] tableValues = variableElement.getElementsByTagName("TABLE").item(0).getNodeValue().split("\\s+");

            int variableKey = this.getVariableKey(name);
            List<Integer> givenKeys = new LinkedList<>();

            for (int j = 0; j < givens.getLength(); j++) {
                String givenName = givens.item(j).getTextContent();
                int givenKey = this.getVariableKey(givenName);

                // not itself
                if (variableKey != givenKey) {
                    givenKeys.add(givenKey);
                    this.relations[variableKey][givenKey] = true;
                }
            }

            // load CPT
            int cptLength = this.variables[variableKey].getLength();
            for (Integer givenKey: givenKeys) {
                cptLength *= this.variables[givenKey].getLength();
            }

            this.CPTs[variableKey] = new int[cptLength];

            for (int j = 0; j < tableValues.length; j++) {
                // TODO:
            }
        }
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
