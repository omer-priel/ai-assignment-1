public class Query {
    // Members
    public int queryVarible;
    public int queryValue;
    public int[] evidencesVaribles;
    public int[] evidencesValues;

    public int type;

    public QueryResults results;

    public Query(BNetwork network, String input) {
        String[] parts = input.split("\\(")[1].split("\\),");

        String[] parametersPart = parts[0].split("\\|");
        String[] queryParts = parametersPart[0].split("\\=");
        String[] evidencesParts = parametersPart[1].split("\\,");

        this.queryVarible = network.getVariableKey(queryParts[0]);
        this.queryValue = network.getVariable(this.queryValue).getValueKey(queryParts[1]);

        this.evidencesVaribles = new int[evidencesParts.length];
        this.evidencesValues = new int[evidencesParts.length];

        for (int i = 0; i < evidencesParts.length; i++) {
            String[] evidenceParts = evidencesParts[i].split("\\=");

            this.evidencesVaribles[i] = network.getVariableKey(evidenceParts[0]);
            this.evidencesValues[i] = network.getVariable(this.evidencesVaribles[i]).getValueKey(evidenceParts[1]);
        }

        this.type = Integer.parseInt(parts[1]);

        this.results = new QueryResults();
    }
}
