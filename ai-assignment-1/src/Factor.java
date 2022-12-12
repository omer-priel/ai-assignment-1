import java.util.Arrays;
public class Factor {
    public int[] variables;
    public double[] probabilities;

    public Factor(int[] variables, double[] probabilities) {
        this.variables = variables;
        this.probabilities = probabilities;
    }

    // getters
    public boolean variableExists(int variable) {
        for (int i = 0; i < this.variables.length; i++) {
            if (this.variables[i] == variable) {
                return true;
            }
        }
        return false;
    }
}
