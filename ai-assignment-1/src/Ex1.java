class Ex1 {

  public static void main(String[] args) {
    printVersion();
  }

  public static void printVersion() {
    // Print java version
    String version = System.getProperty("java.version");
    System.out.println("java version: " + version);
  }
}