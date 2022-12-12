import java.util.List;

@FunctionalInterface
public interface HiddenChooser {
    /**
     * Choosing hidden variable for Variable Elimination in BNetwork
     *
     * @param hiddenVariables
     * @param factors
     * @return the index of the hidden variable to remove in hiddenVariables
     */
    int choose(List<Integer> hiddenVariables, List<Factor> factors);
}
