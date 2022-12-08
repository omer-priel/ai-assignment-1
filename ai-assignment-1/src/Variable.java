public class Variable {
    // Members
    private String name;
    private String[] values;

    public Variable(String name, String[] values) {
        this.name = name;
        this.values = values;
    }

    // getters
    public String getName() {
        return this.name;
    }

    /**
     * get value key by name
     *
     * @param name the name of the value
     * @return
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
