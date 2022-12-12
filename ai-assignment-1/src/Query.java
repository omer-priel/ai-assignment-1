/**
 * A query to find probability in Bayesian Network
 */
public class Query {
    // Members
    /**
     * the query variable
     */
    public int queryVariable;

    /**
     * the value of the query variable
     */
    public int queryValue;

    /**
     * evidences variables
     */
    public int[] evidencesVariables;

    /**
     * evidences values
     */
    public int[] evidencesValues;

    /**
     * the algorithm type to solve the query
     */
    public int type;

    /**
     * results of the query
     */
    public QueryResults results;

    /**
     * convert from string
     *
     * @param network the network
     * @param input query, example: P(B=T|J=T,M=T),1
     */
    public Query(BNetwork network, String input) {
        String[] parts = input.split("\\(")[1].split("\\),");

        String[] parametersPart = parts[0].split("\\|");
        String[] queryParts = parametersPart[0].split("=");

        this.queryVariable = network.getVariableKey(queryParts[0]);
        this.queryValue = network.variableClasses[this.queryVariable].getValueKey(queryParts[1]);

        if (parametersPart.length == 1) {
            this.evidencesVariables = new int[0];
            this.evidencesValues = new int[0];
        } else {

            String[] evidencesParts = parametersPart[1].split(",");

            this.evidencesVariables = new int[evidencesParts.length];
            this.evidencesValues = new int[evidencesParts.length];

            for (int i = 0; i < evidencesParts.length; i++) {
                String[] evidenceParts = evidencesParts[i].split("=");

                this.evidencesVariables[i] = network.getVariableKey(evidenceParts[0]);
                this.evidencesValues[i] = network.variableClasses[this.evidencesVariables[i]].getValueKey(evidenceParts[1]);
            }
        }

        this.type = Integer.parseInt(parts[1]);

        this.results = new QueryResults();
    }
}
