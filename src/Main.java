import java.io.IOException;

import client.HTTPClient;

public class Main {

	public static void main(String[] args) throws IOException {
		FakeServerTest server = new FakeServerTest(7777);
		server.launch();
		
	}

}
