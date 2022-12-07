import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class Ex1 {

  static private Scanner scanner = new Scanner(System.in);

  // testing
  public static void printJavaVersion() {
    // Print java version
    String version = System.getProperty("java.version");
    System.out.println("java version: " + version);
  }

  private static void setTestsInputs() {
    String input = "alarm_net.xml\n" +
            "P(B=T|J=T,M=T),1\n" +
            "P(B=T|J=T,M=T),2";
    InputStream inputStream = new ByteArrayInputStream(input.getBytes());
    scanner = new Scanner(inputStream);
  }

  public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
    setTestsInputs();

    String networkPath = scanner.nextLine();
    System.out.println(networkPath);

    BNetwork network = new BNetwork(networkPath);

    while (scanner.hasNext()) {
      String queryInput = scanner.nextLine();
      System.out.println(queryInput);

      Query query = new Query(network, queryInput);

      network.callQuery(query);
    }
  }
}