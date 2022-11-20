import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

class Ex1 {

  static private Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
    String networkPath = scanner.nextLine();
    System.out.println(networkPath);

    BNetwork network = new BNetwork(networkPath);

    while (scanner.hasNext()) {
      String queryInput = scanner.nextLine();
      System.out.println(queryInput);

      Query query = new Query(queryInput);
    }
  }

  public static void printVersion() {
    // Print java version
    String version = System.getProperty("java.version");
    System.out.println("java version: " + version);
  }
}