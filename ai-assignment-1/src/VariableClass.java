/**
 * Saving Variable Class info
 * Using for converting strings to indexes
 */
public class VariableClass {
    // Members
    /**
     * the variable name
     */
    private final String name;

    /**
     * the values (outcomes) of the variable
     */
    private final String[] values;

    public VariableClass(String name, String[] values) {
        this.name = name;
        this.values = values;
    }

    // getters

    /**
     * return factor name
     *
     * @return factor name
     */
    public String getName() {
        return this.name;
    }

    /**
     * get value key by name
     *
     * @param name the name of the value
     * @return the index of the value in values
     */
    public int getValueKey(String name) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * get length of the variable values
     *
     * @return length of the variable values
     */
    public int getLength() {
        return this.values.length;
    }
}
