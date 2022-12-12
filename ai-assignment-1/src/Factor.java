/**
 * Factor of Variable Elimination
 */
public class Factor {
    /**
     * variables of the factor
     */
    public int[] variables;

    /**
     * probabilities of the factor
     */
    public double[] probabilities;

    public Factor(int[] variables, double[] probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters

    /**
     * check if variable exists in the factor
     *
     * @param variable the variable
     * @return True if the variable in the factor, False elsewhere
     */
    public boolean variableExists(int variable) {
        for (int j : this.variables) {
            if (j == variable) {
                return true;
            }
        }
        return false;
    }
}
