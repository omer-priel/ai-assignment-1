import java.util.*;
import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Saving Bayesian Network
 */
public class BNetwork {
    // static utils

    /**
     * load xml file to xml Document
     *
     * @param filepath xml filepath
     * @return xml Document
     */
    private static Document loadXMLFile(String filepath) throws ParserConfigurationException, IOException, SAXException {
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
    public VariableClass[] variableClasses = null;

    /**
     * array of the lengths (count of the values) of the variables.
     */
    public int[] variablesLengths = null;

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

    /**
     * Create Bayesian Network from xml file
     *
     * @param filepath path of the xml file
     */
    public BNetwork(String filepath) throws ParserConfigurationException, IOException, SAXException {

        // load the network from XML
        Document document= BNetwork.loadXMLFile(filepath);
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
        List<VariableClass> variableClasses = new LinkedList<>();

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

            VariableClass variableClass = new VariableClass(name, valuesAsArray);
            variableClasses.add(variableClass);
        }

        this.variableClasses = new VariableClass[variableClasses.size()];
        variableClasses.toArray(this.variableClasses);

        this.variablesLengths = new int[variableClasses.size()];

        for (int i = 0; i < this.variableClasses.length; i++) {
            this.variablesLengths[i] = this.variableClasses[i].getLength();
        }
    }

    /**
     * initialize the parents and the CPTs from the XML
     *
     * @param network network xml node
     */
    private void initCPTs(Element network) {
        int variablesLen = this.variableClasses.length;

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
        for (int i = 0; i < variableClasses.length; i++) {
            if (variableClasses[i].getName().equals(name)) {
                return i;
            }
        }

        return -1;
    }
}
