public class Factor {
    public int[] variables;
    public double[] probabilities;

    public Factor(int[] variables, double[] probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters
    public boolean variableExists(int variable) {
        for (int j : this.variables) {
            if (j == variable) {
                return true;
            }
        }
        return false;
    }
}
