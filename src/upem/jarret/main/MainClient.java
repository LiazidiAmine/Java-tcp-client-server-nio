package upem.jarret.main;
import upem.jarret.client.HTTPClient;

public class MainClient {

	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080, "Amine");
		//HTTPClient client = new HTTPClient("localhost", 3000, "Amine");
		client.run();		
	}

}
