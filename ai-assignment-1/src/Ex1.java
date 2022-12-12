import java.io.*;
import java.util.*;

/**
 * Entry Point class of the application
 */
class Ex1 {

  // testing

  /**
   * Print the java version
   */
  public static void printJavaVersion() {
    // Print java version
    String version = System.getProperty("java.version");
    System.out.println("java version: " + version);
  }

  /**
   * set input from memory
   */
  private static void setTestsInput() {
    String input = "alarm_net.xml" +
            "\nP(B=T|J=T,M=T),1" +
            "\nP(B=T|J=T,M=T),2" +
            "\nP(B=T|J=T,M=T),3";

    input = "big_net.xml\n" +
            "P(B0=v3|C3=T,B2=F,C2=v3),1\n" +
            "P(B0=v3|C3=T,B2=F,C2=v3),2\n" +
            "P(B0=v3|C3=T,B2=F,C2=v3),3\n" +
            "P(A2=T|C2=v1),1\n" +
            "P(A2=T|C2=v1),2\n" +
            "P(A2=T|C2=v1),3\n" +
            "P(D1=T|C2=v1,C3=F),1\n" +
            "P(D1=T|C2=v1,C3=F),2\n" +
            "P(D1=T|C2=v1,C3=F),3\n" +
            "P(D1=T|C2=v1,C3=F),1\n" +
            "P(D1=T|C2=v1,C3=F),2\n" +
            "P(D1=T|C2=v1,C3=F),3";

    InputStream inputStream = new ByteArrayInputStream(input.getBytes());
    scanner = new Scanner(inputStream);
  }

  // production

  /**
   * load the input from file called input.txt
   * and change the output to file called output.txt
   *
   * @throws FileNotFoundException the file not found
   */
  private static void setProductionInputAndOutput() throws FileNotFoundException {
    scanner = new Scanner(new File("input.txt"));

    System.setOut(new PrintStream(new File("output.txt")));
  }

  // input scanner
  static private Scanner scanner = new Scanner(System.in);

  /**
   * Entry Point of the application
   *
   * @param args args
   */
  public static void main(String[] args) {
    // production
    try {
      setProductionInputAndOutput();
    } catch (FileNotFoundException ex) {
      System.out.println("Can't load the input file");
      return;
    }

    // development
    // printJavaVersion();
    // setTestsInput();

    // load the network
    BNetwork network;

    String networkPath = scanner.nextLine();
    try {
      network = new BNetwork(networkPath);
    } catch (Exception ex) {
      System.out.println("Can't load the " + networkPath + " file");
      return;
    }

    // the main loop
    while (scanner.hasNext()) {
      String queryInput = scanner.nextLine();
      Query query = new Query(network, queryInput);

      Algorithms.callQuery(query, network);
    }
  }
}