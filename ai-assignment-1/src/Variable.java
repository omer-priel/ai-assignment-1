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

    public int getValueKey(String value) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(value)) {
                return i;
            }
        }

        return -1;
    }

    public int getLength() {
        return this.values.length;
    }
}
