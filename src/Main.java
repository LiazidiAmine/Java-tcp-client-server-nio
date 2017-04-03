import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import client.HTTPClient;

public class Main {

	public static void main(String[] args) throws IOException, IllegalArgumentException, IllegalAccessException {
		/*Thread threadServer = new Thread(()->{
			try {
				FakeServerTest server = new FakeServerTest(7777);
				server.launch();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		threadServer.start();*/
		
		InetSocketAddress inetAdd = new InetSocketAddress("ns3001004.ip-5-196-73.eu",8080);
		HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080);
		client.sendTaskRequest(inetAdd).get().toJson();
		
	}

}
