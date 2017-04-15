import java.io.IOException;

import client.HTTPClient;
import server.Server;

public class Main {

	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		
		Thread t = new Thread(()->{
			try {
				new Server(3000).launch();
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		t.start();
		//HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080, "Amine");
		/*HTTPClient client = new HTTPClient("localhost", 3000, "Amine");
		client.run();*/
		
		

		

	}

}
