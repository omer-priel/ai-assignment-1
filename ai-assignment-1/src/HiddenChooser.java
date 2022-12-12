import java.util.List;

/**
 * Hidden Chooser for Variable Elimination
 */
@FunctionalInterface
public interface HiddenChooser {
    /**
     * Choosing hidden variable for Variable Elimination in BNetwork
     *
     * @param hiddenVariables the hidden variables
     * @param factors the factors
     * @return the index of the hidden variable to remove in hiddenVariables
     */
    int choose(List<Integer> hiddenVariables, List<Factor> factors);
}
