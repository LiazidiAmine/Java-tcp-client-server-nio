package upem.jarret.test;
import java.io.IOException;

import upem.jarret.client.HTTPClient;
import upem.jarret.server.Server;

public class Main {

	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
		
		/*Thread server = new Thread(()->{
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
		server.start();*/
		
		
		HTTPClient client = new HTTPClient("ns3001004.ip-5-196-73.eu", 8080, "Amine");
		//HTTPClient client = new HTTPClient("localhost", 3000, "Amine");
		/*int i = 0;
		while(true){
			i++;
			System.err.println("CLIENT "+i+" \n");
			client.run();
			System.err.println("---------------------------------------------");
		}*/
		while(!Thread.interrupted()){
			client.run();
		}
		
	}

}
