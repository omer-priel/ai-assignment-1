import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class Ex1 {

  // testing
  public static void printJavaVersion() {
    // Print java version
    String version = System.getProperty("java.version");
    System.out.println("java version: " + version);
  }

  private static void setTestsInputs() {
    String input = "alarm_net.xml" +
            "\nP(B=T|J=T,M=T),1" +
            "\nP(B=T|J=T,M=T),2" +
            "\nP(B=T|J=T,M=T),3";

    input = "big_net.xml\n" +
            "P(B0=v3|C3=T,B2=F,C2=v3),1\n" +
            "P(B0=v3|C3=T,B2=F,C2=v3),2\n" +
            "P(A2=T|C2=v1),1\n" +
            "P(A2=T|C2=v1),2\n" +
            "P(D1=T|C2=v1,C3=F),1\n" +
            "P(D1=T|C2=v1,C3=F),2\n" +
            "P(D1=T|C2=v1,C3=F),1\n" +
            "P(D1=T|C2=v1,C3=F),2";

    InputStream inputStream = new ByteArrayInputStream(input.getBytes());
    scanner = new Scanner(inputStream);
  }

  // input scanner
  static private Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) {
    //setTestsInputs();

    // load the network
    BNetwork network = null;

    String networkPath = scanner.nextLine();
    try {
      network = new BNetwork(networkPath);
    } catch (Exception ex) {
      System.out.println("Can't load the " + networkPath + " file");
    }

    if (network == null) {
      return;
    }

    // main loop
    while (scanner.hasNext()) {
      String queryInput = scanner.nextLine();
      Query query = new Query(network, queryInput);

      network.callQuery(query);
    }
  }
}