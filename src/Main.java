import java.io.IOException;
import client.HTTPClient;

public class Main {

	public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		
		HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080, "Amine");
		client.run();

	}

}
