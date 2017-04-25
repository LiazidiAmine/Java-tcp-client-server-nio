package upem.jarret.main;
import java.io.IOException;

import upem.jarret.server.Server;

public class MainServer {

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

	}

}
