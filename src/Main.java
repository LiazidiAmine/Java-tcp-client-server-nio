import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import client.HTTPClient;

public class Main {

	public static void main(String[] args) throws IOException {
		
		HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080);
		Optional<String> json = client.sendTaskRequest();
		if(json.isPresent()){
			System.out.println(json.get()+"*********************");
			System.out.println(client.sendAnswerTask(json.get())+"----------------");
		}

	}

}
