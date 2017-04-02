import java.io.IOException;
import java.net.InetSocketAddress;

import com.fasterxml.jackson.databind.JsonNode;

import client.HTTPClient;

public class Main {

	public static void main(String[] args) throws IOException {
		Thread threadServer = new Thread(()->{
			try {
				FakeServerTest server = new FakeServerTest(7777);
				server.launch();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		threadServer.start();
		
		InetSocketAddress inetAdd = new InetSocketAddress("localhost",7777);
		HTTPClient client = new HTTPClient("localhost", 7777);
		JsonNode c = client.sendTaskRequest("/", inetAdd);
		System.out.println(c.asText());
		System.out.println("********************************************\n");
		System.out.println(client.sendAnswerTask(inetAdd, c));
		try {
			client.runWorker();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
