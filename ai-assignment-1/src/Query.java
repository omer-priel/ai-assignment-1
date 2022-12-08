public class Query {
    // Members
    public int queryVariable;
    public int queryValue;
    public int[] evidencesVariables;
    public int[] evidencesValues;

    public int type;

    public QueryResults results;

    public Query(BNetwork network, String input) {
        String[] parts = input.split("\\(")[1].split("\\),");

        String[] parametersPart = parts[0].split("\\|");
        String[] queryParts = parametersPart[0].split("\\=");

        this.queryVariable = network.getVariableKey(queryParts[0]);
        this.queryValue = network.getVariable(this.queryVariable).getValueKey(queryParts[1]);

        if (parametersPart.length == 1) {
            this.evidencesVariables = new int[0];
            this.evidencesValues = new int[0];
        } else {

            String[] evidencesParts = parametersPart[1].split("\\,");

            this.evidencesVariables = new int[evidencesParts.length];
            this.evidencesValues = new int[evidencesParts.length];

            for (int i = 0; i < evidencesParts.length; i++) {
                String[] evidenceParts = evidencesParts[i].split("\\=");

                this.evidencesVariables[i] = network.getVariableKey(evidenceParts[0]);
                this.evidencesValues[i] = network.getVariable(this.evidencesVariables[i]).getValueKey(evidenceParts[1]);
            }
        }

        this.type = Integer.parseInt(parts[1]);

        this.results = new QueryResults();
    }
}
